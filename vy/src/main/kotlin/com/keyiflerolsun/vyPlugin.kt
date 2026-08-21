package com.keyiflerolsun

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class vyPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(vy())
    }

    override fun openSettings(context: Context) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Erişim Şifresi")
        
        val input = android.widget.EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Şifrenizi girin"
        input.setText(sharedPref.getString("vy_password", ""))
        
        builder.setView(input)

        builder.setPositiveButton("Kaydet") { dialog, _ ->
            val enteredPassword = input.text.toString()
            sharedPref.edit().putString("vy_password", enteredPassword).apply()
            dialog.dismiss()
        }
        builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
