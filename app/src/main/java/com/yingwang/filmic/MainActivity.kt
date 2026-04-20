package com.yingwang.filmic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yingwang.filmic.nav.FilmicNav
import com.yingwang.filmic.ui.theme.FilmicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FilmicTheme {
                FilmicNav()
            }
        }
    }
}
