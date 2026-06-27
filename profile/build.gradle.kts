plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.android.library)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/KotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/HiltComposeConventionPlugin.kt
    alias(libs.plugins.nordic.feature.hilt.compose)
}

android {
    namespace = "no.nordicsemi.android.toolbox.profile"
}

dependencies {
    implementation(project(":lib_analytics"))
    implementation(project(":profile_data"))
    implementation(project(":lib_ui"))
    implementation(project(":lib_utils"))
    implementation(project(":profile_parsers"))
    api(project(":lib_service"))
    implementation(project(":profile_manager"))
    implementation(project(":lib_storage"))

    implementation(nordic.core)
    implementation(nordic.navigation)
    implementation(nordic.ui)
    implementation(nordic.theme)
    implementation(nordic.permissions.ble)
    implementation(nordic.permissions.notification)
    implementation(nordic.logger)
    implementation(nordic.log.timber)
    implementation(nordic.blek.client.core.android)

    implementation(libs.chart)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.compose.material.icons.extended)

    // DataStore
    implementation(libs.androidx.dataStore.core)
    implementation(libs.androidx.dataStore.preferences)

    // coroutine core
    implementation(libs.kotlinx.coroutines.core)

    // Simple XML
    implementation("org.simpleframework:simple-xml:2.7.1") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
    }
}