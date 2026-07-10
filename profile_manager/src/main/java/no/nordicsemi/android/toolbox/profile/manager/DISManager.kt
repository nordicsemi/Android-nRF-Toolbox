package no.nordicsemi.android.toolbox.profile.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.toolbox.lib.utils.spec.DIS_SERVICE_UUID
import no.nordicsemi.android.toolbox.profile.manager.repository.DISRepository
import no.nordicsemi.android.toolbox.profile.parser.dis.DeviceInformationParser
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import timber.log.Timber
import kotlin.uuid.Uuid
import no.nordicsemi.android.toolbox.lib.utils.Profile as ServiceType

private val MANUFACTURER_NAME_CHARACTERISTIC_UUID = Uuid.parse("00002A29-0000-1000-8000-00805f9b34fb")
private val MODEL_NUMBER_CHARACTERISTIC_UUID = Uuid.parse("00002A24-0000-1000-8000-00805f9b34fb")
private val SERIAL_NUMBER_CHARACTERISTIC_UUID = Uuid.parse("00002A25-0000-1000-8000-00805f9b34fb")
private val HARDWARE_REVISION_CHARACTERISTIC_UUID = Uuid.parse("00002A27-0000-1000-8000-00805f9b34fb")
private val FIRMWARE_REVISION_CHARACTERISTIC_UUID = Uuid.parse("00002A26-0000-1000-8000-00805f9b34fb")
private val SOFTWARE_REVISION_CHARACTERISTIC_UUID = Uuid.parse("00002A28-0000-1000-8000-00805f9b34fb")
private val SYSTEM_ID_CHARACTERISTIC_UUID = Uuid.parse("00002A23-0000-1000-8000-00805f9b34fb")
private val IEEE_CERTIFICATION_DATA_LIST_CHARACTERISTIC_UUID = Uuid.parse("00002A2A-0000-1000-8000-00805f9b34fb")
private val PNP_ID_CHARACTERISTIC_UUID = Uuid.parse("00002A50-0000-1000-8000-00805f9b34fb")

class DISManager(
    deviceId: String,
    onReady: (ServiceManager) -> Unit,
) : ServiceManager(DIS_SERVICE_UUID, deviceId, "DIS", onReady) {
    override val profile: ServiceType = ServiceType.DIS
    private val tag = "DIS ($deviceId)"

    val repository = DISRepository()

    private var manufacturerNameCharacteristic: RemoteCharacteristic? = null
    private var modelNumberCharacteristic: RemoteCharacteristic? = null
    private var serialNumberCharacteristic: RemoteCharacteristic? = null
    private var hardwareRevisionCharacteristic: RemoteCharacteristic? = null
    private var firmwareRevisionCharacteristic: RemoteCharacteristic? = null
    private var softwareRevisionCharacteristic: RemoteCharacteristic? = null
    private var systemIdCharacteristic: RemoteCharacteristic? = null
    private var ieeeCertificationDataCharacteristic: RemoteCharacteristic? = null
    private var pnpIdCharacteristic: RemoteCharacteristic? = null

    override fun prepare(service: RemoteService) {
        // Every Device Information Service characteristic is optional.
        manufacturerNameCharacteristic = service.characteristics.firstOrNull { it.uuid == MANUFACTURER_NAME_CHARACTERISTIC_UUID }
        modelNumberCharacteristic = service.characteristics.firstOrNull { it.uuid == MODEL_NUMBER_CHARACTERISTIC_UUID }
        serialNumberCharacteristic = service.characteristics.firstOrNull { it.uuid == SERIAL_NUMBER_CHARACTERISTIC_UUID }
        hardwareRevisionCharacteristic = service.characteristics.firstOrNull { it.uuid == HARDWARE_REVISION_CHARACTERISTIC_UUID }
        firmwareRevisionCharacteristic = service.characteristics.firstOrNull { it.uuid == FIRMWARE_REVISION_CHARACTERISTIC_UUID }
        softwareRevisionCharacteristic = service.characteristics.firstOrNull { it.uuid == SOFTWARE_REVISION_CHARACTERISTIC_UUID }
        systemIdCharacteristic = service.characteristics.firstOrNull { it.uuid == SYSTEM_ID_CHARACTERISTIC_UUID }
        ieeeCertificationDataCharacteristic = service.characteristics.firstOrNull { it.uuid == IEEE_CERTIFICATION_DATA_LIST_CHARACTERISTIC_UUID }
        pnpIdCharacteristic = service.characteristics.firstOrNull { it.uuid == PNP_ID_CHARACTERISTIC_UUID }
    }

    override suspend fun CoroutineScope.initialize() {
        onReady(this@DISManager)

        readInto(manufacturerNameCharacteristic, "Manufacturer Name", DeviceInformationParser::parseString, repository::updateManufacturerName)
        readInto(modelNumberCharacteristic, "Model Number", DeviceInformationParser::parseString, repository::updateModelNumber)
        readInto(serialNumberCharacteristic, "Serial Number", DeviceInformationParser::parseString, repository::updateSerialNumber)
        readInto(hardwareRevisionCharacteristic, "Hardware Revision", DeviceInformationParser::parseString, repository::updateHardwareRevision)
        readInto(firmwareRevisionCharacteristic, "Firmware Revision", DeviceInformationParser::parseString, repository::updateFirmwareRevision)
        readInto(softwareRevisionCharacteristic, "Software Revision", DeviceInformationParser::parseString, repository::updateSoftwareRevision)
        readInto(systemIdCharacteristic, "System ID", DeviceInformationParser::parseSystemId, repository::updateSystemId)
        readInto(ieeeCertificationDataCharacteristic, "IEEE Certification Data", DeviceInformationParser::parseIeeeCertificationData, repository::updateIeeeCertificationData)
        readInto(pnpIdCharacteristic, "PnP ID", DeviceInformationParser::parsePnpId, repository::updatePnpId)
    }

    private suspend fun readInto(
        characteristic: RemoteCharacteristic?,
        label: String,
        parse: (ByteArray) -> String?,
        onResult: (String) -> Unit,
    ) {
        val char = characteristic ?: return
        if (!char.isReadable()) return
        try {
            Timber.tag(tag).v("Reading $label...")
            parse(char.read())?.let {
                Timber.tag(tag).log(Log.Level.APPLICATION, "$label: $it")
                onResult(it)
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error reading $label")
        }
    }
}
