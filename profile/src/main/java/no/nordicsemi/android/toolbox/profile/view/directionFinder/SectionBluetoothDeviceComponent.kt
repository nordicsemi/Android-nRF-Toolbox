package no.nordicsemi.android.toolbox.profile.view.directionFinder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.ui.view.SectionTitle
import no.nordicsemi.android.toolbox.profile.data.DFSServiceData
import no.nordicsemi.android.toolbox.profile.parser.directionFinder.PeripheralBluetoothAddress
import no.nordicsemi.android.toolbox.profile.parser.gls.data.RequestStatus
import no.nordicsemi.android.toolbox.profile.viewmodel.DFSEvent
import no.nordicsemi.android.ui.view.ScreenSection

@Composable
internal fun SectionBluetoothDeviceComponent(
    data: DFSServiceData,
    selectedDevice: PeripheralBluetoothAddress?,
    onEvent: (DFSEvent) -> Unit
) {
    val devices = data.data.keys.toList()
    val shape = MaterialTheme.shapes.medium
        .let {
            if (devices.isNotEmpty()) {
                it.copy(bottomStart = CornerSize(4.dp), bottomEnd = CornerSize(4.dp))
            } else {
                it
            }
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ScreenSection(shape = shape) {
            SectionTitle(
                painter = rememberVectorPainter(Icons.Default.MyLocation),
                title = "Distance measurement",
            )
            when {
                data.requestStatus != RequestStatus.SUCCESS -> ProgressView("Loading...")
                devices.isEmpty() -> ProgressView("Scanning...")
                else -> {
                    Text(
                        text = "Select the device to measure distance to:",
                    )
                }
            }
        }
        if (devices.isNotEmpty()) {
            if (selectedDevice == null) {
                DeviceList(devices) {
                    onEvent(DFSEvent.OnBluetoothDeviceSelected(it))
                }
            } else {
                DeviceSelected(selectedDevice, devices, onEvent)
            }
        }
    }
}

@Composable
private fun ProgressView(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
        Text(text = text)
    }
}

@Composable
private fun DeviceSelected(
    selectedDevice: PeripheralBluetoothAddress,
    devices: List<PeripheralBluetoothAddress>,
    onEvent: (DFSEvent) -> Unit
) {
    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
    val icon = if (showDropdownMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown
    val otherDevices = devices.filter { it != selectedDevice }

    Box {
        BluetoothDeviceView(
            device = selectedDevice,
            enabled = otherDevices.isNotEmpty(),
            isLast = true,
            onClick = { showDropdownMenu = true },
            menu = {
                // Don't show icon if only one device is available
                if (otherDevices.isNotEmpty()) {
                    Icon(icon, contentDescription = "")
                }
            }
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
        ) {
            otherDevices.forEach {
                DropdownMenuItem(
                    text = { Text(it.toString()) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onEvent(DFSEvent.OnBluetoothDeviceSelected(it))
                        showDropdownMenu = false
                    },
                )
            }
        }
    }

}

@Composable
internal fun DeviceList(
    devices: List<PeripheralBluetoothAddress>,
    onClick: (PeripheralBluetoothAddress) -> Unit
) {
    val count = devices.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        devices.forEachIndexed { index,  address ->
            BluetoothDeviceView(
                device = address,
                isLast = index == count - 1,
                onClick = onClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceSelectedPreview() {
    DeviceSelected(
        selectedDevice = PeripheralBluetoothAddress.TEST,
        devices = listOf(
            PeripheralBluetoothAddress.TEST,
            PeripheralBluetoothAddress.TEST,
            PeripheralBluetoothAddress.TEST
        ),
        onEvent = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun DeviceListPreview() {
    DeviceList(
        devices = listOf(
            PeripheralBluetoothAddress.TEST,
            PeripheralBluetoothAddress.TEST,
            PeripheralBluetoothAddress.TEST
        ),
        onClick = {}
    )
}

@Composable
internal fun BluetoothDeviceView(
    device: PeripheralBluetoothAddress,
    enabled: Boolean = true,
    isLast: Boolean = true,
    onClick: (PeripheralBluetoothAddress) -> Unit = {},
    menu: @Composable () -> Unit = {},
) {
    val shape = MaterialTheme.shapes.extraSmall
        .let {
            if (isLast) {
                it.copy(bottomStart = CornerSize(12.dp), bottomEnd = CornerSize(12.dp))
            } else {
                it
            }
        }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick(device) },
        shape = shape,
    ) {
        // Note: The nRF DM sample sends distance to a device with address AA:BB:CC:DD:EE:FF
        //       with Azimuth and Elevation. This is a fake device to test the data.
        val isTestDevice = device == PeripheralBluetoothAddress.TEST
        val name = if (isTestDevice) "Test Data" else "nRF DM"

        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text(device.address) },
            leadingContent = { CircularIcon(imageVector = Icons.Default.MyLocation) },
            trailingContent = menu,
        )
    }
}