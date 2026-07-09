/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

plugins {
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidApplicationConventionPlugin.kt
    alias(libs.plugins.nordic.android.application)
    // https://github.com/nordicsemi/Nordic-Gradle-Plugins/blob/main/plugins/src/main/kotlin/HiltComposeConventionPlugin.kt
    alias(libs.plugins.nordic.feature.hilt.compose)
    alias(libs.plugins.kotlin.parcelize)
}
if (getGradle().startParameter.taskRequests.toString().contains("Release")) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "no.nordicsemi.android.nrftoolbox"
}

dependencies {
    implementation(project(":lib_ui"))
    implementation(project(":lib_utils"))
    implementation(project(":lib_analytics"))
    implementation(project(":profile"))
    implementation(project(":profile_data"))
    implementation(project(":profile_parsers"))
    implementation(project(":profile_manager"))

    implementation(nordic.navigation)
    implementation(nordic.theme)
    implementation(nordic.logger)
    implementation(nordic.analytics)
    implementation(nordic.ui)
    implementation(nordic.core)
    implementation(nordic.scanner.ble)
    implementation(nordic.blek.client.android)
    implementation(nordic.kotlin.log.timber)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)

    // Temporary fix:
    // After updating Kotlin to 2.4.0 there's no Hilt (Dagger) version yet updated.
    // Build fails with error:
    // [Hilt] Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0.
    //        To support newer versions, update the kotlin-metadata-jvm library.
    ksp(libs.kotlin.metadata.jvm)
}
