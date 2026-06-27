package no.nordicsemi.android.toolbox.profile.parser.prx

object AlertLevelInputParser {

    fun parse(alarmLevel: AlarmLevel): Byte {
        return alarmLevel.value
    }
}