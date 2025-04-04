package org.lineageos.aperture.ui

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import org.lineageos.aperture.R

class InactivityDialog(activity: Activity) : AlertDialog(activity) {
    private var countdownDuration = INACTIVITY_CLOSE_APP_DELAY

    var onResultCallback: (Boolean) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        setTitle(R.string.inactivity_dialog_title)
        setMessage(context.getString(R.string.inactivity_dialog_message))

        setButton(
            DialogInterface.BUTTON_NEGATIVE,
            context.getString(android.R.string.cancel),
        ) { dialog, _ -> dialog.dismiss() }

        setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(android.R.string.ok),
        ) { _, _ -> onResultCallback(true) }

        super.onCreate(savedInstanceState)
    }

    override fun show() {
        super.show()
        countdownDuration = INACTIVITY_CLOSE_APP_DELAY
        updateButtonText()
    }

    fun updateCountdown() {
        countdownDuration -= 1
        if (countdownDuration <= 0) {
            onResultCallback(true)
            return
        }

        updateButtonText()
        onResultCallback(false)
    }

    private fun updateButtonText() {
        val buttonText = "${context.getString(android.R.string.ok)} ($countdownDuration)"
        getButton(DialogInterface.BUTTON_POSITIVE).text = buttonText
    }

    companion object {
        // Delay when inactivity duration is reached, after which the app will close.
        private const val INACTIVITY_CLOSE_APP_DELAY = 10
    }
}
