package no.nordicsemi.android.toolbox.profile.parser.directionFinder

data class PeripheralBluetoothAddress(
    val type: AddressType,
    val address: String
) {
    override fun toString(): String = "$address ($type)"

    companion object {
        // Note: The nRF DM sample sends distance to a device with address AA:BB:CC:DD:EE:FF
        //       with Azimuth and Elevation. This is a fake device to test the data.
        //       Don't change the address below.
        val TEST = PeripheralBluetoothAddress(AddressType.RANDOM, "AA:BB:CC:DD:EE:FF")
    }
}

enum class AddressType(val id: Int) {
    /** Public Bluetooth Address. */
    PUBLIC(0),
    /** Random Bluetooth Address. */
    RANDOM(1),
    /** Public Bluetooth Address identity, which may be resolved to the real Public Bluetooth Address. */
    PUBLIC_ID(2),
    /** Random Bluetooth Address identity, which may be resolved to the real Random Bluetooth Address. */
    RANDOM_ID(3);

    override fun toString(): String = when (this) {
        PUBLIC -> "Public"
        RANDOM -> "Random"
        PUBLIC_ID -> "Public ID"
        RANDOM_ID -> "Random ID"
    }

    companion object {
        fun create(id: Int): AddressType = entries.find { it.id == id }
            ?: throw IllegalArgumentException("Cannot find AddressType for specified id: $id")
    }
}
