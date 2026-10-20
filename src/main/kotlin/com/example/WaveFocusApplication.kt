package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseManager

class WaveFocusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application entry point initialized - setting up Firebase connection")
        // Initialize Firebase connection (FirebaseApp, Firestore, Auth) at application start
        FirebaseManager.initialize(this)
    }

    companion object {
        private const val TAG = "WaveFocusApplication"
    }
}
