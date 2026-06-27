package no.nordicsemi.android.toolbox.profile.parser.prx

data class PRXData(
    val localAlarmLevel: AlarmLevel = AlarmLevel.NONE,
    val isRemoteAlarm: Boolean = false,
    val linkLossAlarmLevel: AlarmLevel = AlarmLevel.HIGH
)