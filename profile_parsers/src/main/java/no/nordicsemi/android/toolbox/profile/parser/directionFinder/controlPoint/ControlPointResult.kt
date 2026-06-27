package no.nordicsemi.android.toolbox.profile.parser.directionFinder.controlPoint

sealed class ControlPointResult

data class ControlPointCheckModeSuccess(
    val modes: List<ControlPointMode>
) : ControlPointResult()

data object ControlPointCheckModeError : ControlPointResult()

data object ControlPointChangeModeSuccess : ControlPointResult()

data object ControlPointChangeModeError : ControlPointResult()
