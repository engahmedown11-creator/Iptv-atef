package com.example.player

enum class AspectRatioMode(val labelAr: String) {
    AUTO("تلقائي"),
    ORIGINAL("الأصلي"),
    FIT("ملاءمة الشاشة"),
    FILL("ملء الشاشة"),
    STRETCH("تمديد"),
    ZOOM("تكبير"),
    ASPECT_16_9("16:9"),
    ASPECT_4_3("4:3"),
    ASPECT_21_9("21:9"),
    ASPECT_1_1("1:1")
}

data class TrackItem(
    val id: String,
    val name: String,
    val language: String = "",
    val isSelected: Boolean = false,
    val formatInfo: String = ""
)

data class QualityItem(
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isAuto: Boolean = false,
    val isSelected: Boolean = false
) {
    val displayLabel: String
        get() = if (isAuto) "تلقائي (Auto)" else if (height > 0) "${height}p (${bitrate / 1000} kbps)" else "جودة ثابتة"
}

data class SubtitleConfig(
    val isEnabled: Boolean = true,
    val sizeSp: Float = 18f,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#80000000",
    val timeOffsetMs: Long = 0L
)

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}
