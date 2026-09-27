package com.example.safesquare

import android.app.Application
import com.example.safesquare.data.SettingsRepository
import com.example.safesquare.feedback.Haptics
import com.example.safesquare.feedback.SoundEngine

class SafeSquareApplication : Application() {
    val settingsRepository by lazy { SettingsRepository(this) }
    val soundEngine by lazy { SoundEngine(this) }
    val haptics by lazy { Haptics(this) }
}
