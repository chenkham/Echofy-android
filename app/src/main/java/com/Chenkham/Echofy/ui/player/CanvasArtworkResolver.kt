/*
 * Echofy Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Orquestador de proveedores de canvas.
 * Usa Apple Music Artwork Provider como fuente principal.
 * Orden: Apple Music ÔåÆ Custom Canvas ÔåÆ Tidal (fallback)
 */

package com.Chenkham.Echofy.ui.player

import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.canvas.providers.AppleMusicArtworkProvider
import com.arturo254.opentune.canvas.providers.CustomCanvasProvider
import com.arturo254.opentune.canvas.providers.TidalCanvasProvider
import com.Chenkham.Echofy.constants.CanvasSource
import timber.log.Timber
import java.util.Locale

/**
 * Intenta obtener el canvas animado para la canci├│n en reproducci├│n.
 *
 * @param songTitleRaw  T├¡tulo tal como viene del MediaMetadata (puede tener feat., etc.)
 * @param artistNameRaw Nombre del artista tal como viene del MediaMetadata
 * @param albumName     Nombre del ├ílbum (opcional, mejora la precisi├│n)
 * @param source        Fuente a usar; [CanvasSource.AUTO] prueba todas en orden
 */
internal suspend fun fetchCanvasArtworkForPlayback(
    songTitleRaw: String,
    artistNameRaw: String,
    albumName: String? = null,
    source: CanvasSource = CanvasSource.AUTO,
): CanvasArtwork? {
    val songTitle = normalizeCanvasSongTitle(songTitleRaw)
    val artistName = normalizeCanvasArtistName(artistNameRaw)

    Timber.d("­ƒÄÁ Canvas Resolver - Buscando: $songTitle - $artistName (album=$albumName)")

    // Genera combinaciones (normalizado + raw) para mayor cobertura de b├║squeda
    val candidates = linkedSetOf(
        Triple(songTitle, artistName, albumName),
        Triple(songTitleRaw, artistName, albumName),
        Triple(songTitle, artistNameRaw, albumName),
        Triple(songTitleRaw, artistNameRaw, albumName),
    )

    // ­ƒöº Fix: adem├ís del artista primario, probar cada artista individual
    // (feat., colabs, "x", "&", etc.) porque el canvas puede estar guardado
    // solo con el artista secundario (ej: "Tito Double P, Peso Pluma" -> el
    // canvas est├í indexado ├║nicamente como "Peso Pluma").
    splitAndNormalizeArtists(artistNameRaw).forEach { singleArtist ->
        candidates += Triple(songTitle, singleArtist, albumName)
        candidates += Triple(songTitleRaw, singleArtist, albumName)
    }

    val filteredCandidates = candidates
        .filter { (song, artist, _) -> song.isNotBlank() && artist.isNotBlank() }

    return filteredCandidates.firstNotNullOfOrNull { (song, artist, album) ->
        fetchFromSource(song, artist, album, source)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
    }
}

private suspend fun fetchFromSource(
    song: String,
    artist: String,
    album: String?,
    source: CanvasSource,
): CanvasArtwork? {
    val result = when (source) {
        CanvasSource.AUTO -> {
            // ­ƒöÑ NUEVO ORDEN: Apple Music ÔåÆ Custom Canvas ÔåÆ Tidal (fallback)
            AppleMusicArtworkProvider.getBySongArtist(song, artist, album)
                ?: CustomCanvasProvider.getBySongArtist(
                    song = song,
                    artist = artist,
                    // ­ƒöº Fix: NO inventar el ├ílbum con el t├¡tulo de la canci├│n.
                    // Si no hay ├ílbum real, se pasa vac├¡o para que el matching
                    // de ├ílbum en CustomCanvasProvider simplemente no aplique
                    // en vez de comparar contra un valor incorrecto.
                    album = album.orEmpty()
                )
                ?: TidalCanvasProvider.getBySongArtist(song, artist, album)
        }

        CanvasSource.APPLE_MUSIC -> {
            AppleMusicArtworkProvider.getBySongArtist(song, artist, album)
        }

        CanvasSource.CUSTOM -> {
            CustomCanvasProvider.getBySongArtist(
                song = song,
                artist = artist,
                album = album.orEmpty()
            )
        }

        CanvasSource.TIDAL -> {
            TidalCanvasProvider.getBySongArtist(song, artist, album)
        }
    }

    if (result != null) {
        Timber.d("­ƒÄÁ Canvas Resolver - Ô£à Encontrado en ${source.name}: ${result.preferredAnimationUrl}")
    } else {
        Timber.d("­ƒÄÁ Canvas Resolver - ÔØî No encontrado para: $song - $artist")
    }

    return result
}

// ---------------------------------------------------------------------------
// Funciones de normalizaci├│n
// ---------------------------------------------------------------------------

internal fun normalizeCanvasSongTitle(raw: String): String =
    raw
        // Eliminar corchetes: [Official Video], [Remastered], etc.
        .replace(Regex("\\s*\\[[^]]*]"), "")
        // Eliminar feat. / ft. / featuring entre par├®ntesis
        .replace(
            Regex(
                "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        // Eliminar sufijos de tipo de contenido entre par├®ntesis
        .replace(
            Regex(
                "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        // Eliminar sufijos separados por guion
        .replace(
            Regex(
                "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun normalizeCanvasArtistName(raw: String): String =
    raw
        .split(
            Regex(
                "(?:\\s*,\\s*|\\s*&\\s*|\\s+├ù\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                RegexOption.IGNORE_CASE,
            ),
            limit = 2,
        )
        .firstOrNull().orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun splitAndNormalizeArtists(raw: String): List<String> =
    raw.split(
        Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+├ù\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        )
    )
        .map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }