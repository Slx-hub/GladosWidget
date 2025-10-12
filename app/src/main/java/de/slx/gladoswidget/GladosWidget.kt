package de.slx.gladoswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    companion object {
        private const val TAG = "GladosWidget"
        private const val ACTION_LIVINGROOM_ON = "de.slx.gladoswidget.LIVINGROOM_ON"
        private const val ACTION_LIVINGROOM_OFF = "de.slx.gladoswidget.LIVINGROOM_OFF"
        private const val ACTION_BEDROOM_ON = "de.slx.gladoswidget.BEDROOM_ON"
        private const val ACTION_BEDROOM_OFF = "de.slx.gladoswidget.BEDROOM_OFF"
        private const val ACTION_ALARM_ON = "de.slx.gladoswidget.ALARM_ON"
        private const val ACTION_ALARM_OFF = "de.slx.gladoswidget.ALARM_OFF"
        private const val ACTION_SONOS_RESET = "de.slx.gladoswidget.SONOS_RESET"
        private const val WORK_TAG_PREFIX = "LightState"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for widget IDs: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget ID: $appWidgetId")
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Livingroom buttons
        setupButton(context, views, ACTION_LIVINGROOM_ON, R.id.btn_livingroom_on, 0)
        setupButton(context, views, ACTION_LIVINGROOM_OFF, R.id.btn_livingroom_off, 1)

        // Bedroom buttons
        setupButton(context, views, ACTION_BEDROOM_ON, R.id.btn_bedroom_on, 2)
        setupButton(context, views, ACTION_BEDROOM_OFF, R.id.btn_bedroom_off, 3)

        // Alarm buttons
        setupButton(context, views, ACTION_ALARM_ON, R.id.btn_alarm_on, 4)
        setupButton(context, views, ACTION_ALARM_OFF, R.id.btn_alarm_off, 5)

        // Sonos button
        setupButton(context, views, ACTION_SONOS_RESET, R.id.btn_sonos_reset, 6)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupButton(
        context: Context,
        views: RemoteViews,
        action: String,
        buttonId: Int,
        requestCode: Int
    ) {
        val intent = Intent(context, GladosWidget::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(buttonId, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        val (endpoint, params) = when (intent.action) {
            ACTION_LIVINGROOM_ON -> "ChangeLightState" to mapOf("source" to "livingroom", "state" to "on")
            ACTION_LIVINGROOM_OFF -> "ChangeLightState" to mapOf("source" to "livingroom", "state" to "off")
            ACTION_BEDROOM_ON -> "ChangeLightState" to mapOf("source" to "bedroom", "state" to "on")
            ACTION_BEDROOM_OFF -> "ChangeLightState" to mapOf("source" to "bedroom", "state" to "off")
            ACTION_ALARM_ON -> "ChangeAlarmState" to mapOf("state" to "on")
            ACTION_ALARM_OFF -> "ChangeAlarmState" to mapOf("state" to "off")
            ACTION_SONOS_RESET -> "ResetSocket" to mapOf("socket_source" to "sonos")
            else -> return
        }

        val workTag = buildString {
            append("$WORK_TAG_PREFIX:$endpoint")
            params.forEach { (key, value) ->
                append(":$value")
            }
        }

        val workRequest = OneTimeWorkRequestBuilder<HttpRequestWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString("endpoint", endpoint)
                    .putAll(params)
                    .build()
            )
            .addTag(workTag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d(TAG, "Work request scheduled for endpoint: $endpoint, params: $params")
    }
}

class HttpRequestWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    companion object {
        private const val TAG = "HttpRequestWorker"
        private const val BASE_URL = "http://192.168.178.30:5123/intent"
    }

    override fun doWork(): Result {
        val endpoint = inputData.getString("endpoint") ?: return Result.failure()

        // Build URL with parameters based on endpoint
        val url = when (endpoint) {
            "ChangeLightState" -> {
                val source = inputData.getString("source") ?: return Result.failure()
                val state = inputData.getString("state") ?: return Result.failure()
                "$BASE_URL/$endpoint?source=$source&state=$state"
            }
            "ChangeAlarmState" -> {
                val state = inputData.getString("state") ?: return Result.failure()
                "$BASE_URL/$endpoint?state=$state"
            }
            "ResetSocket" -> {
                val socketSource = inputData.getString("socket_source") ?: return Result.failure()
                "$BASE_URL/$endpoint?socket_source=$socketSource"
            }
            else -> return Result.failure()
        }

        Log.d(TAG, "Starting HTTP request work for endpoint: $endpoint")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d(TAG, "Attempting to connect to: $url")

        return try {
            Log.d(TAG, "Executing request...")
            val response: Response = client.newCall(request).execute()
            Log.d(TAG, "Received response code: ${response.code}")

            response.use {
                if (response.isSuccessful) {
                    Log.d(TAG, "Request successful")
                    Result.success()
                } else {
                    val errorMsg = "Request failed with code: ${response.code}, message: ${response.message}"
                    Log.e(TAG, errorMsg)
                    Result.failure(
                        Data.Builder()
                            .putString("error", errorMsg)
                            .build()
                    )
                }
            }
        } catch (e: IOException) {
            val errorMsg = "Network error connecting to $url: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(
                Data.Builder()
                    .putString("error", errorMsg)
                    .build()
            )
        } catch (e: Exception) {
            val errorMsg = "Unexpected error: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(
                Data.Builder()
                    .putString("error", errorMsg)
                    .build()
            )
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
