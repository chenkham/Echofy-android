package com.Chenkham.Echofy.utils

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Returns a standard Compose Shape based on the shape name.
 * Since MaterialShapes from Material3 Expressive is not available in stable versions,
 * we provide a simplified set of standard shapes.
 */
fun getSmallButtonShape(shapeName: String): Shape {
    return when (shapeName) {
        "Pill" -> MaterialShapes.Pill
        "Circle" -> MaterialShapes.Circle
        "Square" -> MaterialShapes.Square
        "Diamond" -> MaterialShapes.Diamond
        "Pentagon" -> MaterialShapes.Pentagon
        "Heart" -> MaterialShapes.Heart
        "Oval" -> MaterialShapes.Oval
        "Arch" -> MaterialShapes.Arch
        "SemiCircle" -> MaterialShapes.SemiCircle
        "Triangle" -> MaterialShapes.Triangle
        "Arrow" -> MaterialShapes.Arrow
        "Fan" -> MaterialShapes.Fan
        "Gem" -> MaterialShapes.Gem
        "Bun" -> MaterialShapes.Bun
        "Ghostish" -> MaterialShapes.Ghostish
        "Cookie4Sided" -> MaterialShapes.Cookie4Sided
        "Cookie6Sided" -> MaterialShapes.Cookie6Sided
        "Cookie7Sided" -> MaterialShapes.Cookie7Sided
        "Cookie9Sided" -> MaterialShapes.Cookie9Sided
        "Cookie12Sided" -> MaterialShapes.Cookie12Sided
        "Clover4Leaf" -> MaterialShapes.Clover4Leaf
        "Clover8Leaf" -> MaterialShapes.Clover8Leaf
        "Sunny" -> MaterialShapes.Sunny
        "VerySunny" -> MaterialShapes.VerySunny
        "Burst" -> MaterialShapes.Burst
        "SoftBurst" -> MaterialShapes.SoftBurst
        "Boom" -> MaterialShapes.Boom
        "SoftBoom" -> MaterialShapes.SoftBoom
        "Flower" -> MaterialShapes.Flower
        "PixelCircle" -> MaterialShapes.PixelCircle
        "PixelTriangle" -> MaterialShapes.PixelTriangle
        "Puffy" -> MaterialShapes.Puffy
        "PuffyDiamond" -> MaterialShapes.PuffyDiamond
        "Slanted" -> MaterialShapes.Slanted
        "ClamShell" -> MaterialShapes.ClamShell
        "Rounded" -> RoundedCornerShape(12.dp)
        else -> CircleShape // Default to CircleShape
    }
}

fun getPlayPauseShape(shapeName: String): Shape = getSmallButtonShape(shapeName)

fun getMiniPlayerThumbnailShape(shapeName: String): Shape = getSmallButtonShape(shapeName)

fun String.toShape(): Shape = getSmallButtonShape(this)

fun Shape.toShape(): Shape = this

object MaterialShapes {
    val Pill = CircleShape
    val Circle = CircleShape
    val Square = RectangleShape
    val Diamond = RoundedCornerShape(4.dp)
    val Pentagon = RoundedCornerShape(8.dp)
    val Heart = CircleShape
    val Oval = CircleShape
    val Arch = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val SemiCircle = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp)
    val Triangle = RoundedCornerShape(4.dp)
    val Arrow = RoundedCornerShape(4.dp)
    val Fan = RoundedCornerShape(topEnd = 16.dp)
    val Gem = RoundedCornerShape(8.dp)
    val Bun = RoundedCornerShape(8.dp)
    val Ghostish = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val Cookie4Sided = RoundedCornerShape(4.dp)
    val Cookie6Sided = RoundedCornerShape(6.dp)
    val Cookie7Sided = RoundedCornerShape(7.dp)
    val Cookie9Sided = RoundedCornerShape(9.dp)
    val Cookie12Sided = RoundedCornerShape(12.dp)
    val Clover4Leaf = RoundedCornerShape(16.dp)
    val Clover8Leaf = RoundedCornerShape(16.dp)
    val Sunny = CircleShape
    val VerySunny = CircleShape
    val Burst = CircleShape
    val SoftBurst = CircleShape
    val Boom = CircleShape
    val SoftBoom = CircleShape
    val Flower = CircleShape
    val PixelCircle = CircleShape
    val PixelTriangle = RectangleShape
    val Puffy = RoundedCornerShape(12.dp)
    val PuffyDiamond = RoundedCornerShape(8.dp)
    val Slanted = RoundedCornerShape(8.dp)
    val ClamShell = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
}