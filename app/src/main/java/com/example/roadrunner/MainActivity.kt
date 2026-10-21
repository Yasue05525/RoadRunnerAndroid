package com.example.roadrunner

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 20, 28)
        window.navigationBarColor = Color.rgb(12, 20, 28)
        setContentView(GameView(this))
    }
}

private class GameView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lane = 1
    private var score = 0
    private var best = 0
    private var coins = 0
    private var distance = 0f
    private var scoreCarry = 0f
    private var coinCarry = 0f
    private var speed = 8f
    private var roadOffset = 0f
    private var lastTime = System.nanoTime()
    private var started = false
    private var gameOver = false
    private var spawnTimer = 0f
    private val cars = mutableListOf<Car>()
    private var downX = 0f

    private data class Car(var lane: Int, var y: Float, val color: Int)

    init { isFocusable = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.04f)
        lastTime = now
        if (started && !gameOver) update(dt, w, h)
        drawScene(canvas, w, h)
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float, w: Float, h: Float) {
        // Keep fractional progress between frames so score doesn't stay at zero.
        distance += speed * dt
        scoreCarry += dt * 12f
        val gained = scoreCarry.toInt()
        if (gained > 0) { score += gained; scoreCarry -= gained }
        coinCarry += dt * (0.7f + speed * 0.055f)
        val earned = coinCarry.toInt()
        if (earned > 0) { coins += earned; coinCarry -= earned }
        speed = (8f + score / 90f).coerceAtMost(19f)
        roadOffset = (roadOffset + speed * h * 0.09f * dt) % (h * 0.18f)
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            val available = (0..2).filter { cars.none { c -> c.lane == it && c.y < h * 0.30f } }
            if (available.isNotEmpty()) cars.add(Car(available.random(), -h * 0.15f,
                listOf(Color.rgb(244, 89, 76), Color.rgb(255, 193, 7), Color.rgb(91, 174, 255), Color.rgb(181, 112, 235)).random()))
            spawnTimer = (1.0f - score / 1800f).coerceAtLeast(0.42f)
        }
        val iterator = cars.iterator()
        while (iterator.hasNext()) {
            val car = iterator.next()
            car.y += speed * h * 0.11f * dt
            if (car.y > h + 100f) iterator.remove()
        }
        val playerY = h * 0.78f
        val carH = h * 0.105f
        for (car in cars) {
            if (car.lane == lane && car.y + carH * 0.45f > playerY - carH * 0.45f &&
                car.y - carH * 0.45f < playerY + carH * 0.45f) {
                gameOver = true
                best = maxOf(best, score)
                break
            }
        }
    }

    private fun drawScene(c: Canvas, w: Float, h: Float) {
        c.drawColor(Color.rgb(25, 92, 58))
        p.color = Color.rgb(34, 111, 65)
        c.drawRect(0f, 0f, w * 0.12f, h, p)
        c.drawRect(w * 0.88f, 0f, w, h, p)

        // Perspective road: narrow at the horizon, wide near the player.
        val horizonY = h * 0.16f
        val topLeft = w * 0.43f
        val topRight = w * 0.57f
        val bottomLeft = w * 0.08f
        val bottomRight = w * 0.92f
        p.color = Color.rgb(48, 52, 60)
        val road = android.graphics.Path().apply {
            moveTo(topLeft, horizonY); lineTo(topRight, horizonY)
            lineTo(bottomRight, h); lineTo(bottomLeft, h); close()
        }
        c.drawPath(road, p)

        // Perspective edge lines and dashed lane dividers.
        p.color = Color.rgb(231, 230, 213)
        p.strokeWidth = w * 0.009f
        c.drawLine(topLeft, horizonY, bottomLeft, h, p)
        c.drawLine(topRight, horizonY, bottomRight, h, p)
        for (divider in 1..2) {
            val topX = topLeft + (topRight - topLeft) * divider / 3f
            val bottomX = bottomLeft + (bottomRight - bottomLeft) * divider / 3f
            var t = 0f
            while (t < 1f) {
                val t1 = ((t + roadOffset / (h * 0.8f)) % 1f)
                val t2 = (t1 + 0.065f).coerceAtMost(1f)
                val y1 = horizonY + (h - horizonY) * t1 * t1
                val y2 = horizonY + (h - horizonY) * t2 * t2
                val x1 = topX + (bottomX - topX) * t1
                val x2 = topX + (bottomX - topX) * t2
                p.strokeWidth = w * (0.002f + 0.008f * t1)
                c.drawLine(x1, y1, x2, y2, p)
                t += 0.18f
            }
        }

        cars.forEach {
            val t = ((it.y / h) + 0.15f).coerceIn(0.05f, 1.1f)
            val x = perspectiveLaneCenter(it.lane, t, w)
            val scale = (0.35f + t * 0.8f).coerceIn(0.35f, 1.1f)
            drawCar(c, x, it.y, w * 0.15f * scale, h * 0.105f * scale, it.color)
        }
        drawCar(c, perspectiveLaneCenter(lane, 0.9f, w), h * 0.79f, w * 0.155f, h * 0.11f, Color.rgb(42, 194, 231), true)

        p.color = Color.argb(190, 10, 17, 25)
        c.drawRoundRect(w * 0.035f, h * 0.025f, w * 0.57f, h * 0.125f, 22f, 22f, p)
        p.color = Color.WHITE
        p.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        p.textSize = w * 0.047f
        c.drawText("СЧЁТ  $score", w * 0.065f, h * 0.065f, p)
        p.color = Color.rgb(255, 208, 64)
        c.drawText("● МОНЕТЫ  $coins", w * 0.065f, h * 0.105f, p)
        p.color = Color.WHITE
        p.textSize = w * 0.033f
        c.drawText("РЕКОРД  ${maxOf(best, score)}", w * 0.64f, h * 0.065f, p)
        if (!started) drawOverlay(c, w, h, "ROAD RUNNER", "Нажми, чтобы начать", "Свайпай влево и вправо")
        if (gameOver) drawOverlay(c, w, h, "СТОЛКНОВЕНИЕ!", "Счёт: $score  •  Монеты: $coins", "Нажми, чтобы сыграть снова")
    }

    private fun perspectiveLaneCenter(l: Int, t: Float, w: Float): Float {
        val topLeft = w * 0.43f
        val topRight = w * 0.57f
        val bottomLeft = w * 0.08f
        val bottomRight = w * 0.92f
        val top = topLeft + (topRight - topLeft) * (l + 0.5f) / 3f
        val bottom = bottomLeft + (bottomRight - bottomLeft) * (l + 0.5f) / 3f
        val depth = t.coerceIn(0f, 1f)
        return top + (bottom - top) * depth
    }

    private fun drawCar(c: Canvas, x: Float, y: Float, cw: Float, ch: Float, color: Int, player: Boolean = false) {
        // Wheels and body have highlights/shadows to give a more dimensional look.
        p.color = Color.argb(100, 0, 0, 0)
        c.drawOval(x - cw * 0.52f, y + ch * 0.30f, x + cw * 0.52f, y + ch * 0.52f, p)
        p.color = Color.rgb(19, 23, 29)
        c.drawRoundRect(x - cw * 0.48f, y - ch * 0.30f, x - cw * 0.31f, y + ch * 0.36f, 8f, 8f, p)
        c.drawRoundRect(x + cw * 0.31f, y - ch * 0.30f, x + cw * 0.48f, y + ch * 0.36f, 8f, 8f, p)
        p.color = color
        c.drawRoundRect(x - cw * 0.38f, y - ch * 0.5f, x + cw * 0.38f, y + ch * 0.5f, cw * 0.18f, cw * 0.18f, p)
        p.color = adjustColor(color, 1.22f)
        c.drawRoundRect(x - cw * 0.30f, y - ch * 0.46f, x + cw * 0.30f, y - ch * 0.30f, 5f, 5f, p)
        // Windshield at the front/top: the player's car now visibly faces forward.
        p.color = if (player) Color.rgb(173, 239, 255) else Color.rgb(140, 193, 215)
        c.drawRoundRect(x - cw * 0.25f, y - ch * 0.27f, x + cw * 0.25f, y + ch * 0.04f, 7f, 7f, p)
        p.color = if (player) Color.rgb(255, 64, 75) else Color.rgb(255, 224, 135)
        c.drawRoundRect(x - cw * 0.28f, y + ch * 0.30f, x - cw * 0.10f, y + ch * 0.42f, 4f, 4f, p)
        c.drawRoundRect(x + cw * 0.10f, y + ch * 0.30f, x + cw * 0.28f, y + ch * 0.42f, 4f, 4f, p)
        p.color = Color.argb(90, 255, 255, 255)
        p.strokeWidth = cw * 0.025f
        c.drawLine(x - cw * 0.31f, y - ch * 0.12f, x - cw * 0.31f, y + ch * 0.18f, p)
    }

    private fun adjustColor(color: Int, factor: Float): Int {
        return Color.rgb((Color.red(color) * factor).toInt().coerceAtMost(255),
            (Color.green(color) * factor).toInt().coerceAtMost(255),
            (Color.blue(color) * factor).toInt().coerceAtMost(255))
    }

    private fun drawOverlay(c: Canvas, w: Float, h: Float, title: String, subtitle: String, hint: String) {
        p.color = Color.argb(185, 7, 12, 19)
        c.drawRect(0f, 0f, w, h, p)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        p.color = Color.WHITE
        p.textSize = min(w * 0.095f, h * 0.06f)
        c.drawText(title, w / 2f, h * 0.40f, p)
        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        p.textSize = w * 0.052f
        c.drawText(subtitle, w / 2f, h * 0.48f, p)
        p.color = Color.rgb(98, 222, 245)
        p.textSize = w * 0.043f
        c.drawText(hint, w / 2f, h * 0.55f, p)
        p.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; return true }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                if (!started || gameOver) {
                    started = true; gameOver = false; score = 0; scoreCarry = 0f
                    speed = 8f; lane = 1; distance = 0f; coinCarry = 0f
                    cars.clear(); spawnTimer = 0.7f; lastTime = System.nanoTime()
                } else if (abs(dx) > width * 0.06f) {
                    lane = (lane + if (dx < 0) -1 else 1).coerceIn(0, 2)
                } else {
                    lane = (lane + if (event.x < width / 2f) -1 else 1).coerceIn(0, 2)
                }
                invalidate()
                return true
            }
        }
        return true
    }
}
