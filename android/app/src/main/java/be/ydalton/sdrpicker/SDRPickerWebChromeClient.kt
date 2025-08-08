package be.ydalton.sdrpicker

import android.app.AlertDialog
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

open class SDRPickerWebChromeClient() : WebChromeClient() {
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
}