package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType
import no.nordicsemi.android.toolbox.lib.utils.spec.CHANNEL_SOUND_SERVICE_UUID
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid

private val RAS_FEATURES = Uuid.parse("00002C14-0000-1000-8000-00805F9B34FB")

internal class ChannelSoundingManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(CHANNEL_SOUND_SERVICE_UUID, deviceId, "Channel Sounding", onReady) {
    override val profile: ServiceType = ServiceType.CHANNEL_SOUNDING

    private var rasFeaturesCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        rasFeaturesCharacteristic = service.characteristics.firstOrNull { it.uuid == RAS_FEATURES }
    }

    override suspend fun CoroutineScope.initialize() {
        rasFeaturesCharacteristic?.let { char ->
            launch {
                try {
                    val rasFeature = RasFeatureParser.parse(char.read())
                    Timber.tag("ChannelSoundingManager").d("Ranging Feature: $rasFeature")
                } catch (e: Exception) {
                    Timber.e("Error reading RAS features: ${e.message}")
                }
            }
        }

        onReady(this@ChannelSoundingManager)
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
            val bits = (data[0].toInt() and 0xFF) or
                    ((data[1].toInt() and 0xFF) shl 8) or
                    ((data[2].toInt() and 0xFF) shl 16) or
                    ((data[3].toInt() and 0xFF) shl 24)
            return RasFeature(
                realTimeRangingData = bits and (1 shl 0) != 0,
                retrieveLostSegments = bits and (1 shl 1) != 0,
                abortOperation = bits and (1 shl 2) != 0,
                filterRangingData = bits and (1 shl 3) != 0,
            )
        }
    }
}
