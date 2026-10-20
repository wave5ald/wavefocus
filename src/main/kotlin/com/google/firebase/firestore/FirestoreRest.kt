package com.google.firebase.firestore

import com.example.desktop.platform.CloudConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Firestore REST transport. The desktop build talks to the very same Firestore database as the
 * Android app (same project, same vault documents) without the Android-only Firebase SDK.
 *
 * The project's security rules allow per-document get/create/update/delete but not collection
 * listing, so collection reads transparently fall back to an id index maintained by this client
 * (`sync_vaults/<KEY>/_index/state`).
 */
internal object FirestoreRest {

    private const val BASE = "https://firestore.googleapis.com/v1"

    /** Highest sequential `task_N` / `script_N` id probed when the vault index is unfamiliar. */
    private const val MAX_SEQUENTIAL_IDS = 200

    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun resourcePath(path: String): String =
        "projects/${CloudConfig.PROJECT_ID}/databases/${CloudConfig.FIRESTORE_DATABASE_ID}/documents/" + encodePath(path)

    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun urlFor(path: String): String = "$BASE/${resourcePath(path)}?key=${CloudConfig.API_KEY}"

    // ------------------------------------------------------------------ list

    fun list(collectionPath: String): List<DocumentSnapshot> {
        return try {
            rawList(collectionPath)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == 403 || e.code == 404) indexedList(collectionPath) else throw e
        }
    }

    private fun rawList(collectionPath: String): List<DocumentSnapshot> {
        val result = mutableListOf<DocumentSnapshot>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$BASE/")
                append("projects/${CloudConfig.PROJECT_ID}/databases/${CloudConfig.FIRESTORE_DATABASE_ID}/documents/")
                append(encodePath(collectionPath))
                append("?key=${CloudConfig.API_KEY}")
                append("&pageSize=300")
                if (pageToken != null) append("&pageToken=${URLEncoder.encode(pageToken, "UTF-8")}")
            }
            val body = execute(Request.Builder().url(url).get().build())
            if (body.isBlank()) break
            val root = JSONObject(body)
            val docs = root.optJSONArray("documents")
            if (docs != null) {
                for (i in 0 until docs.length()) {
                    val obj = docs.getJSONObject(i)
                    val name = obj.getString("name")
                    result.add(DocumentSnapshot(name.substringAfterLast('/'), decodeFields(obj.optJSONObject("fields"))))
                }
            }
            pageToken = root.optString("nextPageToken", "").ifBlank { null }
        } while (pageToken != null)
        return result
    }

    /** Reads ids from the per-vault index document when collection listing is not permitted. */
    private fun indexedList(collectionPath: String): List<DocumentSnapshot> {
        val parts = collectionPath.split('/')
        if (parts.size != 3 || parts[0] != "sync_vaults") return emptyList()
        val vaultKey = parts[1]
        val collection = parts[2]

        val found = LinkedHashMap<String, DocumentSnapshot>()

        for (doc in listedFromIndex(vaultKey, collection, collectionPath)) {
            found[doc.id] = doc
        }
        for (doc in probedByConvention(collectionPath, collection)) {
            found.putIfAbsent(doc.id, doc)
        }

        return found.values.toList()
    }

    private fun listedFromIndex(
        vaultKey: String,
        collection: String,
        collectionPath: String
    ): List<DocumentSnapshot> {
        val indexData = try {
            readDocument("sync_vaults/$vaultKey/_index/state")
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        val idsMap = indexData["ids"] as? Map<*, *> ?: return emptyList()
        val collectionIds = idsMap[collection] as? Map<*, *> ?: return emptyList()
        val ids = collectionIds.entries.filter { it.value == true }.map { it.key.toString() }
        if (ids.isEmpty()) return emptyList()
        return batchGet(ids.map { "$collectionPath/$it" })
    }

    /**
     * Documents written by the Android app are not registered in the index (that client does not
     * know about it), so the ids used by this app (`task_1`, `script_1`, ...) are probed directly.
     * `:batchGet` treats absent documents as "missing" entries, so this costs one request per
     * 100 ids and discovers vault contents created on another device.
     */
    private fun probedByConvention(collectionPath: String, collection: String): List<DocumentSnapshot> {
        val prefix = when (collection) {
            "tasks" -> "task_"
            "scripts" -> "script_"
            else -> return emptyList()
        }
        return try {
            (1..MAX_SEQUENTIAL_IDS)
                .map { "$collectionPath/$prefix$it" }
                .chunked(100)
                .flatMap { batchGet(it, fallbackToIndividual = false) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ------------------------------------------------------------------ get

    fun readDocument(path: String): Map<String, Any?>? {
        return try {
            val body = execute(Request.Builder().url(urlFor(path)).get().build())
            if (body.isBlank()) null
            else decodeFields(JSONObject(body).optJSONObject("fields"))
        } catch (e: FirebaseFirestoreException) {
            if (e.code == 404) null else throw e
        }
    }

    fun batchGet(paths: List<String>, fallbackToIndividual: Boolean = true): List<DocumentSnapshot> {
        if (paths.isEmpty()) return emptyList()
        return try {
            val names = paths.map { resourcePath(it) }
            val payload = JSONObject().put("documents", JSONArray(names)).toString()
            val url = "$BASE/projects/${CloudConfig.PROJECT_ID}/databases/${CloudConfig.FIRESTORE_DATABASE_ID}/documents:batchGet?key=${CloudConfig.API_KEY}"
            val body = execute(Request.Builder().url(url).post(payload.toRequestBody(jsonMedia)).build())
            val arr = JSONArray(body)
            val out = mutableListOf<DocumentSnapshot>()
            for (i in 0 until arr.length()) {
                val entry = arr.getJSONObject(i)
                val found = entry.optJSONObject("found") ?: continue
                val name = found.getString("name")
                out.add(
                    DocumentSnapshot(
                        name.substringAfterLast('/'),
                        decodeFields(found.optJSONObject("fields"))
                    )
                )
            }
            out
        } catch (e: Exception) {
            if (!fallbackToIndividual) return emptyList()
            // Last resort: fetch each document individually.
            paths.mapNotNull { path ->
                val data = try {
                    readDocument(path)
                } catch (_: Exception) {
                    null
                }
                if (data == null) null else DocumentSnapshot(path.substringAfterLast('/'), data)
            }
        }
    }

    // ------------------------------------------------------------------ write

    fun patch(path: String, data: Map<String, Any?>, merge: Boolean) {
        val fields = JSONObject()
        for ((k, v) in data) fields.put(k, encodeValue(v))
        // The Firestore REST document resource wraps every field in a "fields" object.
        val body = JSONObject().put("fields", fields).toString()

        if (merge && data.isNotEmpty()) {
            val mask = data.keys.joinToString("&") { "updateMask.fieldPaths=$it" }
            val url = "${urlFor(path)}&$mask"
            execute(Request.Builder().url(url).patch(body.toRequestBody(jsonMedia)).build())
        } else {
            execute(Request.Builder().url(urlFor(path)).patch(body.toRequestBody(jsonMedia)).build())
        }

        registerInIndex(path)
    }

    fun delete(path: String) {
        execute(Request.Builder().url(urlFor(path)).delete().build())
    }

    /**
     * Adds the document id to `sync_vaults/<KEY>/_index/state` using a field-path mask so no
     * read-modify-write race is possible.
     */
    private fun registerInIndex(path: String) {
        val parts = path.split('/')
        if (parts.size != 4 || parts[0] != "sync_vaults") return
        val vaultKey = parts[1]
        val collection = parts[2]
        val docId = parts[3]
        if (docId == "_index" || docId.contains('.')) return

        val indexPath = "sync_vaults/$vaultKey/_index/state"
        val fieldPath = "ids.$collection.$docId"
        val payload = JSONObject()
            .put(
                "fields",
                JSONObject().put(
                    "ids",
                    JSONObject().put(
                        "mapValue",
                        JSONObject().put(
                            "fields",
                            JSONObject().put(
                                collection,
                                JSONObject().put(
                                    "mapValue",
                                    JSONObject().put(
                                        "fields",
                                        JSONObject().put(
                                            docId,
                                            JSONObject().put("booleanValue", true)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        try {
            val url = "${urlFor(indexPath)}&updateMask.fieldPaths=$fieldPath"
            execute(Request.Builder().url(url).patch(payload.toString().toRequestBody(jsonMedia)).build())
        } catch (_: Exception) {
            // Index maintenance is best-effort; the sync itself still succeeded.
        }
    }

    // ------------------------------------------------------------------ http

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = try {
                    JSONObject(body).getJSONObject("error").optString("message", body)
                } catch (_: Exception) {
                    body
                }
                throw FirebaseFirestoreException(
                    message.ifBlank { "HTTP ${response.code}" },
                    response.code
                )
            }
            return body
        }
    }

    // ------------------------------------------------------------- (de)coding

    private fun encodeValue(value: Any?): JSONObject = when (value) {
        null -> JSONObject().put("nullValue", JSONObject.NULL)
        is String -> JSONObject().put("stringValue", value)
        is Boolean -> JSONObject().put("booleanValue", value)
        is Int, is Long, is Short, is Byte -> JSONObject().put("integerValue", value.toString())
        is Float, is Double -> JSONObject().put("doubleValue", (value as Number).toDouble())
        is Map<*, *> -> {
            val fields = JSONObject()
            for ((k, v) in value) fields.put(k.toString(), encodeValue(v))
            JSONObject().put("mapValue", JSONObject().put("fields", fields))
        }
        is Iterable<*> -> {
            val values = JSONArray()
            for (v in value) values.put(encodeValue(v))
            JSONObject().put("arrayValue", JSONObject().put("values", values))
        }
        else -> JSONObject().put("stringValue", value.toString())
    }

    fun decodeFields(fields: JSONObject?): Map<String, Any?>? {
        if (fields == null) return null
        val map = LinkedHashMap<String, Any?>()
        val keys = fields.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = decodeValue(fields.optJSONObject(key) ?: continue)
        }
        return map
    }

    private fun decodeValue(obj: JSONObject): Any? = when {
        obj.has("nullValue") -> null
        obj.has("stringValue") -> obj.getString("stringValue")
        obj.has("booleanValue") -> obj.getBoolean("booleanValue")
        obj.has("integerValue") -> obj.getString("integerValue").toLongOrNull() ?: 0L
        obj.has("doubleValue") -> obj.getDouble("doubleValue")
        obj.has("timestampValue") -> obj.getString("timestampValue")
        obj.has("mapValue") -> decodeFields(obj.getJSONObject("mapValue").optJSONObject("fields"))
        obj.has("arrayValue") -> {
            val arr = obj.getJSONObject("arrayValue").optJSONArray("values")
            val list = mutableListOf<Any?>()
            if (arr != null) for (i in 0 until arr.length()) list.add(decodeValue(arr.getJSONObject(i)))
            list
        }
        else -> null
    }
}
