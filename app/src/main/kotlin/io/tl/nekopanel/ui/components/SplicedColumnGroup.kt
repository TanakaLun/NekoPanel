package io.tl.nekopanel.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.shape.RoundedCornerShape

data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SplicedGroupScope.() -> Unit,
) {
    val scope = SplicedGroupScope().apply(content)
    val allItems = scope.items
    if (allItems.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
        }

        Column(verticalArrangement = Arrangement.Top) {
            val firstVisibleIndex = allItems.indexOfFirst { it.visible }
            val lastVisibleIndex = allItems.indexOfLast { it.visible }
            val sharedStiffness = Spring.StiffnessMediumLow

            allItems.forEachIndexed { index, itemData ->
                key(itemData.key) {
                    val zIndex = if (itemData.visible) 0f else 1f

                    AnimatedVisibility(
                        visible = itemData.visible,
                        modifier = Modifier.zIndex(zIndex),
                        enter = expandVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            expandFrom = Alignment.Top,
                        ) + fadeIn(
                            animationSpec = spring(stiffness = sharedStiffness),
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            shrinkTowards = Alignment.Top,
                        ) + fadeOut(
                            animationSpec = spring(stiffness = sharedStiffness),
                        ),
                    ) {
                        val isFirstVisible = index <= firstVisibleIndex && itemData.visible
                        val isLastVisible = index >= lastVisibleIndex && itemData.visible
                        var isPressed by remember { mutableStateOf(false) }

                        val targetTopRadius = if (isPressed) 16.dp else if (isFirstVisible) 16.dp else 2.dp
                        val targetBottomRadius = if (isPressed) 16.dp else if (isLastVisible) 16.dp else 2.dp

                        val isAtLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                        val currentTopRadius = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetTopRadius,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "TopRadius",
                            ).value
                        } else targetTopRadius

                        val currentBottomRadius = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetBottomRadius,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "BottomRadius",
                            ).value
                        } else targetBottomRadius

                        val shape = RoundedCornerShape(
                            topStart = currentTopRadius,
                            topEnd = currentTopRadius,
                            bottomStart = currentBottomRadius,
                            bottomEnd = currentBottomRadius,
                        )

                        val targetTopPadding = if (isFirstVisible) 0.dp else 2.dp
                        val currentTopPadding = if (isAtLeastTiramisu) {
                            animateDpAsState(
                                targetValue = targetTopPadding,
                                animationSpec = spring(stiffness = sharedStiffness),
                                label = "TopGap",
                            ).value
                        } else targetTopPadding

                        Column(
                            modifier = Modifier
                                .padding(top = currentTopPadding)
                                .pointerInput(itemData.key) {
                                    while (true) {
                                        awaitPointerEventScope {
                                            var event = awaitPointerEvent(PointerEventPass.Main)
                                            val firstChange = event.changes.firstOrNull() ?: return@awaitPointerEventScope
                                            if (!firstChange.pressed) return@awaitPointerEventScope
                                            val longPressMs = 400L
                                            val longPressActivated = withTimeoutOrNull(longPressMs) {
                                                while (true) {
                                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                                    val c = ev.changes.firstOrNull() ?: break
                                                    if (!c.pressed) return@withTimeoutOrNull false
                                                }
                                                false
                                            }
                                            if (longPressActivated == null) {
                                                isPressed = true
                                                while (true) {
                                                    event = awaitPointerEvent(PointerEventPass.Main)
                                                    val c = event.changes.firstOrNull() ?: break
                                                    isPressed = c.pressed
                                                    if (!c.pressed) break
                                                }
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    this.shape = shape
                                    this.clip = true
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            itemData.content()
                        }
                    }
                }
            }
        }
    }
}
