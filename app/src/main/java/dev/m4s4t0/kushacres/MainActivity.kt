package dev.m4s4t0.kushacres

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Sky
import dev.m4s4t0.kushacres.model.Weather
import dev.m4s4t0.kushacres.ui.App

class MainActivity : ComponentActivity() {
    private val openSave = registerForActivityResult(ActivityResultContracts.OpenDocument()) { SaveFile.picked(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            intent.getStringExtra("weather")?.let { w -> Sky.forceWeather = Weather.entries.firstOrNull { it.name == w.uppercase() } }
            intent.getStringExtra("hour")?.toFloatOrNull()?.let { Sky.forceHour = it }
            intent.getStringExtra("season")?.let { w -> Season.force = Season.entries.firstOrNull { it.name == w.uppercase() } }
            // Sets the shack tier, for previews. This is saved, so launch again with the old tier to undo it.
            intent.getStringExtra("shack")?.toIntOrNull()?.let { Store.farm.shack = it }
        }
        SaveFile.open = { openSave.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) }
        setContent { App() }
        if (intent?.action == Updater.ACTION_INSTALLED) Updater.onStatus(this, intent) else Updater.check(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Updater.ACTION_INSTALLED) Updater.onStatus(this, intent)
    }

    override fun onStart() {
        super.onStart()
        Store.resume()
        Audio.start()
    }

    override fun onStop() {
        super.onStop()
        Store.save()
        Audio.stop()
    }
}
