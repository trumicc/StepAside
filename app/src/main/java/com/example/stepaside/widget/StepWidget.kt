package com.example.stepaside.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.stepaside.R
import com.example.stepaside.StepAsideApp
import kotlinx.coroutines.*
import java.time.LocalDate

class StepWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Visa placeholder direkt
            val views = RemoteViews(context.packageName, R.layout.widget_step_layout)
            views.setTextViewText(R.id.widget_steps, "0")
            views.setTextViewText(R.id.widget_remaining, "10,000 to goal")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Ladda riktig data async
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = (context.applicationContext as StepAsideApp).database
                    val today = LocalDate.now().toString()
                    val dailySteps = db.dailyStepsDao().getByDate(today)

                    val steps = dailySteps?.steps ?: 0
                    val goal = dailySteps?.goalSteps ?: 10000
                    val remaining = (goal - steps).coerceAtLeast(0)

                    val updatedViews = RemoteViews(context.packageName, R.layout.widget_step_layout)
                    updatedViews.setTextViewText(R.id.widget_steps, "%,d".format(steps))
                    updatedViews.setTextViewText(R.id.widget_remaining, "%,d to goal".format(remaining))

                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}