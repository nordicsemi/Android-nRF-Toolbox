package no.nordicsemi.android.toolbox.profile.parser.directionFinder.ddf

data class DDFData(
    val isMcpdAvailable: Boolean,
    val isRttAvailable: Boolean
) {
    override fun toString() = "MCPD: ${if (isMcpdAvailable) "" else "not "}available, RTT: ${if (isRttAvailable) "" else "not "}available"
}