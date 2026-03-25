package com.Chenkham.Echofy.instagram

import kotlinx.serialization.Serializable

@Serializable
data class InstaLoginResponse(
    val status: String,
    val logged_in_user: InstaUser? = null,
    val message: String? = null,
    val error_type: String? = null
)

@Serializable
data class InstaUser(
    val pk: Long,
    val username: String,
    val full_name: String,
    val profile_pic_url: String
)

@Serializable
data class InstaClipsResponse(
    val items: List<InstaClipItem> = emptyList(),
    val paging_info: PagingInfo? = null,
    val status: String
)

@Serializable
data class PagingInfo(
    val max_id: String? = null,
    val more_available: Boolean = false
)

@Serializable
data class InstaClipItem(
    val id: String,
    val code: String,
    val video_versions: List<VideoVersion>? = null,
    val image_versions2: ImageVersions? = null,
    val music_metadata: InstaMusicMetadata? = null, // Custom field mapping
    val organic_tracking_token: String? = null,
    val original_media_has_visual_reply_media: Boolean = false,
    // Add other fields as discovered/needed
    // Often music info is inside 'clip_metadata' or 'music_info'
    val music_info: InstaMusicInfo? = null,
    val original_sound_info: InstaOriginalSoundInfo? = null
) {
    fun getBestVideoUrl(): String? {
        return video_versions?.maxByOrNull { it.width * it.height }?.url
    }
    
    fun getThumbnailUrl(): String? {
        return image_versions2?.candidates?.maxByOrNull { it.width * it.height }?.url
    }
}

@Serializable
data class VideoVersion(
    val width: Int,
    val height: Int,
    val url: String
)

@Serializable
data class ImageVersions(
    val candidates: List<ImageCandidate>
)

@Serializable
data class ImageCandidate(
    val width: Int,
    val height: Int,
    val url: String
)

@Serializable
data class InstaMusicInfo(
    val music_asset_info: InstaMusicAssetInfo? = null
)

@Serializable
data class InstaOriginalSoundInfo(
    val audio_asset_id: String,
    val music_canonical_id: String?,
    val original_audio_title: String,
    val ig_artist: InstaUser?
)

@Serializable
data class InstaMusicAssetInfo(
    val title: String,
    val display_artist: String,
    val cover_artwork_uri: String? = null
)

@Serializable
data class InstaMusicMetadata(
    val music_info: InstaMusicInfo? = null,
    val original_sound_info: InstaOriginalSoundInfo? = null
)
