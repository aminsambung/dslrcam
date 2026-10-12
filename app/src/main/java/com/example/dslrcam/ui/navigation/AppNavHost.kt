package com.example.dslrcam.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.dslrcam.ui.camera.CameraScreen
import com.example.dslrcam.ui.gallery.GalleryScreen
import com.example.dslrcam.ui.settings.SettingsScreen

object Routes {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onOpenGallery = { navController.navigate(Routes.GALLERY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
