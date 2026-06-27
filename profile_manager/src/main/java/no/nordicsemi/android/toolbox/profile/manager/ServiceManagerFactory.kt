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

    private val serviceManagers = mapOf(
        BATTERY_SERVICE_UUID to ::BatteryManager,
        BPS_SERVICE_UUID to ::BPSManager,
        CSC_SERVICE_UUID to ::CSCManager,
        CGMS_SERVICE_UUID to ::CGMManager,
        DF_SERVICE_UUID to ::DFSManager,
        GLS_SERVICE_UUID to ::GLSManager,
        HTS_SERVICE_UUID to ::HTSManager,
        HRS_SERVICE_UUID to ::HRSManager,
        RSCS_SERVICE_UUID to ::RSCSManager,
        THROUGHPUT_SERVICE_UUID to ::ThroughputManager,
        UART_SERVICE_UUID to ::UARTManager,
        CHANNEL_SOUND_SERVICE_UUID to ::ChannelSoundingManager,
        LBS_SERVICE_UUID to ::LBSManager,
        DFU_SERVICE_UUID  to ::DFUManager,
        SMP_SERVICE_UUID to ::DFUManager,
        MDS_SERVICE_UUID to ::DFUManager,
        LEGACY_DFU_SERVICE_UUID to ::DFUManager,
        EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID to ::DFUManager,

        // Add more service UUIDs to handler mappings as needed
    ).mapKeys { it.key }

    fun createServiceManager(serviceUuid: Uuid): ServiceManager? {
        return serviceManagers[serviceUuid]?.invoke()
    }
}
