package com.yingwang.filmic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yingwang.filmic.R
import com.yingwang.filmic.lut.Style

/**
 * Localised description lookup for built-in styles. Falls back to the baked-in
 * description from the Style data class for any id we don't have a string
 * resource for (e.g., user-imported LUTs in the future).
 */
@Composable
fun Style.localizedDescription(): String {
    val res = when (id) {
        "hb_natural" -> R.string.desc_hb_natural
        "hb_xpan" -> R.string.desc_hb_xpan
        "fj_classic_chrome" -> R.string.desc_fj_classic_chrome
        "fj_velvia" -> R.string.desc_fj_velvia
        "fj_astia" -> R.string.desc_fj_astia
        "fj_acros" -> R.string.desc_fj_acros
        "lc_standard" -> R.string.desc_lc_standard
        "lc_chrome" -> R.string.desc_lc_chrome
        "lc_mono" -> R.string.desc_lc_mono
        else -> null
    }
    return if (res != null) stringResource(res) else description
}
