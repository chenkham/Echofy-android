package com.Chenkham.Echofy.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AppFont

/**
 * Maps a user-selected [AppFont] onto a Compose [FontFamily].
 */
fun AppFont.toFontFamily(): FontFamily? =
    when (this) {
        AppFont.SYSTEM -> null
        AppFont.LINOTTE -> FontFamily(Font(R.font.linotte))
        AppFont.POPPINS -> FontFamily(Font(R.font.poppins))
        AppFont.SF_PRO -> FontFamily(Font(R.font.sfprodisplaybold))
        AppFont.ANYBODY -> FontFamily(Font(R.font.anybody))
        AppFont.SANS_SERIF -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONOSPACE -> FontFamily.Monospace
        AppFont.CURSIVE -> FontFamily.Cursive
    }
