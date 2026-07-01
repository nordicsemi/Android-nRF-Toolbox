package no.nordicsemi.android.nrftoolbox

import android.os.Bundle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import no.nordicsemi.android.common.navigation.createDestination
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.common.scanner.DeviceSelected
import no.nordicsemi.android.common.scanner.ScannerScreen
import no.nordicsemi.android.common.scanner.ScanningCancelled
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.argName
import no.nordicsemi.kotlin.ble.client.android.ScanResult

val ScannerDestinationId = createDestination<Unit, ScanResult>("ble-scanner")

val ScannerDestination = defineDestination(ScannerDestinationId) {
    val navigationVM = hiltViewModel<SimpleNavigationViewModel>()

    ScannerScreen(
        cancellable = true,
        onResultSelected = {
            when (it) {
                is DeviceSelected -> {
                    val bundle = Bundle().apply {
                        putString(argAddress, it.scanResult.peripheral.address)
                        putString(argName, it.scanResult.peripheral.name)
                    }
                    navigationVM.navigateTo(ProfileDestinationId, bundle) {
                        popUpTo(ScannerDestinationId.toString()) {
                            inclusive = true
                        }
                    }
                }

                ScanningCancelled -> {
                    navigationVM.navigateUp()
                }
            }
        }
    )
}
