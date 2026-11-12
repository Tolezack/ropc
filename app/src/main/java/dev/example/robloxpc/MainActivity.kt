package dev.example.robloxpc

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ROPC is a small controller for Cordial running in Termux + Debian.
 * The actual Roblox runtime is Cordial; this APK is not an emulator.
 */
class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var runtimeView: TextView

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val X11_PACKAGE = "com.termux.x11"
        private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        private const val SCRIPT_URL =
            "https://raw.githubusercontent.com/Tolezack/ropc/main/scripts/ropc-cordial.sh"
        private const val SCRIPT_HOME = "/data/data/com.termux/files/home/.local/share/ropc-cordial"
        private const val SCRIPT_PATH = "/data/data/com.termux/files/home/.local/share/ropc-cordial/ropc-cordial.sh"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::runtimeView.isInitialized) refreshStatus()
    }

    private fun buildUi() {
        val background = Color.rgb(13, 18, 27)
        val card = Color.rgb(25, 34, 48)
        val accent = Color.rgb(48, 126, 220)
        val green = Color.rgb(126, 211, 155)
        val secondary = Color.rgb(176, 190, 210)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(background)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        content.addView(TextView(this).apply {
            text = "ROPC"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, matchWrap())

        content.addView(TextView(this).apply {
            text = "Roblox ARM64 experimental"
            textSize = 17f
            setTextColor(green)
            setPadding(0, dp(3), 0, dp(18))
        }, matchWrap())

        runtimeView = TextView(this).apply {
            textSize = 13f
            setTextColor(secondary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(card)
        }
        content.addView(runtimeView, matchWrap())

        val setup = Button(this).apply {
            text = "1. PREPARAR CORDIAL + DEBIAN"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { runTermuxAction("setup") }
        }
        val setupParams = matchWrap().apply { topMargin = dp(18) }
        content.addView(setup, setupParams)

        val launch = Button(this).apply {
            text = "2. INICIAR ROBLOX"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(38, 145, 91)))
            setOnClickListener { runTermuxAction("run") }
        }
        val launchParams = matchWrap().apply { topMargin = dp(8) }
        content.addView(launch, launchParams)

        val explanation = TextView(this).apply {
            text = "O QUE ESTE APP FAZ\n\n" +
                "• Prepara o Debian ARM64 no Termux.\n" +
                "• Baixa e verifica o pacote Cordial ARM64 e o wrapper Vulkan.\n" +
                "• Inicia o Termux:X11 e abre o Cordial.\n\n" +
                "Na primeira abertura do Cordial, escolha “Download Roblox” para baixar " +
                "o cliente oficial. Isso precisa de internet e espaço livre."
            textSize = 13f
            setTextColor(secondary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(card)
        }
        val explanationParams = matchWrap().apply { topMargin = dp(14) }
        content.addView(explanation, explanationParams)

        val warning = TextView(this).apply {
            text = "IMPORTANTE: Cordial executa o cliente ANDROID do Roblox dentro do Linux, " +
                "não o Roblox Player de Windows. Alguns jogos podem continuar mostrando a interface " +
                "móvel ou bloquear clientes Android. O Cordial ARM64 ainda não foi validado pelos " +
                "desenvolvedores em celulares reais; esta integração é experimental."
            textSize = 12f
            setTextColor(Color.rgb(245, 199, 120))
            setPadding(dp(4), dp(16), dp(4), dp(12))
        }
        content.addView(warning, matchWrap())

        statusView = TextView(this).apply {
            text = "Pronto."
            textSize = 13f
            setTextColor(green)
            setPadding(dp(3), dp(8), dp(3), dp(8))
        }
        content.addView(statusView, matchWrap())

        setContentView(scroll)
    }

    private fun refreshStatus() {
        val termux = isPackageInstalled(TERMUX_PACKAGE)
        val x11 = isPackageInstalled(X11_PACKAGE)
        runtimeView.text = buildString {
            append("APARELHO\n")
            append("Android: ").append(Build.VERSION.RELEASE).append('\n')
            append("ABI: ").append(Build.SUPPORTED_ABIS.joinToString()).append("\n\n")
            append("TERMUX: ").append(if (termux) "instalado" else "não instalado").append('\n')
            append("TERMUX:X11: ").append(if (x11) "instalado" else "não instalado").append('\n')
            append("Debian/Cordial: verificado durante a preparação")
        }
    }

    private fun runTermuxAction(action: String) {
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            AlertDialog.Builder(this)
                .setTitle("Termux necessário")
                .setMessage("Instale o Termux oficial e o aplicativo Termux:X11 antes de continuar.")
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Baixar Termux") { _, _ ->
                    openUrl("https://github.com/termux/termux-app/releases")
                }
                .show()
            return
        }

        if (!isPackageInstalled(X11_PACKAGE)) {
            AlertDialog.Builder(this)
                .setTitle("Termux:X11 necessário")
                .setMessage("Instale o APK complementar Termux:X11. Ele é a tela gráfica onde o Cordial será exibido.")
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Baixar Termux:X11") { _, _ ->
                    openUrl("https://github.com/termux/termux-x11/releases")
                }
                .show()
            return
        }

        if (checkSelfPermission(RUN_COMMAND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle("Permissão do Termux")
                .setMessage(
                    "Para o ROPC controlar o Termux, faça duas coisas:\n\n" +
                        "1. No Termux, abra ~/.termux/termux.properties e defina allow-external-apps=true; depois execute termux-reload-settings.\n" +
                        "2. Nas configurações do Android, permita que o ROPC execute comandos no Termux.\n\n" +
                        "Depois volte aqui e tente novamente."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Abrir Termux") { _, _ ->
                    openAppDetails(TERMUX_PACKAGE)
                }
                .show()
            return
        }

        val command = buildString {
            append("mkdir -p ").append(shellQuote(SCRIPT_HOME)).append(" && ")
            append("curl -fL --retry 3 ").append(shellQuote(SCRIPT_URL))
            append(" -o ").append(shellQuote(SCRIPT_PATH)).append(" && ")
            append("chmod +x ").append(shellQuote(SCRIPT_PATH)).append(" && ")
            append("bash ").append(shellQuote(SCRIPT_PATH)).append(' ').append(action)
        }

        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }

        try {
            startService(intent)
            statusView.text = if (action == "setup") {
                "Preparação enviada ao Termux. Acompanhe o progresso na janela do Termux."
            } else {
                "Inicialização enviada ao Termux. O Termux:X11 deverá abrir o Cordial."
            }
        } catch (error: SecurityException) {
            statusView.text = "O Android bloqueou o comando. Confira RUN_COMMAND e allow-external-apps."
            openAppDetails(TERMUX_PACKAGE)
        } catch (error: Exception) {
            statusView.text = "Não foi possível iniciar o Termux: ${error.message}"
        }
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openAppDetails(packageName: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            statusView.text = "Abra Configurações → Aplicativos → Termux manualmente."
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            statusView.text = "Não foi possível abrir o link."
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
}
