package com.example.stepaside

import com.example.stepaside.R

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val requiredSteps: Long,
    var unlocked: Boolean = false,
    val imageRes: Int? = null
)

val ALL_ACHIEVEMENTS = listOf(
    Achievement("first_steps", "👶", "First Steps", "Walk your first 1,000 steps", 1_000, imageRes = R.drawable.achievement_first_step),
    Achievement("daily_goal", "🎯", "Goal Getter", "Reach your daily goal of 10,000 steps", 10_000, imageRes = R.drawable.achievement_goal_getter),
    Achievement("everest", "🏔️", "Mt. Everest", "Climb the height of Everest in elevation steps", 11_800, imageRes = R.drawable.achievement_everest),
    Achievement("marathon", "🏅", "Marathon", "Walk a full marathon distance (42km)", 55_000, imageRes = R.drawable.achievement_marathon),
    Achievement("steps_10k", "💪", "10K Club", "Walk 10,000 lifetime steps", 10_000, imageRes = R.drawable.achievement_10k),
    Achievement("steps_100k", "🔥", "100K Steps", "Walk 100,000 lifetime steps", 100_000, imageRes = R.drawable.achievement_100k),
    Achievement("millionaire", "⭐", "Millionaire", "Walk 1,000,000 lifetime steps", 1_000_000, imageRes = R.drawable.achievement_millionaire),
    Achievement("great_wall", "🧱", "Great Wall", "Walk the length of the Great Wall of China", 26_000_000, imageRes = R.drawable.achievement_great_wall),
    Achievement("going_global", "🌍", "Going Global", "Walk the Earth's equator (40,075 km)", 52_000_000, imageRes = R.drawable.achievement_going_global),
    Achievement("moon", "🌕", "To the Moon", "Walk 384,400 km to the Moon", 499_720_000, imageRes = R.drawable.achievement_moon),
    Achievement("mars", "🔴", "To Mars", "Walk the shortest distance to Mars (55M km)", 72_000_000_000, imageRes = R.drawable.achievement_mars),
)