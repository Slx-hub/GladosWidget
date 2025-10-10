package de.slx.gladoswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.Data
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class GladosWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val BUTTON_CLICK_ACTION = "de.slx.gladoswidget.BUTTON_CLICK"
        private const val WORK_TAG = "LivingRoomRequest"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Create an Intent for the button click
            val intent = Intent(context, GladosWidget::class.java).apply {
                action = BUTTON_CLICK_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Assign the click listener to the button
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BUTTON_CLICK_ACTION) {
            val workRequest = OneTimeWorkRequestBuilder<HttpRequestWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

class HttpRequestWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("http://192.168.178.30:5123/turn_on_livingroom")
            .get()
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            response.use {
                if (response.isSuccessful) {
                    showToast("Request successful")
                    Result.success()
                } else {
                    showToast("Request failed: ${response.code}")
                    Result.failure(
                        Data.Builder()
                            .putString("error", "HTTP ${response.code}")
                            .build()
                    )
                }
            }
        } catch (e: IOException) {
            showToast("Network error: ${e.message}")
            Result.retry()
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
