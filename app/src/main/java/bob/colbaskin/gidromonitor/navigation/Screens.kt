package bob.colbaskin.gidromonitor.navigation

import kotlinx.serialization.Serializable

interface Screens {
    @Serializable
    data object Onboarding : Screens

    @Serializable
    data object Analytics : Screens

    @Serializable
    data object Events : Screens

    @Serializable
    data class AnalysisDetails(val analysisId: String) : Screens

    @Serializable
    data class Comparison(val analysisId: String) : Screens
}
