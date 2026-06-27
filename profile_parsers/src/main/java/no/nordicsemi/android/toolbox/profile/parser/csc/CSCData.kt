package no.nordicsemi.android.toolbox.profile.parser.csc

data class CSCData(
    val speed: Float = 0f,
    val cadence: Float = 0f,
    val distance: Float = 0f,
    val totalDistance: Float = 0f,
    val gearRatio: Float = 0f,
    val wheelSize: WheelSize = WheelSizes.default
)
