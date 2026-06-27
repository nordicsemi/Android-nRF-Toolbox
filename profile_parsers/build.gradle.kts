plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/KotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin)
}

android {
    namespace = "no.nordicsemi.android.toolbox.profile.parser"
}

dependencies {
    implementation(nordic.kotlin.data)
    api(libs.androidx.annotation)

    // Unit test dependencies
    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.junit)
}