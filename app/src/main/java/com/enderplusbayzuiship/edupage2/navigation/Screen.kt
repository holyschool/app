package com.enderplusbayzuiship.edupage2.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    // username and subdomain are optional query params used to pre-fill the login form
    // after a failed auto-login attempt
    object Login : Screen("login?username={username}&subdomain={subdomain}") {
        fun route(username: String = "", subdomain: String = "") =
            "login?username=${java.net.URLEncoder.encode(username, "UTF-8")}" +
            "&subdomain=${java.net.URLEncoder.encode(subdomain, "UTF-8")}"
    }
    object Timetable : Screen("timetable")
}
