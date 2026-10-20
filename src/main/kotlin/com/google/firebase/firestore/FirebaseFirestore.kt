package com.google.firebase.firestore

/**
 * Firestore facade with the small API surface used by CloudSyncManager.
 * Backed by the Firestore REST API so the desktop app shares the Android app's cloud vaults.
 */
class FirebaseFirestore private constructor() {

    fun collection(path: String): CollectionReference = CollectionReference(path)

    companion object {
        @Volatile
        private var instance: FirebaseFirestore? = null

        fun getInstance(): FirebaseFirestore =
            instance ?: synchronized(this) { instance ?: FirebaseFirestore().also { instance = it } }

        fun getInstance(databaseId: String): FirebaseFirestore = getInstance()
    }
}

class CollectionReference internal constructor(internal val path: String) {

    fun document(documentId: String): DocumentReference = DocumentReference("$path/$documentId")

    fun get(): Task<QuerySnapshot> = try {
        Task.success(QuerySnapshot(FirestoreRest.list(path)))
    } catch (e: Exception) {
        Task.failure(e)
    }

    fun add(data: Map<String, Any?>): Task<DocumentReference> = try {
        val id = java.util.UUID.randomUUID().toString()
        FirestoreRest.patch("$path/$id", data, merge = false)
        Task.success(document(id))
    } catch (e: Exception) {
        Task.failure(e)
    }
}

class DocumentReference internal constructor(internal val path: String) {

    val id: String get() = path.substringAfterLast('/')

    fun collection(name: String): CollectionReference = CollectionReference("$path/$name")

    fun get(): Task<DocumentSnapshot> = try {
        val data = FirestoreRest.readDocument(path)
        if (data == null) Task.failure(FirebaseFirestoreException("Document $path not found", 404))
        else Task.success(DocumentSnapshot(id, data))
    } catch (e: Exception) {
        Task.failure(e)
    }

    fun set(data: Map<String, Any?>, options: SetOptions? = null): Task<Unit> = try {
        FirestoreRest.patch(path, data, options?.merge ?: false)
        Task.success(Unit)
    } catch (e: Exception) {
        Task.failure(e)
    }

    fun update(data: Map<String, Any?>): Task<Unit> = set(data, SetOptions.merge())

    fun delete(): Task<Unit> = try {
        FirestoreRest.delete(path)
        Task.success(Unit)
    } catch (e: Exception) {
        Task.failure(e)
    }
}
