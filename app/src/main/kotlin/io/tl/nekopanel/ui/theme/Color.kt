package io.tl.nekopanel.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemeScheme(
    val name: String, val key: String, val seedColor: Color,
    val origin: String = "", // "japanese" or "chinese"
)

val JapaneseThemeSchemes = listOf(
    ThemeScheme("绯红", "akabeni", Color(0xFFD43B3B), "japanese"),
    ThemeScheme("群青", "gunjou", Color(0xFF1E3A8A), "japanese"),
    ThemeScheme("萌黄", "moegi", Color(0xFF2E7D32), "japanese"),
    ThemeScheme("山吹", "yamabuki", Color(0xFFDAA520), "japanese"),
    ThemeScheme("藤紫", "fujimurasaki", Color(0xFF7C3AED), "japanese"),
    ThemeScheme("空色", "sorairo", Color(0xFF0288D1), "japanese"),
    ThemeScheme("若竹", "wakatake", Color(0xFF2E7D32), "japanese"),
    ThemeScheme("胭脂", "enji", Color(0xFF9B1B30), "japanese"),
)

val ChineseThemeSchemes = listOf(
    ThemeScheme("朱红", "zhuhong", Color(0xFFFF4D4D), "chinese"),
    ThemeScheme("琉璃蓝", "liulilan", Color(0xFF005AB5), "chinese"),
    ThemeScheme("松花绿", "songhualv", Color(0xFF057748), "chinese"),
    ThemeScheme("琥珀", "hupo", Color(0xFFCA6924), "chinese"),
    ThemeScheme("丁香紫", "dingxiangzi", Color(0xFF8B5CF6), "chinese"),
    ThemeScheme("天青", "tianqing", Color(0xFF0099CC), "chinese"),
    ThemeScheme("竹青", "zhuqing", Color(0xFF006633), "chinese"),
    ThemeScheme("绀青", "ganqing", Color(0xFF1B317A), "chinese"),
    ThemeScheme("赤金", "chijin", Color(0xFFC9752D), "chinese"),
    ThemeScheme("月白", "yuebai", Color(0xFFA8C8E0), "chinese"),
)

val AllThemeSchemes = JapaneseThemeSchemes + ChineseThemeSchemes