package dev.example.robloxpc

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * ROPC Runner is a native Android launcher for user-selected program files.
 *
 * Windows executables are handed off to the installed Winlator app.
 * Linux scripts/ELF files are handed off to Termux + a Debian proot-distro.
 * No pretend Windows desktop or bundled fake terminal is shown.
 */
class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var selectedView: TextView
    private lateinit var runtimeView: TextView

    private data class StoredProgram(val name: String, val absolutePath: String)

    companion object {
        private const val PICK_PROGRAM = 410
        private const val TERMUX_PACKAGE = "com.termux"
        private const val WINLATOR_PACKAGE = "com.winlator"
        private const val WINLATOR_CMOD_PACKAGE = "com.winlator.cmod"
        private const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"
        private const val SHARED_DIR = "/storage/emulated/0/Download/ROPC"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildLauncher()
        refreshRuntimeStatus()
    }

    private fun buildLauncher() {
        val background = Color.rgb(15, 19, 27)
        val card = Color.rgb(27, 34, 46)
        val accent = Color.rgb(54, 133, 224)
        val secondary = Color.rgb(165, 178, 198)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(background)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val title = TextView(this).apply {
            text = "ROPC Runner"
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Abra programas reais usando runtimes instalados no Android."
            textSize = 15f
            setTextColor(secondary)
            setPadding(0, dp(8), 0, dp(22))
        }
        root.addView(subtitle, matchWrap())

        val chooseButton = Button(this).apply {
            text = "ESCOLHER ARQUIVO (.EXE / .SH / ELF)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { openProgramPicker() }
        }
        root.addView(chooseButton, matchWrap())

        selectedView = TextView(this).apply {
            text = "Nenhum programa selecionado."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(card)
        }
        val selectedParams = matchWrap().apply { topMargin = dp(18) }
        root.addView(selectedView, selectedParams)

        runtimeView = TextView(this).apply {
            textSize = 13f
            setTextColor(secondary)
            setPadding(dp(2), dp(18), dp(2), dp(8))
        }
        root.addView(runtimeView, matchWrap())

        val explanation = TextView(this).apply {
            text = "COMO FUNCIONA\n\n" +
                "• .EXE / .MSI: o arquivo é copiado para Download/ROPC e o Winlator é aberto. " +
                "No Winlator, selecione o arquivo e crie um atalho para executá-lo.\n\n" +
                "• .SH / ELF Linux: o arquivo é copiado para Download/ROPC e enviado ao " +
                "Termux para execução dentro de uma distribuição Debian via proot-distro.\n\n" +
                "O ROPC não finge ser Windows e não desenha uma área de trabalho falsa."
            textSize = 13f
            setTextColor(secondary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(card)
        }
        val explanationParams = matchWrap().apply { topMargin = dp(12) }
        root.addView(explanation, explanationParams)

        statusView = TextView(this).apply {
            text = "Pronto para selecionar um programa."
            textSize = 13f
            setTextColor(Color.rgb(133, 203, 159))
            setPadding(dp(2), dp(18), dp(2), dp(4))
        }
        root.addView(statusView, matchWrap())

        setContentView(scroll)
    }

    private fun refreshRuntimeStatus() {
        val winlator = isPackageInstalled(WINLATOR_PACKAGE) || isPackageInstalled(WINLATOR_CMOD_PACKAGE)
        val termux = isPackageInstalled(TERMUX_PACKAGE)
        runtimeView.text = buildString {
            append("RUNTIMES DETECTADOS\n")
            append("Winlator: ")
            append(if (winlator) "instalado" else "não instalado")
            append("\nTermux: ")
            append(if (termux) "instalado" else "não instalado")
            append("\nArquiteturas Android: ")
            append(Build.SUPPORTED_ABIS.joinToString())
            append("\n\nLinux exige Termux + proot-distro + Debian.")
        }
    }

    private fun openProgramPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, PICK_PROGRAM)
        } catch (error: Exception) {
            statusView.text = "Não foi possível abrir o seletor de arquivos: ${error.message}"
        }
    }

    @Deprecated("Uses the platform document picker for compatibility with the current project.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PROGRAM || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        statusView.text = "Copiando arquivo para Download/ROPC…"

        Thread {
            try {
                val originalName = queryDisplayName(uri)
                val stored = copyToSharedDownloads(uri, originalName)
                runOnUiThread {
                    selectedView.text = "Arquivo: ${stored.name}\nLocal: ${stored.absolutePath}"
                    routeProgram(stored)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusView.text = "Não foi possível importar o arquivo: ${error.message}"
                }
            }
        }.start()
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return "programa"
    }

    private fun safeFileName(rawName: String): String {
        val cleaned = rawName
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._() -]"), "_")
            .replace(' ', '_')
            .trim('.', '_')
            .take(100)
        return cleaned.ifBlank { "programa.bin" }
    }

    private fun copyToSharedDownloads(source: Uri, originalName: String): StoredProgram {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw IOException("A importação automática exige Android 10 ou superior.")
        }

        val safeName = "${System.currentTimeMillis()}_${safeFileName(originalName)}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ROPC")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val destination = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("O Android não permitiu criar o arquivo em Downloads/ROPC.")

        try {
            val input = contentResolver.openInputStream(source)
                ?: throw IOException("Não foi possível ler o arquivo selecionado.")
            val output = contentResolver.openOutputStream(destination, "w")
                ?: throw IOException("Não foi possível gravar o arquivo importado.")
            input.use { sourceStream -> output.use { targetStream -> sourceStream.copyTo(targetStream) } }

            val completed = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            contentResolver.update(destination, completed, null, null)
        } catch (error: Exception) {
            contentResolver.delete(destination, null, null)
            throw error
        }

        return StoredProgram(
            name = safeName,
            absolutePath = "$SHARED_DIR/$safeName"
        )
    }

    private fun routeProgram(program: StoredProgram) {
        val extension = program.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        when {
            extension == "exe" || extension == "msi" -> openWindowsProgram(program)
            extension == "sh" -> confirmLinuxProgram(program, isElf = false)
            isElfFile(program.absolutePath) -> confirmLinuxProgram(program, isElf = true)
            else -> {
                statusView.text = "Formato não reconhecido. Escolha um .exe, .msi, .sh ou executável ELF Linux."
            }
        }
    }

    private fun isElfFile(path: String): Boolean {
        return try {
            val uri = MediaStore.Files.getContentUri("external")
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf("Download/ROPC/", File(path).name),
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return false
                val id = cursor.getLong(0)
                val fileUri = Uri.withAppendedPath(uri, id.toString())
                contentResolver.openInputStream(fileUri)?.use { stream ->
                    val header = ByteArray(4)
                    stream.read(header) == 4 &&
                        header[0] == 0x7f.toByte() &&
                        header[1] == 'E'.code.toByte() &&
                        header[2] == 'L'.code.toByte() &&
                        header[3] == 'F'.code.toByte()
                } ?: false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun openWindowsProgram(program: StoredProgram) {
        val launchIntent = packageManager.getLaunchIntentForPackage(WINLATOR_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(WINLATOR_CMOD_PACKAGE)
        if (launchIntent == null) {
            statusView.text = "Winlator não está instalado. O arquivo foi salvo em Download/ROPC."
            AlertDialog.Builder(this)
                .setTitle("Instale um runtime Windows")
                .setMessage("Para abrir .EXE no Android, este projeto usa o Winlator (Wine + Box86/Box64). O ROPC não contém um runtime Windows próprio. Depois de instalar, escolha o arquivo em Z:\\storage\\emulated\\0\\Download\\ROPC dentro do Winlator.")
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Ver Winlator") { _, _ ->
                    openWebPage("https://github.com/brunodev85/winlator/releases")
                }
                .show()
            return
        }

        val winlatorPath = "Z:\\storage\\emulated\\0\\Download\\ROPC\\${program.name}"
        AlertDialog.Builder(this)
            .setTitle("Executável Windows importado")
            .setMessage(
                "O arquivo foi copiado para Download/ROPC. O Winlator não oferece uma API pública documentada para iniciar um .EXE diretamente por outro app; vou abrir o Winlator para você selecionar o arquivo e criar um atalho.\n\nCaminho no ambiente Wine:\n$winlatorPath"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir Winlator") { _, _ ->
                try {
                    startActivity(launchIntent)
                    statusView.text = "Winlator aberto. Selecione o arquivo em Z:\\storage\\emulated\\0\\Download\\ROPC."
                } catch (error: Exception) {
                    statusView.text = "Não foi possível iniciar o Winlator: ${error.message}"
                }
            }
            .show()
    }

    private fun confirmLinuxProgram(program: StoredProgram, isElf: Boolean) {
        val kind = if (isElf) "executável ELF Linux" else "script Shell"
        AlertDialog.Builder(this)
            .setTitle("Executar $kind?")
            .setMessage(
                "O arquivo será executado dentro do Debian pelo Termux. Só execute arquivos de origem confiável. É necessário ter Termux, proot-distro e Debian instalados, além de permitir comandos externos no Termux.\n\nArquivo: ${program.name}"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Executar") { _, _ -> runLinuxInTermux(program, isElf) }
            .show()
    }

    private fun runLinuxInTermux(program: StoredProgram, isElf: Boolean) {
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            statusView.text = "Termux não está instalado. O arquivo foi salvo em Download/ROPC."
            AlertDialog.Builder(this)
                .setTitle("Instale o Termux")
                .setMessage("Instale o Termux pelo projeto oficial. Depois configure proot-distro e Debian para executar programas Linux.")
                .setPositiveButton("Ver Termux") { _, _ ->
                    openWebPage("https://github.com/termux/termux-app/releases")
                }
                .setNegativeButton("Fechar", null)
                .show()
            return
        }

        if (checkSelfPermission(TERMUX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle("Permissão do Termux necessária")
                .setMessage(
                    "Ative a permissão adicional “Executar comandos no ambiente Termux” para o ROPC Runner nas configurações de aplicativos do Android. Depois, no Termux, configure allow-external-apps=true em ~/.termux/termux.properties e instale Debian com proot-distro."
                )
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Abrir permissões") { _, _ ->
                    val settings = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(settings)
                }
                .show()
            return
        }

        val guestPath = "/mnt/ropc/${program.name}"
        val guestCommand = if (isElf) {
            "chmod +x ${shellQuote(guestPath)} && exec ${shellQuote(guestPath)}"
        } else {
            "bash ${shellQuote(guestPath)}"
        }
        val termuxCommand = buildString {
            append("if ! command -v proot-distro >/dev/null 2>&1; then ")
            append("echo 'Runtime Linux ausente. Execute: pkg update && pkg install proot-distro'; exit 127; fi; ")
            append("proot-distro login debian --bind ")
            append(shellQuote("$SHARED_DIR:/mnt/ropc"))
            append(" -- /bin/bash -lc ")
            append(shellQuote(guestCommand))
        }

        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", termuxCommand))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }

        try {
            startService(intent)
            statusView.text = "Comando enviado ao Termux. Se Debian ainda não estiver instalado, configure-o no Termux primeiro."
        } catch (error: SecurityException) {
            statusView.text = "Termux recusou o comando. Confira a permissão RUN_COMMAND e allow-external-apps."
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

    private fun openWebPage(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            statusView.text = "Não foi possível abrir o navegador."
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_O && event.isCtrlPressed) {
            openProgramPicker()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
