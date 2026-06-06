package com.example.chessboard.localization

/*
 * Defines the app-supported language set and Compose locale provider.
 * Keep app-wide localization plumbing here.
 * Do not add screen-specific strings or persistence access to this file.
 * Validation date: 2026-05-28
 */

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    RUSSIAN("ru"),
    SERBIAN("sr");

    companion object {
        val Default = ENGLISH

        fun fromTag(tag: String): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: Default
        }
    }
}

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedConfiguration = remember(configuration, language) {
        Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        LocalizedResourceContext(
            base = context,
            localizedContext = context.createConfigurationContext(localizedConfiguration),
        )
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        content = content,
    )
}

private class LocalizedResourceContext(
    base: Context,
    private val localizedContext: Context,
) : ContextWrapper(base) {

    override fun getResources(): Resources {
        return localizedContext.resources
    }

    override fun getAssets(): AssetManager {
        return localizedContext.assets
    }
}
