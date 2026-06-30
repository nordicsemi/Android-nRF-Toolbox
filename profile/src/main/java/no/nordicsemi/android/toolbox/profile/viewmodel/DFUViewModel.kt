package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.DFUManager

@HiltViewModel(assistedFactory = DFUViewModel.Factory::class)
internal class DFUViewModel @AssistedInject constructor(
    @Assisted private val manager: DFUManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: DFUManager): DFUViewModel }

    val state = manager.repository.data
}
