package no.nordicsemi.android.toolbox.profile.manager

import no.nordicsemi.android.toolbox.lib.utils.spec.BATTERY_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.BPS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.CGMS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.CHANNEL_SOUND_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.CSC_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.DF_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.GLS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.HRS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.HTS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.LBS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.LEGACY_DFU_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.MDS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.RSCS_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.SMP_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.THROUGHPUT_SERVICE_UUID
import no.nordicsemi.android.toolbox.lib.utils.spec.UART_SERVICE_UUID
import kotlin.uuid.Uuid

object ServiceManagerFactory {

    private val knownServiceUuids: Set<Uuid> = setOf(
        BATTERY_SERVICE_UUID,
        BPS_SERVICE_UUID,
        CSC_SERVICE_UUID,
        CGMS_SERVICE_UUID,
        DF_SERVICE_UUID,
        GLS_SERVICE_UUID,
        HTS_SERVICE_UUID,
        HRS_SERVICE_UUID,
        RSCS_SERVICE_UUID,
        THROUGHPUT_SERVICE_UUID,
        UART_SERVICE_UUID,
        CHANNEL_SOUND_SERVICE_UUID,
        LBS_SERVICE_UUID,
        DFU_SERVICE_UUID,
        SMP_SERVICE_UUID,
        MDS_SERVICE_UUID,
        LEGACY_DFU_SERVICE_UUID,
        EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID,
    )

    fun isKnownService(uuid: Uuid): Boolean = uuid in knownServiceUuids

    fun createAllManagers(
        deviceId: String,
        onReady: (ServiceManager) -> Unit,
    ): List<ServiceManager> = listOf(
        BatteryManager(deviceId, onReady),
        BPSManager(deviceId, onReady),
        CSCManager(deviceId, onReady),
        CGMManager(deviceId, onReady),
        DFSManager(deviceId, onReady),
        GLSManager(deviceId, onReady),
        HTSManager(deviceId, onReady),
        HRSManager(deviceId, onReady),
        RSCSManager(deviceId, onReady),
        ThroughputManager(deviceId, onReady),
        UARTManager(deviceId, onReady),
        ChannelSoundingManager(deviceId, onReady),
        LBSManager(deviceId, onReady),
        DFUManager(DFU_SERVICE_UUID, deviceId, onReady),
        DFUManager(SMP_SERVICE_UUID, deviceId, onReady),
        DFUManager(MDS_SERVICE_UUID, deviceId, onReady),
        DFUManager(LEGACY_DFU_SERVICE_UUID, deviceId, onReady),
        DFUManager(EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID, deviceId, onReady),
    )
}
