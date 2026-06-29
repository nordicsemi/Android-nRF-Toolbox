package no.nordicsemi.android.toolbox.profile.parser.rscs

data class RSCSData(
    val running: Boolean = false,
    val instantaneousSpeed: Float = 0f,
    val instantaneousCadence: Int = 0,
    val strideLength: Int? = null,
    val totalDistance: Long? = null
) {
    override fun toString() = buildString {
        if (running) {
            append("Running")
        } else {
            append("Walking")
        }
        append(" (speed: ")
        append("%.1f".format(instantaneousSpeed))
        append(" m/s, cadence: ")
        append(instantaneousCadence)
        append(" rpm")
        strideLength?.let {
            append(", stride length: ")
            append(it)
            append(" cm")
        }
        append(")")
    }
}
