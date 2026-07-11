package io.tl.nekopanel.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CapsuleTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit, tabs: List<String>) {
    val density = LocalDensity.current
    var tabWidths by remember { mutableStateOf(List(tabs.size) { 0f }) }

    val targetOffset = with(density) { tabWidths.take(selectedTab).sum().toDp() }
    val targetWidth = with(density) { (tabWidths.getOrNull(selectedTab) ?: 0f).toDp() }

    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "offset"
    )
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "width"
    )

    val contentWidth = with(density) { tabWidths.sum().toDp() }
    val squishLeft = animatedOffset.coerceAtLeast(0.dp)
    val squishRight = (animatedOffset + animatedWidth).coerceAtMost(contentWidth)
    val squishWidth = (squishRight - squishLeft).coerceAtLeast(0.dp)

    Surface(
        modifier = Modifier.wrapContentWidth().height(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = squishLeft)
                    .width(squishWidth)
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val textScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                tabWidths = tabWidths.toMutableList().also {
                                    while (it.size <= index) it.add(0f)
                                    it[index] = coords.size.width.toFloat()
                                }
                            }
                            .height(32.dp).wrapContentWidth().clip(CircleShape)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTabSelected(index) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            modifier = Modifier.graphicsLayer { scaleX = textScale; scaleY = textScale }
                        )
                    }
                }
            }
        }
    }
}
