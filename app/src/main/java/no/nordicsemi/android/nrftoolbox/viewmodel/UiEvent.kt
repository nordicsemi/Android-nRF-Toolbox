package no.nordicsemi.android.nrftoolbox.viewmodel

/**
 * HomeViewEvent is a sealed interface that represents the events that can be emitted by the Home view.
 */
sealed interface UiEvent {

    /**  OnConnectDeviceClick event that is emitted when the user clicks on the Connect Device button. */
    data object OnConnectDeviceClick : UiEvent

    /**  OnDeviceClick event is emitted when the user clicks on a connected device. */
    data class OnDeviceClick(val deviceAddress: String, val name: String?) : UiEvent

    /**
     * OnGitHubClick event is emitted when the user clicks on the GitHub repository option.
     */
    data object OnGitHubClick : UiEvent

    /** OnNordicDevZoneClick event is emitted when the user clicks on the Nordic DevZone option. */
    data object OnNordicDevZoneClick : UiEvent
}