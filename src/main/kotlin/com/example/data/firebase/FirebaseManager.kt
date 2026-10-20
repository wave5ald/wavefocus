package com.example.data.firebase

import android.content.Context
import android.util.Log

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    fun initialize(context: Context) {
        Log.d(TAG, "Local-first mode active. Firestore disabled.")
    }
}
