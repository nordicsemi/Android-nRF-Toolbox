package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.profile.parser.throughput.ThroughputMetrics
import no.nordicsemi.android.toolbox.lib.utils.Profile

data class ThroughputServiceData(
    override val profile: Profile = Profile.THROUGHPUT,
    val throughputData: ThroughputMetrics = ThroughputMetrics(),
    val writingStatus: WritingStatus = WritingStatus.IDLE,
    val maxWriteValueLength: Int? = null
) : ProfileServiceData()

sealed interface ThroughputInputType

data class NumberOfBytes(
    val numberOfBytes: Int
) : ThroughputInputType {

    companion object {
        private const val DISPLAY_NAME = "Test in size (in kB) "
        fun getString(): String = DISPLAY_NAME
    }

    override fun toString() = "$numberOfBytes bytes"
}

data class NumberOfSeconds(
    val numberOfSeconds: Int
) : ThroughputInputType {

    companion object {
        private const val DISPLAY_NAME = "Test in time (seconds)"
        fun getString(): String = DISPLAY_NAME
    }

    override fun toString() = "$numberOfSeconds seconds"
}

enum class WritingStatus {
    IDLE, IN_PROGRESS, COMPLETED
}