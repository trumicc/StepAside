package com.example.stepaside

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://mfvlupwxzfugirppspow.supabase.co",
    supabaseKey = "din_publishable_key"
) {
    install(Auth) {
        autoLoadFromStorage = true
        autoSaveToStorage = true
        alwaysAutoRefresh = true
    }
    install(Postgrest)
}