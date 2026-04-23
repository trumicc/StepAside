package com.example.stepaside.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.*
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
            val views = RemoteViews(context.packageName, R.layout.widget_step_layout)
            views.setTextViewText(R.id.widget_steps, "0")
            views.setTextViewText(R.id.widget_remaining, "10,000 to goal")
            views.setImageViewBitmap(R.id.widget_ring, drawBorder(0, 10000))
            appWidgetManager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = (context.applicationContext as StepAsideApp).database
                    val today = LocalDate.now().toString()
                    val dailySteps = db.dailyStepsDao().getByDate(today)

                    val steps     = dailySteps?.steps     ?: 0
                    val goal      = dailySteps?.goalSteps ?: 10000
                    val remaining = (goal - steps).coerceAtLeast(0)

                    val updatedViews = RemoteViews(context.packageName, R.layout.widget_step_layout)
                    updatedViews.setTextViewText(R.id.widget_steps,     "%,d".format(steps))
                    updatedViews.setTextViewText(R.id.widget_remaining, "%,d to goal".format(remaining))
                    updatedViews.setImageViewBitmap(R.id.widget_ring,   drawBorder(steps, goal))

                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Draws a rounded-rect border where the green stroke fills up
         * clockwise from the top-center as progress increases.
         *
         * 0%   → full grey border
         * 50%  → half green, half grey
         * 100% → full green border
         */
        private fun drawBorder(steps: Int, goal: Int): Bitmap {
            val width = 300
            val height = 180
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val radius = 40f
            val stroke = 8f
            val inset = stroke / 2

            val rect = RectF(inset, inset, width - inset, height - inset)
            val path = Path()
            path.addRoundRect(rect, radius, radius, Path.Direction.CW)

            val pathMeasure = PathMeasure(path, false)
            val totalLength = pathMeasure.length
            val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)

            // 1. Draw full background ring in dark green
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = android.graphics.Color.parseColor("#1A3D1A")
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, bgPaint)

            // 2. Draw progress arc on top in bright green
            if (progress > 0f) {
                val progressPath = Path()
                pathMeasure.getSegment(0f, totalLength * progress, progressPath, true)

                val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    color = android.graphics.Color.parseColor("#39D353")
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawPath(progressPath, fgPaint)
            }

            return bitmap
        }


    }
}