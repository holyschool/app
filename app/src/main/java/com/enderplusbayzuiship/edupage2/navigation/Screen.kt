package com.enderplusbayzuiship.edupage2.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login?username={username}&subdomain={subdomain}") {
        fun route(username: String = "", subdomain: String = "") =
            "login?username=${java.net.URLEncoder.encode(username, "UTF-8")}" +
            "&subdomain=${java.net.URLEncoder.encode(subdomain, "UTF-8")}"
    }
    /** Root destination after login — hosts the bottom-nav scaffold. */
    object Main : Screen("main")
}

/** Destinations inside the bottom-nav nested graph. */
sealed class Tab(val route: String) {
    object Overview  : Tab("tab_overview")
    object Timetable : Tab("tab_timetable")
    object Grades    : Tab("tab_grades")
    object Settings  : Tab("tab_settings")
}
