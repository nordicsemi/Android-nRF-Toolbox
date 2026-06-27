plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/KotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin)
}

android {
    namespace = "no.nordicsemi.android.toolbox.profile.data"
}

dependencies {
    implementation(project(":profile_parsers"))
    implementation(project(":lib_utils"))
}