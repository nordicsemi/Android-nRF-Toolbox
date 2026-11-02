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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.analytics.view.AnalyticsPermissionButton
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrftoolbox.BuildConfig
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
                // Extra space, so the list can be scrolled above the FAB.
                bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
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
                        DeviceItem(
                            deviceName = peripheral.name,
                            deviceAddress = peripheral.address,
                            // While not connected (reconnecting), the services are not known,
                            // so the last known profiles are used.
                            profiles = deviceData.services.map { it.profile }
                                .ifEmpty { deviceData.knownProfiles },
                            isConnected = deviceData.connectionState.isConnected,
                            values = deviceData.values(),
                            onDisconnect = { onEvent(UiEvent.OnDisconnectClick(peripheral.address)) },
                            onClick = {
                                onEvent(
                                    UiEvent.OnDeviceClick(
                                        deviceAddress = peripheral.address,
                                        name = peripheral.name,
                                    )
                                )
                            },
                        )
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
            item {
                SectionTitle(title = stringResource(R.string.about) )
            }
            item {
                Version()
            }
        }
    }
}

@Composable
private fun Version() {
    OutlinedCard {
        ListItem(
            headlineContent = { Text(stringResource(R.string.version)) },
            supportingContent = { Text(stringResource(R.string.version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

/**
 * A device item on the list.
 *
 * Only one item is shown per peripheral. Its icon and description are taken from the most
 * specific of the [profiles].
 */
@Composable
private fun DeviceItem(
    deviceName: String?,
    deviceAddress: String,
    profiles: List<Profile>,
    isConnected: Boolean,
    values: List<DeviceValue>,
    onDisconnect: () -> Unit,
    onClick: () -> Unit,
) {
    val profileNames = profiles.map { it.toString() }
    // Case 1: If Battery and/or Device Information are the only services, show Device Information.
    if (profiles.contains(Profile.DIS) &&
        profiles.all { it == Profile.BATTERY || it == Profile.DIS }
    ) {
        FeatureButton(
            icon = rememberVectorPainter(Icons.Default.Info),
            description = stringResource(R.string.dis_module_full),
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            profileNames = profileNames,
            isConnected = isConnected,
            values = values,
            onDisconnect = onDisconnect,
            onClick = onClick,
        )
        return
    }
    // Case 2: If Battery service is the only one, show it.
    if (profiles.size == 1 && profiles.first() == Profile.BATTERY) {
        FeatureButton(
            icon = painterResource(R.drawable.ic_battery),
            description = stringResource(R.string.battery_module_full),
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            isConnected = isConnected,
            values = values,
            onDisconnect = onDisconnect,
            onClick = onClick,
        )
        return
    }
    // Case 3: Show the first *non-Battery, non-Device-Information* profile.
    // This ensures only one service is shown per peripheral when multiple services are available.
    val profile = profiles.firstOrNull { it != Profile.BATTERY && it != Profile.DIS }
    // Case 4: No profiles are known yet (the services are being discovered).
    val icon: Painter
    val description: String
    when (profile) {
        Profile.HRS -> { icon = painterResource(R.drawable.ic_hrs); description = stringResource(R.string.hrs_module_full) }
        Profile.HTS -> { icon = painterResource(R.drawable.ic_hts); description = stringResource(R.string.hts_module_full) }
        Profile.BPS -> { icon = painterResource(R.drawable.ic_bps); description = stringResource(R.string.bps_module_full) }
        Profile.GLS -> { icon = painterResource(R.drawable.ic_gls); description = stringResource(R.string.gls_module_full) }
        Profile.CGM -> { icon = painterResource(R.drawable.ic_cgm); description = stringResource(R.string.cgm_module_full) }
        Profile.RSCS -> { icon = painterResource(R.drawable.ic_rscs); description = stringResource(R.string.rscs_module_full) }
        Profile.DDFS -> { icon = rememberVectorPainter(Icons.Default.MyLocation); description = stringResource(R.string.direction_module_full) }
        Profile.CSC -> { icon = painterResource(R.drawable.ic_csc); description = stringResource(R.string.csc_module_full) }
        Profile.THROUGHPUT -> { icon = rememberVectorPainter(Icons.Default.SyncAlt); description = stringResource(R.string.throughput_module_full) }
        Profile.UART -> { icon = painterResource(R.drawable.ic_uart); description = stringResource(R.string.uart_module_full) }
        Profile.CHANNEL_SOUNDING -> { icon = rememberVectorPainter(Icons.Default.SocialDistance); description = stringResource(R.string.channel_sounding_module_full) }
        Profile.LBS -> { icon = rememberVectorPainter(Icons.Default.Lightbulb); description = stringResource(R.string.lbs_blinky_module_full) }
        Profile.QUICK_START -> { icon = painterResource(R.drawable.ic_quick_start); description = stringResource(R.string.quick_start_module_full) }
        Profile.MDS -> { icon = painterResource(R.drawable.ic_mds); description = stringResource(R.string.mds_module_full) }
        Profile.DFU -> { icon = painterResource(R.drawable.ic_dfu); description = stringResource(R.string.dfu_module_full) }
        // Battery and Device Information are handled above.
        Profile.BATTERY, Profile.DIS, null -> {
            icon = rememberVectorPainter(Icons.Default.Bluetooth)
            description = stringResource(R.string.unknown_device)
        }
    }
    FeatureButton(
        icon = icon,
        description = description,
        deviceName = deviceName,
        deviceAddress = deviceAddress,
        profileNames = profileNames,
        isConnected = isConnected,
        values = values,
        onDisconnect = onDisconnect,
        onClick = onClick,
    )
}
