package com.brafik.samples

abstract class Screen(val route: String) {
    object CreatePin : Screen("create_pin")
    object Home : Screen("home")
    object OngoingElections : Screen("ongoing_elections")
    object FinalizedElections : Screen("finalized_elections")
}