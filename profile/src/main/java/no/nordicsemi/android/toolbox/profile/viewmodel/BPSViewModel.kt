package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.BPSManager

@HiltViewModel(assistedFactory = BPSViewModel.Factory::class)
internal class BPSViewModel @AssistedInject constructor(
    @Assisted private val manager: BPSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: BPSManager): BPSViewModel }

    val state = manager.repository.data
}
