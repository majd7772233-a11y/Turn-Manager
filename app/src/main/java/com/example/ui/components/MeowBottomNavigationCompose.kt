package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TabItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A custom Shape for MeowBottomNavigation background.
 * Draws a smooth concave curved dip (cutout) at [curveCenterX].
 */
class MeowBottomNavShape(
    private val curveCenterX: Float,
    private val curveWidth: Float,
    private val curveDepth: Float,
    private val topPadding: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, topPadding)
            
            val startOfCurve = curveCenterX - (curveWidth / 2f)
            val endOfCurve = curveCenterX + (curveWidth / 2f)
            
            if (startOfCurve > 0f) {
                lineTo(startOfCurve, topPadding)
            }
            
            // First cubic curve down to the center dip
            cubicTo(
                x1 = startOfCurve + curveWidth * 0.25f, y1 = topPadding,
                x2 = curveCenterX - curveWidth * 0.22f, y2 = topPadding + curveDepth,
                x3 = curveCenterX, y3 = topPadding + curveDepth
            )
            
            // Second cubic curve up to the end of the dip
            cubicTo(
                x1 = curveCenterX + curveWidth * 0.22f, y1 = topPadding + curveDepth,
                x2 = endOfCurve - curveWidth * 0.25f, y2 = topPadding,
                x3 = endOfCurve, y3 = topPadding
            )
            
            lineTo(size.width, topPadding)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun MeowBottomNavigationCompose(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabLongPress: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val tabCount = tabs.size

    // Sizing in DP
    val barHeight = 60.dp
    val topPadding = 18.dp
    val bubbleSize = 50.dp
    val curveWidth = 92.dp
    val curveDepth = 26.dp

    // Convert to PX for path drawing
    val topPaddingPx = with(density) { topPadding.toPx() }
    val curveWidthPx = with(density) { curveWidth.toPx() }
    val curveDepthPx = with(density) { curveDepth.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight + topPadding)
            .navigationBarsPadding()
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val tabWidthPx = if (tabCount > 0) widthPx / tabCount else 0f

        // Calculate the ideal horizontal center for the active bubble
        val targetCenterX = if (tabCount > 0) (selectedIndex + 0.5f) * tabWidthPx else 0f

        // Fluid horizontal slide using a spring animation
        val animatedCenterX by animateFloatAsState(
            targetValue = targetCenterX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "curveCenterX"
        )

        // Custom curved background with shadow and primary glow outline
        val customShape = MeowBottomNavShape(
            curveCenterX = animatedCenterX,
            curveWidth = curveWidthPx,
            curveDepth = curveDepthPx,
            topPadding = topPaddingPx
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 12.dp,
                    shape = customShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = customShape
                )
        )

        // --- Active Floating Bubble ---
        val bubbleOffsetDP = with(density) { (animatedCenterX - (with(density) { bubbleSize.toPx() } / 2f)).toDp() }
        val bubbleYOffset = 4.dp

        Box(
            modifier = Modifier
                .offset(x = bubbleOffsetDP, y = bubbleYOffset)
                .size(bubbleSize)
                .shadow(6.dp, CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                )
                .pointerInput(selectedIndex) {
                    detectTapGestures(
                        onPress = {
                            var is3SecTriggered = false
                            val holdJob = coroutineScope.launch {
                                delay(3000)
                                is3SecTriggered = true
                                onTabLongPress?.invoke(selectedIndex)
                            }
                            tryAwaitRelease()
                            holdJob.cancel()
                            if (!is3SecTriggered) {
                                onTabSelected(selectedIndex)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val activeTab = tabs.getOrNull(selectedIndex)
            if (activeTab != null) {
                Icon(
                    imageVector = activeTab.icon,
                    contentDescription = activeTab.title,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- Inactive Tabs row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = index == selectedIndex
                
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isActive) 0f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tabAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(index) {
                            detectTapGestures(
                                onPress = {
                                    var is3SecTriggered = false
                                    val holdJob = coroutineScope.launch {
                                        delay(3000)
                                        is3SecTriggered = true
                                        onTabLongPress?.invoke(index)
                                    }
                                    tryAwaitRelease()
                                    holdJob.cancel()
                                    if (!is3SecTriggered) {
                                        onTabSelected(index)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.alpha(animatedAlpha)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
