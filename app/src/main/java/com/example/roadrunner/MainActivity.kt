package com.example.roadrunner

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var game: GameView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 18, 27)
        window.navigationBarColor = Color.rgb(10, 18, 27)
        game = GameView(this)
        setContentView(game)
    }
    override fun onPause() { game.saveProgress(); super.onPause() }
    override fun onStop() { game.saveProgress(); super.onStop() }
}

private class GameView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = context.getSharedPreferences("road_runner_save", Context.MODE_PRIVATE)
    private var screen = Screen.MENU
    private enum class Screen { MENU, PLAYING, GAME_OVER, SHOP }
    private var lane = 1
    private var score = 0
    private var best = prefs.getInt("best", 0)
    private var coins = prefs.getInt("coins", 0)
    private var scoreCarry = 0f
    private var coinCarry = 0f
    private var speed = 8f
    private var roadOffset = 0f
    private var lastTime = System.nanoTime()
    private var spawnTimer = 0.7f
    private var downX = 0f
    private var downY = 0f
    private val carColors = intArrayOf(Color.rgb(42,194,231), Color.rgb(255,92,78), Color.rgb(255,190,45), Color.rgb(153,111,255), Color.rgb(80,220,130))
    private val carPrices = intArrayOf(0, 100, 250, 500, 900)
    private var selectedCar = prefs.getInt("selected_car", 0).coerceIn(0, carColors.lastIndex)
    private val owned = mutableSetOf<Int>().apply {
        add(0)
        addAll(prefs.getString("owned_cars", "0")!!.split(",").mapNotNull { it.toIntOrNull() })
    }
    private val cars = mutableListOf<Car>()
    private data class Car(val lane: Int, var y: Float, val color: Int)

    init { isFocusable = true }

    fun saveProgress() {
        prefs.edit().putInt("coins", coins).putInt("best", best)
            .putInt("selected_car", selectedCar)
            .putString("owned_cars", owned.sorted().joinToString(",")).apply()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.04f)
        lastTime = now
        if (screen == Screen.PLAYING) update(dt, h)
        drawWorld(canvas, w, h)
        when (screen) {
            Screen.MENU -> drawMenu(canvas, w, h)
            Screen.SHOP -> drawShop(canvas, w, h)
            Screen.GAME_OVER -> drawGameOver(canvas, w, h)
            Screen.PLAYING -> drawHud(canvas, w, h)
        }
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float, h: Float) {
        scoreCarry += dt * 12f
        val scoreAdd = scoreCarry.toInt()
        if (scoreAdd > 0) { score += scoreAdd; scoreCarry -= scoreAdd }
        speed = (8f + score / 90f).coerceAtMost(20f)
        // Earn a coin for roughly every second of driving; save immediately so closing the app won't lose them.
        coinCarry += dt * (0.8f + speed * 0.04f)
        val add = coinCarry.toInt()
        if (add > 0) { coins += add; coinCarry -= add; saveProgress() }
        roadOffset = (roadOffset + speed * dt) % 1f
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            val possible = (0..2).filter { ln -> cars.none { it.lane == ln && it.y < h * 0.43f } }
            if (possible.isNotEmpty()) cars.add(Car(possible.random(), 0.18f, intArrayOf(Color.rgb(240,76,68), Color.rgb(255,186,40), Color.rgb(165,115,240), Color.rgb(80,160,255)).random()))
            spawnTimer = (1.05f - score / 1600f).coerceAtLeast(0.48f)
        }
        val iter = cars.iterator()
        while (iter.hasNext()) {
            val car = iter.next()
            // y is perspective depth (0 horizon, 1 foreground), not screen pixels.
            car.y += dt * (0.30f + speed * 0.012f)
            if (car.y > 1.12f) iter.remove()
        }
        for (car in cars) {
            if (car.lane == lane && car.y > 0.77f && car.y < 0.98f) {
                screen = Screen.GAME_OVER
                best = maxOf(best, score)
                saveProgress()
                break
            }
        }
    }

    private fun drawWorld(c: Canvas, w: Float, h: Float) {
        c.drawColor(Color.rgb(100, 177, 224))
        // Distant hills, trees and roadside grass give the road a proper environment.
        p.color = Color.rgb(64, 143, 84); c.drawRect(0f, h*.28f, w, h, p)
        p.color = Color.rgb(52, 119, 72)
        val hills = Path().apply { moveTo(0f,h*.34f); lineTo(w*.15f,h*.22f); lineTo(w*.30f,h*.34f); lineTo(w*.48f,h*.20f); lineTo(w*.66f,h*.34f); lineTo(w*.84f,h*.23f); lineTo(w,h*.33f); lineTo(w,h*.48f); lineTo(0f,h*.48f); close() }
        c.drawPath(hills,p)
        for (i in 0..10) {
            val x = w * (i / 10f)
            val y = h * (.29f + (i % 3) * .025f)
            p.color = Color.rgb(91,62,38); c.drawRect(x-w*.009f,y,x+w*.009f,y+h*.08f,p)
            p.color = Color.rgb(28,105,57); c.drawCircle(x,y,w*.035f,p)
            p.color = Color.rgb(42,133,67); c.drawCircle(x-w*.014f,y-h*.018f,w*.027f,p)
        }
        val hy=h*.22f; val tl=w*.43f; val tr=w*.57f; val bl=w*.035f; val br=w*.965f
        p.color=Color.rgb(45,48,55)
        c.drawPath(Path().apply { moveTo(tl,hy); lineTo(tr,hy); lineTo(br,h); lineTo(bl,h); close() },p)
        p.color=Color.rgb(237,230,201); p.strokeWidth=w*.009f
        c.drawLine(tl,hy,bl,h,p); c.drawLine(tr,hy,br,h,p)
        for (d in 1..2) {
            val xt=tl+(tr-tl)*d/3f; val xb=bl+(br-bl)*d/3f
            for (i in 0..7) {
                val t=((i/8f + roadOffset)%1f)
                val t2=(t+.045f).coerceAtMost(1f)
                val y1=hy+(h-hy)*t*t; val y2=hy+(h-hy)*t2*t2
                val x1=xt+(xb-xt)*t; val x2=xt+(xb-xt)*t2
                p.color=Color.rgb(245,242,222); p.strokeWidth=w*(.003f+.009f*t)
                c.drawLine(x1,y1,x2,y2,p)
            }
        }
        // Draw distant traffic first; vehicles grow as they approach the camera.
        cars.sortedBy { it.y }.forEach {
            val t=it.y.coerceIn(0f,1f); val x=laneX(it.lane,t,w)
            val scale=.28f+t*.95f
            drawCar(c,x,hy+(h-hy)*t*t,w*.12f*scale,h*.075f*scale,it.color,false)
        }
        drawCar(c,laneX(lane,.87f,w),h*.80f,w*.19f,h*.125f,carColors[selectedCar],true)
    }

    private fun laneX(ln:Int,t0:Float,w:Float):Float {
        val t=t0.coerceIn(0f,1f)
        val topL=w*.43f; val topR=w*.57f; val botL=w*.035f; val botR=w*.965f
        val top=topL+(topR-topL)*(ln+.5f)/3f
        val bottom=botL+(botR-botL)*(ln+.5f)/3f
        return top+(bottom-top)*t
    }

    private fun drawCar(c:Canvas,x:Float,y:Float,cw:Float,ch:Float,color:Int,player:Boolean) {
        p.color=Color.argb(95,0,0,0); c.drawOval(x-cw*.48f,y+ch*.28f,x+cw*.48f,y+ch*.55f,p)
        p.color=Color.rgb(18,22,28)
        c.drawRoundRect(x-cw*.47f,y-ch*.24f,x-cw*.30f,y+ch*.34f,8f,8f,p)
        c.drawRoundRect(x+cw*.30f,y-ch*.24f,x+cw*.47f,y+ch*.34f,8f,8f,p)
        p.color=color; c.drawRoundRect(x-cw*.37f,y-ch*.5f,x+cw*.37f,y+ch*.5f,cw*.16f,cw*.16f,p)
        p.color=Color.rgb(175,222,239); c.drawRoundRect(x-cw*.24f,y-ch*.29f,x+cw*.24f,y+ch*.02f,7f,7f,p)
        p.color = if (player) Color.rgb(255,55,65) else Color.rgb(255,228,145)
        c.drawRoundRect(x-cw*.27f,y+ch*.31f,x-cw*.10f,y+ch*.43f,4f,4f,p)
        c.drawRoundRect(x+cw*.10f,y+ch*.31f,x+cw*.27f,y+ch*.43f,4f,4f,p)
        p.color=Color.argb(100,255,255,255); c.drawLine(x-cw*.29f,y-ch*.35f,x-cw*.29f,y+ch*.18f,p)
    }

    private fun panel(c:Canvas,w:Float,h:Float,title:String,subtitle:String) {
        p.color=Color.argb(218,8,16,26); c.drawRoundRect(w*.07f,h*.22f,w*.93f,h*.76f,30f,30f,p)
        p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
        p.color=Color.WHITE; p.textSize=min(w*.09f,h*.055f); c.drawText(title,w/2,h*.34f,p)
        p.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL); p.textSize=w*.045f
        c.drawText(subtitle,w/2,h*.41f,p); p.textAlign=Paint.Align.LEFT
    }
    private fun button(c:Canvas,w:Float,h:Float,y:Float,label:String,color:Int=Color.rgb(24,157,190)) {
        p.color=color; c.drawRoundRect(w*.20f,y-h*.035f,w*.80f,y+h*.035f,24f,24f,p)
        p.color=Color.WHITE; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create("sans-serif-bold",Typeface.BOLD)
        p.textSize=w*.047f; c.drawText(label,w/2,y+w*.015f,p); p.textAlign=Paint.Align.LEFT
    }
    private fun drawMenu(c:Canvas,w:Float,h:Float) {
        panel(c,w,h,"ROAD RUNNER","Счёт: $best     🟡 $coins монет")
        button(c,w,h,h*.49f,"ИГРАТЬ")
        button(c,w,h,h*.60f,"МАГАЗИН",Color.rgb(180,111,42))
        p.color=Color.rgb(211,232,245); p.textAlign=Paint.Align.CENTER; p.textSize=w*.034f
        c.drawText("Свайп влево/вправо — смена полосы",w/2,h*.69f,p); p.textAlign=Paint.Align.LEFT
    }
    private fun drawShop(c:Canvas,w:Float,h:Float) {
        p.color=Color.argb(235,8,16,26); c.drawRect(0f,0f,w,h,p)
        p.color=Color.WHITE; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
        p.textSize=w*.075f; c.drawText("МАГАЗИН МАШИН",w/2,h*.09f,p)
        p.color=Color.rgb(255,210,70); p.textSize=w*.05f; c.drawText("Монеты: $coins",w/2,h*.15f,p)
        for (i in carColors.indices) {
            val y=h*(.25f+i*.125f)
            p.color=Color.argb(190,30,43,56); c.drawRoundRect(w*.10f,y-h*.045f,w*.90f,y+h*.045f,18f,18f,p)
            drawCar(c,w*.25f,y,w*.13f,h*.065f,carColors[i],true)
            p.color=Color.WHITE; p.textAlign=Paint.Align.LEFT; p.textSize=w*.041f
            c.drawText("Машина ${i+1}",w*.39f,y+h*.008f,p)
            p.textAlign=Paint.Align.RIGHT; p.color=Color.rgb(255,210,70)
            val label=if (selectedCar==i) "ВЫБРАНА" else if (owned.contains(i)) "ВЫБРАТЬ" else "${carPrices[i]} 🟡"
            c.drawText(label,w*.85f,y+h*.008f,p)
        }
        button(c,w,h,h*.91f,"НАЗАД",Color.rgb(80,91,105))
        p.textAlign=Paint.Align.LEFT
    }
    private fun drawHud(c:Canvas,w:Float,h:Float) {
        p.color=Color.argb(190,8,16,26); c.drawRoundRect(w*.035f,h*.025f,w*.61f,h*.125f,18f,18f,p)
        p.color=Color.WHITE; p.typeface=Typeface.create("sans-serif-bold",Typeface.BOLD); p.textSize=w*.045f
        c.drawText("СЧЁТ $score",w*.065f,h*.065f,p)
        p.color=Color.rgb(255,210,70); c.drawText("МОНЕТЫ $coins",w*.065f,h*.108f,p)
        p.color=Color.WHITE; p.textSize=w*.033f; c.drawText("РЕКОРД $best",w*.68f,h*.065f,p)
    }
    private fun drawGameOver(c:Canvas,w:Float,h:Float) {
        p.color=Color.argb(180,5,10,17); c.drawRect(0f,0f,w,h,p)
        panel(c,w,h,"АВАРИЯ!","Счёт: $score   •   Монеты: $coins")
        button(c,w,h,h*.51f,"ИГРАТЬ СНОВА")
        button(c,w,h,h*.62f,"В МЕНЮ",Color.rgb(80,91,105))
    }

    override fun onTouchEvent(e:MotionEvent):Boolean {
        when(e.action) {
            MotionEvent.ACTION_DOWN -> { downX=e.x; downY=e.y; return true }
            MotionEvent.ACTION_UP -> {
                val w=width.toFloat(); val h=height.toFloat(); val dx=e.x-downX; val dy=e.y-downY
                when(screen) {
                    Screen.MENU -> {
                        if(e.y in h*.43f..h*.55f) startGame()
                        else if(e.y in h*.55f..h*.66f) screen=Screen.SHOP
                    }
                    Screen.SHOP -> {
                        if(e.y>h*.86f) screen=Screen.MENU
                        else {
                            val i=((e.y/h-.205f)/.125f).toInt()
                            if(i in carColors.indices) {
                                if(owned.contains(i)) selectedCar=i
                                else if(coins>=carPrices[i]) { coins-=carPrices[i]; owned.add(i); selectedCar=i }
                                saveProgress()
                            }
                        }
                    }
                    Screen.GAME_OVER -> {
                        if(e.y in h*.46f..h*.57f) startGame()
                        else if(e.y in h*.57f..h*.68f) screen=Screen.MENU
                    }
                    Screen.PLAYING -> {
                        if(abs(dx)>w*.06f) lane = (lane + (if (dx < 0) -1 else 1)).coerceIn(0, 2)
                        else if(abs(dy)<h*.08f) lane = (lane + (if (e.x < w / 2) -1 else 1)).coerceIn(0, 2)
                    }
                }
                invalidate(); return true
            }
        }
        return true
    }
    private fun startGame() {
        screen=Screen.PLAYING; lane=1; score=0; scoreCarry=0f; coinCarry=0f; speed=8f
        cars.clear(); spawnTimer=.65f; roadOffset=0f; lastTime=System.nanoTime()
    }
}
