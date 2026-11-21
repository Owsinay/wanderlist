package com.wanderlist

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanderlist.domain.model.ThemeMode
import com.wanderlist.ui.navigation.WanderlistApp
import com.wanderlist.ui.theme.WanderlistTheme
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity, bo wymaga jej BiometricPrompt (blokada aplikacji). */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WanderlistTheme(darkTheme = darkTheme) {
                WanderlistApp()
            }
        }
    }
}
