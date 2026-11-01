package com.example.durtlesreborn.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.example.durtlesreborn.DurtlesApp
import com.example.durtlesreborn.MainActivity
import com.example.durtlesreborn.R
import com.example.durtlesreborn.ui.AppTheme
import com.example.durtlesreborn.ui.DarkScheme
import com.example.durtlesreborn.ui.LightScheme
import com.example.durtlesreborn.ui.contentColorOn
import com.example.durtlesreborn.ui.fixedScheme
import com.example.durtlesreborn.ui.isDarkSurface
import com.example.durtlesreborn.ui.paletteFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** Home screen widget: how many reviews and lessons are waiting. Tapping it opens the app. */
class SessionWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = refresh(context)

    /** The first widget on the home screen starts the hourly alarm that keeps it current. */
    override fun onEnabled(context: Context) {
        NotificationScheduler.scheduleNext(context)
    }

    private fun refresh(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetUpdater.update(context)
            } finally {
                pending.finish()
            }
        }
    }
}

object WidgetUpdater {
    /** Asks the launcher to add the widget (Android 8 and later). Returns false if the launcher cannot. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, SessionWidgetProvider::class.java), null, null)
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, SessionWidgetProvider::class.java)).isNotEmpty()
    }

    suspend fun update(context: Context) {
        val app = context.applicationContext as DurtlesApp
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SessionWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val now = System.currentTimeMillis()
        val ctx = currentAlertContext(app.container.database.alerts(), app.container.database.syncState(), now)
        val settings = app.container.settingsStore.current()
        val theme = AppTheme.fromName(settings.theme, AppTheme.DYNAMIC)
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = isDarkSurface(theme, night)
        val scheme = fixedScheme(theme) ?: if (dark) DarkScheme else LightScheme
        val palette = paletteFor(theme, dark)
        val footer = WidgetFormatter.upcomingMessage(
            ctx.upcomingAvailableAt, now, ZoneId.systemDefault(), Locale.getDefault(),
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (id in ids) {
            val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val compact = height < 80
            val views = RemoteViews(context.packageName, R.layout.widget_session)
            views.setOnClickPendingIntent(R.id.widgetSurface, open)
            views.setInt(R.id.widgetBackground, "setColorFilter", scheme.background.toArgb())
            setCard(
                views, context, compact,
                backgroundId = R.id.lessonsCardBackground,
                iconId = R.id.lessonsIcon,
                iconRes = R.drawable.ic_widget_book,
                countId = R.id.lessonsCount,
                labelId = R.id.lessonsLabel,
                count = ctx.numLessons,
                active = palette.actions.lessons,
                muted = scheme.surfaceVariant,
                mutedContent = scheme.onSurfaceVariant,
            )
            setCard(
                views, context, compact,
                backgroundId = R.id.reviewsCardBackground,
                iconId = R.id.reviewsIcon,
                iconRes = R.drawable.ic_widget_pencil,
                countId = R.id.reviewsCount,
                labelId = R.id.reviewsLabel,
                count = ctx.numReviews,
                active = palette.actions.reviews,
                muted = scheme.surfaceVariant,
                mutedContent = scheme.onSurfaceVariant,
            )
            if (footer == null || compact) {
                views.setViewVisibility(R.id.footer, View.GONE)
            } else {
                views.setViewVisibility(R.id.footer, View.VISIBLE)
                views.setTextViewText(R.id.footer, footer)
                views.setTextColor(R.id.footer, scheme.onBackground.toArgb())
            }
            manager.updateAppWidget(id, views)
        }
    }

    /**
     * One card, laid out like the dashboard's Lessons/Reviews cards: the theme's action colour while
     * something is waiting, a muted surface at zero; the icon sits top right, and the label and a
     * white count circle run along the bottom.
     */
    private fun setCard(
        views: RemoteViews,
        context: Context,
        compact: Boolean,
        backgroundId: Int,
        iconId: Int,
        iconRes: Int,
        countId: Int,
        labelId: Int,
        count: Int,
        active: Color,
        muted: Color,
        mutedContent: Color,
    ) {
        val enabled = count > 0
        val cardColor = if (enabled) active else muted
        val content = if (enabled) contentColorOn(active) else mutedContent
        views.setInt(backgroundId, "setColorFilter", cardColor.toArgb())
        views.setTextViewText(countId, count.toString())
        // Like the dashboard, the count sits in a white circle and takes the card's own colour.
        views.setTextColor(countId, cardColor.toArgb())
        views.setTextColor(labelId, content.toArgb())
        if (compact) {
            views.setViewVisibility(iconId, View.GONE)
            views.setViewVisibility(labelId, View.GONE)
            views.setTextViewTextSize(countId, TypedValue.COMPLEX_UNIT_SP, 18f)
        } else {
            views.setViewVisibility(iconId, View.VISIBLE)
            views.setViewVisibility(labelId, View.VISIBLE)
            views.setImageViewBitmap(iconId, iconBitmap(context, iconRes, content.copy(alpha = 0.4f).toArgb()))
            views.setTextViewTextSize(countId, TypedValue.COMPLEX_UNIT_SP, 14f)
        }
    }

    /** Vector drawables are not reliable in RemoteViews, so the icon is rendered to a tinted bitmap. */
    private fun iconBitmap(context: Context, resId: Int, color: Int): Bitmap {
        val size = (26 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = ContextCompat.getDrawable(context, resId) ?: return bitmap
        drawable.setTint(color)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
