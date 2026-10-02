package com.synthesis.engine.cosmology

import com.synthesis.domain.model.cosmology.BlackHoleTarget
import com.synthesis.domain.model.cosmology.RelativisticBlackHoleProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class BlackHoleRelativityEngine @Inject constructor() {

    companion object {
        const val G = 6.67430e-11
        const val C = 2.99792458e8
        const val SOLAR_MASS_KG = 1.98847e30
        const val HBAR = 1.054571817e-34
        const val K_BOLTZMANN = 1.380649e-23
        const val METERS_PER_LY = 9.460730472e15
        const val RAD_TO_MICROARCSEC = 2.06265e11
    }

    fun getBenchmarkBlackHoles(): List<BlackHoleTarget> {
        return listOf(
            BlackHoleTarget(
                id = "sagittarius_a_star",
                name = "Sagittarius A* (Centrul Căii Lactee)",
                classification = "Gaură Neagră Supermasivă (SMBH)",
                massSolar = 4.154e6,
                spinParameterAStar = 0.90, // EHT 2024 polarized emission constraints
                distanceLightYears = 26700.0, // ~8.2 kpc
                constellation = "Săgetătorul",
                hostSystem = "Nucleul Galactic al Căii Lactee",
                accretionRateEddingtonFraction = 1.0e-8, // Quiescent state
                jetLorentzFactor = null,
                discoveryMethod = "Orbita stelei S2 (Genzel & Ghez - Nobel 2020) & Imagine EHT",
                description = "Monstrul din centrul galaxiei noastre. În 2022, Event Horizon Telescope a fotografiat umbra inelului său de fotoni."
            ),
            BlackHoleTarget(
                id = "m87_star",
                name = "Messier 87* (M87* Quasar)",
                classification = "Gaură Neagră Supermasivă Extremă",
                massSolar = 6.5e9,
                spinParameterAStar = 0.94, // EHT relativistic jet launching spin
                distanceLightYears = 53.5e6, // 16.4 Mpc in Virgo Cluster
                constellation = "Fecioara",
                hostSystem = "Galaxia eliptică gigantă M87",
                accretionRateEddingtonFraction = 1.5e-3,
                jetLorentzFactor = 6.3, // Superluminal relativistic jet
                discoveryMethod = "Prima imagine directă a orizontului de evenimente (EHT 2019)",
                description = "Prima gaură neagră fotografiată vreodată de omenire. Emite un jet relativist de plasmă ce se întinde pe 5.000 de ani-lumină."
            ),
            BlackHoleTarget(
                id = "cygnus_x1",
                name = "Cygnus X-1",
                classification = "Gaură Neagră de Masă Stelară (Microquasar)",
                massSolar = 21.2,
                spinParameterAStar = 0.98, // Extremely close to theoretical Thorne limit
                distanceLightYears = 7200.0,
                constellation = "Lebăda",
                hostSystem = "Sistem binar cu supergiganta albastră HDE 226868",
                accretionRateEddingtonFraction = 0.04,
                jetLorentzFactor = 1.4,
                discoveryMethod = "Emisie de raze X tari & Spectroscopie optică (1964/1971)",
                description = "Prima gaură neagră confirmată fizic în istorie. Rotația sa este atât de rapidă încât spațiul din jur este târât la viteze relativiste (Frame-dragging)."
            ),
            BlackHoleTarget(
                id = "gw150914",
                name = "GW150914 (Prima Fuziune LIGO)",
                classification = "Rămășiță Fuziune Binară de Găuri Negre",
                massSolar = 62.0, // Final remnant (36 M_sun + 29 M_sun -> 62 M_sun + 3 M_sun radiate)
                spinParameterAStar = 0.68,
                distanceLightYears = 1.3e9,
                constellation = "Peștele Zburător",
                hostSystem = "Fuziune binară izolată în spațiul profund",
                accretionRateEddingtonFraction = 0.0,
                jetLorentzFactor = null,
                discoveryMethod = "Unde Gravitaționale LIGO / Virgo (14 Septembrie 2015)",
                description = "Coliziunea a două găuri negre care a transformat 3 mase solare de energie pură în unde gravitaționale într-o fracțiune de secundă."
            ),
            BlackHoleTarget(
                id = "gaia_bh1",
                name = "Gaia BH1",
                classification = "Gaură Neagră Dormantă Apropiată",
                massSolar = 9.62,
                spinParameterAStar = 0.15,
                distanceLightYears = 1560.0,
                constellation = "Ophiuchus",
                hostSystem = "Sistem binar cu o stea similară Soarelui (G-type)",
                accretionRateEddingtonFraction = 1.0e-11, // Dormant, no X-ray accretion
                jetLorentzFactor = null,
                discoveryMethod = "Astrometrie de precizie înaltă ESA Gaia DR3 (2022)",
                description = "Cea mai apropiată gaură neagră cunoscută de planeta Pământ. Este complet invizibilă în raze X și a fost detectată doar prin mișcarea orbitală a stelei însoțitoare."
            ),
            BlackHoleTarget(
                id = "ton618",
                name = "TON 618 (Hypermasivă)",
                classification = "Gaură Neagră Ultramasivă (UMBH)",
                massSolar = 6.6e10, // 66 billion solar masses!
                spinParameterAStar = 0.99,
                distanceLightYears = 18.2e9,
                constellation = "Câinii de Vânătoare",
                hostSystem = "Quasar Hiperluminos activ la z = 2.219",
                accretionRateEddingtonFraction = 0.75,
                jetLorentzFactor = 12.0,
                discoveryMethod = "Lățimea liniei de emisie H-beta & Luminozitate Quasar",
                description = "Una dintre cele mai mari găuri negre descoperite în universul observabil. Diametrul orizontului său depășește de 40 de ori orbita planetei Neptun."
            )
        )
    }

    /**
     * Solves the exact Kerr and Schwarzschild relativistic equations
     */
    fun computeRelativisticProfile(target: BlackHoleTarget): RelativisticBlackHoleProfile {
        val massKg = target.massSolar * SOLAR_MASS_KG
        val aStar = target.spinParameterAStar.coerceIn(0.0, 0.998)

        // Gravitational radius Rg = GM / c^2
        val rgMeters = (G * massKg) / (C * C)
        val rgKm = rgMeters / 1000.0

        // Schwarzschild radius Rs = 2 GM / c^2
        val rsKm = 2.0 * rgKm

        // Kerr Event Horizon r+ = Rg * (1 + sqrt(1 - a*^2))
        val rPlusKm = rgKm * (1.0 + sqrt(1.0 - aStar.pow(2.0)))

        // Ergosphere equatorial radius = 2 Rg
        val rErgoEquatorKm = 2.0 * rgKm

        // Innermost Stable Circular Orbit (ISCO) Bardeen et al. (1972)
        val z1 = 1.0 + (1.0 - aStar.pow(2.0)).pow(1.0 / 3.0) *
                ((1.0 + aStar).pow(1.0 / 3.0) + (1.0 - aStar).pow(1.0 / 3.0))
        val z2 = sqrt(3.0 * aStar.pow(2.0) + z1.pow(2.0))
        val rIscoKm = rgKm * (3.0 + z2 - sqrt((3.0 - z1) * (3.0 + z1 + 2.0 * z2)))

        // Photon sphere radius r_ph (prograde)
        val rPhotonKm = 2.0 * rgKm * (1.0 + cos((2.0 / 3.0) * acos(-aStar)))

        // Shadow angular diameter on Earth theta = sqrt(27) * Rs / D
        val distMeters = target.distanceLightYears * METERS_PER_LY
        val shadowPhysicalRadiusMeters = sqrt(27.0) * (rsKm * 1000.0 / 2.0)
        val shadowAngularRad = (2.0 * shadowPhysicalRadiusMeters) / distMeters
        val shadowMicroArcsec = shadowAngularRad * RAD_TO_MICROARCSEC

        // Hawking radiation temperature: T_H = hbar * c^3 / (8 * pi * G * M * k_B)
        val hawkingTempKelvin = (HBAR * C.pow(3.0)) / (8.0 * PI * G * massKg * K_BOLTZMANN)

        // Evaporation lifetime in years: t_evap = (5120 * pi * G^2 * M^3) / (hbar * c^4)
        val tEvapSec = (5120.0 * PI * G.pow(2.0) * massKg.pow(3.0)) / (HBAR * C.pow(4.0))
        val tEvapYears = tEvapSec / (365.25 * 86400.0)

        // Gravitational redshift at 1.5 Rg: 1 + z = 1 / sqrt(1 - 2M/r)
        val rTest = 3.0 * rgKm // 1.5 * Rs
        val redshiftFactor = 1.0 / sqrt(max(0.01, 1.0 - (rsKm / rTest))) - 1.0

        // Penrose Process Maximum Energy Extraction Efficiency:
        // eta = 1 - sqrt(0.5 * (1 + sqrt(1 - a*^2)))
        val penroseEfficiencyPercent = (1.0 - sqrt(0.5 * (1.0 + sqrt(1.0 - aStar.pow(2.0))))) * 100.0

        // Orbital velocity at ISCO: v_isco = c / sqrt(r_isco / Rg + a*)
        val iscoVelFraction = 1.0 / sqrt((rIscoKm / rgKm) + aStar)

        // GW details if merger
        val gwChirpHz = if (target.id == "gw150914") 250.0 else null
        val gwRadiatedSolar = if (target.id == "gw150914") 3.0 else null

        return RelativisticBlackHoleProfile(
            target = target,
            schwarzschildRadiusKm = (rsKm * 100.0).roundToLong() / 100.0,
            eventHorizonRadiusKm = (rPlusKm * 100.0).roundToLong() / 100.0,
            ergosphereEquatorialRadiusKm = (rErgoEquatorKm * 100.0).roundToLong() / 100.0,
            photonSphereRadiusKm = (rPhotonKm * 100.0).roundToLong() / 100.0,
            iscoRadiusKm = (rIscoKm * 100.0).roundToLong() / 100.0,
            shadowDiameterMicroArcsec = (shadowMicroArcsec * 100.0).roundToLong() / 100.0,
            hawkingTemperatureKelvin = hawkingTempKelvin,
            evaporationLifetimeYears = tEvapYears,
            gravitationalRedshiftAt1_5Rg = (redshiftFactor * 100.0).roundToLong() / 100.0,
            penroseEnergyEfficiencyPercent = (penroseEfficiencyPercent * 10.0).roundToLong() / 10.0,
            gravitationalWaveChirpFrequencyHz = gwChirpHz,
            gravitationalWaveRadiatedMassSolar = gwRadiatedSolar,
            iscoVelocityFractionC = (iscoVelFraction * 1000.0).roundToLong() / 1000.0
        )
    }
}
