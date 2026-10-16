package com.example.roadrunner

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.graphics.Typeface
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 27, 36)
        window.navigationBarColor = Color.rgb(20, 27, 36)
        setContentView(GameView())
    }
}

private class GameView : View {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lane = 1
    private var score = 0
    private var best = 0
    private var speed = 8f
    private var roadOffset = 0f
    private var lastTime = System.nanoTime()
    private var started = false
    private var gameOver = false
    private var spawnTimer = 0f
    private val cars = mutableListOf<Car>()
    private var downX = 0f

    private data class Car(var lane: Int, var y: Float, val color: Int)

    constructor(context: android.content.Context) : super(context) { isFocusable = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val dt = ((System.nanoTime() - lastTime) / 1_000_000_000f).coerceIn(0f, 0.04f)
        lastTime = System.nanoTime()
        if (started && !gameOver) update(dt, w, h)
        drawScene(canvas, w, h)
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float, w: Float, h: Float) {
        score += (dt * 12).toInt()
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
        c.drawColor(Color.rgb(35, 120, 76))
        p.color = Color.rgb(47, 143, 85)
        c.drawRect(0f, 0f, w * 0.12f, h, p)
        c.drawRect(w * 0.88f, 0f, w, h, p)
        val left = w * 0.12f
        val right = w * 0.88f
        p.color = Color.rgb(54, 58, 66)
        c.drawRect(left, 0f, right, h, p)
        p.color = Color.rgb(230, 230, 220)
        p.strokeWidth = w * 0.012f
        c.drawLine(left, 0f, left, h, p)
        c.drawLine(right, 0f, right, h, p)
        for (divider in 1..2) {
            val x = left + (right - left) * divider / 3f
            var y = -h * 0.18f + roadOffset
            p.color = Color.rgb(220, 220, 210)
            while (y < h) {
                c.drawRoundRect(x - w * 0.006f, y, x + w * 0.006f, y + h * 0.09f, 8f, 8f, p)
                y += h * 0.18f
            }
        }
        val carW = w * 0.15f
        val carH = h * 0.105f
        cars.forEach { drawCar(c, laneCenter(it.lane, w), it.y, carW, carH, it.color) }
        drawCar(c, laneCenter(lane, w), h * 0.78f, carW * 1.03f, carH * 1.03f, Color.rgb(50, 205, 235), true)
        p.color = Color.argb(175, 10, 17, 25)
        c.drawRoundRect(w * 0.04f, h * 0.025f, w * 0.46f, h * 0.105f, 22f, 22f, p)
        p.color = Color.WHITE
        p.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        p.textSize = w * 0.055f
        c.drawText("СЧЁТ  $score", w * 0.075f, h * 0.072f, p)
        p.textSize = w * 0.035f
        c.drawText("РЕКОРД  ${maxOf(best, score)}", w * 0.075f, h * 0.095f, p)
        if (!started) drawOverlay(c, w, h, "ROAD RUNNER", "Нажми, чтобы начать", "Свайпай влево и вправо")
        if (gameOver) drawOverlay(c, w, h, "СТОЛКНОВЕНИЕ!", "Счёт: $score", "Нажми, чтобы сыграть снова")
    }

    private fun laneCenter(l: Int, w: Float) = w * (0.12f + 0.76f * (l + 0.5f) / 3f)

    private fun drawCar(c: Canvas, x: Float, y: Float, cw: Float, ch: Float, color: Int, player: Boolean = false) {
        p.color = Color.rgb(22, 27, 33)
        c.drawRoundRect(x - cw * 0.48f, y - ch * 0.35f, x - cw * 0.32f, y + ch * 0.35f, 8f, 8f, p)
        c.drawRoundRect(x + cw * 0.32f, y - ch * 0.35f, x + cw * 0.48f, y + ch * 0.35f, 8f, 8f, p)
        p.color = color
        c.drawRoundRect(x - cw * 0.38f, y - ch * 0.5f, x + cw * 0.38f, y + ch * 0.5f, cw * 0.18f, cw * 0.18f, p)
        p.color = if (player) Color.rgb(180, 244, 255) else Color.rgb(190, 224, 238)
        c.drawRoundRect(x - cw * 0.25f, y - ch * 0.27f, x + cw * 0.25f, y + ch * 0.02f, 7f, 7f, p)
        p.color = if (player) Color.WHITE else Color.rgb(255, 240, 185)
        c.drawRoundRect(x - cw * 0.27f, y + ch * 0.30f, x - cw * 0.10f, y + ch * 0.42f, 4f, 4f, p)
        c.drawRoundRect(x + cw * 0.10f, y + ch * 0.30f, x + cw * 0.27f, y + ch * 0.42f, 4f, 4f, p)
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
                    started = true; gameOver = false; score = 0; speed = 8f; lane = 1
                    cars.clear(); spawnTimer = 0.7f; lastTime = System.nanoTime()
                } else if (kotlin.math.abs(dx) > width * 0.06f) {
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
