package com.google.firebase.firestore

/** Minimal Task type matching the Firebase listener API used by CloudSyncManager. */
class Task<T> private constructor(private val result: T?, private val error: Exception?) {

    fun addOnSuccessListener(listener: (T) -> Unit): Task<T> {
        val value = result
        if (error == null && value != null) listener(value)
        return this
    }

    fun addOnFailureListener(listener: (Exception) -> Unit): Task<T> {
        error?.let(listener)
        return this
    }

    fun addOnCanceledListener(listener: () -> Unit): Task<T> = this

    companion object {
        fun <T> success(value: T): Task<T> = Task(value, null)
        fun <T> failure(error: Exception): Task<T> = Task(null, error)
    }
}
