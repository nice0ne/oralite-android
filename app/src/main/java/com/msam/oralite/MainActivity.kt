package com.msam.oralite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.msam.oralite.ui.screen.EditorScreen
import com.msam.oralite.ui.screen.HistoryDialog
import com.msam.oralite.ui.screen.ProfileDialog
import com.msam.oralite.ui.theme.OraLiteTheme
import com.msam.oralite.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            var showProfileDialog by remember { mutableStateOf(false) }
            var showHistoryDialog by remember { mutableStateOf(false) }

            OraLiteTheme(darkTheme = isDark) {
                EditorScreen(
                    viewModel = viewModel,
                    onOpenProfiles = { showProfileDialog = true },
                    onOpenHistory = { showHistoryDialog = true }
                )

                if (showProfileDialog) {
                    ProfileDialog(
                        viewModel = viewModel,
                        onDismiss = { showProfileDialog = false }
                    )
                }

                if (showHistoryDialog) {
                    HistoryDialog(
                        viewModel = viewModel,
                        onDismiss = { showHistoryDialog = false }
                    )
                }
            }
        }
    }
}
