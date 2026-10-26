package dev.m4s4t0.kushacres

import android.app.Application
import dev.m4s4t0.kushacres.audio.Audio

class KushApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Audio.init(this)
        Store.init(this)
    }
}
