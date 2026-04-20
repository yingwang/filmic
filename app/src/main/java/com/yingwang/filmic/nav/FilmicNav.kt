package com.yingwang.filmic.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yingwang.filmic.ui.ImportScreen
import com.yingwang.filmic.ui.PreviewScreen
import com.yingwang.filmic.ui.StyleScreen

private object Routes {
    const val IMPORT = "import"
    const val STYLE = "style"
    const val PREVIEW = "preview"
}

@Composable
fun FilmicNav() {
    val nav = rememberNavController()
    var pickedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedStyleId by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(navController = nav, startDestination = Routes.IMPORT) {
        composable(Routes.IMPORT) {
            ImportScreen(
                onPicked = { uri ->
                    pickedUri = uri.toString()
                    nav.navigate(Routes.STYLE)
                },
            )
        }
        composable(Routes.STYLE) {
            val uri = remember(pickedUri) { pickedUri?.let(Uri::parse) }
            StyleScreen(
                sourceUri = uri,
                onBack = { nav.popBackStack() },
                onPick = { styleId ->
                    pickedStyleId = styleId
                    nav.navigate(Routes.PREVIEW)
                },
            )
        }
        composable(Routes.PREVIEW) {
            val uri = remember(pickedUri) { pickedUri?.let(Uri::parse) }
            PreviewScreen(
                sourceUri = uri,
                styleId = pickedStyleId,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
