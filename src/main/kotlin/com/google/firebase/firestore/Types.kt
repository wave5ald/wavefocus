package com.google.firebase.firestore

class SetOptions internal constructor(internal val merge: Boolean) {
    companion object {
        fun merge(): SetOptions = SetOptions(true)
        fun overwrite(): SetOptions = SetOptions(false)
    }
}

class FirebaseFirestoreException(message: String, val code: Int = 0) : RuntimeException(message)

class DocumentSnapshot(
    val id: String,
    val data: Map<String, Any?>?
)

class QuerySnapshot(
    val documents: List<DocumentSnapshot>
) {
    fun isEmpty(): Boolean = documents.isEmpty()
    fun size(): Int = documents.size
}
