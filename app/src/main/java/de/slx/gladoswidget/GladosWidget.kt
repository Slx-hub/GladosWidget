package de.slx.gladoswidget

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text

class GladosWidget : GlanceAppWidget() {

	override suspend fun provideGlance(context: Context, id: GlanceId) {
		// Load data needed to render the AppWidget.
		// Use `withContext` to switch to another thread for long running
		// operations.

		provideContent {
			GlanceTheme(
				GlanceTheme.colors
			) {
				MyContent(context)
			}
		}
	}

	@Composable
	private fun MyContent(context: Context) {
		Column(
			modifier = GlanceModifier.fillMaxSize(),
			verticalAlignment = Alignment.Top,
			horizontalAlignment = Alignment.CenterHorizontally
		) {

			Row(horizontalAlignment = Alignment.CenterHorizontally) {
				// FUCK ME
				Text(text = "192.168.178.30")
				Button(
					text = "Sync",
					onClick = actionRunCallback<ToastAction2>()
				)
				Button(
					text = "Edit",
					onClick = actionStartActivity(
						Intent(context, EditActivity::class.java)
					)
				)
			}
		}
	}

	private fun doNothing(test: String) {
		println(test)
	}
}

class ToastAction : ActionCallback {
	override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
		Handler(context.mainLooper).post {
			Toast.makeText(context, "You're dumb!", Toast.LENGTH_SHORT).show()
		}
	}
}

class ToastAction2 : ActionCallback {
	override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
		Handler(context.mainLooper).post {
			Toast.makeText(context, "Your stupid!", Toast.LENGTH_SHORT).show()
		}
	}
}
