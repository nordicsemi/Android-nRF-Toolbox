plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/KotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin)
}

android {
    namespace = "no.nordicsemi.android.toolbox.profile.manager"
}

dependencies {
    implementation(project(":lib_utils"))
    implementation(project(":profile_data"))
    implementation(project(":profile_parsers"))

    implementation(libs.timber)
    implementation(nordic.log)
    implementation(nordic.blek.client.core.android)

}