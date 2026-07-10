package no.nordicsemi.android.nrftoolbox.viewmodel

import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.Link
import no.nordicsemi.android.analytics.LinkOpenEvent
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.nrftoolbox.ScannerDestinationId
import no.nordicsemi.android.service.profile.ProfileServiceManager
import no.nordicsemi.android.service.profile.ServiceApi
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.argAddress
import no.nordicsemi.android.toolbox.profile.argName
import javax.inject.Inject

internal data class HomeViewState(
    val connectedDevices: Map<String, ServiceApi.DeviceData> = emptyMap(),
)

private const val GITHUB_REPO_URL = "https://github.com/nordicsemi/Android-nRF-Toolbox"
private const val NORDIC_DEV_ZONE_URL = "https://devzone.nordicsemi.com/"

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val navigator: Navigator,
    private val profileServiceManager: ProfileServiceManager,
    private val analytics: AppAnalytics,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeViewState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val api = profileServiceManager.bindService()

            // Observe connected devices from the repository
            api.devices
                .onEach { devices ->
                    _state.update { currentState ->
                        currentState.copy(connectedDevices = devices)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onClickEvent(event: UiEvent) {
        when (event) {
            UiEvent.OnConnectDeviceClick -> navigator.navigateTo(ScannerDestinationId)
            is UiEvent.OnDeviceClick -> {
                val bundle = Bundle().apply {
                    putString(argAddress, event.deviceAddress)
                    putString(argName, event.name)
                }
                navigator.navigateTo(ProfileDestinationId, bundle)
            }

            UiEvent.OnGitHubClick -> {
                // Log the event for analytics.
                analytics.logEvent(LinkOpenEvent(Link.GITHUB))
                navigator.open(GITHUB_REPO_URL.toUri())
            }

            UiEvent.OnNordicDevZoneClick -> {
                // Log the event for analytics.
                analytics.logEvent(LinkOpenEvent(Link.DEV_ZONE))
                navigator.open(NORDIC_DEV_ZONE_URL.toUri())
            }
        }
    }

    override fun onCleared() {
        profileServiceManager.unbindService()
    }

}