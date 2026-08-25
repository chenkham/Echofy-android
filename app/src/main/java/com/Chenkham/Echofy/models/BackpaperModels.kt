package com.Chenkham.Echofy.models

import androidx.annotation.DrawableRes
import com.Chenkham.Echofy.constants.WallpaperCategory

/**
 * Represents a built-in wallpaper bundled with the app
 */
data class BuiltInWallpaper(
    val id: String,
    @DrawableRes val resourceId: Int,
    val category: WallpaperCategory,
    val name: String
)

/**
 * List of all built-in wallpapers available in the app (Removed default stock pictures)
 */
object BuiltInWallpapers {
    val all: List<BuiltInWallpaper> = emptyList()
    
    fun getByCategory(category: WallpaperCategory): List<BuiltInWallpaper> {
        return all.filter { it.category == category }
    }
    
    fun getById(id: String): BuiltInWallpaper? {
        return all.find { it.id == id }
    }
    
    val categories: List<WallpaperCategory>
        get() = all.map { it.category }.distinct()
}
