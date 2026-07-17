package io.tl.nekopanel.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun highlightJson(jsonStr: String): AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    return remember(jsonStr, colorScheme) {
        buildAnnotatedString {
            val keyColor = colorScheme.primary
            val stringColor = colorScheme.tertiary
            val numberColor = colorScheme.secondary
            val booleanColor = colorScheme.error
            val nullColor = colorScheme.error
            val punctuationColor = colorScheme.onSurfaceVariant

            var i = 0
            while (i < jsonStr.length) {
                when {
                    jsonStr[i] == '"' -> {
                        val start = i; i++
                        while (i < jsonStr.length) {
                            if (jsonStr[i] == '\\' && i + 1 < jsonStr.length) i += 2
                            else if (jsonStr[i] == '"') { i++; break }
                            else i++
                        }
                        val str = jsonStr.substring(start, i)
                        var lookAhead = i
                        while (lookAhead < jsonStr.length && jsonStr[lookAhead].isWhitespace()) lookAhead++
                        val isKey = lookAhead < jsonStr.length && jsonStr[lookAhead] == ':'
                        withStyle(SpanStyle(color = if (isKey) keyColor else stringColor, fontWeight = if(isKey) FontWeight.Bold else FontWeight.Normal)) { append(str) }
                    }
                    jsonStr[i].isDigit() || jsonStr[i] == '-' -> {
                        val start = i
                        while (i < jsonStr.length && (jsonStr[i].isDigit() || jsonStr[i] in ".-eE")) i++
                        withStyle(SpanStyle(color = numberColor)) { append(jsonStr.substring(start, i)) }
                    }
                    jsonStr.startsWith("true", i) || jsonStr.startsWith("false", i) -> {
                        val bool = if (jsonStr.startsWith("true", i)) "true" else "false"
                        withStyle(SpanStyle(color = booleanColor, fontWeight = FontWeight.Medium)) { append(bool) }
                        i += bool.length
                    }
                    jsonStr.startsWith("null", i) -> {
                        withStyle(SpanStyle(color = nullColor)) { append("null") }; i += 4
                    }
                    else -> {
                        val isPunctuation = jsonStr[i] in "{}[],:"
                        withStyle(SpanStyle(color = if(isPunctuation) punctuationColor else Color.Unspecified)) { append(jsonStr[i]) }
                        i++
                    }
                }
            }
        }
    }
}
