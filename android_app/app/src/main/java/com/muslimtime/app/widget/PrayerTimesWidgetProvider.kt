package com.muslimtime.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.PrayerCompletionState
import com.muslimtime.app.data.PrayerCompletionStore
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTime
import com.muslimtime.app.notifications.PrayerActionReceiver
import com.muslimtime.app.ui.MainActivity
import java.util.Calendar

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, options))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, newOptions))
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerTimesWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(componentName)
            if (widgetIds.isNotEmpty()) {
                widgetIds.forEach { widgetId ->
                    val options = manager.getAppWidgetOptions(widgetId)
                    manager.updateAppWidget(widgetId, buildRemoteViews(context, options))
                }
            }
        }

        private fun buildRemoteViews(context: Context, options: Bundle?): RemoteViews {
            val localizedContext = localizedContext(context)
            val prayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context)
            val isCompact = isCompactWidget(options)
            val layoutRes = if (isCompact) {
                R.layout.widget_prayer_times_compact
            } else {
                R.layout.widget_prayer_times
            }
            val views = RemoteViews(context.packageName, layoutRes)

            if (prayerTimes == null) {
                views.setTextViewText(R.id.widget_status, localizedContext.getString(R.string.widget_status_waiting))
                views.setTextViewText(R.id.widget_next_prayer, localizedContext.getString(R.string.widget_no_prayer))
                views.setTextViewText(R.id.widget_next_time, "--:--")
                views.setViewVisibility(R.id.widget_done_button, View.GONE)
                applyContentStyle(views, context, StatusStyle.NEUTRAL)
                styleStatus(views, context, StatusStyle.NEUTRAL)
            } else {
                val candidate = findWidgetPrayer(localizedContext, prayerTimes.times)
                views.setTextViewText(R.id.widget_status, candidate.status)
                views.setTextViewText(R.id.widget_next_prayer, candidate.prayer.name)
                views.setTextViewText(R.id.widget_next_time, candidate.prayer.time)
                if (candidate.style == StatusStyle.PENDING) {
                    views.setViewVisibility(R.id.widget_done_button, View.VISIBLE)
                    views.setTextViewText(R.id.widget_done_button, localizedContext.getString(R.string.widget_action_done))
                    val doneIntent = Intent(context, PrayerActionReceiver::class.java).apply {
                        action = PrayerActionReceiver.ACTION_MARK_DONE
                        putExtra(PrayerActionReceiver.EXTRA_PRAYER, candidate.prayer.name)
                        putExtra(PrayerActionReceiver.EXTRA_ID, candidate.prayerId)
                    }
                    val donePendingIntent = PendingIntent.getBroadcast(
                        context,
                        7000 + candidate.prayerId,
                        doneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.widget_done_button, donePendingIntent)
                } else {
                    views.setViewVisibility(R.id.widget_done_button, View.GONE)
                }
                applyContentStyle(views, context, candidate.style)
                styleStatus(views, context, candidate.style)
            }

            applyResponsiveSizing(views, options)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                5000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }

        private fun applyResponsiveSizing(views: RemoteViews, options: Bundle?) {
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val maxWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth) ?: minWidth
            val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight) ?: minHeight

            val profile = when {
                minHeight in 1..90 -> WidgetSizeProfile.COMPACT
                maxWidth >= 250 || maxHeight >= 180 -> WidgetSizeProfile.EXPANDED
                else -> WidgetSizeProfile.REGULAR
            }

            when (profile) {
                WidgetSizeProfile.COMPACT -> {
                    views.setTextViewTextSize(R.id.widget_status, TypedValue.COMPLEX_UNIT_SP, 11f)
                    views.setTextViewTextSize(R.id.widget_next_prayer, TypedValue.COMPLEX_UNIT_SP, 16f)
                    views.setTextViewTextSize(R.id.widget_next_time, TypedValue.COMPLEX_UNIT_SP, if (maxWidth >= 240) 42f else 34f)
                    views.setTextViewTextSize(R.id.widget_done_button, TypedValue.COMPLEX_UNIT_SP, 12f)
                }

                WidgetSizeProfile.REGULAR -> {
                    views.setTextViewTextSize(R.id.widget_status, TypedValue.COMPLEX_UNIT_SP, 13f)
                    views.setTextViewTextSize(R.id.widget_next_prayer, TypedValue.COMPLEX_UNIT_SP, 22f)
                    views.setTextViewTextSize(R.id.widget_next_time, TypedValue.COMPLEX_UNIT_SP, if (minWidth <= 140) 36f else 40f)
                    views.setTextViewTextSize(R.id.widget_done_button, TypedValue.COMPLEX_UNIT_SP, 14f)
                }

                WidgetSizeProfile.EXPANDED -> {
                    views.setTextViewTextSize(R.id.widget_status, TypedValue.COMPLEX_UNIT_SP, 15f)
                    views.setTextViewTextSize(R.id.widget_next_prayer, TypedValue.COMPLEX_UNIT_SP, 26f)
                    views.setTextViewTextSize(R.id.widget_next_time, TypedValue.COMPLEX_UNIT_SP, if (minWidth <= 180) 42f else 48f)
                    views.setTextViewTextSize(R.id.widget_done_button, TypedValue.COMPLEX_UNIT_SP, 14f)
                }
            }
        }

        private fun localizedContext(context: Context): Context {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (tags.isBlank()) return context
            val locale = LocaleListCompat.forLanguageTags(tags)[0] ?: return context
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        }

        private fun isCompactWidget(options: Bundle?): Boolean {
            if (options == null) return false
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            return minHeight in 1..90
        }

        private data class WidgetPrayerDisplay(
            val prayer: PrayerTime,
            val prayerId: Int,
            val status: String,
            val style: StatusStyle,
        )

        private enum class StatusStyle {
            PENDING,
            ACTIVE,
            NEUTRAL,
        }

        private enum class WidgetSizeProfile {
            COMPACT,
            REGULAR,
            EXPANDED,
        }

        private fun findWidgetPrayer(context: Context, times: List<PrayerTime>): WidgetPrayerDisplay {
            val now = Calendar.getInstance()
            val prayerIndexes = listOf(0, 2, 3, 4, 5)
            val requestCodes = listOf(7, 9, 10, 11, 12)

            prayerIndexes.forEachIndexed { mappedIndex, prayerIndex ->
                val prayer = times.getOrNull(prayerIndex) ?: return@forEachIndexed
                val target = buildPrayerCalendar(prayer.time, now, false)
                val state = PrayerCompletionStore.getState(context, requestCodes[mappedIndex])
                if (target.timeInMillis <= now.timeInMillis && state != PrayerCompletionState.DONE) {
                    return WidgetPrayerDisplay(
                        prayer = prayer,
                        prayerId = requestCodes[mappedIndex],
                        status = context.getString(R.string.widget_status_pending),
                        style = StatusStyle.PENDING,
                    )
                }
            }

            prayerIndexes.forEach { prayerIndex ->
                val prayer = times.getOrNull(prayerIndex) ?: return@forEach
                val target = buildPrayerCalendar(prayer.time, now, false)
                if (target.timeInMillis > now.timeInMillis) {
                    return WidgetPrayerDisplay(
                        prayer = prayer,
                        prayerId = requestCodes[prayerIndexes.indexOf(prayerIndex)],
                        status = context.getString(R.string.widget_status_next),
                        style = StatusStyle.ACTIVE,
                    )
                }
            }

            val firstPrayer = times.getOrNull(0) ?: PrayerTime(context.getString(R.string.widget_no_prayer), "--:--")
            return WidgetPrayerDisplay(
                prayer = firstPrayer,
                prayerId = requestCodes.first(),
                status = context.getString(R.string.widget_status_next),
                style = StatusStyle.ACTIVE,
            )
        }

        private fun styleStatus(views: RemoteViews, context: Context, style: StatusStyle) {
            val (bg, text) = when (style) {
                StatusStyle.PENDING -> R.drawable.bg_widget_status_pending to R.color.status_pending_text
                StatusStyle.ACTIVE -> R.drawable.bg_widget_status_active to R.color.active_card_stroke
                StatusStyle.NEUTRAL -> R.drawable.bg_widget_status_neutral to R.color.status_upcoming_text
            }
            views.setInt(R.id.widget_status, "setBackgroundResource", bg)
            views.setTextColor(R.id.widget_status, context.getColor(text))
        }

        private fun applyContentStyle(views: RemoteViews, context: Context, style: StatusStyle) {
            val prayerTextColor = when (style) {
                StatusStyle.PENDING -> R.color.status_pending_text
                StatusStyle.ACTIVE -> R.color.text_primary
                StatusStyle.NEUTRAL -> R.color.text_primary
            }
            val timeTextColor = when (style) {
                StatusStyle.PENDING -> R.color.status_pending_text
                StatusStyle.ACTIVE -> R.color.text_primary
                StatusStyle.NEUTRAL -> R.color.text_primary
            }
            val actionTextColor = when (style) {
                StatusStyle.PENDING -> R.color.status_pending_text
                else -> R.color.accent_primary
            }
            val actionBg = when (style) {
                StatusStyle.PENDING -> R.drawable.bg_widget_action_pending
                else -> R.drawable.bg_widget_action
            }
            views.setTextColor(R.id.widget_next_prayer, context.getColor(prayerTextColor))
            views.setTextColor(R.id.widget_next_time, context.getColor(timeTextColor))
            views.setTextColor(R.id.widget_done_button, context.getColor(actionTextColor))
            views.setInt(R.id.widget_done_button, "setBackgroundResource", actionBg)
        }

        private fun buildPrayerCalendar(time: String, now: Calendar, nextDay: Boolean): Calendar {
            return Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                set(Calendar.HOUR_OF_DAY, time.substringBefore(":").toIntOrNull() ?: 0)
                set(Calendar.MINUTE, time.substringAfter(":", "0").take(2).toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (nextDay) add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}
