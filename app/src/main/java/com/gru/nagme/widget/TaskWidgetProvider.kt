package com.gru.nagme.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.gru.nagme.MainActivity
import com.gru.nagme.R

/**
 * Widget homescreen: daftar tugas aktif (terdekat dulu). Ketuk tugas = buka layar edit,
 * ketuk "+" = tambah tugas, ketuk judul = buka app. Data diambil oleh [TaskWidgetService].
 */
class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    companion object {
        const val EXTRA_TASK_ID = "open_task_id"
        const val EXTRA_ADD = "open_add"

        /** Dipanggil saat data tugas berubah, supaya widget ikut segar. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_tasks)

            val serviceIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(flags),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val addTask = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(flags).putExtra(EXTRA_ADD, true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // Template harus mutable supaya fill-in intent (id tugas) bisa digabung.
            val itemTemplate = PendingIntent.getActivity(
                context, 2,
                Intent(context, MainActivity::class.java).addFlags(flags),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_title, openApp)
            views.setOnClickPendingIntent(R.id.widget_add, addTask)
            views.setPendingIntentTemplate(R.id.widget_list, itemTemplate)
            return views
        }
    }
}
