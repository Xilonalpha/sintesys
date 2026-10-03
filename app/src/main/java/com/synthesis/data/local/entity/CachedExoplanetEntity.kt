package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.domain.model.ExoplanetCatalogSource
import com.synthesis.domain.model.TfopwgDisposition

@Entity(tableName = "cached_exoplanets")
data class CachedExoplanetEntity(
    @PrimaryKey val toiId: String,
    val ticId: String,
    val disposition: String,
    val catalogSource: String,
    val orbitalPeriodDays: Double?,
    val transitDurationHours: Double?,
    val transitDepthPpm: Double?,
    val planetRadiusEarth: Double?,
    val planetMassEarth: Double?,
    val insolationEarthFlux: Double?,
    val equilibriumTempK: Double?,
    val stellarTessMagnitude: Double?,
    val stellarDistancePc: Double?,
    val stellarEffTempK: Double?,
    val stellarRadiusSun: Double?,
    val createdDate: String?,
    val lastUpdated: String?,
    val worthInvestigatingScore: Float,
    val earthSimilarityIndex: Float?,
    val isInHabitableZone: Boolean,
    val isUnvettedCandidate: Boolean,
    val anomalyType: String?,
    val discoveryMethod: String?,
    val discoveryYear: Int?,
    val scoreReasoningJoined: String,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ExoplanetCandidate {
        val disp = try {
            TfopwgDisposition.valueOf(disposition)
        } catch (_: Exception) {
            TfopwgDisposition.UNKNOWN
        }
        val source = try {
            ExoplanetCatalogSource.valueOf(catalogSource)
        } catch (_: Exception) {
            ExoplanetCatalogSource.TESS
        }
        return ExoplanetCandidate(
            toiId = toiId,
            ticId = ticId,
            disposition = disp,
            catalogSource = source,
            orbitalPeriodDays = orbitalPeriodDays,
            transitDurationHours = transitDurationHours,
            transitDepthPpm = transitDepthPpm,
            planetRadiusEarth = planetRadiusEarth,
            planetMassEarth = planetMassEarth,
            insolationEarthFlux = insolationEarthFlux,
            equilibriumTempK = equilibriumTempK,
            stellarTessMagnitude = stellarTessMagnitude,
            stellarDistancePc = stellarDistancePc,
            stellarEffTempK = stellarEffTempK,
            stellarRadiusSun = stellarRadiusSun,
            createdDate = createdDate,
            lastUpdated = lastUpdated,
            worthInvestigatingScore = worthInvestigatingScore,
            earthSimilarityIndex = earthSimilarityIndex,
            isInHabitableZone = isInHabitableZone,
            isUnvettedCandidate = isUnvettedCandidate,
            anomalyType = anomalyType,
            discoveryMethod = discoveryMethod,
            discoveryYear = discoveryYear,
            scoreReasoning = if (scoreReasoningJoined.isBlank()) emptyList() else scoreReasoningJoined.split(";;")
        )
    }

    companion object {
        fun fromDomain(candidate: ExoplanetCandidate): CachedExoplanetEntity {
            return CachedExoplanetEntity(
                toiId = candidate.toiId,
                ticId = candidate.ticId,
                disposition = candidate.disposition.name,
                catalogSource = candidate.catalogSource.name,
                orbitalPeriodDays = candidate.orbitalPeriodDays,
                transitDurationHours = candidate.transitDurationHours,
                transitDepthPpm = candidate.transitDepthPpm,
                planetRadiusEarth = candidate.planetRadiusEarth,
                planetMassEarth = candidate.planetMassEarth,
                insolationEarthFlux = candidate.insolationEarthFlux,
                equilibriumTempK = candidate.equilibriumTempK,
                stellarTessMagnitude = candidate.stellarTessMagnitude,
                stellarDistancePc = candidate.stellarDistancePc,
                stellarEffTempK = candidate.stellarEffTempK,
                stellarRadiusSun = candidate.stellarRadiusSun,
                createdDate = candidate.createdDate,
                lastUpdated = candidate.lastUpdated,
                worthInvestigatingScore = candidate.worthInvestigatingScore,
                earthSimilarityIndex = candidate.earthSimilarityIndex,
                isInHabitableZone = candidate.isInHabitableZone,
                isUnvettedCandidate = candidate.isUnvettedCandidate,
                anomalyType = candidate.anomalyType,
                discoveryMethod = candidate.discoveryMethod,
                discoveryYear = candidate.discoveryYear,
                scoreReasoningJoined = candidate.scoreReasoning.joinToString(";;")
            )
        }
    }
}
