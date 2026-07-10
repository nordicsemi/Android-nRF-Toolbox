package no.nordicsemi.android.toolbox.profile.parser.csc

/**
 * @property speed the average speed in meters per second.
 * @property cadence the average cadence in revolutions per minute.
 * @property distance the distance traveled since the last message, in meters.
 * @property totalDistance the total distance traveled, in meters.
 * @property gearRatio the gear ratio.
 * @property wheelSize the wheel size.
 */
data class CSCData(
    val speed: Float = 0f,
    val cadence: Float = 0f,
    val distance: Float = 0f,
    val totalDistance: Float = 0f,
    val gearRatio: Float = 0f,
    val wheelSize: WheelSize = WheelSizes.default
) {
    override fun toString() =
        "Speed: %.1f m/s, Cadence: %.1f RPM, Total Distance: %d m".format(speed, cadence, totalDistance.toInt())
}
