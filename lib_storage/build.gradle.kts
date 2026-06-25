plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/KotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/HiltConventionPlugin.kt
    alias(libs.plugins.nordic.feature.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "no.nordicsemi.android.toolbox.lib.storage"
}

dependencies {
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}