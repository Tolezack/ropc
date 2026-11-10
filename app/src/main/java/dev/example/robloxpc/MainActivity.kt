package dev.example.robloxpc

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * ROPC Desktop: a local desktop-style interface running as a native Android app.
 * It is a user-interface simulator, not a Windows emulator or a Roblox client.
 */
class MainActivity : Activity() {
    private lateinit var desktop: DesktopView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        desktop = DesktopView(this)
        setContentView(desktop)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        desktop.isFocusableInTouchMode = true
        desktop.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        if (::desktop.isInitialized) desktop.invalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (::desktop.isInitialized && desktop.handleKey(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }
}

private enum class DesktopApp(val title: String, val symbol: String) {
    THIS_PC("Este PC", "PC"),
    FILES("Arquivos", "AR"),
    BROWSER("Navegador", "WEB"),
    TERMINAL("Terminal", ">_"),
    SETTINGS("Configurações", "CFG")
}

private data class DesktopWindow(
    val app: DesktopApp,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var maximized: Boolean = false,
    var minimized: Boolean = false
)

private class DesktopView(activity: Activity) : View(activity) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val windows = mutableListOf<DesktopWindow>()
    private var activeWindow: DesktopWindow? = null
    private var startMenuOpen = false
    private var dragWindow: DesktopWindow? = null
    private var downX = 0f
    private var downY = 0f
    private var dragOriginX = 0f
    private var dragOriginY = 0f
    private var dragged = false
    private var terminalInput = ""
    private val terminalLines = mutableListOf(
        "ROPC Desktop Terminal",
        "Digite help para ver os comandos disponíveis."
    )

    private val taskbarHeight = 54f
    private val titlebarHeight = 38f
    private val apps = DesktopApp.values()

    private fun dp(value: Float): Float = value * density
    private fun color(hex: Long): Int = Color.rgb(
        ((hex shr 16) and 0xFF).toInt(),
        ((hex shr 8) and 0xFF).toInt(),
        (hex and 0xFF).toInt()
    )

    private fun drawText(
        canvas: Canvas,
        value: String,
        x: Float,
        baseline: Float,
        size: Float = 14f,
        tint: Int = Color.WHITE,
        bold: Boolean = false
    ) {
        paint.shader = null
        paint.color = tint
        paint.textSize = dp(size)
        paint.typeface = if (bold) android.graphics.Typeface.create(
            "sans-serif", android.graphics.Typeface.BOLD
        ) else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        canvas.drawText(value, dp(x), dp(baseline), paint)
    }

    private fun fill(canvas: Canvas, rect: RectF, tint: Int, radius: Float = 0f) {
        paint.shader = null
        paint.color = tint
        canvas.drawRoundRect(rect, dp(radius), dp(radius), paint)
    }

    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, tint: Int, width: Float = 1f) {
        paint.shader = null
        paint.color = tint
        paint.strokeWidth = dp(width)
        canvas.drawLine(dp(x1), dp(y1), dp(x2), dp(y2), paint)
    }

    private fun screenWidth(): Float = width / density
    private fun screenHeight(): Float = height / density
    private fun taskbarTop(): Float = screenHeight() - taskbarHeight

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sw = screenWidth()
        val sh = screenHeight()

        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            color(0x102B55), color(0x176B78), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Abstract desktop wallpaper shapes.
        paint.color = Color.argb(28, 255, 255, 255)
        canvas.drawCircle(width * 0.78f, height * 0.30f, dp(150f), paint)
        paint.color = Color.argb(18, 255, 255, 255)
        canvas.drawCircle(width * 0.84f, height * 0.40f, dp(230f), paint)

        drawText(canvas, "ROPC DESKTOP", 18f, 30f, 12f, color(0xDCEBFF), true)
        drawDesktopIcons(canvas)

        windows.filter { !it.minimized }.forEach { drawWindow(canvas, it) }
        if (startMenuOpen) drawStartMenu(canvas)
        drawTaskbar(canvas, sw, sh)
    }

    private fun drawDesktopIcons(canvas: Canvas) {
        val labels = listOf(
            DesktopApp.THIS_PC,
            DesktopApp.FILES,
            DesktopApp.BROWSER,
            DesktopApp.TERMINAL,
            DesktopApp.SETTINGS
        )
        labels.forEachIndexed { index, app ->
            val top = 56f + index * 76f
            val box = RectF(dp(18f), dp(top), dp(62f), dp(top + 42f))
            fill(canvas, box, Color.argb(55, 255, 255, 255), 10f)
            drawText(canvas, app.symbol, 27f, top + 27f, 13f, Color.WHITE, true)
            drawText(canvas, app.title, 12f, top + 57f, 10f, Color.WHITE)
        }
    }

    private fun windowBounds(win: DesktopWindow): RectF {
        if (win.maximized) {
            return RectF(8f, 8f, screenWidth() - 8f, taskbarTop() - 6f)
        }
        return RectF(win.x, win.y, win.x + win.width, win.y + win.height)
    }

    private fun drawWindow(canvas: Canvas, win: DesktopWindow) {
        val r = windowBounds(win)
        val left = r.left
        val top = r.top
        val w = r.width()
        val h = r.height()

        fill(canvas, RectF(dp(left + 3), dp(top + 5), dp(r.right + 3), dp(r.bottom + 5)), Color.argb(70, 0, 0, 0), 13f)
        fill(canvas, RectF(dp(r.left), dp(r.top), dp(r.right), dp(r.bottom)), color(0xF4F7FC), 12f)
        fill(canvas, RectF(dp(left), dp(top), dp(r.right), dp(top + titlebarHeight)), color(0x172B49), 12f)
        fill(canvas, RectF(dp(left), dp(top + 25f), dp(r.right), dp(top + titlebarHeight)), color(0x172B49), 0f)

        drawText(canvas, win.app.symbol, left + 14f, top + 25f, 12f, color(0xA8D8FF), true)
        drawText(canvas, win.app.title, left + 48f, top + 25f, 13f, Color.WHITE, true)

        val buttonY = top + 19f
        val closeX = r.right - 22f
        val maxX = r.right - 48f
        val minX = r.right - 74f
        fill(canvas, RectF(dp(minX - 8), dp(buttonY - 12), dp(minX + 8), dp(buttonY + 12)), color(0x29415F), 5f)
        fill(canvas, RectF(dp(maxX - 8), dp(buttonY - 12), dp(maxX + 8), dp(buttonY + 12)), color(0x29415F), 5f)
        fill(canvas, RectF(dp(closeX - 8), dp(buttonY - 12), dp(closeX + 8), dp(buttonY + 12)), color(0xB94B57), 5f)
        line(canvas, minX - 4f, buttonY + 3f, minX + 4f, buttonY + 3f, Color.WHITE, 1.5f)
        fill(canvas, RectF(dp(maxX - 4f), dp(buttonY - 5f), dp(maxX + 4f), dp(buttonY + 3f)), Color.TRANSPARENT, 0f)
        line(canvas, maxX - 4f, buttonY - 5f, maxX + 4f, buttonY - 5f, Color.WHITE, 1f)
        line(canvas, maxX - 4f, buttonY - 5f, maxX - 4f, buttonY + 3f, Color.WHITE, 1f)
        line(canvas, maxX + 4f, buttonY - 5f, maxX + 4f, buttonY + 3f, Color.WHITE, 1f)
        line(canvas, maxX - 4f, buttonY + 3f, maxX + 4f, buttonY + 3f, Color.WHITE, 1f)
        line(canvas, closeX - 3f, buttonY - 4f, closeX + 3f, buttonY + 4f, Color.WHITE, 1.5f)
        line(canvas, closeX + 3f, buttonY - 4f, closeX - 3f, buttonY + 4f, Color.WHITE, 1.5f)

        val contentTop = top + titlebarHeight + 18f
        val contentLeft = left + 20f
        when (win.app) {
            DesktopApp.THIS_PC -> drawThisPc(canvas, contentLeft, contentTop, w)
            DesktopApp.FILES -> drawFiles(canvas, contentLeft, contentTop, w)
            DesktopApp.BROWSER -> drawBrowser(canvas, contentLeft, contentTop, w)
            DesktopApp.TERMINAL -> drawTerminal(canvas, contentLeft, contentTop, w, h)
            DesktopApp.SETTINGS -> drawSettings(canvas, contentLeft, contentTop, w)
        }
    }

    private fun drawThisPc(canvas: Canvas, x: Float, y: Float, w: Float) {
        drawText(canvas, "Bem-vindo ao ROPC Desktop", x, y + 4f, 19f, color(0x20334F), true)
        drawText(canvas, "Um ambiente de desktop local dentro do Android.", x, y + 30f, 12f, color(0x58677B))
        val (keyboards, mice) = inputCounts()
        val rows = listOf(
            "Arquitetura Android: ${Build.SUPPORTED_ABIS.joinToString()}",
            "Teclados físicos detectados: $keyboards",
            "Mouses físicos detectados: $mice",
            "Modo: simulador de interface desktop"
        )
        rows.forEachIndexed { index, value ->
            val yy = y + 74f + index * 34f
            fill(canvas, RectF(dp(x), dp(yy - 18f), dp(x + min(w - 40f, 500f)), dp(yy + 9f)), color(0xE7EDF5), 6f)
            drawText(canvas, value, x + 12f, yy, 11f, color(0x263A55))
        }
        drawText(canvas, "Isto não é Windows e não executa aplicativos .exe.", x, y + 235f, 11f, color(0x8C3E45), true)
    }

    private fun drawFiles(canvas: Canvas, x: Float, y: Float, w: Float) {
        drawText(canvas, "Explorador de arquivos", x, y + 4f, 18f, color(0x20334F), true)
        drawText(canvas, "Área demonstrativa — integração com arquivos Android ainda não ativada.", x, y + 29f, 11f, color(0x58677B))
        val entries = listOf("Este PC", "Armazenamento do dispositivo", "Downloads", "Documentos")
        entries.forEachIndexed { index, name ->
            val yy = y + 75f + index * 42f
            fill(canvas, RectF(dp(x), dp(yy - 24f), dp(x + min(w - 40f, 420f)), dp(yy + 10f)), color(0xE7EDF5), 6f)
            drawText(canvas, "▰", x + 12f, yy, 14f, color(0x287BBE), true)
            drawText(canvas, name, x + 40f, yy, 12f, color(0x263A55))
        }
    }

    private fun drawBrowser(canvas: Canvas, x: Float, y: Float, w: Float) {
        drawText(canvas, "Navegador", x, y + 4f, 18f, color(0x20334F), true)
        fill(canvas, RectF(dp(x), dp(y + 24f), dp(x + min(w - 40f, 520f)), dp(y + 58f)), color(0xE7EDF5), 8f)
        drawText(canvas, "Endereço da página (demonstração)", x + 12f, y + 46f, 11f, color(0x66758A))
        drawText(canvas, "O navegador ainda não está conectado à internet.", x, y + 92f, 12f, color(0x263A55))
        drawText(canvas, "Esta janela é parte da interface simulada.", x, y + 116f, 11f, color(0x66758A))
    }

    private fun drawTerminal(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val left = x - 8f
        val top = y - 8f
        val right = x + min(w - 24f, 620f)
        val bottom = top + max(120f, h - titlebarHeight - 28f)
        fill(canvas, RectF(dp(left), dp(top), dp(right), dp(bottom)), color(0x101923), 8f)
        var baseline = top + 26f
        val maxLines = max(3, ((bottom - top - 66f) / 19f).toInt())
        terminalLines.takeLast(maxLines).forEach { lineText ->
            drawText(canvas, lineText, left + 12f, baseline, 11f, color(0xB6E6C4))
            baseline += 19f
        }
        drawText(canvas, "ropc@android:~$ ${terminalInput}_", left + 12f, bottom - 18f, 11f, color(0xFFFFFF))
    }

    private fun drawSettings(canvas: Canvas, x: Float, y: Float, w: Float) {
        drawText(canvas, "Configurações", x, y + 4f, 18f, color(0x20334F), true)
        val rows = listOf(
            "Interface: ROPC Desktop",
            "Entrada: teclado e mouse físicos",
            "ABIs: ${Build.SUPPORTED_ABIS.joinToString()}",
            "Streaming: desativado",
            "Botões virtuais de jogo: não utilizados"
        )
        rows.forEachIndexed { index, value ->
            drawText(canvas, value, x, y + 42f + index * 30f, 12f, color(0x354A65))
            line(canvas, x, y + 51f + index * 30f, x + min(w - 40f, 480f), y + 51f + index * 30f, color(0xD9E1EA))
        }
    }

    private fun drawStartMenu(canvas: Canvas) {
        val left = 12f
        val top = taskbarTop() - 232f
        val right = min(screenWidth() - 12f, 286f)
        fill(canvas, RectF(dp(left + 2), dp(top + 4), dp(right + 2), dp(taskbarTop() - 8f)), Color.argb(90, 0, 0, 0), 14f)
        fill(canvas, RectF(dp(left), dp(top), dp(right), dp(taskbarTop() - 12f)), color(0xF1F5FB), 14f)
        drawText(canvas, "ROPC", left + 16f, top + 28f, 16f, color(0x20334F), true)
        drawText(canvas, "Aplicativos", left + 16f, top + 52f, 10f, color(0x69788C), true)
        apps.forEachIndexed { index, app ->
            val rowTop = top + 62f + index * 32f
            fill(canvas, RectF(dp(left + 8f), dp(rowTop), dp(right - 8f), dp(rowTop + 28f)), color(0xE5ECF5), 6f)
            drawText(canvas, app.symbol, left + 18f, rowTop + 19f, 10f, color(0x2774B9), true)
            drawText(canvas, app.title, left + 52f, rowTop + 19f, 11f, color(0x263A55))
        }
    }

    private fun drawTaskbar(canvas: Canvas, sw: Float, sh: Float) {
        fill(canvas, RectF(0f, dp(sh - taskbarHeight), dp(sw), dp(sh)), color(0xE8162943), 0f)
        fill(canvas, RectF(dp(12f), dp(sh - 44f), dp(104f), dp(sh - 10f)), color(0x2D4565), 10f)
        drawText(canvas, "⊞  Iniciar", 25f, sh - 22f, 12f, Color.WHITE, true)

        val taskApps = windows.filter { true }
        taskApps.take(5).forEachIndexed { index, win ->
            val x = 120f + index * 78f
            val tint = if (win == activeWindow && !win.minimized) color(0x42668E) else color(0x233A58)
            fill(canvas, RectF(dp(x), dp(sh - 44f), dp(x + 70f), dp(sh - 10f)), tint, 8f)
            drawText(canvas, win.app.symbol.take(4), x + 10f, sh - 22f, 10f, Color.WHITE, true)
        }
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        drawText(canvas, clock, max(120f, sw - 58f), sh - 22f, 12f, Color.WHITE, true)
    }

    private fun inputCounts(): Pair<Int, Int> {
        var keyboardCount = 0
        var mouseCount = 0
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            if ((device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) keyboardCount++
            if ((device.sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) mouseCount++
        }
        return keyboardCount to mouseCount
    }

    private fun openApp(app: DesktopApp) {
        windows.firstOrNull { it.app == app }?.let {
            it.minimized = false
            windows.remove(it)
            windows.add(it)
            activeWindow = it
            startMenuOpen = false
            invalidate()
            return
        }
        val sw = screenWidth()
        val sh = taskbarTop()
        val winWidth = min(650f, max(280f, sw - 30f))
        val winHeight = min(440f, max(210f, sh - 40f))
        val win = DesktopWindow(
            app = app,
            x = max(8f, (sw - winWidth) / 2f),
            y = max(8f, (sh - winHeight) / 2f - 8f),
            width = winWidth,
            height = winHeight
        )
        windows.add(win)
        activeWindow = win
        startMenuOpen = false
        invalidate()
    }

    private fun closeWindow(win: DesktopWindow) {
        windows.remove(win)
        if (activeWindow == win) activeWindow = windows.lastOrNull { !it.minimized }
        invalidate()
    }

    private fun handleTap(x: Float, y: Float) {
        val sw = screenWidth()
        val sh = screenHeight()
        if (y >= taskbarTop()) {
            if (x <= 112f) {
                startMenuOpen = !startMenuOpen
                invalidate()
                return
            }
            val index = ((x - 120f) / 78f).toInt()
            val taskApps = windows.take(5)
            if (index >= 0 && index < taskApps.size) {
                val win = taskApps[index]
                win.minimized = !win.minimized
                if (!win.minimized) {
                    windows.remove(win)
                    windows.add(win)
                    activeWindow = win
                } else {
                    activeWindow = windows.lastOrNull { !it.minimized }
                }
                invalidate()
            }
            return
        }

        if (startMenuOpen) {
            val top = taskbarTop() - 232f
            if (x in 12f..286f && y >= top + 62f && y < top + 62f + apps.size * 32f) {
                val index = ((y - (top + 62f)) / 32f).toInt()
                if (index in apps.indices) openApp(apps[index])
                return
            }
            startMenuOpen = false
        }

        val win = windows.lastOrNull { !it.minimized && windowBounds(it).contains(x, y) }
        if (win != null) {
            activeWindow = win
            val r = windowBounds(win)
            if (y <= r.top + titlebarHeight) {
                when {
                    x >= r.right - 34f -> closeWindow(win)
                    x >= r.right - 60f -> {
                        win.maximized = !win.maximized
                        invalidate()
                    }
                    x >= r.right - 86f -> {
                        win.minimized = true
                        activeWindow = windows.lastOrNull { !it.minimized && it != win }
                        invalidate()
                    }
                }
            }
            invalidate()
            return
        }

        val iconIndex = ((y - 56f) / 76f).toInt()
        if (x <= 92f && iconIndex in apps.indices && y >= 56f && y < 56f + apps.size * 76f) {
            openApp(apps[iconIndex])
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x / density
        val y = event.y / density
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                dragged = false
                val hit = windows.lastOrNull { !it.minimized && windowBounds(it).contains(x, y) }
                if (hit != null) {
                    activeWindow = hit
                    val r = windowBounds(hit)
                    if (!hit.maximized && y <= r.top + titlebarHeight && x < r.right - 88f) {
                        dragWindow = hit
                        dragOriginX = hit.x
                        dragOriginY = hit.y
                    } else {
                        dragWindow = null
                    }
                    windows.remove(hit)
                    windows.add(hit)
                } else {
                    dragWindow = null
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val target = dragWindow
                if (target != null) {
                    val dx = x - downX
                    val dy = y - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 3f) dragged = true
                    if (dragged) {
                        target.x = max(0f, min(screenWidth() - target.width, dragOriginX + dx))
                        target.y = max(0f, min(taskbarTop() - titlebarHeight, dragOriginY + dy))
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragged) handleTap(x, y)
                dragWindow = null
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragWindow = null
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_HOVER_MOVE || event.action == MotionEvent.ACTION_SCROLL) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    fun handleKey(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (activeWindow?.app == DesktopApp.TERMINAL) {
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    if (terminalInput.isNotEmpty()) terminalInput = terminalInput.dropLast(1)
                    invalidate()
                    return true
                }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    runTerminalCommand()
                    invalidate()
                    return true
                }
                else -> {
                    val character = event.unicodeChar
                    if (character != 0 && !character.toChar().isISOControl()) {
                        terminalInput += character.toChar()
                        invalidate()
                        return true
                    }
                }
            }
        }
        when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                if (startMenuOpen) {
                    startMenuOpen = false
                    invalidate()
                    return true
                }
            }
            KeyEvent.KEYCODE_F4 -> {
                activeWindow?.let { closeWindow(it) }
                return true
            }
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                startMenuOpen = !startMenuOpen
                invalidate()
                return true
            }
        }
        return false
    }

    private fun runTerminalCommand() {
        val command = terminalInput.trim()
        terminalLines.add("ropc@android:~$ $command")
        when (command.lowercase(Locale.ROOT)) {
            "help" -> terminalLines.add("Comandos: help, about, clear")
            "about" -> terminalLines.add("ROPC Desktop é uma interface simulada em Android.")
            "clear" -> terminalLines.clear()
            "" -> Unit
            else -> terminalLines.add("Comando não encontrado: $command")
        }
        terminalInput = ""
        if (terminalLines.size > 80) terminalLines.subList(0, terminalLines.size - 80).clear()
    }
}
