package be.ydalton.sdrpicker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.core.net.toUri

class SDRPickerActivity : ComponentActivity() {
    private val kiwipickerExecutableName = "libkiwipicker.so"
    private var kiwipickerProcess: Process? = null
    private var uploadCallback: ValueCallback<Array<out Uri?>?>? = null

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1001
    }

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
        webView.webChromeClient = object : SDRPickerWebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<out Uri?>?>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                    }
                }

                if (uploadCallback != null) {
                    uploadCallback?.onReceiveValue(null)
                    uploadCallback = null
                }

                uploadCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                // SQLite file is not a valid mimetype according to Android
                intent.type = "*/*"
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)

                return true
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("http://localhost:3000")
        setContentView(webView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (uploadCallback == null)
                return;

            var result: Array<Uri>? = null

            if (resultCode == RESULT_OK) {
                if (intent != null) {
                    val dataString = intent.dataString;
                    if (dataString != null) {
                        result = arrayOf(dataString.toUri())
                    }
                }
            }

            uploadCallback?.onReceiveValue(result)
            uploadCallback = null;
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        kiwipickerProcess?.destroy()
    }
}