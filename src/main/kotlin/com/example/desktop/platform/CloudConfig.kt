package com.example.desktop.platform

/**
 * Firebase / Firestore configuration copied from the Android app's google-services.json
 * and firebase_applet_config.xml so the desktop build talks to the exact same cloud project,
 * database and multi-user sync vaults.
 */
object CloudConfig {
    const val PROJECT_ID = "gen-lang-client-0779835305"
    const val API_KEY = "AIzaSyCJoVa0p2N0_94E-CeOSNwEMsrrQ0DQmHA"
    const val FIRESTORE_DATABASE_ID = "ai-studio-android-dailyrou-01bd200a-f072-4f9e-9ea2-3f135237618f"
    const val DEVICE_PLATFORM = "Windows"
}
