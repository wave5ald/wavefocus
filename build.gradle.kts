import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.0"
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
}

group = "com.wavefocus"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Lifecycle / ViewModel (multiplatform, JVM desktop)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    // Room (multiplatform, JVM desktop)
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.sqlite:sqlite-bundled:2.6.1")
    ksp("androidx.room:room-compiler:2.8.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20250517")
}

compose.desktop {
    application {
        mainClass = "com.example.MainActivityKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "WaveFocus"
            packageVersion = "1.0.0"
            description = "Wave Focus - Daily routine tracker, focus timer and cloud sync"
            vendor = "Wave Focus"
            copyright = "© Wave Focus"
            windows {
                iconFile.set(project.file("src/main/resources/wave_focus.ico"))
                menuGroup = "Wave Focus"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "0d3f0f0c-8c4c-4b62-9c0b-7a1f57c9d001"
            }
        }
    }
}
