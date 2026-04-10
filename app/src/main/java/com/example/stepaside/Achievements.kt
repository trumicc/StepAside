package com.example.stepaside

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val requiredSteps: Long,
    var unlocked: Boolean = false
)

val ALL_ACHIEVEMENTS = listOf(
    Achievement("first_steps", "👶", "First Steps", "Walk your first 100 steps", 100),
    Achievement("km1", "🚶", "1 Kilometer", "Walk 1,000 steps total", 1_000),
    Achievement("daily_goal", "🎯", "Goal Getter", "Walk 10,000 steps total", 10_000),
    Achievement("everest", "🏔️", "Everest", "Climb Everest's height in steps (8,849m)", 11_800),
    Achievement("marathon", "🏅", "Marathon", "Walk a marathon distance (42km)", 55_000),
    Achievement("great_wall", "🧱", "Great Wall", "Walk the length of the Great Wall", 26_000_000),
    Achievement("steps_10k", "💪", "10K Club", "Walk 10,000 lifetime steps", 10_000),
    Achievement("steps_100k", "🔥", "100K Steps", "Walk 100,000 lifetime steps", 100_000),
    Achievement("steps_1m", "⭐", "Million Steps", "Walk 1,000,000 lifetime steps", 1_000_000),
    Achievement("moon", "🌕", "To the Moon", "Walk 384,400km to the moon", 499_720_000),
)