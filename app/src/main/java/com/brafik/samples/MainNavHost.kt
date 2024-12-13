package com.brafik.samples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.brafik.samples.ui.election.ElectionScreen
import com.brafik.samples.ui.pin.PinCodeScreen

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.CreatePin.route) { PinCodeScreen(navController) }
        composable(Screen.Home.route) { ElectionScreen() }
    }
}

fun NavController.resetToHome() = navigate(Screen.Home.route) {
    popUpTo(Screen.CreatePin.route) { inclusive = true }
}
