package no.nordicsemi.android.toolbox.lib.utils.spec

import kotlin.uuid.Uuid

// Bluetooth SIG-defined GATT services.

val HTS_SERVICE_UUID = Uuid.parse("00001809-0000-1000-8000-00805f9b34fb")
val BPS_SERVICE_UUID = Uuid.parse("00001810-0000-1000-8000-00805f9b34fb")
val CSC_SERVICE_UUID = Uuid.parse("00001816-0000-1000-8000-00805f9b34fb")
val CGMS_SERVICE_UUID = Uuid.parse("0000181F-0000-1000-8000-00805f9b34fb")
val GLS_SERVICE_UUID = Uuid.parse("00001808-0000-1000-8000-00805f9b34fb")
val HRS_SERVICE_UUID = Uuid.parse("0000180D-0000-1000-8000-00805f9b34fb")
val PRX_SERVICE_UUID = Uuid.parse("00001802-0000-1000-8000-00805f9b34fb")
val RSCS_SERVICE_UUID = Uuid.parse("00001814-0000-1000-8000-00805F9B34FB")
val UART_SERVICE_UUID = Uuid.parse("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
val BATTERY_SERVICE_UUID = Uuid.parse("0000180F-0000-1000-8000-00805f9b34fb")
val RANGING_SERVICE_UUID = Uuid.parse("0000185B-0000-1000-8000-00805F9B34FB")
val DIS_SERVICE_UUID = Uuid.parse("0000180A-0000-1000-8000-00805f9b34fb")

// Nordic specific service.
/**
 * LED Button Service.
 *
 * [Documentation](https://nrfconnectdocs.nordicsemi.com/ncs/latest/nrf/libraries/bluetooth/services/lbs.html)
 */
val LBS_SERVICE_UUID = Uuid.parse("00001523-1212-EFDE-1523-785FEABCD123")

/**
 * Direction and Distance Finding Service.
 *
 * [Documentation](https://nrfconnectdocs.nordicsemi.com/ncs/latest/nrf/libraries/bluetooth/services/ddfs.html)
 */
val DDF_SERVICE_UUID = Uuid.parse("21490000-494a-4573-98af-f126af76f490")

/**
 * Throughput Service.
 *
 * [Documentation](https://nrfconnectdocs.nordicsemi.com/ncs/latest/nrf/libraries/bluetooth/services/throughput.html)
 */
val THROUGHPUT_SERVICE_UUID = Uuid.parse("0483DADD-6C9D-6CA9-5D41-03AD4FFF4ABB")

/**
 * Secure DFU Service.
 *
 * This service was present in nRF5 SDK versions 12+.
 *
 * This is a 16-bit service UUID (0xFE59) with Base Bluetooth UUID.
 */
val DFU_SERVICE_UUID = Uuid.parse("0000FE59-0000-1000-8000-00805F9B34FB")

/**
 * Legacy DFU Service.
 *
 * This service was present in nRF5 SDK versions 4.3-11.
 */
val LEGACY_DFU_SERVICE_UUID = Uuid.parse("00001530-1212-EFDE-1523-785FEABCD123")

/**
 * Experimental Buttonless DFU Service from nRF5 SDK version 12 (only).
 */
val EXPERIMENTAL_BUTTONLESS_DFU_SERVICE_UUID = Uuid.parse("8E400001-F315-4F60-9FB8-838830DAEA50")

/**
 * Simple Management Service.
 *
 * [Documentation](https://nrfconnectdocs.nordicsemi.com/ncs/latest/zephyr/services/device_mgmt/smp_transport.html)
 */
val SMP_SERVICE_UUID = Uuid.parse("8D53DC1D-1DB7-4CD3-868B-8A527460AA84")

/**
 * Memfault Monitoring and Diagnostic Service.
 *
 * [Documentation](https://nrfconnectdocs.nordicsemi.com/ncs/latest/nrf/libraries/bluetooth/services/mds.html)
 */
val MDS_SERVICE_UUID = Uuid.parse("54220000-F6A5-4007-A371-722F4EBD8436")

/**
 * Custom, empty marker service advertised by the "Quick Start" sample for the nRF54L15 DK.
 * Its presence is used to show onboarding instructions for the sample.
 */
val QUICK_START_SERVICE_UUID = Uuid.parse("b2007aaa-c203-43a5-8b6f-a7f3d001a1e0")