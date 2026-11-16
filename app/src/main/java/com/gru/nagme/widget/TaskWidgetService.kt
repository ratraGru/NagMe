package com.gru.nagme.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.data.NO_CATEGORY
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.service.formatRemainingPlain
import com.gru.nagme.ui.Categories
import com.gru.nagme.ui.formatDeadline
import kotlinx.coroutines.runBlocking

/** Penyedia baris daftar untuk widget (RemoteViewsService). */
class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var items: List<TaskEntity> = emptyList()
        private var now: Long = 0L

        private val container get() = (context.applicationContext as NagMeApp).container

        override fun onCreate() = Unit

        // Dipanggil di thread binder, jadi aman membaca DB secara sinkron di sini.
        override fun onDataSetChanged() {
            items = runBlocking { container.taskRepository.activeForWidget() }
            now = container.clock.now()
        }

        override fun onDestroy() {
            items = emptyList()
        }

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews {
            val task = items.getOrNull(position)
                ?: return RemoteViews(context.packageName, R.layout.widget_task_item)
            val views = RemoteViews(context.packageName, R.layout.widget_task_item)

            val remaining = task.deadlineEpoch - now
            val remainingText = if (remaining <= 0L) {
                context.getString(R.string.remaining_passed)
            } else {
                context.getString(R.string.time_left, formatRemainingPlain(context, remaining))
            }
            val urgent = remaining <= URGENT_MS

            views.setTextViewText(R.id.item_title, task.title)
            views.setTextViewText(R.id.item_sub, formatDeadline(task.deadlineEpoch) + " \u00B7 " + remainingText)
            views.setTextColor(R.id.item_sub, if (urgent) COLOR_URGENT else COLOR_NORMAL)

            if (task.category != NO_CATEGORY && task.category in Categories.colors.indices) {
                views.setInt(R.id.item_bar, "setBackgroundColor", Categories.colors[task.category].toArgb())
                views.setViewVisibility(R.id.item_bar, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.item_bar, View.INVISIBLE)
            }

            views.setOnClickFillInIntent(
                R.id.item_root,
                Intent().putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
            )
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds(): Boolean = true
    }

    private companion object {
        const val URGENT_MS = 12 * 3_600_000L
        const val COLOR_URGENT = 0xFFB5603F.toInt()
        const val COLOR_NORMAL = 0xFF2F7D6B.toInt()
    }
}
