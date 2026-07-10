package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.toolbox.profile.data.DISServiceData

class DISRepository {
    private val _data = MutableStateFlow(DISServiceData())
    val data: StateFlow<DISServiceData> = _data.asStateFlow()

    fun updateManufacturerName(value: String) = _data.update { it.copy(manufacturerName = value) }
    fun updateModelNumber(value: String) = _data.update { it.copy(modelNumber = value) }
    fun updateSerialNumber(value: String) = _data.update { it.copy(serialNumber = value) }
    fun updateHardwareRevision(value: String) = _data.update { it.copy(hardwareRevision = value) }
    fun updateFirmwareRevision(value: String) = _data.update { it.copy(firmwareRevision = value) }
    fun updateSoftwareRevision(value: String) = _data.update { it.copy(softwareRevision = value) }
    fun updateSystemId(value: String) = _data.update { it.copy(systemId = value) }
    fun updateIeeeCertificationData(value: String) = _data.update { it.copy(ieeeCertificationData = value) }
    fun updatePnpId(value: String) = _data.update { it.copy(pnpId = value) }
}
