package no.nordicsemi.android.toolbox.lib.utils

enum class Profile {
    BPS,
    CGM,
    CHANNEL_SOUNDING,
    CSC,
    DDFS,
    DFU,
    GLS,
    HRS,
    HTS,
    LBS,
    RSCS,

    //    PRX, TODO: Proximity is not implemented yet, it will be added in the future.
    BATTERY,
    THROUGHPUT,
    UART;

    override fun toString(): String = when (this) {
        BPS -> "Blood Pressure"
        CGM -> "Continuous Glucose"
        CHANNEL_SOUNDING -> "Channel Sounding"
        CSC -> "Cycling Speed and Cadence"
        DDFS -> "Distance Measurement"
        GLS -> "Glucose"
        HRS -> "Heart Rate"
        HTS -> "Health Thermometer"
        LBS -> "LED Button"
        RSCS -> "Running Speed and Cadence"
        BATTERY -> "Battery"
        THROUGHPUT -> "Throughput Service"
        UART -> "UART"
        DFU -> "Device Firmware Update"
    }
}