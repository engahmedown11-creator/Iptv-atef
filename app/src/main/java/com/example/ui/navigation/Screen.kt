package com.example.ui.navigation

sealed class Screen(val route: String, val titleAr: String) {
    object Dashboard : Screen("dashboard", "الرئيسية")
    object LiveTv : Screen("live_tv", "بث مباشر")
    object Movies : Screen("movies", "أفلام")
    object Series : Screen("series", "مسلسلات")
    object Player : Screen("player", "المشغل")
    object Downloads : Screen("downloads", "التنزيلات")
    object Storage : Screen("storage", "إدارة التخزين")
    object Settings : Screen("settings", "الإعدادات")
    object AddSubscription : Screen("add_subscription", "إضافة اشتراك")
    object Support : Screen("support", "الدعم الفني")
}
