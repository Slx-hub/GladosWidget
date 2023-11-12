package de.slx.gladoswidget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class GladosWidgetReceiver : GlanceAppWidgetReceiver() {
    // Let MyAppWidgetReceiver know which GlanceAppWidget to use
    override val glanceAppWidget: GlanceAppWidget = GladosWidget()
}