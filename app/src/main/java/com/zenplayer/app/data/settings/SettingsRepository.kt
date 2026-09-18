package com.zenplayer.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zenDataStore: DataStore<Preferences> by preferencesDataStore(name = "zen_settings")

enum class PlayerEngineChoice(val label: String) {
    EXO("ExoPlayer"),
    VLC("VLC")
}

data class ZenSettings(
    val themePresetId: String = "aqua",
    val customAccentArgb: Long? = null,
    val glassFrost: Float = 0.42f,
    val glassBlurDp: Float = 30f,
    val glassSheen: Float = 0.9f,
    val backgroundType: String = "liquid",
    val backgroundAnimated: Boolean = true,
    val backgroundBlurDp: Float = 0f,
    val focusEffect: String = "ring",
    val cardStyle: String = "glass",
    val navStyle: String = "rail",
    val homeStyle: String = "hub",
    val fontSize: String = "normal",
    val densityScale: Float = 1f,
    val homeScale: Float = 1f,
    val browseScale: Float = 1f,
    val epgScale: Float = 1f,
    val playerScale: Float = 1f,
    val settingsScale: Float = 1f,
    val playerEngine: PlayerEngineChoice = PlayerEngineChoice.EXO,
    val selectedProviderId: Long = -1L,
    val preferHd: Boolean = true,
    val epgFavs: Set<Int> = setOf(0, 2, 4, 6),
    val epgDay: Int = 0,
    val epgFavsOnly: Boolean = false,
    val epgAutoNow: Boolean = true,
    val epgPipp: Boolean = true,
    val epgLogos: Boolean = true,
    val epgSize: Float = 1f,
    val epgColW: Float = 1f,
    val currentChannel: Int = 0,
    val syncTimeoutSeconds: Int = 30,
    val userAgent: String? = null
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val GLASS_FROST = floatPreferencesKey("glass_frost")
        val GLASS_BLUR = floatPreferencesKey("glass_blur")
        val GLASS_SHEEN = floatPreferencesKey("glass_sheen")
        val BACKGROUND_TYPE = stringPreferencesKey("background_type")
        val BACKGROUND_ANIMATED = booleanPreferencesKey("background_animated")
        val BACKGROUND_BLUR = floatPreferencesKey("background_blur")
        val FOCUS_EFFECT = stringPreferencesKey("focus_effect")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val NAV_STYLE = stringPreferencesKey("nav_style")
        val HOME_STYLE = stringPreferencesKey("home_style")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val DENSITY_SCALE = floatPreferencesKey("density_scale")
        val HOME_SCALE = floatPreferencesKey("home_scale")
        val BROWSE_SCALE = floatPreferencesKey("browse_scale")
        val EPG_SCALE = floatPreferencesKey("epg_scale")
        val PLAYER_SCALE = floatPreferencesKey("player_scale")
        val SETTINGS_SCALE = floatPreferencesKey("settings_scale")
        val PLAYER_ENGINE = stringPreferencesKey("player_engine")
        val SELECTED_PROVIDER = longPreferencesKey("selected_provider")
        val PREFER_HD = booleanPreferencesKey("prefer_hd")
        val EPG_FAVS = stringSetPreferencesKey("epg_favs")
        val EPG_DAY = intPreferencesKey("epg_day")
        val EPG_FAVS_ONLY = booleanPreferencesKey("epg_favsonly")
        val EPG_AUTO_NOW = booleanPreferencesKey("epg_autonow")
        val EPG_PIPP = booleanPreferencesKey("epg_pipp")
        val EPG_LOGOS = booleanPreferencesKey("epg_logos")
        val EPG_SIZE = floatPreferencesKey("epg_size")
        val EPG_COL_W = floatPreferencesKey("epg_colw")
        val CURRENT_CHANNEL = intPreferencesKey("current_channel")
        val SYNC_TIMEOUT = intPreferencesKey("sync_timeout_seconds")
        val USER_AGENT = stringPreferencesKey("user_agent")
    }

    val settingsFlow: Flow<ZenSettings> = context.zenDataStore.data.map { prefs ->
        ZenSettings(
            themePresetId = prefs[Keys.THEME_PRESET] ?: "aqua",
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT],
            glassFrost = prefs[Keys.GLASS_FROST] ?: 0.42f,
            glassBlurDp = prefs[Keys.GLASS_BLUR] ?: 30f,
            glassSheen = prefs[Keys.GLASS_SHEEN] ?: 0.9f,
            backgroundType = prefs[Keys.BACKGROUND_TYPE] ?: "liquid",
            backgroundAnimated = prefs[Keys.BACKGROUND_ANIMATED] ?: true,
            backgroundBlurDp = prefs[Keys.BACKGROUND_BLUR] ?: 0f,
            focusEffect = prefs[Keys.FOCUS_EFFECT] ?: "ring",
            cardStyle = prefs[Keys.CARD_STYLE] ?: "glass",
            navStyle = prefs[Keys.NAV_STYLE] ?: "rail",
            homeStyle = prefs[Keys.HOME_STYLE] ?: "hub",
            fontSize = prefs[Keys.FONT_SIZE] ?: "normal",
            densityScale = prefs[Keys.DENSITY_SCALE] ?: 1f,
            homeScale = prefs[Keys.HOME_SCALE] ?: 1f,
            browseScale = prefs[Keys.BROWSE_SCALE] ?: 1f,
            epgScale = prefs[Keys.EPG_SCALE] ?: 1f,
            playerScale = prefs[Keys.PLAYER_SCALE] ?: 1f,
            settingsScale = prefs[Keys.SETTINGS_SCALE] ?: 1f,
            playerEngine = runCatching {
                PlayerEngineChoice.valueOf(prefs[Keys.PLAYER_ENGINE] ?: "EXO")
            }.getOrDefault(PlayerEngineChoice.EXO),
            selectedProviderId = prefs[Keys.SELECTED_PROVIDER] ?: -1L,
            preferHd = prefs[Keys.PREFER_HD] ?: true,
            epgFavs = prefs[Keys.EPG_FAVS]
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(0, 2, 4, 6),
            epgDay = prefs[Keys.EPG_DAY] ?: 0,
            epgFavsOnly = prefs[Keys.EPG_FAVS_ONLY] ?: false,
            epgAutoNow = prefs[Keys.EPG_AUTO_NOW] ?: true,
            epgPipp = prefs[Keys.EPG_PIPP] ?: true,
            epgLogos = prefs[Keys.EPG_LOGOS] ?: true,
            epgSize = prefs[Keys.EPG_SIZE] ?: 1f,
            epgColW = prefs[Keys.EPG_COL_W] ?: 1f,
            currentChannel = prefs[Keys.CURRENT_CHANNEL] ?: 0,
            syncTimeoutSeconds = prefs[Keys.SYNC_TIMEOUT] ?: 30,
            userAgent = prefs[Keys.USER_AGENT]
        )
    }

    suspend fun setThemePreset(id: String) {
        context.zenDataStore.edit { it[Keys.THEME_PRESET] = id }
    }

    suspend fun setCustomAccent(argb: Long?) {
        context.zenDataStore.edit {
            if (argb == null) it.remove(Keys.CUSTOM_ACCENT) else it[Keys.CUSTOM_ACCENT] = argb
        }
    }

    suspend fun setGlass(frost: Float, blurDp: Float, sheen: Float) {
        context.zenDataStore.edit {
            it[Keys.GLASS_FROST] = frost
            it[Keys.GLASS_BLUR] = blurDp
            it[Keys.GLASS_SHEEN] = sheen
        }
    }

    suspend fun setBackgroundType(type: String) {
        context.zenDataStore.edit { it[Keys.BACKGROUND_TYPE] = type }
    }

    suspend fun setBackgroundAnimated(animated: Boolean) {
        context.zenDataStore.edit { it[Keys.BACKGROUND_ANIMATED] = animated }
    }

    suspend fun setBackgroundBlur(blurDp: Float) {
        context.zenDataStore.edit { it[Keys.BACKGROUND_BLUR] = blurDp.coerceAtLeast(0f) }
    }

    suspend fun setFocusEffect(effect: String) {
        context.zenDataStore.edit { it[Keys.FOCUS_EFFECT] = effect }
    }

    suspend fun setCardStyle(style: String) {
        context.zenDataStore.edit { it[Keys.CARD_STYLE] = style }
    }

    suspend fun setNavStyle(style: String) {
        context.zenDataStore.edit { it[Keys.NAV_STYLE] = style }
    }

    suspend fun setHomeStyle(style: String) {
        context.zenDataStore.edit { it[Keys.HOME_STYLE] = style }
    }

    suspend fun setFontSize(size: String) {
        context.zenDataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun setDensityScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.DENSITY_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setHomeScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.HOME_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setBrowseScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.BROWSE_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setEpgScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.EPG_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setPlayerScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.PLAYER_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setSettingsScale(scale: Float) {
        context.zenDataStore.edit { it[Keys.SETTINGS_SCALE] = scale.coerceIn(0.5f, 2f) }
    }

    suspend fun setPlayerEngine(engine: PlayerEngineChoice) {
        context.zenDataStore.edit { it[Keys.PLAYER_ENGINE] = engine.name }
    }

    suspend fun setSelectedProvider(id: Long) {
        context.zenDataStore.edit { it[Keys.SELECTED_PROVIDER] = id }
    }

    suspend fun setPreferHd(value: Boolean) {
        context.zenDataStore.edit { it[Keys.PREFER_HD] = value }
    }

    suspend fun setEpgFavs(favs: Set<Int>) {
        context.zenDataStore.edit {
            it[Keys.EPG_FAVS] = favs.map(Int::toString).toSet()
        }
    }

    suspend fun setEpgDay(day: Int) {
        context.zenDataStore.edit { it[Keys.EPG_DAY] = day }
    }

    suspend fun setEpgFavsOnly(value: Boolean) {
        context.zenDataStore.edit { it[Keys.EPG_FAVS_ONLY] = value }
    }

    suspend fun setEpgAutoNow(value: Boolean) {
        context.zenDataStore.edit { it[Keys.EPG_AUTO_NOW] = value }
    }

    suspend fun setEpgPipp(value: Boolean) {
        context.zenDataStore.edit { it[Keys.EPG_PIPP] = value }
    }

    suspend fun setEpgLogos(value: Boolean) {
        context.zenDataStore.edit { it[Keys.EPG_LOGOS] = value }
    }

    suspend fun setEpgSize(size: Float) {
        context.zenDataStore.edit { it[Keys.EPG_SIZE] = size }
    }

    suspend fun setEpgColW(colW: Float) {
        context.zenDataStore.edit { it[Keys.EPG_COL_W] = colW }
    }

    suspend fun setCurrentChannel(index: Int) {
        context.zenDataStore.edit { it[Keys.CURRENT_CHANNEL] = index }
    }

    suspend fun setSyncTimeout(seconds: Int) {
        context.zenDataStore.edit { it[Keys.SYNC_TIMEOUT] = seconds.coerceAtLeast(5) }
    }

    suspend fun setUserAgent(agent: String?) {
        context.zenDataStore.edit {
            if (agent.isNullOrBlank()) it.remove(Keys.USER_AGENT) else it[Keys.USER_AGENT] = agent
        }
    }
}