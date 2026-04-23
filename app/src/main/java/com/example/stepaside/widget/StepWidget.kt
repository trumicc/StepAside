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
            views.setTextViewText(R.id.widget_distance, "0.0 km")
            views.setTextViewText(R.id.widget_calories, "0")
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
                    val distanceKm = steps * 0.000762f
                    val calories  = (steps * 0.04f).toInt()

                    val updatedViews = RemoteViews(context.packageName, R.layout.widget_step_layout)
                    updatedViews.setTextViewText(R.id.widget_steps,     "%,d".format(steps))
                    updatedViews.setTextViewText(R.id.widget_remaining, "%,d to goal".format(remaining))
                    updatedViews.setTextViewText(R.id.widget_distance,  "%.1f km".format(distanceKm))
                    updatedViews.setTextViewText(R.id.widget_calories,  "$calories")
                    updatedViews.setImageViewBitmap(R.id.widget_ring,   drawBorder(steps, goal))

                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun drawFootprints(canvas: Canvas, width: Int, height: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#39D353")
                alpha = 33
                style = Paint.Style.FILL
            }

            val footprints = listOf(
                Pair(60f, height - 30f),
                Pair(45f, height - 70f),
                Pair(65f, height - 110f),
                Pair(48f, height - 150f),
                Pair(70f, height - 190f),
                Pair(90f, height - 225f),
                Pair(120f, height - 250f),
                Pair(155f, height - 260f),
                Pair(190f, height - 255f),
                Pair(220f, height - 240f),
            )

            footprints.forEachIndexed { index, (x, y) ->
                val offset = if (index % 2 == 0) -10f else 10f
                canvas.drawOval(
                    RectF(x + offset - 8f, y - 14f, x + offset + 8f, y + 14f),
                    paint
                )
                for (i in 0..4) {
                    canvas.drawCircle(x + offset - 8f + (i * 4f), y - 18f, 3f, paint)
                }
            }
        }

        private fun drawBorder(steps: Int, goal: Int): Bitmap {
            val width = 300
            val height = 360
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawFootprints(canvas, width, height)

            val stroke = 8f
            val inset = stroke / 2

            val rect = RectF(inset, inset, width - inset, height - inset)
            val path = Path()
            path.addRoundRect(rect, 40f, 40f, Path.Direction.CW)

            val pathMeasure = PathMeasure(path, false)
            val totalLength = pathMeasure.length
            val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = android.graphics.Color.parseColor("#1A3D1A")
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, bgPaint)

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