package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.toolbox.lib.utils.spec.DIS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.QUICK_START_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.parser.dis.DeviceInformationParser
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val HARDWARE_REVISION_CHARACTERISTIC_UUID = Uuid.parse("00002A27-0000-1000-8000-00805f9b34fb")

/** The development kits running the Quick Start sample. */
enum class QuickStartBoard(val displayName: String) {
    NRF54L15_DK("nRF54L15 DK"),
    NRF54LM20_DK("nRF54LM20 DK");

    companion object {
        /**
         * Returns the board for the value of the Hardware Revision characteristic of
         * the Device Information Service.
         *
         * The sample sets it to the name of the board, e.g. `nrf54l15dk` or `nrf54lm20dk`.
         * Older versions of the sample always reported `nrf54l15`.
         */
        fun fromHardwareRevision(hardwareRevision: String?): QuickStartBoard =
            if (hardwareRevision?.contains("54lm20", ignoreCase = true) == true) NRF54LM20_DK
            else NRF54L15_DK
    }
}

/**
 * The Quick Start service has no characteristics — its presence alone is used to
 * trigger the onboarding card for the "Quick Start" sample.
 *
 * The Device Information Service is optional. If it has the Hardware Revision characteristic,
 * the value is used to tell which development kit the sample is running on.
 */
class QuickStartManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(
    requiredServiceUuids = listOf(QUICK_START_SERVICE_UUID),
    optionalServiceUuids = listOf(DIS_SERVICE_UUID),
    deviceId = deviceId,
    name = "Quick Start",
    onReady = onReady,
) {
    override val profile: ServiceType = ServiceType.QUICK_START
    private val tag = "Quick Start ($deviceId)"

    private val _board = MutableStateFlow(QuickStartBoard.NRF54L15_DK)

    /** The development kit the sample is running on. */
    val board: StateFlow<QuickStartBoard> = _board.asStateFlow()

    private var hardwareRevisionCharacteristic: RemoteCharacteristic? = null

    override fun prepare(peripheral: Peripheral<*, *>, services: List<RemoteService>) {
        // The Quick Start service has no characteristics to validate. The Hardware Revision
        // characteristic of the Device Information Service is optional.
        hardwareRevisionCharacteristic = services
            .firstOrNull { it.uuid == DIS_SERVICE_UUID }
            ?.characteristics
            ?.firstOrNull { it.uuid == HARDWARE_REVISION_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize(peripheral: Peripheral<*, *>) {
        onReady(this@QuickStartManager)

        val characteristic = hardwareRevisionCharacteristic ?: return
        if (!characteristic.isReadable()) return
        try {
            Timber.tag(tag).v("Reading Hardware Revision...")
            DeviceInformationParser.parseString(characteristic.read())?.let {
                Timber.tag(tag).v("Hardware Revision: $it")
                _board.value = QuickStartBoard.fromHardwareRevision(it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error reading Hardware Revision")
        }
    }
}
