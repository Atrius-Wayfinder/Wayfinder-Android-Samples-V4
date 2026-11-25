package com.myco

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.locuslabs.sdk.llpublic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeadlessModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.headless_mode)

        startHeadlessMode()
    }

    private fun startHeadlessMode() {

        // Once LLConfiguration's applicationContext and accountID are set (see MyApplication), the SDK can perform all
        // headless operations without a map: venue data (LLVenueDatabase), POIs and search results (LLPOIDatabase),
        // and directions (LLNavigationDatabase). For example, get the details for a POI:
        val llpoiDatabase = LLPOIDatabase()

        // Data APIs can run on a background thread (recommended), then UI work is posted back to the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            llpoiDatabase.getPOIDetails("lax", "870", object : LLOnGetPOIDetailsCallback {
                override fun successCallback(poi: LLPOI) {
                    val message = "Name: ${poi.name}\nID: ${poi.id}\nDesc: ${poi.description}"
                    runOnUiThread {
                        val dialog = AlertDialog.Builder(this@HeadlessModeActivity)
                        dialog.setMessage(message)
                        dialog.setTitle("Got POI details in headless mode")
                        dialog.setPositiveButton("OK", null)
                        dialog.create().show()
                    }
                }

                override fun failureCallback(throwable: Throwable) {
                    Log.e("LOG", "Failed to get POI details", throwable)
                }
            })
        }
    }
}
