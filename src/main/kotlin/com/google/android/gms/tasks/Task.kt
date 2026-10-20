package com.google.android.gms.tasks

/**
 * The desktop build replaces the Play Services Tasks library with the small
 * REST-backed implementation in [com.google.firebase.firestore.Task].
 * CloudSyncManager imports this package name, so alias it here.
 */
typealias Task<T> = com.google.firebase.firestore.Task<T>
