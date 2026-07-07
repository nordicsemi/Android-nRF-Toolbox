package no.nordicsemi.android.toolbox.profile

import android.os.Bundle
import kotlinx.serialization.Serializable
import no.nordicsemi.android.common.navigation.createDestination
import no.nordicsemi.android.common.navigation.defineDestination

const val argAddress = "address"
const val argName = "name"

val ProfileDestinationId = createDestination<Bundle, Unit>("profile-destination")
val ProfileDestination = listOf(
    defineDestination(ProfileDestinationId) {
        ProfileScreen()
    }
)
