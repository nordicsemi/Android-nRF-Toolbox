package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.CSCManager
import no.nordicsemi.android.toolbox.profile.parser.csc.SpeedUnit
import no.nordicsemi.android.toolbox.profile.parser.csc.WheelSize

internal sealed interface CSCEvent {
    data class OnWheelSizeSelected(val wheelSize: WheelSize) : CSCEvent
    data class OnSelectedSpeedUnitSelected(val selectedSpeedUnit: SpeedUnit) : CSCEvent
}

@HiltViewModel(assistedFactory = CSCViewModel.Factory::class)
internal class CSCViewModel @AssistedInject constructor(
    @Assisted private val manager: CSCManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: CSCManager): CSCViewModel }

    val state = manager.repository.data

    fun onEvent(event: CSCEvent) {
        when (event) {
            is CSCEvent.OnWheelSizeSelected ->
                manager.repository.setWheelSize(event.wheelSize)

            is CSCEvent.OnSelectedSpeedUnitSelected ->
                manager.repository.setSpeedUnit(event.selectedSpeedUnit)
        }
    }
}
