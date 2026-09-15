package com.synthesis.data.remote.dto

import com.google.gson.annotations.SerializedName

// NASA NEO Feed Response
// Endpoint: GET /neo/rest/v1/feed?start_date={}&end_date={}&api_key={}
data class NasaNeoFeedResponse(
    @SerializedName("element_count") val elementCount: Int?,
    @SerializedName("near_earth_objects") val nearEarthObjects: Map<String, List<NasaNeoObject>>?
)

data class NasaNeoObject(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("nasa_jpl_url") val nasaJplUrl: String?,
    @SerializedName("absolute_magnitude_h") val absoluteMagnitudeH: Double?,
    @SerializedName("estimated_diameter") val estimatedDiameter: NasaEstimatedDiameter?,
    @SerializedName("is_potentially_hazardous_asteroid") val isPotentiallyHazardous: Boolean?,
    @SerializedName("close_approach_data") val closeApproachData: List<NasaCloseApproachData>?,
    @SerializedName("orbital_data") val orbitalData: NasaOrbitalData?
)

data class NasaEstimatedDiameter(
    @SerializedName("kilometers") val kilometers: NasaDiameterMinMax?,
    @SerializedName("meters") val meters: NasaDiameterMinMax?
)

data class NasaDiameterMinMax(
    @SerializedName("estimated_diameter_min") val min: Double?,
    @SerializedName("estimated_diameter_max") val max: Double?
)

data class NasaCloseApproachData(
    @SerializedName("close_approach_date") val closeApproachDate: String?,
    @SerializedName("close_approach_date_full") val closeApproachDateFull: String?,
    @SerializedName("epoch_date_close_approach") val epochDateCloseApproach: Long?,
    @SerializedName("relative_velocity") val relativeVelocity: NasaRelativeVelocity?,
    @SerializedName("miss_distance") val missDistance: NasaMissDistance?,
    @SerializedName("orbiting_body") val orbitingBody: String?
)

data class NasaRelativeVelocity(
    @SerializedName("kilometers_per_second") val kmPerSecond: String?,
    @SerializedName("kilometers_per_hour") val kmPerHour: String?,
    @SerializedName("miles_per_hour") val milesPerHour: String?
)

data class NasaMissDistance(
    @SerializedName("astronomical") val astronomical: String?,
    @SerializedName("lunar") val lunar: String?,
    @SerializedName("kilometers") val kilometers: String?,
    @SerializedName("miles") val miles: String?
)

data class NasaOrbitalData(
    @SerializedName("orbit_id") val orbitId: String?,
    @SerializedName("orbit_determination_date") val orbitDeterminationDate: String?,
    @SerializedName("first_observation_date") val firstObservationDate: String?,
    @SerializedName("last_observation_date") val lastObservationDate: String?,
    @SerializedName("orbital_period") val orbitalPeriod: String?,
    @SerializedName("perihelion_distance") val perihelionDistance: String?,
    @SerializedName("aphelion_distance") val aphelionDistance: String?,
    @SerializedName("eccentricity") val eccentricity: String?
)

// NASA APOD Response
// Endpoint: GET /planetary/apod?api_key={}&date={}
data class NasaApodResponse(
    @SerializedName("date") val date: String?,
    @SerializedName("explanation") val explanation: String?,
    @SerializedName("hdurl") val hdurl: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("service_version") val serviceVersion: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("copyright") val copyright: String?
)
