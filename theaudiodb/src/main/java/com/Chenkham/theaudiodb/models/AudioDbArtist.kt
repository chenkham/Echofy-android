package com.Chenkham.theaudiodb.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioDbArtistResponse(
    val artists: List<AudioDbArtist>? = null,
)

@Serializable
data class AudioDbArtist(
    @SerialName("idArtist") val idArtist: String? = null,
    @SerialName("strArtist") val strArtist: String? = null,
    @SerialName("strBiographyEN") val biographyEn: String? = null,
    @SerialName("strArtistThumb") val thumb: String? = null,
    @SerialName("strArtistFanart") val fanart: String? = null,
    @SerialName("strArtistFanart2") val fanart2: String? = null,
    @SerialName("strArtistFanart3") val fanart3: String? = null,
    @SerialName("strArtistLogo") val logo: String? = null,
    @SerialName("strArtistBanner") val banner: String? = null,
    @SerialName("strGenre") val genre: String? = null,
    @SerialName("strStyle") val style: String? = null,
    @SerialName("strMood") val mood: String? = null,
    @SerialName("intFormedYear") val formedYear: String? = null,
    @SerialName("strCountry") val country: String? = null,
    @SerialName("strWebsite") val website: String? = null,
)

@Serializable
data class AudioDbAlbumResponse(
    val album: List<AudioDbAlbum>? = null,
)

@Serializable
data class AudioDbAlbum(
    @SerialName("idAlbum") val idAlbum: String? = null,
    @SerialName("strAlbum") val strAlbum: String? = null,
    @SerialName("strArtist") val strArtist: String? = null,
    @SerialName("intYearReleased") val yearReleased: String? = null,
    @SerialName("strAlbumThumb") val thumb: String? = null,
    @SerialName("strDescriptionEN") val descriptionEn: String? = null,
    @SerialName("strGenre") val genre: String? = null,
)
