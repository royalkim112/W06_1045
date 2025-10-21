package com.example.w06

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.w06.ui.theme.W06Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

const val CHANNEL_ID = "bubble_game_notification_channel"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            W06Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BubbleGameScreen()
                }
            }
        }
    }
}

data class Bubble(
    val id: Int,
    var position: Offset,
    val radius: Float,
    val color: Color,
    val creationTime: Long = System.currentTimeMillis(),
    val velocityX: Float = 0f,
    val velocityY: Float = 0f
)

class GameState(
    initialBubbles: List<Bubble> = emptyList()
) {
    var bubbles by mutableStateOf(initialBubbles)
    var score by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)
    var timeLeft by mutableStateOf(10)
    var isGameStarted by mutableStateOf(false)
}

fun makeNewBubble(maxWidth: Dp, maxHeight: Dp): Bubble {
    val randomVelocityX = (Random.nextFloat() - 0.5f) * 2
    val randomVelocityY = (Random.nextFloat() - 0.5f) * 2

    return Bubble(
        id = Random.nextInt(),
        position = Offset(
            x = Random.nextFloat() * maxWidth.value,
            y = Random.nextFloat() * maxHeight.value
        ),
        radius = Random.nextFloat() * 50 + 50,
        color = Color(
            red = Random.nextInt(256),
            green = Random.nextInt(256),
            blue = Random.nextInt(256),
            alpha = 200
        ),
        velocityX = randomVelocityX,
        velocityY = randomVelocityY
    )
}

fun updateBubblePositions(
    bubbles: List<Bubble>,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    density: Density
): List<Bubble> {
    return bubbles.map { bubble ->
        with(density) {
            val radiusPx = bubble.radius.dp.toPx()
            var xPx = bubble.position.x.dp.toPx()
            var yPx = bubble.position.y.dp.toPx()
            val vxPx = bubble.velocityX.dp.toPx()
            val vyPx = bubble.velocityY.dp.toPx()

            xPx += vxPx
            yPx += vyPx

            var newVx = bubble.velocityX
            var newVy = bubble.velocityY

            if (xPx < radiusPx || xPx > canvasWidthPx - radiusPx) newVx *= -1
            if (yPx < radiusPx || yPx > canvasHeightPx - radiusPx) newVy *= -1

            xPx = xPx.coerceIn(radiusPx, canvasWidthPx - radiusPx)
            yPx = yPx.coerceIn(radiusPx, canvasHeightPx - radiusPx)

            bubble.copy(
                position = Offset(
                    x = xPx.toDp().value,
                    y = yPx.toDp().value
                ),
                velocityX = newVx,
                velocityY = newVy
            )
        }
    }
}

fun onGameOver(gameState: GameState, showDialog: MutableState<Boolean>) {
    gameState.isGameOver = true
    showDialog.value = true
}

fun restartGame(gameState: GameState) {
    gameState.score = 0
    gameState.timeLeft = 10
    gameState.isGameOver = false
    gameState.bubbles = emptyList()
}

@SuppressLint("MissingPermission")
fun showNotification(context: Context, score: Int) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("버블 게임 종료")
        .setContentText("최종 점수: ${score}점! 고생하셨습니다.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(context)) {
        notify(100, builder.build())
    }
}

@Composable
fun GameStatusRow(score: Int, timeLeft: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "Time: ${timeLeft}s", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BubbleComposable(bubble: Bubble, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size((bubble.radius * 2).dp)
            .offset(x = bubble.position.x.dp - bubble.radius.dp, y = bubble.position.y.dp - bubble.radius.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircle(
                color = bubble.color,
                radius = size.width / 2,
                center = center
            )
        }
    }
}

@Composable
fun GameOverDialog(score: Int, onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("게임 오버") },
        text = { Text("당신의 점수는 ${score}점입니다.") },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("다시 시작")
            }
        }
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BubbleGameScreen() {
    val context = LocalContext.current
    val gameState = remember { GameState() }
    var showDialog by remember { mutableStateOf(false) }

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    LaunchedEffect(gameState.isGameOver) {
        if (!gameState.isGameOver && gameState.timeLeft > 0) {
            while (true) {
                delay(1000L)
                gameState.timeLeft--

                val currentTime = System.currentTimeMillis()
                gameState.bubbles = gameState.bubbles.filter {
                    currentTime - it.creationTime < 3000
                }

                if (gameState.timeLeft <= 0) {
                    onGameOver(gameState, mutableStateOf(true))
                    break
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameStatusRow(score = gameState.score, timeLeft = gameState.timeLeft)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }
            val canvasHeightPx = with(density) { maxHeight.toPx() }

            LaunchedEffect(key1 = gameState.isGameOver) {
                if (!gameState.isGameOver) {
                    while (true) {
                        delay(16)

                        if (gameState.bubbles.isEmpty()) {
                            val newBubbles = List(3) {
                                makeNewBubble(maxWidth, maxHeight)
                            }
                            gameState.bubbles = newBubbles
                        }

                        if (Random.nextFloat() < 0.05f && gameState.bubbles.size < 15) {
                            val newBubble = makeNewBubble(maxWidth, maxHeight)
                            gameState.bubbles = gameState.bubbles + newBubble
                        }

                        gameState.bubbles = updateBubblePositions(
                            gameState.bubbles,
                            canvasWidthPx,
                            canvasHeightPx,
                            density
                        )
                    }
                }
            }

            gameState.bubbles.forEach { bubble ->
                BubbleComposable(bubble = bubble) {
                    vibrate()
                    gameState.score++
                    gameState.bubbles =
                        gameState.bubbles.filterNot { it.id == bubble.id }
                }
            }
        }
    }

    if (showDialog) {
        GameOverDialog(
            score = gameState.score,
            onRestart = {
                restartGame(gameState)
                showDialog = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BubbleGamePreview() {
    W06Theme {
        BubbleGameScreen()
    }
}