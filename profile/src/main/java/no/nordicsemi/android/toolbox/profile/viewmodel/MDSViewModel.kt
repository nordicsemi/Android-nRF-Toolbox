package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.DFUManager
import no.nordicsemi.android.toolbox.profile.manager.MDSManager

@HiltViewModel(assistedFactory = MDSViewModel.Factory::class)
internal class MDSViewModel @AssistedInject constructor(
    @Assisted private val manager: MDSManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: MDSManager): MDSViewModel }

    val state = manager.manager.state
}
