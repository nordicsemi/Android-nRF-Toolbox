package no.nordicsemi.android.toolbox.profile.parser.cgms.data

class CGMCalibrationStatus(val value: Int) {
    val rejected: Boolean = value and 0x01 != 0
    val dataOutOfRange: Boolean = value and 0x02 != 0
    val processPending: Boolean = value and 0x04 != 0
}