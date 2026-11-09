package dev.example.robloxpc

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView

/**
 * Native Android prototype shell.
 * Detects physical input devices. It does not spoof a PC, modify Roblox,
 * bypass anti-cheat, or run the Windows Roblox client.
 */
class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            textSize = 16f
            setPadding(28, 32, 28, 32)
        }
        setContentView(status)
        status.isFocusableInTouchMode = true
        status.requestFocus()
        refreshDevices()
    }

    override fun onResume() {
        super.onResume()
        refreshDevices()
    }

    private fun refreshDevices() {
        val abis: String = Build.SUPPORTED_ABIS.joinToString()
        val keyboards = mutableListOf<InputDevice>()
        val mice = mutableListOf<InputDevice>()

        for (deviceId: Int in InputDevice.getDeviceIds()) {
            val device: InputDevice = InputDevice.getDevice(deviceId) ?: continue
            val sources: Int = device.sources

            if ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                keyboards.add(device)
            }
            if ((sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) {
                mice.add(device)
            }
        }

        status.text = listOf(
            "Roblox PC Mode — protótipo",
            "",
            "Arquitetura Android: $abis",
            "Teclados físicos detectados: ${keyboards.size}",
            "Mouses físicos detectados: ${mice.size}",
            "",
            "Entrada: ${if (keyboards.isNotEmpty()) "teclado OK" else "aguardando teclado"} / ${if (mice.isNotEmpty()) "mouse OK" else "aguardando mouse"}",
            "",
            "Esta versão é somente a base Android de entrada e interface.",
            "Ainda NÃO executa o Roblox para Windows nem inclui Wine/Box64.",
            "Sem botões virtuais e sem streaming."
        ).joinToString("\n")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        status.contentDescription = "Tecla física recebida: ${event.keyCode}"
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Mouse movement follows Android's native input pipeline.
        return super.onGenericMotionEvent(event)
    }
}
