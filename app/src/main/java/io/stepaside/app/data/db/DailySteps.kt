package io.stepaside.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey
    val dateStr: String,
    val steps: Int = 0,
    val goalSteps: Int = 10000,
<<<<<<< Updated upstream:app/src/main/java/com/example/stepaside/data/db/DailySteps.kt
    val goalReached: Boolean = false

)
=======
    val goalReached: Boolean = false,
)
>>>>>>> Stashed changes:app/src/main/java/io/stepaside/app/data/db/DailySteps.kt
