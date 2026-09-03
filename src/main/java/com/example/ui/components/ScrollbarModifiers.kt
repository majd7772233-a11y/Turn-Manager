package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    this.drawWithContent {
        drawContent()
        val viewportHeight = size.height
        val totalHeight = scrollState.maxValue + viewportHeight
        if (totalHeight > viewportHeight) {
            val scrollPercent = scrollState.value.toFloat() / scrollState.maxValue
            val barHeight = (viewportHeight / totalHeight) * viewportHeight
            val barTop = (viewportHeight - barHeight) * scrollPercent
            drawRect(
                color = color,
                topLeft = Offset(size.width - widthPx, barTop),
                size = Size(widthPx, barHeight)
            )
        }
    }
}

fun Modifier.horizontalScrollbar(
    scrollState: ScrollState,
    height: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    this.drawWithContent {
        drawContent()
        val viewportWidth = size.width
        val totalWidth = scrollState.maxValue + viewportWidth
        if (totalWidth > viewportWidth) {
            val scrollPercent = scrollState.value.toFloat() / scrollState.maxValue
            val barWidth = (viewportWidth / totalWidth) * viewportWidth
            val barLeft = (viewportWidth - barWidth) * scrollPercent
            drawRect(
                color = color,
                topLeft = Offset(barLeft, size.height - heightPx),
                size = Size(barWidth, heightPx)
            )
        }
    }
}

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    this.drawWithContent {
        drawContent()
        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        if (totalItemsCount > 0) {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isNotEmpty()) {
                val firstVisibleItem = visibleItemsInfo.first()
                val lastVisibleItem = visibleItemsInfo.last()
                
                val viewportHeight = size.height
                val firstIndex = firstVisibleItem.index
                val lastIndex = lastVisibleItem.index
                
                val visibleItemsCount = lastIndex - firstIndex + 1
                val barHeight = (visibleItemsCount.toFloat() / totalItemsCount) * viewportHeight
                val progress = firstIndex.toFloat() / totalItemsCount
                val barTop = progress * viewportHeight
                
                drawRect(
                    color = color,
                    topLeft = Offset(size.width - widthPx, barTop),
                    size = Size(widthPx, barHeight)
                )
            }
        }
    }
}

fun Modifier.horizontalScrollbar(
    state: LazyListState,
    height: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    this.drawWithContent {
        drawContent()
        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        if (totalItemsCount > 0) {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isNotEmpty()) {
                val firstVisibleItem = visibleItemsInfo.first()
                val lastVisibleItem = visibleItemsInfo.last()
                
                val viewportWidth = size.width
                val firstIndex = firstVisibleItem.index
                val lastIndex = lastVisibleItem.index
                
                val visibleItemsCount = lastIndex - firstIndex + 1
                val barWidth = (visibleItemsCount.toFloat() / totalItemsCount) * viewportWidth
                val progress = firstIndex.toFloat() / totalItemsCount
                val barLeft = progress * viewportWidth
                
                drawRect(
                    color = color,
                    topLeft = Offset(barLeft, size.height - heightPx),
                    size = Size(barWidth, heightPx)
                )
            }
        }
    }
}
