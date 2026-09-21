package bob.colbaskin.gidromonitor.features.map.domain.model

enum class BaseMapStyle(
    val label: String,
    private val configuredStyleUrl: String
) {
    SATELLITE(
        label = "Спутник",
        configuredStyleUrl = "https://api.maptiler.com/maps/01a0c3c5-31ca-706b-8d23-08391d4d1d8c/style.json?key=I45PE7YnKzVVaH92vG7h"
    ),
    STREETS(
        label = "Векторная карта",
        configuredStyleUrl = "https://api.maptiler.com/maps/01a0c3c7-941c-7410-b2e2-48ad23ab0a11/style.json?key=I45PE7YnKzVVaH92vG7h"
    );

    fun styleUrl(): String = configuredStyleUrl
}
