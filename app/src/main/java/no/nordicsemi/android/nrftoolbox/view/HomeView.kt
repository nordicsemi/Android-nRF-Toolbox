package no.nordicsemi.android.nrftoolbox.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.analytics.view.AnalyticsPermissionButton
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrftoolbox.R
import no.nordicsemi.android.nrftoolbox.viewmodel.HomeViewModel
import no.nordicsemi.android.nrftoolbox.viewmodel.UiEvent
import no.nordicsemi.android.toolbox.lib.utils.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeView() {
    val viewModel = hiltViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onEvent: (UiEvent) -> Unit = { viewModel.onClickEvent(it) }

    Scaffold(
        topBar = {
            NordicAppBar(
                title = {
                    Text(stringResource(id = R.string.app_name))
                },
                actions = {
                    AnalyticsPermissionButton()
                }
            )
         },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(UiEvent.OnConnectDeviceClick) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = stringResource(R.string.connect_device))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                )
                .padding(horizontal = 16.dp)
                .consumeWindowInsets(paddingValues),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionTitle(title = stringResource(R.string.connected_devices))
            }
            if (state.connectedDevices.isNotEmpty()) {
                val items = state.connectedDevices.keys.toList()
                items(items = items, key = { it }) { address ->
                    state.connectedDevices[address]?.let { deviceData ->
                        val peripheral = deviceData.peripheral
                        val services = deviceData.services
                        val onClick: (Profile) -> Unit = { profile ->
                            onEvent(
                                UiEvent.OnDeviceClick(
                                    deviceAddress = peripheral.address,
                                    name = peripheral.name,
                                    profile = profile
                                )
                            )
                        }

                        // Case 1: If Battery service is the only one, show it.
                        if (services.size == 1 && services.first().profile == Profile.BATTERY) {
                            FeatureButton(
                                icon = painterResource(R.drawable.ic_battery),
                                description = stringResource(R.string.battery_module_full),
                                deviceName = peripheral.name,
                                deviceAddress = peripheral.address,
                                onClick = { onClick(Profile.BATTERY) },
                            )
                        }
                        // Case 2: Show the first *non-Battery* profile.
                        // This ensures only one service is shown per peripheral when multiple services are available.
                        services.firstOrNull { it.profile != Profile.BATTERY }?.let { serviceManager ->
                            when (serviceManager.profile) {
                                Profile.HRS -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_hrs),
                                    description = stringResource(R.string.hrs_module_full),
                                    deviceName = peripheral.name,
                                    profileNames = services.map { it.profile.toString() },
                                    deviceAddress = peripheral.address,
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.HTS -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_hts),
                                    description = stringResource(R.string.hts_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.BPS -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_bps),
                                    description = stringResource(R.string.bps_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.GLS -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_gls),
                                    description = stringResource(R.string.gls_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.CGM -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_cgm),
                                    description = stringResource(R.string.cgm_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.RSCS -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_rscs),
                                    description = stringResource(R.string.rscs_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.DDFS -> FeatureButton(
                                    icon = rememberVectorPainter(Icons.Default.MyLocation),
                                    description = stringResource(R.string.direction_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.CSC -> FeatureButton(
                                    icon = painterResource(R.drawable.ic_csc),
                                    description = stringResource(R.string.csc_module_full),
                                    deviceName = peripheral.name,
                                    deviceAddress = peripheral.address,
                                    profileNames = services.map { it.profile.toString() },
                                    onClick = { onClick(serviceManager.profile) },
                                )

                                Profile.THROUGHPUT -> {
                                    FeatureButton(
                                        icon = rememberVectorPainter(Icons.Default.SyncAlt),
                                        description = stringResource(R.string.throughput_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.UART -> {
                                    FeatureButton(
                                        icon = painterResource(R.drawable.ic_uart),
                                        description = stringResource(R.string.uart_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.CHANNEL_SOUNDING -> {
                                    FeatureButton(
                                        icon = rememberVectorPainter(Icons.Default.SocialDistance),
                                        description = stringResource(R.string.channel_sounding_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.LBS -> {
                                    FeatureButton(
                                        icon = rememberVectorPainter(Icons.Default.Lightbulb),
                                        description = stringResource(R.string.lbs_blinky_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.QUICK_START -> {
                                    FeatureButton(
                                        icon = painterResource(R.drawable.ic_quick_start),
                                        description = stringResource(R.string.quick_start_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.MDS -> {
                                    FeatureButton(
                                        icon = painterResource(R.drawable.ic_mds),
                                        description = stringResource(R.string.mds_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.DFU -> {
                                    FeatureButton(
                                        icon = painterResource(R.drawable.ic_dfu),
                                        description = stringResource(R.string.dfu_module_full),
                                        deviceName = peripheral.name,
                                        deviceAddress = peripheral.address,
                                        profileNames = services.map { it.profile.toString() },
                                        onClick = { onClick(serviceManager.profile) },
                                    )
                                }

                                Profile.BATTERY -> {
                                    // Battery service is handled above.
                                    // Do not show it if it's not an only service.
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    NoConnectedDeviceView()
                }
            }
            item {
                SectionTitle(title = stringResource(R.string.links) )
            }
            item {
                Links { onEvent(it) }
            }
        }
    }
}
