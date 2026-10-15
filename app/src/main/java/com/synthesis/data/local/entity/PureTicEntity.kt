package com.synthesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synthesis.domain.model.xilon.PureTicCandidate

@Entity(tableName = "pure_tic_candidates")
data class PureTicEntity(
    @PrimaryKey val ticId: String,
    val name: String,
    val sector: String,
    val apparentMag: Double,
    val estimatedPeriodDays: Double?,
    val transitDurationHours: Double,
    val transitDepthPpm: Double,
    val stellarTeffK: Double,
    val stellarRadiusSun: Double,
    val stellarMassSun: Double,
    val distancePc: Double,
    val discoveryOrigin: String,
    val whyNoToi: String,
    val potentialPlanetaryType: String,
    val isInHabitableZone: Boolean,
    val scientificPriority: String,
    val scientificNotes: String,
    val isLiveIngested: Boolean,
    val ingestionTimestamp: Long,
    val snrSigma: Double,
    val profileSymmetryDeltaSigma: Double
) {
    fun toDomain(): PureTicCandidate {
        return PureTicCandidate(
            ticId = ticId,
            name = name,
            sector = sector,
            apparentMag = apparentMag,
            estimatedPeriodDays = estimatedPeriodDays,
            transitDurationHours = transitDurationHours,
            transitDepthPpm = transitDepthPpm,
            stellarTeffK = stellarTeffK,
            stellarRadiusSun = stellarRadiusSun,
            stellarMassSun = stellarMassSun,
            distancePc = distancePc,
            discoveryOrigin = discoveryOrigin,
            whyNoToi = whyNoToi,
            potentialPlanetaryType = potentialPlanetaryType,
            isInHabitableZone = isInHabitableZone,
            scientificPriority = scientificPriority,
            scientificNotes = scientificNotes,
            isLiveIngested = isLiveIngested,
            ingestionTimestamp = ingestionTimestamp,
            snrSigma = snrSigma,
            profileSymmetryDeltaSigma = profileSymmetryDeltaSigma
        )
    }

    companion object {
        fun fromDomain(c: PureTicCandidate): PureTicEntity {
            return PureTicEntity(
                ticId = c.ticId,
                name = c.name,
                sector = c.sector,
                apparentMag = c.apparentMag,
                estimatedPeriodDays = c.estimatedPeriodDays,
                transitDurationHours = c.transitDurationHours,
                transitDepthPpm = c.transitDepthPpm,
                stellarTeffK = c.stellarTeffK,
                stellarRadiusSun = c.stellarRadiusSun,
                stellarMassSun = c.stellarMassSun,
                distancePc = c.distancePc,
                discoveryOrigin = c.discoveryOrigin,
                whyNoToi = c.whyNoToi,
                potentialPlanetaryType = c.potentialPlanetaryType,
                isInHabitableZone = c.isInHabitableZone,
                scientificPriority = c.scientificPriority,
                scientificNotes = c.scientificNotes,
                isLiveIngested = c.isLiveIngested,
                ingestionTimestamp = c.ingestionTimestamp,
                snrSigma = c.snrSigma,
                profileSymmetryDeltaSigma = c.profileSymmetryDeltaSigma
            )
        }
    }
}
