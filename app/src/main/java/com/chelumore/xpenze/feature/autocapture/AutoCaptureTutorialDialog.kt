package com.chelumore.xpenze.feature.autocapture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chelumore.xpenze.R
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.DarkSurface
import com.chelumore.xpenze.ui.theme.YellowPrimary
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class ArrowDirection {
    Left,
    Right,
    Up,
    Down
}

@Composable
fun AutoCaptureTutorialDialog(
    onDismiss: () -> Unit,
    onChooseBankApps: () -> Unit,
    canChooseBankApps: Boolean = true
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.auto_capture_tutorial_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.auto_capture_tutorial_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> TutorialPageOne()
                        else -> TutorialPageTwo()
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 9.dp else 7.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) {
                                        YellowPrimary
                                    } else {
                                        Color.Gray.copy(alpha = 0.45f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (pagerState.currentPage == 0) {
                    Button(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowPrimary,
                            contentColor = BlackBackground
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.auto_capture_tutorial_next),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.auto_capture_tutorial_got_it),
                            color = Color.Gray
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.auto_capture_tutorial_replay_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (canChooseBankApps) {
                        Button(
                            onClick = onChooseBankApps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YellowPrimary,
                                contentColor = BlackBackground
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.auto_capture_tutorial_choose_apps_cta),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auto_capture_tutorial_got_it),
                            fontWeight = FontWeight.SemiBold,
                            color = YellowPrimary
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.auto_capture_tutorial_back),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialPageOne() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFB74D).copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_capture_tutorial_general_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }

        AnnotatedScreenshot(
            imageRes = R.drawable.auto_capture_tutorial_draft_inbox,
            contentDescription = stringResource(R.string.auto_capture_tutorial_page1_image_cd),
            callouts = listOf(
                CalloutSpec(
                    number = 1,
                    caption = stringResource(R.string.auto_capture_tutorial_callout_specific_apps),
                    xFraction = 0.72f,
                    yFraction = 0.34f,
                    arrowDirection = ArrowDirection.Left
                )
            )
        )

        Text(
            text = stringResource(R.string.auto_capture_tutorial_page1_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun TutorialPageTwo() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnnotatedScreenshot(
            imageRes = R.drawable.auto_capture_tutorial_app_picker,
            contentDescription = stringResource(R.string.auto_capture_tutorial_page2_image_cd),
            callouts = listOf(
                CalloutSpec(
                    number = 1,
                    caption = stringResource(R.string.auto_capture_tutorial_callout_select_app),
                    xFraction = 0.90f,
                    yFraction = 0.42f,
                    arrowDirection = ArrowDirection.Left
                ),
                CalloutSpec(
                    number = 2,
                    caption = stringResource(R.string.auto_capture_tutorial_callout_search),
                    xFraction = 0.50f,
                    yFraction = 0.18f,
                    arrowDirection = ArrowDirection.Down
                ),
                CalloutSpec(
                    number = 3,
                    caption = stringResource(R.string.auto_capture_tutorial_callout_save),
                    xFraction = 0.78f,
                    yFraction = 0.92f,
                    arrowDirection = ArrowDirection.Up
                )
            )
        )

        Text(
            text = stringResource(R.string.auto_capture_tutorial_page2_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = stringResource(R.string.auto_capture_tutorial_notification_note),
            style = MaterialTheme.typography.bodySmall,
            color = YellowPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class CalloutSpec(
    val number: Int,
    val caption: String,
    val xFraction: Float,
    val yFraction: Float,
    val arrowDirection: ArrowDirection
)

@Composable
private fun AnnotatedScreenshot(
    imageRes: Int,
    contentDescription: String,
    callouts: List<CalloutSpec>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 18.5f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                callouts.forEach { callout ->
                    NumberedArrowCallout(
                        number = callout.number,
                        direction = callout.arrowDirection,
                        modifier = Modifier.offset(
                            x = maxWidth * callout.xFraction - 18.dp,
                            y = maxHeight * callout.yFraction - 18.dp
                        )
                    )
                }
            }
        }

        callouts
            .sortedBy { it.number }
            .forEach { callout ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(YellowPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callout.number.toString(),
                            color = BlackBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = callout.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
    }
}

@Composable
private fun NumberedArrowCallout(
    number: Int,
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 28.dp,
    stemLength: Dp = 22.dp
) {
    val arrowColor = YellowPrimary
    Box(modifier = modifier) {
        when (direction) {
            ArrowDirection.Left -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberBadge(number = number, size = badgeSize)
                    ShortArrow(
                        direction = direction,
                        color = arrowColor,
                        length = stemLength,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
            ArrowDirection.Right -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShortArrow(
                        direction = direction,
                        color = arrowColor,
                        length = stemLength,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    NumberBadge(number = number, size = badgeSize)
                }
            }
            ArrowDirection.Up -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShortArrow(
                        direction = direction,
                        color = arrowColor,
                        length = stemLength,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    NumberBadge(number = number, size = badgeSize)
                }
            }
            ArrowDirection.Down -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NumberBadge(number = number, size = badgeSize)
                    ShortArrow(
                        direction = direction,
                        color = arrowColor,
                        length = stemLength,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberBadge(
    number: Int,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(YellowPrimary, CircleShape)
            .border(2.dp, BlackBackground, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = BlackBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ShortArrow(
    direction: ArrowDirection,
    color: Color,
    length: Dp,
    modifier: Modifier = Modifier
) {
    val isHorizontal = direction == ArrowDirection.Left || direction == ArrowDirection.Right
    Canvas(
        modifier = modifier.then(
            if (isHorizontal) {
                Modifier
                    .width(length)
                    .height(14.dp)
            } else {
                Modifier
                    .width(14.dp)
                    .height(length)
            }
        )
    ) {
        val strokeWidth = 3.5.dp.toPx()
        val head = 7.dp.toPx()
        val (start, end) = when (direction) {
            ArrowDirection.Left -> Offset(size.width, size.height / 2f) to Offset(0f, size.height / 2f)
            ArrowDirection.Right -> Offset(0f, size.height / 2f) to Offset(size.width, size.height / 2f)
            ArrowDirection.Up -> Offset(size.width / 2f, size.height) to Offset(size.width / 2f, 0f)
            ArrowDirection.Down -> Offset(size.width / 2f, 0f) to Offset(size.width / 2f, size.height)
        }
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val angle = atan2(end.y - start.y, end.x - start.x)
        val path = Path().apply {
            moveTo(end.x, end.y)
            lineTo(
                end.x - head * cos(angle - 0.55f),
                end.y - head * sin(angle - 0.55f)
            )
            lineTo(
                end.x - head * cos(angle + 0.55f),
                end.y - head * sin(angle + 0.55f)
            )
            close()
        }
        drawPath(path, color = color)
        drawPath(path, color = color, style = Stroke(width = 1.dp.toPx()))
    }
}
