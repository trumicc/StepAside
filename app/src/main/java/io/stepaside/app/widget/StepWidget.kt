package io.stepaside.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.widget.RemoteViews
import io.stepaside.app.MainActivity
import io.stepaside.app.R
import io.stepaside.app.StepAsideApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Home screen widget showing today's steps + progress ring.
 *
 * Updates are pushed from [io.stepaside.app.service.StepCounterService]
 * whenever the step count changes.
 *
 * The widget's tap target opens the main app (handled here, not in XML, so it
 * survives every RemoteViews update — a known Android widget quirk).
 */
class StepWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        private const val WIDTH = 300
        private const val HEIGHT = 360
        private const val CORNER_RADIUS = 40f
        private const val STROKE_WIDTH = 8f

        private const val COLOR_TRACK = "#1A3D1A"
        private const val COLOR_PROGRESS = "#39D353"
        private const val COLOR_FOOTPRINT = "#39D353"

        /** Approximate stride in meters per step, used for distance estimation. */
        private const val METERS_PER_STEP = 0.762f

        /** Approximate calories burned per step. */
        private const val KCAL_PER_STEP = 0.04f

        /**
         * Renders the widget. Always attaches a tap PendingIntent — must be
         * re-attached on every update, otherwise taps stop working.
         */
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            // Optimistic render so the widget shows something immediately,
            // even before the DB query returns.
            val pendingViews = buildViews(context, steps = 0, goal = 10_000)
            appWidgetManager.updateAppWidget(appWidgetId, pendingViews)

            val app = context.applicationContext as? StepAsideApp ?: return
            app.applicationScope.launch {
                runCatching {
                    val today = LocalDate.now().toString()
                    val dailySteps = withContext(Dispatchers.IO) {
                        app.database.dailyStepsDao().getByDate(today)
                    }
                    val steps = dailySteps?.steps ?: 0
                    val goal = dailySteps?.goalSteps ?: 10_000

                    val updatedViews = buildViews(context, steps, goal)
                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                }.onFailure { it.printStackTrace() }
            }
        }

        /** Pushes an update to every active StepWidget instance. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, StepWidget::class.java)
            val ids = manager.getAppWidgetIds(component) ?: return
            for (id in ids) updateWidget(context, manager, id)
        }

        private fun buildViews(context: Context, steps: Int, goal: Int): RemoteViews {
            val safeGoal = if (goal <= 0) 10_000 else goal
            val remaining = (safeGoal - steps).coerceAtLeast(0)
            val distanceKm = steps * METERS_PER_STEP / 1000f
            val calories = (steps * KCAL_PER_STEP).toInt()

            val views = RemoteViews(context.packageName, R.layout.widget_step_layout)
            views.setTextViewText(R.id.widget_steps, "%,d".format(steps))
            views.setTextViewText(R.id.widget_remaining, "%,d to goal".format(remaining))
            views.setTextViewText(R.id.widget_distance, "%.1f km".format(distanceKm))
            views.setTextViewText(R.id.widget_calories, "$calories kcal")
            views.setImageViewBitmap(R.id.widget_ring, drawRing(steps, safeGoal))

            // Tap target — must be set on every update.
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            return views
        }

        // ---- Bitmap rendering ----

        private fun drawRing(steps: Int, goal: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawFootprints(canvas, WIDTH, HEIGHT)

            val inset = STROKE_WIDTH / 2f
            val rect = RectF(inset, inset, WIDTH - inset, HEIGHT - inset)
            val path = Path().apply {
                addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
            }
            val pathMeasure = PathMeasure(path, false)
            val totalLength = pathMeasure.length

            // Track (always full).
            canvas.drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = STROKE_WIDTH
                    color = android.graphics.Color.parseColor(COLOR_TRACK)
                    strokeCap = Paint.Cap.ROUND
                },
            )

            // Progress arc.
            val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
            if (progress > 0f) {
                val progressPath = Path()
                pathMeasure.getSegment(0f, totalLength * progress, progressPath, true)
                canvas.drawPath(
                    progressPath,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = STROKE_WIDTH
                        color = android.graphics.Color.parseColor(COLOR_PROGRESS)
                        strokeCap = Paint.Cap.ROUND
                    },
                )
            }

            return bitmap
        }

        private fun drawFootprints(canvas: Canvas, width: Int, height: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(COLOR_FOOTPRINT)
                alpha = 33
                style = Paint.Style.FILL
            }
            val footprints = listOf(
                60f to height - 30f,
                45f to height - 70f,
                65f to height - 110f,
                48f to height - 150f,
                70f to height - 190f,
                90f to height - 225f,
                120f to height - 250f,
                155f to height - 260f,
                190f to height - 255f,
                220f to height - 240f,
            )
            footprints.forEachIndexed { index, (x, y) ->
                val offset = if (index % 2 == 0) -10f else 10f
                canvas.drawOval(
                    RectF(x + offset - 8f, y - 14f, x + offset + 8f, y + 14f),
                    paint,
                )
                for (i in 0..4) {
                    canvas.drawCircle(x + offset - 8f + (i * 4f), y - 18f, 3f, paint)
                }
            }
        }
    }
}
