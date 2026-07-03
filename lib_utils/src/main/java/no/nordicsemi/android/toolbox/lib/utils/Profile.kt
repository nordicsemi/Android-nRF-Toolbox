package no.nordicsemi.android.toolbox.lib.utils

enum class Profile {
    QUICK_START,
    BPS,
    CGM,
    CHANNEL_SOUNDING,
    CSC,
    DDFS,
    GLS,
    HRS,
    HTS,
    LBS,
    RSCS,
    //    PRX, TODO: Proximity is not implemented yet, it will be added in the future.
    BATTERY,
    THROUGHPUT,
    UART,
    DFU;

    override fun toString(): String = when (this) {
        QUICK_START -> "Quick Start"
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