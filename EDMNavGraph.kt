package com.edm.downloadmanager.ui.navigation

sealed class EDMDestination(val route: String, val label: String) {
    object Splash : EDMDestination("splash", "Splash")
    object Home : EDMDestination("home", "Home")
    object Downloads : EDMDestination("downloads", "Downloads")
    object Queue : EDMDestination("queue", "Queue")
    object History : EDMDestination("history", "History")
    object Favorites : EDMDestination("favorites", "Favorites")
    object Statistics : EDMDestination("statistics", "Statistics")
    object FileManager : EDMDestination("file_manager", "Files")
    object Settings : EDMDestination("settings", "Settings")
    object About : EDMDestination("about", "About")

    companion object {
        val bottomBarItems = listOf(Home, Downloads, Queue, Statistics, Settings)
    }
}
