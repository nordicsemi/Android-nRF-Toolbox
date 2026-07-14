package no.nordicsemi.android.toolbox.profile.data

import no.nordicsemi.android.toolbox.lib.utils.Profile

data class ChannelSoundingServiceData(
    override val profile: Profile = Profile.CHANNEL_SOUNDING,
    val rangingSessionAction: RangingSessionAction? = null,
    val updateRate: UpdateRate = UpdateRate.NORMAL,
    val interval: Int = 1000,
) : ProfileServiceData()

sealed interface RangingSessionAction {
    data object OnStart : RangingSessionAction
    data class OnResult(
        val data: CsRangingData,
        val previousData: List<Float> = emptyList()
    ) : RangingSessionAction

    data class OnError(val reason: SessionCloseReasonProvider) : RangingSessionAction
    object OnClosed : RangingSessionAction
    data object OnRestarting : RangingSessionAction
}

data class CsRangingData(
    val distance: CSRangingMeasurement? = null,
    val azimuth: CSRangingMeasurement? = null,
    val elevation: CSRangingMeasurement? = null,
    val technology: RangingTechnology,
    val timeStamp: Long,
    val hasRssi: Boolean = false,
    val rssi: Int? = null,
)

data class CSRangingMeasurement(
    val measurement: Double,
    val confidenceLevel: ConfidenceLevel,
)

enum class UpdateRate {
    FREQUENT,
    NORMAL,
    INFREQUENT;
}

enum class ConfidenceLevel(val value: Int) {
    CONFIDENCE_HIGH(2),
    CONFIDENCE_MEDIUM(1),
    CONFIDENCE_LOW(0);

    companion object {
        fun from(value: Int): ConfidenceLevel = entries.find { it.value == value } ?: CONFIDENCE_LOW
    }
}

enum class RangingTechnology(val value: Int) {
    BLE_CS(1),
    BLE_RSSI(3),
    UWB(0),
    WIFI_NAN_RTT(2),
    WIFI_STA_RTT(4), ;

    companion object {
        fun from(value: Int): RangingTechnology? = entries.find { it.value == value }
    }
}

enum class SessionClosedReason : SessionCloseReasonProvider {
    MISSING_PERMISSION,
    NOT_SUPPORTED,
    RANGING_NOT_AVAILABLE,
    CS_SECURITY_NOT_AVAILABLE,
    UNKNOWN;
}

sealed interface SessionCloseReasonProvider

enum class RangingSessionFailedReason(val reason: Int) : SessionCloseReasonProvider {
    UNKNOWN(0),
    LOCAL_REQUEST(1),
    REMOTE_REQUEST(2),
    UNSUPPORTED(3),
    SYSTEM_POLICY(4),
    NO_PEERS_FOUND(5), ;

    companion object {
        fun getReason(value: Int): RangingSessionFailedReason {
            return entries.firstOrNull { it.reason == value } ?: UNKNOWN
        }
    }
}
