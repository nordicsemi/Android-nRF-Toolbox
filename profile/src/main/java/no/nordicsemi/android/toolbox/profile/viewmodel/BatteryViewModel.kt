package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.BatteryManager

@HiltViewModel(assistedFactory = BatteryViewModel.Factory::class)
internal class BatteryViewModel @AssistedInject constructor(
    @Assisted private val manager: BatteryManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: BatteryManager): BatteryViewModel }

    val state = manager.repository.data
}
