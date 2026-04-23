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
        fun drawBorder(
            steps: Int,
            goal: Int,
            widthPx: Int = 400,
            heightPx: Int = 220,
            cornerDp: Float = 20f,
            strokeDp: Float = 4f
        ): Bitmap {
            val density   = 3f                          // ~xxhdpi
            val stroke    = strokeDp * density
            val corner    = cornerDp * density
            val half      = stroke / 2f

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val rect = RectF(half, half, widthPx - half, heightPx - half)

            val progress = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

            // Perimeter of the rounded rect
            val w = rect.width()
            val h = rect.height()
            val r = corner
            // straight segments + 4 quarter-circle arcs
            val straightW  = w - 2 * r
            val straightH  = h - 2 * r
            val arcLen     = (Math.PI * r).toFloat()    // quarter arc = π*r/2, but 4 of them = 2πr
            val perimeter  = 2 * straightW + 2 * straightH + 2 * Math.PI.toFloat() * r
            val greenLen   = progress * perimeter

            // Base paint — grey track
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style       = Paint.Style.STROKE
                strokeWidth = stroke
                color       = Color.parseColor("#21262D")
                strokeCap   = Paint.Cap.BUTT
            }
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style       = Paint.Style.STROKE
                strokeWidth = stroke
                color       = Color.parseColor("#39D353")
                strokeCap   = Paint.Cap.ROUND
            }

            // Draw full grey border first
            val path = buildRoundRectPath(rect, corner)
            canvas.drawPath(path, trackPaint)

            // Draw green progress on top using PathMeasure
            if (greenLen > 0f) {
                val measure = PathMeasure(path, false)
                val totalLen = measure.length

                // Start from top-center: offset into the path
                // Path starts at top-left corner arc; top-center is at straightW/2 + quarter-arc
                val topCenterOffset = (Math.PI * r / 2).toFloat() + straightW / 2f
                val startOffset = topCenterOffset

                val greenPath = Path()
                // We draw from startOffset, wrapping around if needed
                val endOffset = startOffset + greenLen

                if (endOffset <= totalLen) {
                    measure.getSegment(startOffset, endOffset, greenPath, true)
                } else {
                    // Wrap: draw to end, then from beginning
                    measure.getSegment(startOffset, totalLen, greenPath, true)
                    val wrapPath = Path()
                    measure.getSegment(0f, endOffset - totalLen, wrapPath, true)
                    greenPath.addPath(wrapPath)
                }

                greenPaint.strokeCap = Paint.Cap.ROUND
                canvas.drawPath(greenPath, greenPaint)
            }

            return bitmap
        }

        private fun buildRoundRectPath(rect: RectF, r: Float): Path {
            // Build path starting from top-left arc, going clockwise
            // so top-center is a known offset into the path
            return Path().apply {
                moveTo(rect.left + r, rect.top)
                // Top edge →
                lineTo(rect.right - r, rect.top)
                // Top-right arc
                arcTo(RectF(rect.right - 2*r, rect.top, rect.right, rect.top + 2*r), -90f, 90f, false)
                // Right edge ↓
                lineTo(rect.right, rect.bottom - r)
                // Bottom-right arc
                arcTo(RectF(rect.right - 2*r, rect.bottom - 2*r, rect.right, rect.bottom), 0f, 90f, false)
                // Bottom edge ←
                lineTo(rect.left + r, rect.bottom)
                // Bottom-left arc
                arcTo(RectF(rect.left, rect.bottom - 2*r, rect.left + 2*r, rect.bottom), 90f, 90f, false)
                // Left edge ↑
                lineTo(rect.left, rect.top + r)
                // Top-left arc
                arcTo(RectF(rect.left, rect.top, rect.left + 2*r, rect.top + 2*r), 180f, 90f, false)
                close()
            }
        }
    }
}