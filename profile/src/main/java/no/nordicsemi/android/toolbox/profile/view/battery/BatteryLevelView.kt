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

package no.nordicsemi.android.toolbox.profile.view.battery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.Battery1Bar
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.Battery3Bar
import androidx.compose.material.icons.outlined.Battery4Bar
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material.icons.outlined.Battery6Bar
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.theme.nordicGreen
import no.nordicsemi.android.common.theme.nordicSun
import no.nordicsemi.android.common.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.manager.BatteryManager
import no.nordicsemi.android.toolbox.profile.viewmodel.BatteryViewModel
import no.nordicsemi.android.toolbox.profile.R
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun BatteryScreen(manager: BatteryManager) {
    val batteryViewModel = hiltViewModel<BatteryViewModel, BatteryViewModel.Factory>(
        key = manager.instanceId,
        creationCallback = { factory -> factory.create(manager) }
    )
    val batteryServiceData by batteryViewModel.state.collectAsStateWithLifecycle()

    BatteryView(batteryServiceData.batteryLevel)
}

@Composable
private fun BatteryView(batteryLevel: Int?) {
    ScreenSection {
        SectionTitle(
            icon = Icons.Default.BatteryChargingFull,
            title = stringResource(id = R.string.battery_label),
            menu = {
                batteryLevel?.let { batteryLevel ->
                    Text(text = stringResource(R.string.battery_value, batteryLevel))
                    DynamicBatteryStatus(batteryLevel)
                }
            }
        )
    }
}

@Composable
private fun DynamicBatteryStatus(batteryLevel: Int) {
    val (batteryIcon: ImageVector, color: Color) = when {
        // Full Battery
        batteryLevel > 95 -> Icons.Outlined.BatteryFull to nordicGreen
        batteryLevel > 80 -> Icons.Outlined.Battery6Bar to nordicGreen
        batteryLevel > 70 -> Icons.Outlined.Battery5Bar to nordicGreen
        // Moderate Battery
        batteryLevel > 55 -> Icons.Outlined.Battery4Bar to nordicSun
        batteryLevel > 40 -> Icons.Outlined.Battery3Bar to nordicSun
        batteryLevel > 25 -> Icons.Outlined.Battery2Bar to nordicSun
        // Low Battery
        batteryLevel > 10 -> Icons.Outlined.Battery1Bar to MaterialTheme.colorScheme.error
        batteryLevel > 5 -> Icons.Outlined.Battery0Bar to MaterialTheme.colorScheme.error
        // Critically Low Battery
        else -> Icons.Outlined.BatteryAlert to MaterialTheme.colorScheme.error
    }

    Icon(
        imageVector = batteryIcon,
        contentDescription = "Battery icon",
        tint = color,
    )
}

@Preview
@Composable
private fun BatteryPreview() {
    BatteryView(100)
}

@Preview
@Composable
private fun BatteryPreview_50() {
    BatteryView(50)
}

@Preview
@Composable
private fun BatteryPreview_20() {
    BatteryView(20)
}

@Preview
@Composable
private fun BatteryPreview_0() {
    BatteryView(0)
}

@Preview
@Composable
private fun BatteryPreview_unknown() {
    BatteryView(null)
}