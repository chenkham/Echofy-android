package com.Chenkham.Echofy.models

import androidx.annotation.DrawableRes
import com.Chenkham.Echofy.R
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
 * List of all built-in wallpapers available in the app
 */
object BuiltInWallpapers {
    val all: List<BuiltInWallpaper> = listOf(
        // Abstract / Modern
        BuiltInWallpaper(
            id = "abstract_dark",
            resourceId = R.drawable.wallpaper_abstract_dark,
            category = WallpaperCategory.ABSTRACT,
            name = "Dark Abstract Flows"
        ),
        BuiltInWallpaper(
            id = "nature_mist",
            resourceId = R.drawable.wallpaper_nature_mist,
            category = WallpaperCategory.NATURE,
            name = "Misty Mountains"
        ),
        BuiltInWallpaper(
            id = "geo_colorful",
            resourceId = R.drawable.wallpaper_geometric_colorful,
            category = WallpaperCategory.ABSTRACT,
            name = "Geometric Prism"
        ),
        BuiltInWallpaper(
            id = "min_gradient",
            resourceId = R.drawable.wallpaper_minimalist_gradient,
            category = WallpaperCategory.ABSTRACT,
            name = "Lo-Fi Gradient"
        ),
        
        // Winter (Legacy)
        BuiltInWallpaper(
            id = "winter_1",
            resourceId = R.drawable.wallpaper_winter_1,
            category = WallpaperCategory.WINTER,
            name = "Winter River"
        ),
        BuiltInWallpaper(
            id = "winter_2",
            resourceId = R.drawable.wallpaper_winter_2,
            category = WallpaperCategory.WINTER,
            name = "Night Village"
        ),
        
        // Autumn (Legacy)
        BuiltInWallpaper(
            id = "autumn_1",
            resourceId = R.drawable.wallpaper_autumn_1,
            category = WallpaperCategory.AUTUMN,
            name = "Golden Park"
        )
    )
    
    fun getByCategory(category: WallpaperCategory): List<BuiltInWallpaper> {
        return all.filter { it.category == category }
    }
    
    fun getById(id: String): BuiltInWallpaper? {
        return all.find { it.id == id }
    }
    
    val categories: List<WallpaperCategory>
        get() = all.map { it.category }.distinct()
}
