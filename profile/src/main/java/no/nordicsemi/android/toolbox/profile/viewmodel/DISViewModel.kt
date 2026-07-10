package no.nordicsemi.android.toolbox.profile.viewmodel

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import no.nordicsemi.android.toolbox.profile.manager.DISManager

@HiltViewModel(assistedFactory = DISViewModel.Factory::class)
internal class DISViewModel @AssistedInject constructor(
    @Assisted private val manager: DISManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory { fun create(manager: DISManager): DISViewModel }

    val state = manager.repository.data
}
