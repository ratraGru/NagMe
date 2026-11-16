package com.gru.nagme.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.gru.nagme.MainActivity
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.data.NO_CATEGORY
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.service.formatRemainingPlain
import com.gru.nagme.ui.Categories
import com.gru.nagme.ui.formatDeadline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Widget homescreen: 5 tugas aktif terdekat. Baris dibuat statis (tanpa ListView/adapter),
 * karena widget berdaftar sering mentok "Memuat..." di launcher OEM.
 * Ketuk tugas = layar edit, "+" = tambah tugas, judul = buka app.
 */
class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, manager, ids)
            } catch (e: Exception) {
                Log.e(TAG, "gagal memperbarui widget", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "open_task_id"
        const val EXTRA_ADD = "open_add"
        private const val TAG = "NagMeWidget"
        private const val MAX_ROWS = 5
        private const val URGENT_MS = 12 * 3_600_000L
        private const val COLOR_URGENT = 0xFFB5603F.toInt()
        private const val COLOR_NORMAL = 0xFF2F7D6B.toInt()

        private val ROW = intArrayOf(R.id.row_0, R.id.row_1, R.id.row_2, R.id.row_3, R.id.row_4)
        private val BAR = intArrayOf(R.id.bar_0, R.id.bar_1, R.id.bar_2, R.id.bar_3, R.id.bar_4)
        private val TITLE = intArrayOf(R.id.title_0, R.id.title_1, R.id.title_2, R.id.title_3, R.id.title_4)
        private val SUB = intArrayOf(R.id.sub_0, R.id.sub_1, R.id.sub_2, R.id.sub_3, R.id.sub_4)

        /** Dipanggil saat data tugas berubah, supaya widget ikut segar. Tidak boleh merusak operasi data. */
        suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
                if (ids.isNotEmpty()) render(context, manager, ids)
            } catch (e: Exception) {
                Log.e(TAG, "gagal me-refresh widget", e)
            }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val container = (context.applicationContext as NagMeApp).container
            val tasks = container.taskRepository.activeForWidget()
            val now = container.clock.now()
            ids.forEach { manager.updateAppWidget(it, build(context, tasks, now)) }
        }

        private fun build(context: Context, tasks: List<TaskEntity>, now: Long): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_tasks)
            val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val immutable = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

            fun appIntent() = Intent(context, MainActivity::class.java).addFlags(flags)

            views.setOnClickPendingIntent(
                R.id.widget_title, PendingIntent.getActivity(context, 0, appIntent(), immutable)
            )
            views.setOnClickPendingIntent(
                R.id.widget_add,
                PendingIntent.getActivity(context, 1, appIntent().putExtra(EXTRA_ADD, true), immutable)
            )

            for (i in 0 until MAX_ROWS) {
                val task = tasks.getOrNull(i)
                if (task == null) {
                    views.setViewVisibility(ROW[i], View.GONE)
                    continue
                }
                try {
                    bindRow(context, views, i, task, now, flags, immutable)
                } catch (e: Exception) {
                    Log.e(TAG, "gagal membuat baris $i", e)
                    views.setTextViewText(TITLE[i], task.title)
                    views.setViewVisibility(ROW[i], View.VISIBLE)
                }
            }

            val extra = tasks.size - MAX_ROWS
            if (extra > 0) {
                views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, extra))
                views.setViewVisibility(R.id.widget_more, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE)
            }
            views.setViewVisibility(R.id.widget_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)
            return views
        }

        private fun bindRow(
            context: Context, views: RemoteViews, i: Int, task: TaskEntity,
            now: Long, flags: Int, immutable: Int
        ) {
            val remaining = task.deadlineEpoch - now
            val remainingText = if (remaining <= 0L) {
                context.getString(R.string.remaining_passed)
            } else {
                context.getString(R.string.time_left, formatRemainingPlain(context, remaining))
            }
            views.setTextViewText(TITLE[i], task.title)
            views.setTextViewText(SUB[i], formatDeadline(task.deadlineEpoch) + " \u00B7 " + remainingText)
            views.setTextColor(SUB[i], if (remaining <= URGENT_MS) COLOR_URGENT else COLOR_NORMAL)

            if (task.category != NO_CATEGORY && task.category in Categories.colors.indices) {
                views.setInt(BAR[i], "setBackgroundColor", Categories.colors[task.category].toArgb())
                views.setViewVisibility(BAR[i], View.VISIBLE)
            } else {
                views.setViewVisibility(BAR[i], View.INVISIBLE)
            }

            // Request code beda per baris, jadi extra id tugas tiap baris tidak saling menimpa.
            val open = Intent(context, MainActivity::class.java)
                .addFlags(flags)
                .putExtra(EXTRA_TASK_ID, task.id)
            views.setOnClickPendingIntent(ROW[i], PendingIntent.getActivity(context, 100 + i, open, immutable))
            views.setViewVisibility(ROW[i], View.VISIBLE)
        }
    }
}
