package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.AcraApplication.getKey
import com.lagradost.cloudstream3.AcraApplication.setKey

@CloudstreamPlugin
class vyPlugin: Plugin() {
    override fun load(androidContext: android.content.Context) {
        registerMainAPI(vy())
    }

    override fun openSettings(androidContext: android.content.Context) {
        val builder = android.app.AlertDialog.Builder(androidContext)
        builder.setTitle("Erişim Şifresi")
        
        val input = android.widget.EditText(androidContext)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Şifrenizi girin"
        input.setText(getKey<String>("vy_password") ?: "")
        
        builder.setView(input)

        builder.setPositiveButton("Kaydet") { dialog, _ ->
            val enteredPassword = input.text.toString()
            setKey("vy_password", enteredPassword)
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
