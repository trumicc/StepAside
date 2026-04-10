package com.example.stepaside

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://mfvlupwxzfugirppspow.supabase.co",
    supabaseKey = "sb_publishable_KYMRxtJnpzlpcrvGOliDUg_WcQbYaDu"
) {
    install(Auth)
    install(Postgrest)
}