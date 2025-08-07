package be.ydalton.sdrpicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SDRPickerActivity : ComponentActivity() {
    private val kiwipickerExecutableName = "libkiwipicker.so"
    private var kiwipickerProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shouldStart = startKiwipicker();

        shouldStart.invokeOnCompletion {
            runOnUiThread {
                loadWebView()
            }
        }
    }

    private fun startKiwipicker(): CompletableDeferred<Unit> {
        val ready = CompletableDeferred<Unit>()

        val serverBinary = File(cacheDir, kiwipickerExecutableName)

        assets.open("kiwipicker").use { inputStream ->
            serverBinary.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        serverBinary.setExecutable(true)

        extractWebfolder()

        Log.d(kiwipickerExecutableName, "Starting executable...");

        val process: Process = Runtime.getRuntime().exec(serverBinary.absolutePath, null, cacheDir)

        kiwipickerProcess = process

        Thread {
            // strangely, the Go executable does not output anything
            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    Log.i(kiwipickerExecutableName, line)
                }
            }
        }.start()

        ready.complete(Unit)

        return ready
    }

    private fun extractWebfolder() {
        val staticFolder = File(cacheDir, "static")
        if (staticFolder.exists()) {
            // execute remove
            staticFolder.deleteRecursively()
        }
        staticFolder.mkdir()

        Log.d(kiwipickerExecutableName, "Extracting web folder...")

        val zip = ZipInputStream(assets.open("static.zip"))
        var entry: ZipEntry? = zip.nextEntry

        while (entry != null) {
            val outFile = File(staticFolder, entry.name)

            outFile.outputStream().use { output ->
                zip.copyTo(output)
            }

            zip.closeEntry()
            entry = zip.nextEntry
        }

        zip.close()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView() {
        val webView = WebView(this)
        webView.webChromeClient = SDRPickerWebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("http://localhost:3000")
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        kiwipickerProcess?.destroy()
    }
}