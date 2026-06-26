package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

private val RAS_FEATURES = Uuid.parse("00002C14-0000-1000-8000-00805F9B34FB")
private val REALTIME_RANGING_DATA = Uuid.parse("00002C15-0000-1000-8000-00805F9B34FB")
private val RAS_ON_DEMAND_RD = Uuid.parse("00002C16-0000-1000-8000-00805F9B34FB")
private val RAS_CP = Uuid.parse("00002C17-0000-1000-8000-00805F9B34FB")
private val RAS_RD_READY = Uuid.parse("00002C18-0000-1000-8000-00805F9B34FB")
private val RAS_RD_OVERWRITTEN = Uuid.parse("00002C19-0000-1000-8000-00805F9B34FB")

internal class ChannelSoundingManager : ServiceManager {
    override val profile: Profile
        get() = Profile.CHANNEL_SOUNDING

    @OptIn(ExperimentalUuidApi::class, ExperimentalStdlibApi::class)
    override suspend fun observeServiceInteractions(
        deviceId: String,
        remoteService: RemoteService,
        scope: CoroutineScope
    ) {
        remoteService.characteristics.firstOrNull { it.uuid == RAS_FEATURES }
            ?.read()
            ?.let {
                val rasFeature = RasFeatureParser.parse(it)
                Timber.tag("ChannelSoundingManager").d("Ranging Feature: $rasFeature")
            }
    }

    data class RasFeature(
        val realTimeRangingData: Boolean,
        val retrieveLostSegments: Boolean,
        val abortOperation: Boolean,
        val filterRangingData: Boolean,
    )


    object RasFeatureParser {

        fun parse(data: ByteArray): RasFeature {
            require(data.size >= 4) { "RAS Features characteristic must be at least 4 bytes." }

            val featureBits = (data[0].toInt() and 0xFF) or
                    ((data[1].toInt() and 0xFF) shl 8) or
                    ((data[2].toInt() and 0xFF) shl 16) or
                    ((data[3].toInt() and 0xFF) shl 24)

            return RasFeature(
                realTimeRangingData = featureBits and (1 shl 0) != 0,
                retrieveLostSegments = featureBits and (1 shl 1) != 0,
                abortOperation = featureBits and (1 shl 2) != 0,
                filterRangingData = featureBits and (1 shl 3) != 0
            )
        }

    }
}
