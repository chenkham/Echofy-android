package com.Chenkham.Echofy.audio

data class AutoEqProfile(
    val brand: String,
    val model: String,
    val type: String, // "Over-Ear", "In-Ear / TWS", "IEM"
    val levels: List<Float>, // 5-band normalized 0..1 (0.5 = neutral/flat)
    val description: String = "Calibrated to Harman Target for studio-neutral balance"
)

object AutoEqManager {
    val profiles: List<AutoEqProfile> = listOf(
        // Apple
        AutoEqProfile("Apple", "AirPods Pro 2", "In-Ear / TWS", listOf(0.52f, 0.48f, 0.54f, 0.62f, 0.58f), "Tightens sub-bass and brings vocals forward"),
        AutoEqProfile("Apple", "AirPods Pro (1st Gen)", "In-Ear / TWS", listOf(0.54f, 0.50f, 0.52f, 0.64f, 0.56f), "Compensates upper treble roll-off"),
        AutoEqProfile("Apple", "AirPods Max", "Over-Ear", listOf(0.48f, 0.52f, 0.56f, 0.60f, 0.55f), "Smoothes upper mid-range peaks"),
        AutoEqProfile("Apple", "AirPods 3", "In-Ear / TWS", listOf(0.60f, 0.54f, 0.48f, 0.56f, 0.52f), "Controls loose bass & lifts clarity"),
        AutoEqProfile("Apple", "EarPods (3.5mm / Lightning)", "In-Ear / TWS", listOf(0.64f, 0.52f, 0.48f, 0.54f, 0.50f), "Compensates low sub-bass loss"),

        // Sony
        AutoEqProfile("Sony", "WH-1000XM5", "Over-Ear", listOf(0.42f, 0.48f, 0.58f, 0.64f, 0.60f), "Cleans bloated mid-bass, expands soundstage"),
        AutoEqProfile("Sony", "WH-1000XM4", "Over-Ear", listOf(0.40f, 0.46f, 0.60f, 0.65f, 0.58f), "Tames heavy mid-bass boom for crisp vocals"),
        AutoEqProfile("Sony", "WF-1000XM5", "In-Ear / TWS", listOf(0.46f, 0.50f, 0.55f, 0.62f, 0.58f), "Neutralizes low-end for balanced transient response"),
        AutoEqProfile("Sony", "WF-1000XM4", "In-Ear / TWS", listOf(0.44f, 0.48f, 0.58f, 0.63f, 0.56f), "Brightens vocal region and improves airiness"),
        AutoEqProfile("Sony", "LinkBuds S", "In-Ear / TWS", listOf(0.48f, 0.50f, 0.54f, 0.60f, 0.57f), "Harman Target correction for natural tone"),
        AutoEqProfile("Sony", "MDR-7506", "Over-Ear", listOf(0.56f, 0.52f, 0.48f, 0.44f, 0.46f), "Reduces treble brightness and adds warmth"),

        // Samsung
        AutoEqProfile("Samsung", "Galaxy Buds 2 Pro", "In-Ear / TWS", listOf(0.50f, 0.52f, 0.54f, 0.58f, 0.56f), "Refines Harman curve perfection"),
        AutoEqProfile("Samsung", "Galaxy Buds Pro", "In-Ear / TWS", listOf(0.48f, 0.50f, 0.55f, 0.57f, 0.54f), "Smoothes sibilance in female vocals"),
        AutoEqProfile("Samsung", "Galaxy Buds FE", "In-Ear / TWS", listOf(0.46f, 0.50f, 0.56f, 0.60f, 0.55f), "Tightens low end and improves mids"),
        AutoEqProfile("Samsung", "Galaxy Buds Live", "In-Ear / TWS", listOf(0.58f, 0.52f, 0.50f, 0.58f, 0.52f), "Compensates open-ear bass leak"),

        // Bose
        AutoEqProfile("Bose", "QuietComfort Ultra", "Over-Ear", listOf(0.46f, 0.50f, 0.58f, 0.60f, 0.56f), "Controls sub-bass prominence for flat response"),
        AutoEqProfile("Bose", "QC45 / QC SE", "Over-Ear", listOf(0.48f, 0.52f, 0.56f, 0.58f, 0.54f), "Restores natural mids and eases peaky treble"),
        AutoEqProfile("Bose", "QC35 II", "Over-Ear", listOf(0.52f, 0.50f, 0.54f, 0.56f, 0.52f), "Boosts modern clarity while keeping comfort"),
        AutoEqProfile("Bose", "NC 700", "Over-Ear", listOf(0.54f, 0.52f, 0.50f, 0.54f, 0.56f), "Adds punchy bass weight and vocal depth"),

        // Sennheiser
        AutoEqProfile("Sennheiser", "HD 600", "Over-Ear", listOf(0.60f, 0.55f, 0.50f, 0.52f, 0.55f), "Extends sub-bass while keeping legendary mids"),
        AutoEqProfile("Sennheiser", "HD 650 / HD 6XX", "Over-Ear", listOf(0.58f, 0.54f, 0.50f, 0.54f, 0.56f), "Opens up treble air without veil"),
        AutoEqProfile("Sennheiser", "HD 560S", "Over-Ear", listOf(0.54f, 0.50f, 0.50f, 0.52f, 0.50f), "Flattens analytical upper-mid energy"),
        AutoEqProfile("Sennheiser", "Momentum 4 Wireless", "Over-Ear", listOf(0.44f, 0.48f, 0.56f, 0.60f, 0.58f), "Tames bass emphasis for audiophile precision"),
        AutoEqProfile("Sennheiser", "Momentum TW 3", "In-Ear / TWS", listOf(0.46f, 0.50f, 0.55f, 0.59f, 0.57f), "Clearer vocal separation"),

        // Audio-Technica & Beyerdynamic
        AutoEqProfile("Audio-Technica", "ATH-M50x", "Over-Ear", listOf(0.48f, 0.52f, 0.55f, 0.58f, 0.52f), "Recovers recessed mids and eases treble spike"),
        AutoEqProfile("Audio-Technica", "ATH-M40x", "Over-Ear", listOf(0.52f, 0.50f, 0.52f, 0.54f, 0.52f), "Subtle flattening for true studio monitoring"),
        AutoEqProfile("Beyerdynamic", "DT 770 Pro (80Ω)", "Over-Ear", listOf(0.46f, 0.52f, 0.50f, 0.52f, 0.46f), "Reduces famous Beyer treble spike (Mount Beyer)"),
        AutoEqProfile("Beyerdynamic", "DT 990 Pro (250Ω)", "Over-Ear", listOf(0.48f, 0.54f, 0.52f, 0.50f, 0.44f), "Tames piercing highs and boosts sub-bass"),
        AutoEqProfile("Beyerdynamic", "DT 1990 Pro", "Over-Ear", listOf(0.50f, 0.52f, 0.50f, 0.52f, 0.45f), "Smooths treble peak for fatigue-free listening"),

        // IEMs (Moondrop, KZ, 7Hz, Tangzu)
        AutoEqProfile("Moondrop", "Chu II", "IEM", listOf(0.52f, 0.50f, 0.54f, 0.56f, 0.55f), "Harmonizes pinna gain and boosts sub-bass rumble"),
        AutoEqProfile("Moondrop", "Aria / Aria SE", "IEM", listOf(0.50f, 0.52f, 0.52f, 0.56f, 0.54f), "Delivers velvety smooth vocals and instruments"),
        AutoEqProfile("Moondrop", "Blessing 3", "IEM", listOf(0.54f, 0.50f, 0.50f, 0.52f, 0.54f), "Adds warm sub-bass punch to hybrid drivers"),
        AutoEqProfile("Moondrop", "Kato", "IEM", listOf(0.52f, 0.50f, 0.52f, 0.54f, 0.53f), "Near-perfect Harman curve fine-tuning"),
        AutoEqProfile("7Hz", "Salnotes Zero / Zero:2", "IEM", listOf(0.52f, 0.50f, 0.52f, 0.55f, 0.54f), "Studio reference target calibration"),
        AutoEqProfile("Tangzu", "Wan'er S.G", "IEM", listOf(0.50f, 0.52f, 0.53f, 0.55f, 0.52f), "Balances mid-bass and brightens treble shimmer"),
        AutoEqProfile("KZ", "ZSN Pro X", "IEM", listOf(0.44f, 0.48f, 0.58f, 0.60f, 0.42f), "Cuts sharp harsh highs and levels the V-shape"),
        AutoEqProfile("KZ", "ZS10 Pro", "IEM", listOf(0.46f, 0.50f, 0.56f, 0.58f, 0.44f), "Softens treble fatigue and cleans vocals"),

        // Realme, OnePlus & Nothing
        AutoEqProfile("Realme", "Buds Air 5 Pro", "In-Ear / TWS", listOf(0.48f, 0.50f, 0.55f, 0.60f, 0.56f), "Balances planar tweeter and dynamic woofer"),
        AutoEqProfile("Realme", "Buds Wireless 3", "In-Ear / TWS", listOf(0.46f, 0.48f, 0.56f, 0.62f, 0.58f), "Controls heavy bass and boosts vocal clarity"),
        AutoEqProfile("Realme", "Buds T300", "In-Ear / TWS", listOf(0.48f, 0.50f, 0.54f, 0.58f, 0.55f), "Harman Target correction"),
        AutoEqProfile("OnePlus", "Buds Pro 2", "In-Ear / TWS", listOf(0.48f, 0.52f, 0.55f, 0.60f, 0.56f), "Co-developed Dynaudio target compensation"),
        AutoEqProfile("OnePlus", "Buds 3", "In-Ear / TWS", listOf(0.46f, 0.50f, 0.56f, 0.59f, 0.57f), "Enhances vocal detail and spatial width"),
        AutoEqProfile("Nothing", "Ear (2)", "In-Ear / TWS", listOf(0.50f, 0.52f, 0.56f, 0.58f, 0.55f), "Custom calibrated sound curve for dual chamber"),
        AutoEqProfile("Nothing", "Ear (a)", "In-Ear / TWS", listOf(0.48f, 0.50f, 0.56f, 0.60f, 0.56f), "Improves midrange balance and treble openness"),

        // JBL & Anker Soundcore
        AutoEqProfile("JBL", "Tune 760NC / 770NC", "Over-Ear", listOf(0.46f, 0.50f, 0.56f, 0.60f, 0.58f), "Refines JBL Pure Bass into studio neutrality"),
        AutoEqProfile("JBL", "Live Pro 2 TWS", "In-Ear / TWS", listOf(0.48f, 0.52f, 0.55f, 0.58f, 0.56f), "Balanced signature for all music genres"),
        AutoEqProfile("Anker Soundcore", "Liberty 4 NC", "In-Ear / TWS", listOf(0.44f, 0.48f, 0.58f, 0.62f, 0.56f), "Tames boomy bass for punchy clarity"),
        AutoEqProfile("Anker Soundcore", "Space Q45", "Over-Ear", listOf(0.46f, 0.50f, 0.56f, 0.58f, 0.54f), "Restores natural acoustic timber and vocals"),
        AutoEqProfile("Anker Soundcore", "Life Q30", "Over-Ear", listOf(0.42f, 0.46f, 0.60f, 0.62f, 0.52f), "Dramatic cleanup of heavy stock bass boom")
    )

    fun getBrands(): List<String> = profiles.map { it.brand }.distinct().sorted()

    fun findProfile(brand: String, model: String): AutoEqProfile? {
        return profiles.find { it.brand.equals(brand, ignoreCase = true) && it.model.equals(model, ignoreCase = true) }
    }

    fun findProfileByName(fullName: String): AutoEqProfile? {
        if (fullName.isBlank() || fullName.equals("None", ignoreCase = true)) return null
        return profiles.find { " ".equals(fullName, ignoreCase = true) || it.model.equals(fullName, ignoreCase = true) }
    }
}
