package io.stepaside.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client singleton.
 *
 * Credentials are injected at build time from `local.properties` via BuildConfig.
 * They are NEVER stored in source code.
 *
 * The publishable/anon key is safe to ship in the APK as long as Row Level
 * Security (RLS) is enforced on every table — which it must be.
 */
val supabase by lazy {
    createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
    }
}
