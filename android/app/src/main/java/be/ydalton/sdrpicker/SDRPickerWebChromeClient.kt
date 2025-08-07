package be.ydalton.sdrpicker

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat.startActivityForResult

class SDRPickerWebChromeClient() : WebChromeClient() {
    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        AlertDialog.Builder(view?.context)
            .setTitle("SDRpicker")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) {
                                                    dialog, _ -> result?.confirm()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> result?.cancel()}
            .create()
            .show()

        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        AlertDialog.Builder(view?.context)
            .setTitle("SDRpicker")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ -> result?.confirm()}
            .create()
            .show()

        return true
    }

    companion object {
        const val REQUEST_FILE_CHOOSER = 1001
    }
}