package com.muslimtime.app.ui

import android.Manifest
import android.icu.util.IslamicCalendar
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.location.Location
import android.location.LocationManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.RingtoneManager
import android.provider.Settings
import android.provider.OpenableColumns
import android.util.TypedValue
import android.util.Log
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.muslimtime.app.BuildConfig
import com.muslimtime.app.R
import com.muslimtime.app.data.AudioAyahItem
import com.muslimtime.app.data.AudioSuraItem
import com.muslimtime.app.data.AppLocation
import com.muslimtime.app.data.AllahName
import com.muslimtime.app.data.AsmaUlHusnaRepository
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.ManualCityOption
import com.muslimtime.app.data.ManualCountryOption
import com.muslimtime.app.data.ManualLocationCatalog
import com.muslimtime.app.data.NotificationCenterItem
import com.muslimtime.app.data.NotificationCenterStore
import com.muslimtime.app.data.PrayerCompletionState
import com.muslimtime.app.data.PrayerCompletionStore
import com.muslimtime.app.data.PrayerDataSyncManager
import com.muslimtime.app.data.ReminderDiagnosticsStore
import com.muslimtime.app.data.PrayerHistoryStore
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTimesRefreshWorker
import com.muslimtime.app.data.PrayerTimesRepository
import com.muslimtime.app.data.QuranAudioBackendRepository
import com.muslimtime.app.data.QuranEncRepository
import com.muslimtime.app.data.QuranAudioOfflineRepository
import com.muslimtime.app.data.QuranSura
import com.muslimtime.app.data.QuranVerse
import com.muslimtime.app.data.allahNameAzerbaijaniTitle
import com.muslimtime.app.data.samplePrayerTimes
import com.muslimtime.app.data.quranSuras
import com.muslimtime.app.data.sampleDuas
import com.muslimtime.app.data.supportedLanguages
import com.muslimtime.app.notifications.AzanPlaybackService
import com.muslimtime.app.notifications.AppPushManager
import com.muslimtime.app.notifications.PrayerRefreshScheduler
import com.muslimtime.app.notifications.PrayerReminderScheduler
import com.muslimtime.app.notifications.QuranAudioPlaybackService
import java.util.Locale
import java.util.Calendar
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var pager: ViewPager2
    private lateinit var nav: BottomNavigationView
    private var setupMode = false
    private var permissionOnboardingRunning = false
    private var isSyncingNavigation = false
    private val notificationCenterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNotificationBadge()
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        continuePermissionOnboarding()
    }
    private val locationPermissionOnboardingLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        continuePermissionOnboarding()
    }
    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (hasExactAlarmPermission()) {
            continuePermissionOnboarding()
        } else {
            finishPermissionOnboarding()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedThemeMode()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.main_root)
        pager = findViewById(R.id.pager)
        nav = findViewById(R.id.bottom_nav)

        applySystemBarStyle()
        applySystemBarInsets()
        AppPushManager.initialize(this)

        pager.adapter = DashboardPagerAdapter(this)
        pager.isUserInputEnabled = false

        nav.setOnItemSelectedListener { item ->
            if (isSyncingNavigation) return@setOnItemSelectedListener true
            val targetPage = when (item.itemId) {
                R.id.nav_prayer -> 0
                R.id.nav_quran -> 1
                R.id.nav_dua -> 2
                R.id.nav_notifications -> 3
                else -> 4
            }
            if (pager.currentItem != targetPage) {
                pager.currentItem = targetPage
            }
            true
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                syncNavigationForPage(position)
            }
        })

        ensureDefaultAppSetupState()
        setupMode = false
        nav.visibility = View.VISIBLE
        syncNavigationForPage(0)
        PrayerRefreshScheduler.scheduleNextJumaaReminder(this)
        if (PrayerDataSyncManager.shouldUseDailyRefreshAlarm(this)) {
            PrayerRefreshScheduler.scheduleNextDailyRefresh(this)
        }
        if (PrayerDataSyncManager.shouldUsePeriodicWorker(this)) {
            PrayerTimesRefreshWorker.schedule(this)
        } else {
            PrayerTimesRefreshWorker.cancel(this)
        }
        PrayerPreferences.loadSelectedPrayerTimes(this)?.let(::scheduleReminderSet)
        if (PrayerDataSyncManager.needsSync(this)) {
            PrayerTimesRefreshWorker.enqueueImmediate(this)
        }
        if (PrayerPreferences.isAutoLocationEnabled(this)) {
            (supportFragmentManager.findFragmentByTag("f4") as? TimerFragment)?.refreshAutoLocationIfEnabled()
        }

        maybeStartPermissionOnboarding()
        processInboundAnnouncement(intent)
        updateNotificationBadge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processInboundAnnouncement(intent)
        updateNotificationBadge()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            notificationCenterReceiver,
            IntentFilter(NotificationCenterStore.ACTION_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        updateNotificationBadge()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(notificationCenterReceiver) }
        super.onStop()
    }

    private fun applySystemBarStyle() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_dark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.status_bar_dark)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = !isNightMode
        controller.isAppearanceLightNavigationBars = !isNightMode
    }

    private fun applySystemBarInsets() {
        val originalTop = rootView.paddingTop
        val originalBottom = nav.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            rootView.setPadding(
                rootView.paddingLeft,
                originalTop + systemBars.top,
                rootView.paddingRight,
                rootView.paddingBottom,
            )
            nav.setPadding(
                nav.paddingLeft,
                nav.paddingTop,
                nav.paddingRight,
                originalBottom + systemBars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun ensureDefaultAppSetupState() {
        val defaultLocation = PrayerPreferences.loadSelectedLocation(this) ?: PrayerPreferences.suggestedLocation(this)
        if (PrayerPreferences.loadSelectedLocation(this) == null) {
            PrayerPreferences.saveLocation(this, defaultLocation, isAutoDetected = false)
        }
        if (!PrayerPreferences.hasCompletedInitialSetup(this)) {
            PrayerPreferences.setInitialSetupComplete(this, true)
            PrayerPreferences.setProfileSetupCompleted(this, true)
            PrayerPreferences.setNotificationSetupCompleted(this, true)
            PrayerPreferences.setLocationSetupCompleted(this, true)
            PrayerPreferences.setSoundSetupCompleted(this, true)
        }
    }

    private fun applySavedThemeMode() {
        val mode = PrayerPreferences.getThemeMode(this)
        val nightMode = when (mode) {
            com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            com.muslimtime.app.data.AppearancePreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun maybeStartPermissionOnboarding() {
        if (PrayerPreferences.hasCompletedPermissionOnboarding(this) || permissionOnboardingRunning) return
        permissionOnboardingRunning = true
        continuePermissionOnboarding()
    }

    private fun continuePermissionOnboarding() {
        if (!hasNotificationPermission()) {
            requestNotificationPermission()
            return
        }
        if (!hasLocationPermission()) {
            locationPermissionOnboardingLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
            return
        }
        if (!hasExactAlarmPermission()) {
            showExactAlarmPermissionDialog()
            return
        }
        finishPermissionOnboarding()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_exact_alarm_title)
            .setMessage(R.string.permissions_exact_alarm_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permissions_continue) { _, _ ->
                exactAlarmPermissionLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            }
            .setNegativeButton(R.string.permissions_skip) { _, _ ->
                finishPermissionOnboarding()
            }
            .show()
    }

    private fun finishPermissionOnboarding() {
        permissionOnboardingRunning = false
        PrayerPreferences.setPermissionOnboardingCompleted(this, true)
    }

    private fun showSetupRequiredDialog() {
        // Setup mode already routes the user into Settings where the real checklist dialog is shown.
    }

    fun scheduleReminderSet(prayerTimes: CityPrayerTimes) {
        PrayerReminderScheduler.scheduleReminderSet(this, prayerTimes)
    }

    fun applySetupMode(enabled: Boolean, navigateToPrayer: Boolean = true) {
        setupMode = enabled
        nav.visibility = if (enabled) View.GONE else View.VISIBLE
        pager.isUserInputEnabled = !enabled
        if (enabled) {
            pager.currentItem = 4
            syncNavigationForPage(4)
        } else {
            if (navigateToPrayer) {
                pager.currentItem = 0
                syncNavigationForPage(0)
            } else {
                pager.currentItem = 4
                syncNavigationForPage(4)
            }
        }
    }

    fun openSettings() {
        pager.currentItem = 4
        syncNavigationForPage(4)
    }

    private fun updateNotificationBadge() {
        val unreadCount = NotificationCenterStore.unreadCount(this)
        val badge = nav.getOrCreateBadge(R.id.nav_notifications)
        if (unreadCount <= 0) {
            badge.clearNumber()
            badge.isVisible = false
        } else {
            badge.number = unreadCount.coerceAtMost(99)
            badge.isVisible = true
        }
    }

    private fun processInboundAnnouncement(intent: Intent?) {
        val extras = intent?.extras ?: return
        val title = extras.getString("gcm.notification.title")
            ?: extras.getString("google.c.a.c_l")
            ?: extras.getString("title")
        val body = extras.getString("gcm.notification.body")
            ?: extras.getString("body")
            ?: extras.getString("alert")
        if (title.isNullOrBlank() || body.isNullOrBlank()) return
        val uniqueKey = extras.getString("google.message_id")
            ?: extras.getString("message_id")
            ?: "${System.currentTimeMillis() / 60000L}"
        NotificationCenterStore.addAnnouncement(
            this,
            uniqueKey = uniqueKey,
            title = title,
            body = body,
        )
    }

    private fun syncNavigationForPage(position: Int) {
        val targetItemId = when (position) {
            0 -> R.id.nav_prayer
            1 -> R.id.nav_quran
            2 -> R.id.nav_dua
            3 -> R.id.nav_notifications
            4 -> R.id.nav_settings
            else -> View.NO_ID
        }
        if (targetItemId != View.NO_ID && nav.selectedItemId != targetItemId) {
            isSyncingNavigation = true
            nav.selectedItemId = targetItemId
            isSyncingNavigation = false
        }
    }

}

class DashboardPagerAdapter(activity: MainActivity) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> PrayerFragment()
        1 -> QuranFragment()
        2 -> DuaFragment()
        3 -> NotificationsFragment()
        else -> TimerFragment()
    }
}

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    private var listView: ListView? = null
    private var statusView: TextView? = null
    private var emptyView: TextView? = null
    private lateinit var adapter: BaseAdapter
    private var items: List<NotificationCenterItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applySharedUiPreferences(view, requireContext())
        listView = view.findViewById(R.id.notifications_list)
        statusView = view.findViewById(R.id.notifications_status)
        emptyView = view.findViewById(R.id.notifications_empty)
        val clearAllButton = view.findViewById<Button>(R.id.notifications_clear_all_button)
        adapter = object : BaseAdapter() {
            override fun getCount(): Int = items.size
            override fun getItem(position: Int): Any = items[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: layoutInflater.inflate(R.layout.item_notification_center, parent, false)
                val item = items[position]
                val badge = row.findViewById<TextView>(R.id.notification_item_badge)
                val time = row.findViewById<TextView>(R.id.notification_item_time)
                val title = row.findViewById<TextView>(R.id.notification_item_title)
                val body = row.findViewById<TextView>(R.id.notification_item_body)
                val deleteButton = row.findViewById<ImageButton>(R.id.notification_item_delete)

                badge.text = when (item.type) {
                    "announcement" -> getString(R.string.notifications_center_type_announcement)
                    "general" -> getString(R.string.notifications_center_type_general)
                    else -> getString(R.string.notifications_center_type_prayer)
                }
                badge.background = AppCompatResources.getDrawable(
                    requireContext(),
                    if (item.isRead) R.drawable.bg_widget_status_neutral else R.drawable.bg_widget_status_active,
                )
                badge.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (item.isRead) R.color.text_secondary else android.R.color.white,
                    ),
                )
                time.text = NotificationCenterStore.formatTimestamp(item.createdAt)
                title.text = item.title
                body.text = item.body
                title.alpha = if (item.isRead) 0.78f else 1f
                body.alpha = if (item.isRead) 0.78f else 1f
                deleteButton.setOnClickListener {
                    NotificationCenterStore.delete(requireContext(), item.id)
                    refreshInbox(markRead = false)
                }
                return row
            }
        }
        listView?.adapter = adapter
        clearAllButton.setOnClickListener {
            NotificationCenterStore.clearAll(requireContext())
            refreshInbox(markRead = false)
            Toast.makeText(requireContext(), getString(R.string.notifications_center_cleared), Toast.LENGTH_SHORT).show()
        }
        refreshInbox(markRead = true)
    }

    override fun onResume() {
        super.onResume()
        refreshInbox(markRead = true)
    }

    private fun refreshInbox(markRead: Boolean) {
        if (markRead) NotificationCenterStore.markAllRead(requireContext())
        items = NotificationCenterStore.list(requireContext())
        adapter.notifyDataSetChanged()
        val unreadCount = items.count { !it.isRead }
        statusView?.text = if (items.isEmpty()) {
            getString(R.string.notifications_center_empty)
        } else if (unreadCount > 0) {
            getString(R.string.notifications_center_unread_count, unreadCount)
        } else {
            getString(R.string.notifications_center_total_count, items.size)
        }
        emptyView?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listView?.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }
}

private fun applyScaledTextTree(view: View, scale: Float) {
    if (view.tag == "no_scale_text") return
    if (view is TextView) {
        val scaledDensity = view.resources.displayMetrics.density * view.resources.configuration.fontScale
        val originalSp = (view.getTag(R.id.tag_original_text_size_sp) as? Float)
            ?: (view.textSize / scaledDensity).also {
                view.setTag(R.id.tag_original_text_size_sp, it)
            }
        setScaledTextSizeIgnoringSystemFont(view, originalSp, scale)
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            applyScaledTextTree(view.getChildAt(i), scale)
        }
    }
}

private fun setScaledTextSizeIgnoringSystemFont(view: TextView, baseSp: Float, scale: Float) {
    val density = view.resources.displayMetrics.density
    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * scale * density)
}

private fun applySharedUiPreferences(root: View, context: Context) {
    applyScaledTextTree(root, PrayerPreferences.appFontScale(context))
}

class PrayerFragment : Fragment(R.layout.fragment_prayer) {
    companion object {
        private const val PRAYER_REFRESH_THROTTLE_MS = 90_000L
    }
    private var refreshInFlight = false
    private var selectedLocation: AppLocation? = null
    private var renderedLoadingLabel: TextView? = null
    private var gregorianDateLabel: TextView? = null
    private var hijriDateLabel: TextView? = null
    private var localTimeLabel: TextView? = null
    private var nextPrayerLabel: TextView? = null
    private var personalPrayerMessageLabel: TextView? = null
    private var selectedDayLabel: TextView? = null
    private var imsakIftarSection: View? = null
    private var imsakTimeLabel: TextView? = null
    private var iftarTimeLabel: TextView? = null
    private var prevDayButton: View? = null
    private var nextDayButton: View? = null
    private var prayerStatusViews: List<TextView> = emptyList()
    private var prayerCardViews: List<View> = emptyList()
    private var prayerNameViews: List<TextView> = emptyList()
    private var prayerTimeViews: List<TextView> = emptyList()
    private var timelineDotViews: List<View> = emptyList()
    private var timelineLabelViews: List<TextView> = emptyList()
    private var displayedPrayerTimes: CityPrayerTimes? = null
    private var lastRefreshKey: String? = null
    private var lastRefreshAtMs: Long = 0L
    private val selectedDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val reminderRequestCodes = listOf(7, 8, 9, 10, 11, 12)
    private val prayerCardBackgrounds = listOf(
        R.drawable.bg_prayer_fajr,
        R.drawable.bg_prayer_sunrise,
        R.drawable.bg_prayer_dhuhr,
        R.drawable.bg_prayer_asr,
        R.drawable.bg_prayer_maghrib,
        R.drawable.bg_prayer_isha,
    )
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClockAndCountdown()
            clockHandler.postDelayed(this, 1_000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applySharedUiPreferences(view, requireContext())
        val loadingText = view.findViewById<TextView>(R.id.loading_text)
        renderedLoadingLabel = loadingText
        gregorianDateLabel = view.findViewById(R.id.gregorian_date_text)
        hijriDateLabel = view.findViewById(R.id.hijri_date_text)
        localTimeLabel = view.findViewById(R.id.local_time_text)
        nextPrayerLabel = view.findViewById(R.id.next_prayer_text)
        personalPrayerMessageLabel = view.findViewById(R.id.prayer_personal_message_text)
        selectedDayLabel = view.findViewById(R.id.selected_day_text)
        imsakIftarSection = view.findViewById(R.id.today_summary_card)
        imsakTimeLabel = view.findViewById(R.id.today_imsak_time)
        iftarTimeLabel = view.findViewById(R.id.today_iftar_time)
        prevDayButton = view.findViewById(R.id.date_prev_button)
        nextDayButton = view.findViewById(R.id.date_next_button)
        prevDayButton?.setOnClickListener {
            changeSelectedDate(-1)
        }
        nextDayButton?.setOnClickListener {
            changeSelectedDate(1)
        }
        view.findViewById<View>(R.id.date_today_button).setOnClickListener {
            resetToToday()
        }
        prayerNameViews = listOf(
            view.findViewById(R.id.prayer_name_1),
            view.findViewById(R.id.prayer_name_2),
            view.findViewById(R.id.prayer_name_3),
            view.findViewById(R.id.prayer_name_4),
            view.findViewById(R.id.prayer_name_5),
            view.findViewById(R.id.prayer_name_6),
        )
        prayerTimeViews = listOf(
            view.findViewById(R.id.prayer_time_1),
            view.findViewById(R.id.prayer_time_2),
            view.findViewById(R.id.prayer_time_3),
            view.findViewById(R.id.prayer_time_4),
            view.findViewById(R.id.prayer_time_5),
            view.findViewById(R.id.prayer_time_6),
        )
        prayerStatusViews = listOf(
            view.findViewById(R.id.prayer_status_1),
            view.findViewById(R.id.prayer_status_2),
            view.findViewById(R.id.prayer_status_3),
            view.findViewById(R.id.prayer_status_4),
            view.findViewById(R.id.prayer_status_5),
            view.findViewById(R.id.prayer_status_6),
        )
        prayerCardViews = listOf(
            view.findViewById(R.id.prayer_card_1),
            view.findViewById(R.id.prayer_card_2),
            view.findViewById(R.id.prayer_card_3),
            view.findViewById(R.id.prayer_card_4),
            view.findViewById(R.id.prayer_card_5),
            view.findViewById(R.id.prayer_card_6),
        )
        timelineDotViews = listOf(
            view.findViewById(R.id.timeline_dot_1),
            view.findViewById(R.id.timeline_dot_2),
            view.findViewById(R.id.timeline_dot_3),
            view.findViewById(R.id.timeline_dot_4),
            view.findViewById(R.id.timeline_dot_5),
            view.findViewById(R.id.timeline_dot_6),
        )
        timelineLabelViews = listOf(
            view.findViewById(R.id.timeline_label_1),
            view.findViewById(R.id.timeline_label_2),
            view.findViewById(R.id.timeline_label_3),
            view.findViewById(R.id.timeline_label_4),
            view.findViewById(R.id.timeline_label_5),
            view.findViewById(R.id.timeline_label_6),
        )
        prayerCardViews.forEachIndexed { index, card ->
            card.setOnClickListener { markPrayerDoneFromCard(index) }
        }
        val location = PrayerPreferences.loadSelectedLocation(requireContext())
        selectedLocation = location
        loadingText.visibility = View.GONE
        bindPrayerCards(
            PrayerPreferences.loadSelectedPrayerTimes(requireContext()) ?: CityPrayerTimes(
                city = location?.city.orEmpty(),
                country = location?.country.orEmpty(),
                times = PrayerPreferences.localizedPrayerNames(requireContext()).map {
                    com.muslimtime.app.data.PrayerTime(it, "--:--")
                },
                imsakTime = "--:--",
            ),
        )
        updateSelectedDayLabel()
        updateDateNavigationButtons()
        location?.let { refreshForSelectedDate(it) }
    }

    override fun onResume() {
        super.onResume()
        updateClockAndCountdown()
        selectedLocation?.let { refreshForSelectedDate(it) }
    }

    override fun onStart() {
        super.onStart()
        updateClockAndCountdown()
        clockHandler.post(clockRunnable)
    }

    override fun onStop() {
        super.onStop()
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun bindPrayerCards(city: CityPrayerTimes) {
        displayedPrayerTimes = city
        val fallbackNames = PrayerPreferences.localizedPrayerNames(requireContext())
        imsakTimeLabel?.text = city.imsakTime ?: city.times.getOrNull(0)?.time ?: "--:--"
        iftarTimeLabel?.text = city.times.getOrNull(4)?.time ?: "--:--"
        gregorianDateLabel?.text = formatGregorianDate(selectedDate)
        hijriDateLabel?.text = formatHijriDate(selectedDate)
        val showAllMonths = PrayerPreferences.shouldShowImsakIftarOutsideRamadan(requireContext())
        imsakIftarSection?.visibility = if (showAllMonths || isRamadan(selectedDate)) View.VISIBLE else View.GONE
        updateSelectedDayLabel()
        prayerNameViews.forEachIndexed { index, textView ->
            textView.text = city.times.getOrNull(index)?.name ?: fallbackNames.getOrElse(index) { "" }
        }
        prayerTimeViews.forEachIndexed { index, textView ->
            textView.text = city.times.getOrNull(index)?.time ?: "--:--"
        }
        timelineLabelViews.forEachIndexed { index, textView ->
            textView.text = city.times.getOrNull(index)?.name?.substringBefore(" ") ?: fallbackNames.getOrElse(index) { "" }
        }
        updateClockAndCountdown()
    }

    private fun updateClockAndCountdown() {
        val now = Calendar.getInstance()
        localTimeLabel?.text = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            now.get(Calendar.SECOND),
        )
        val prayerTimes = displayedPrayerTimes ?: run {
            nextPrayerLabel?.text = getString(R.string.next_prayer_placeholder)
            nextPrayerLabel?.visibility = View.VISIBLE
            personalPrayerMessageLabel?.text = ""
            return
        }

        if (prayerTimes.times.all { it.time == "--:--" }) {
            nextPrayerLabel?.text = getString(R.string.next_prayer_placeholder)
            nextPrayerLabel?.visibility = View.VISIBLE
            personalPrayerMessageLabel?.text = ""
            updateTimeline(now, prayerTimes)
            updatePrayerStatuses(now, prayerTimes)
            return
        }

        if (!isTodaySelected()) {
            nextPrayerLabel?.text = getString(R.string.next_prayer_selected_day, formatGregorianDate(selectedDate))
            nextPrayerLabel?.visibility = View.VISIBLE
            personalPrayerMessageLabel?.text = ""
            updateTimeline(now, prayerTimes)
            updatePrayerStatuses(now, prayerTimes)
            return
        }

        val next = findNextPrayer(prayerTimes, now)
        val minutesLeft = ((next.second.timeInMillis - now.timeInMillis) / 60000L).toInt().coerceAtLeast(0)
        val hours = minutesLeft / 60
        val minutes = minutesLeft % 60
        val nextPrayerCountdownText = formatRelativeCountdown(minutesLeft)
        nextPrayerLabel?.text = getString(
            R.string.next_prayer_countdown,
            formatPrayerNameForCountdown(next.first.name),
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes),
        )
        nextPrayerLabel?.visibility = View.GONE
        personalPrayerMessageLabel?.text = buildPersonalPrayerMessage(
            now = now,
            prayerTimes = prayerTimes,
            nextPrayerName = next.first.name,
            nextPrayerCountdown = nextPrayerCountdownText,
        )
        updateTimeline(now, prayerTimes)
        updatePrayerStatuses(now, prayerTimes)
    }

    private fun formatPrayerNameForCountdown(prayerName: String): String {
        return prayerName
    }

    private fun formatPrayerNameForObjectCase(prayerName: String): String {
        return when {
            prayerName.endsWith(" namazı", ignoreCase = true) ->
                prayerName.removeSuffix(" namazı") + " namazını"
            prayerName.endsWith(" namazi", ignoreCase = true) ->
                prayerName.removeSuffix(" namazi") + " namazini"
            else -> prayerName
        }
    }

    private fun buildPersonalPrayerMessage(
        now: Calendar,
        prayerTimes: CityPrayerTimes,
        nextPrayerName: String,
        nextPrayerCountdown: String,
    ): String {
        val context = requireContext()
        val address = PrayerPreferences.personalizedAddress(context)
        val blessing = pickPersonalPrayerBlessing(now)

        findFirstPendingPrayer(prayerTimes, now)?.let { prayerName ->
            val pendingPrayerText = formatPrayerNameForObjectCase(prayerName)
            val message = if (address.isNotBlank()) {
                getString(
                    R.string.prayer_personal_pending_with_name,
                    address,
                    pendingPrayerText,
                    nextPrayerName,
                    nextPrayerCountdown,
                )
            } else {
                getString(
                    R.string.prayer_personal_pending_no_name,
                    pendingPrayerText,
                    nextPrayerName,
                    nextPrayerCountdown,
                )
            }
            return getString(R.string.prayer_personal_message_with_blessing, message, blessing)
        }

        findLatestDonePrayerEntry(now.timeInMillis)?.let { entry ->
            val elapsedText = formatRelativeElapsed(now.timeInMillis - entry.timestamp)
            val donePrayerText = formatPrayerNameForObjectCase(entry.prayerName)
            val message = if (address.isNotBlank()) {
                getString(
                    R.string.prayer_personal_done_with_name,
                    address,
                    donePrayerText,
                    elapsedText,
                    nextPrayerName,
                    nextPrayerCountdown,
                )
            } else {
                getString(
                    R.string.prayer_personal_done_no_name,
                    donePrayerText,
                    elapsedText,
                    nextPrayerName,
                    nextPrayerCountdown,
                )
            }
            return getString(R.string.prayer_personal_message_with_blessing, message, blessing)
        }

        val message = if (address.isNotBlank()) {
            getString(R.string.prayer_personal_next_with_name, address, nextPrayerName, nextPrayerCountdown)
        } else {
            getString(R.string.prayer_personal_next_no_name, nextPrayerName, nextPrayerCountdown)
        }
        return getString(R.string.prayer_personal_message_with_blessing, message, blessing)
    }

    private fun pickPersonalPrayerBlessing(now: Calendar): String {
        val blessingRes = when ((now.get(Calendar.DAY_OF_YEAR) + now.get(Calendar.MINUTE)) % 3) {
            0 -> R.string.prayer_personal_blessing_1
            1 -> R.string.prayer_personal_blessing_2
            else -> R.string.prayer_personal_blessing_3
        }
        return getString(blessingRes)
    }

    private fun findFirstPendingPrayer(prayerTimes: CityPrayerTimes, now: Calendar): String? {
        prayerTimes.times.forEachIndexed { index, prayer ->
            if (index == 1) return@forEachIndexed
            val hasTimePassed = buildPrayerCalendar(prayer.time, now, false).timeInMillis <= now.timeInMillis
            val completionState = PrayerCompletionStore.getState(requireContext(), reminderRequestCodes[index])
            if (hasTimePassed && completionState != PrayerCompletionState.DONE) {
                return prayer.name
            }
        }
        return null
    }

    private fun findLatestDonePrayerEntry(nowMs: Long): com.muslimtime.app.data.PrayerHistoryEntry? {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return PrayerHistoryStore.loadEntries(requireContext())
            .firstOrNull { it.timestamp in startOfDay..nowMs }
    }

    private fun formatRelativeElapsed(deltaMs: Long): String {
        val totalMinutes = (deltaMs / 60000L).toInt().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            getString(R.string.prayer_relative_hours_minutes, hours, minutes)
        } else {
            getString(R.string.prayer_relative_minutes, minutes)
        }
    }

    private fun formatRelativeCountdown(totalMinutes: Int): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0)
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60
        return if (hours > 0) {
            getString(R.string.prayer_relative_hours_minutes, hours, minutes)
        } else {
            getString(R.string.prayer_relative_minutes, minutes)
        }
    }

    private fun findNextPrayer(prayerTimes: CityPrayerTimes, now: Calendar): Pair<com.muslimtime.app.data.PrayerTime, Calendar> {
        prayerTimes.times.forEach { prayer ->
            val target = buildPrayerCalendar(prayer.time, now, false)
            if (target.timeInMillis > now.timeInMillis) {
                return prayer to target
            }
        }
        val firstPrayer = prayerTimes.times.firstOrNull()
            ?: com.muslimtime.app.data.PrayerTime(getString(R.string.nav_prayer), "00:00")
        return firstPrayer to buildPrayerCalendar(firstPrayer.time, now, true)
    }

    private fun buildPrayerCalendar(time: String, now: Calendar, nextDay: Boolean): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, time.substringBefore(":").toIntOrNull() ?: 0)
            set(Calendar.MINUTE, time.substringAfter(":", "0").take(2).toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (nextDay) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun formatGregorianDate(calendar: Calendar): String {
        val monthNames = resources.getStringArray(R.array.gregorian_months_az)
        val month = monthNames.getOrElse(calendar.get(Calendar.MONTH)) { "" }
        return "${calendar.get(Calendar.DAY_OF_MONTH)} $month"
    }

    private fun formatHijriDate(calendar: Calendar): String {
        val hijriCalendar = IslamicCalendar().apply {
            timeInMillis = calendar.timeInMillis
        }
        val monthNames = resources.getStringArray(R.array.hijri_months_az)
        val month = monthNames.getOrElse(hijriCalendar.get(IslamicCalendar.MONTH)) { "" }
        return "${hijriCalendar.get(IslamicCalendar.DAY_OF_MONTH)} $month"
    }

    private fun isRamadan(calendar: Calendar): Boolean {
        val hijriCalendar = IslamicCalendar().apply {
            timeInMillis = calendar.timeInMillis
        }
        return hijriCalendar.get(IslamicCalendar.MONTH) == 8
    }

    private fun updatePrayerStatuses(now: Calendar, prayerTimes: CityPrayerTimes) {
        val activeIndex = if (isTodaySelected()) findHighlightedPrayerIndex(prayerTimes, now) else -1
        prayerTimes.times.forEachIndexed { index, prayer ->
            val statusView = prayerStatusViews.getOrNull(index) ?: return@forEachIndexed
            val cardView = prayerCardViews.getOrNull(index) ?: return@forEachIndexed

            if (!isTodaySelected()) {
                statusView.text = getString(R.string.prayer_selected_day_status)
                applyStatusBadge(statusView, R.color.status_upcoming_bg, R.color.status_upcoming_text)
                applyCardBackground(cardView, prayerCardBackgrounds[index], highlighted = false)
                return@forEachIndexed
            }

            if (index == 1) {
                statusView.text = getString(R.string.prayer_status_info)
                applyStatusBadge(statusView, R.color.status_upcoming_bg, R.color.status_upcoming_text)
                applyCardBackground(cardView, prayerCardBackgrounds[index], highlighted = index == activeIndex)
                return@forEachIndexed
            }

            val hasTimePassed = buildPrayerCalendar(prayer.time, now, false).timeInMillis <= now.timeInMillis
            val completionState = PrayerCompletionStore.getState(requireContext(), reminderRequestCodes[index])

            when {
                completionState == PrayerCompletionState.DONE -> {
                    statusView.text = getString(R.string.prayer_status_done)
                    applyStatusBadge(statusView, R.color.status_done_bg, R.color.status_done_text)
                    applyCardBackground(cardView, prayerCardBackgrounds[index], highlighted = index == activeIndex)
                }

                hasTimePassed -> {
                    statusView.text = getString(R.string.prayer_status_pending)
                    applyStatusBadge(statusView, R.color.status_pending_bg, R.color.status_pending_text)
                    applyCardBackground(cardView, prayerCardBackgrounds[index], highlighted = index == activeIndex)
                }

                else -> {
                    statusView.text = getString(R.string.prayer_status_upcoming)
                    applyStatusBadge(statusView, R.color.status_upcoming_bg, R.color.status_upcoming_text)
                    applyCardBackground(cardView, prayerCardBackgrounds[index], highlighted = index == activeIndex)
                }
            }
        }
    }

    private fun findHighlightedPrayerIndex(prayerTimes: CityPrayerTimes, now: Calendar): Int {
        prayerTimes.times.forEachIndexed { index, prayer ->
            if (index == 1) return@forEachIndexed
            val completionState = PrayerCompletionStore.getState(requireContext(), reminderRequestCodes[index])
            val hasTimePassed = buildPrayerCalendar(prayer.time, now, false).timeInMillis <= now.timeInMillis
            if (hasTimePassed && completionState != PrayerCompletionState.DONE) {
                return index
            }
        }
        prayerTimes.times.forEachIndexed { index, prayer ->
            if (buildPrayerCalendar(prayer.time, now, false).timeInMillis > now.timeInMillis) {
                return index
            }
        }
        return prayerTimes.times.lastIndex
    }

    private fun applyStatusBadge(view: TextView, backgroundColorRes: Int, textColorRes: Int) {
        val background = GradientDrawable().apply {
            cornerRadius = 18f
            setColor(resources.getColor(backgroundColorRes, null))
        }
        view.background = background
        view.setTextColor(resources.getColor(textColorRes, null))
    }

    private fun applyCardBackground(view: View, backgroundDrawableRes: Int, highlighted: Boolean) {
        view.background = AppCompatResources.getDrawable(requireContext(), backgroundDrawableRes)
        view.foreground = if (highlighted) {
            AppCompatResources.getDrawable(requireContext(), R.drawable.bg_prayer_highlight_overlay)
        } else {
            null
        }
        view.alpha = if (highlighted) 1f else 0.99f
    }

    private fun updateTimeline(now: Calendar, prayerTimes: CityPrayerTimes) {
        val highlightedIndex = if (isTodaySelected()) findHighlightedPrayerIndex(prayerTimes, now) else -1
        timelineDotViews.forEachIndexed { index, dot ->
            val color = when {
                !isTodaySelected() -> R.color.status_upcoming_text
                index == highlightedIndex -> R.color.active_card_stroke
                buildPrayerCalendar(prayerTimes.times.getOrNull(index)?.time ?: "00:00", now, false).timeInMillis <= now.timeInMillis ->
                    R.color.status_done_text
                else -> R.color.nav_unselected
            }
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(resources.getColor(color, null))
            }
            timelineLabelViews.getOrNull(index)?.setTextColor(
                resources.getColor(
                    if (index == highlightedIndex) R.color.active_card_stroke else R.color.text_secondary,
                    null,
                ),
            )
        }
    }

    private fun markPrayerDoneFromCard(index: Int) {
        if (!isTodaySelected()) {
            Toast.makeText(requireContext(), getString(R.string.next_prayer_selected_day, formatGregorianDate(selectedDate)), Toast.LENGTH_SHORT).show()
            return
        }
        val prayerTimes = displayedPrayerTimes ?: return
        if (index == 1) {
            Toast.makeText(requireContext(), getString(R.string.prayer_status_info), Toast.LENGTH_SHORT).show()
            return
        }

        val prayer = prayerTimes.times.getOrNull(index) ?: return
        val now = Calendar.getInstance()
        val prayerMoment = buildPrayerCalendar(prayer.time, now, false)
        if (prayerMoment.timeInMillis > now.timeInMillis) {
            Toast.makeText(requireContext(), getString(R.string.prayer_status_not_reached), Toast.LENGTH_SHORT).show()
            return
        }

        PrayerCompletionStore.markDone(requireContext(), reminderRequestCodes[index])
        PrayerHistoryStore.addEntry(requireContext(), prayer.name)
        Toast.makeText(requireContext(), getString(R.string.prayer_done_message, prayer.name), Toast.LENGTH_SHORT).show()
        updateClockAndCountdown()
    }

    private fun changeSelectedDate(days: Int) {
        val candidate = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days) }
        if (candidate.before(currentMonthStart()) || candidate.after(currentMonthEnd())) {
            updateDateNavigationButtons()
            return
        }
        selectedDate.timeInMillis = candidate.timeInMillis
        updateSelectedDayLabel()
        updateDateNavigationButtons()
        selectedLocation?.let { refreshForSelectedDate(it) }
    }

    private fun resetToToday() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDate.timeInMillis = today.timeInMillis
        updateSelectedDayLabel()
        updateDateNavigationButtons()
        selectedLocation?.let { refreshForSelectedDate(it) }
    }

    private fun refreshForSelectedDate(location: AppLocation) {
        if (view == null) return
        val context = context ?: return
        if (refreshInFlight) return
        val refreshKey = buildString {
            append(location.city)
            append('|')
            append(location.country)
            append('|')
            append(selectedDate.get(Calendar.YEAR))
            append('-')
            append(selectedDate.get(Calendar.MONTH))
            append('-')
            append(selectedDate.get(Calendar.DAY_OF_MONTH))
        }
        val nowMs = System.currentTimeMillis()
        if (lastRefreshKey == refreshKey && nowMs - lastRefreshAtMs < PRAYER_REFRESH_THROTTLE_MS && hasUsablePrayerTimesNullable(displayedPrayerTimes)) {
            return
        }
        refreshInFlight = true
        renderedLoadingLabel?.visibility = View.VISIBLE
        thread {
            val result = PrayerTimesRepository.fetchByCityAndDate(context, location.city, location.country, selectedDate)
            activity?.runOnUiThread {
                refreshInFlight = false
                if (!isAdded) return@runOnUiThread
                renderedLoadingLabel?.visibility = View.GONE
                result.onSuccess { remote ->
                    val localizedNames = PrayerPreferences.localizedPrayerNames(requireContext())
                    val localizedTimes = remote.times.mapIndexed { index, item ->
                        item.copy(name = localizedNames.getOrElse(index) { item.name })
                    }
                    val updated = CityPrayerTimes(
                        city = if (remote.city.isBlank()) location.city else remote.city,
                        country = if (remote.country.isBlank()) location.country else remote.country,
                        times = localizedTimes,
                        imsakTime = remote.imsakTime,
                    )
                    bindPrayerCards(updated)
                    lastRefreshKey = refreshKey
                    lastRefreshAtMs = System.currentTimeMillis()
                    if (isTodaySelected()) {
                        PrayerPreferences.saveSelectedPrayerTimes(requireContext(), updated)
                        (activity as? MainActivity)?.scheduleReminderSet(updated)
                    }
                }.onFailure {
                    val cached = PrayerPreferences.loadSelectedPrayerTimes(requireContext())
                    if (isTodaySelected() && cached != null) {
                        bindPrayerCards(cached)
                    } else {
                        bindPrayerCards(
                            CityPrayerTimes(
                                city = location.city,
                                country = location.country,
                                times = PrayerPreferences.localizedPrayerNames(requireContext()).map {
                                    com.muslimtime.app.data.PrayerTime(it, "--:--")
                                },
                                imsakTime = "--:--",
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun hasUsablePrayerTimes(city: CityPrayerTimes): Boolean {
        return city.times.any { it.time != "--:--" }
    }

    private fun hasUsablePrayerTimesNullable(city: CityPrayerTimes?): Boolean {
        return city?.times?.any { it.time != "--:--" } == true
    }

    private fun updateSelectedDayLabel() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((selectedDate.timeInMillis - today.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
        selectedDayLabel?.text = when (diffDays) {
            0 -> getString(R.string.prayer_selected_today)
            -1 -> getString(R.string.prayer_selected_yesterday)
            1 -> getString(R.string.prayer_selected_tomorrow)
            else -> formatGregorianDate(selectedDate)
        }
    }

    private fun updateDateNavigationButtons() {
        val canGoPrev = !selectedDate.before(currentMonthStart()) && selectedDate.after(currentMonthStart())
        val canGoNext = !selectedDate.after(currentMonthEnd()) && selectedDate.before(currentMonthEnd())
        prevDayButton?.isEnabled = canGoPrev
        prevDayButton?.alpha = if (canGoPrev) 1f else 0.35f
        nextDayButton?.isEnabled = canGoNext
        nextDayButton?.alpha = if (canGoNext) 1f else 0.35f
    }

    private fun currentMonthStart(): Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun currentMonthEnd(): Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun isTodaySelected(): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return selectedDate.timeInMillis == today.timeInMillis
    }
}

class QuranFragment : Fragment(R.layout.fragment_quran) {
    private var currentPlayingUrl: String? = null
    private var currentPlaying: Boolean = false
    private var currentAudioAyahs: List<AudioAyahItem> = emptyList()
    private var activeAudioIndex: Int? = null
    private var rememberedAudioAyahKey: String? = null
    private var rememberedAudioSuraTitle: String? = null
    private var currentAudioPositionMs: Int = 0
    private var currentAudioDurationMs: Int = 0
    private var renderAudioAyahList: ((Int?) -> Unit)? = null
    private var renderAudioModeList: (() -> Unit)? = null
    private val quranAudioStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != QuranAudioPlaybackService.ACTION_STATE_CHANGED) return
            currentPlayingUrl = intent.getStringExtra(QuranAudioPlaybackService.EXTRA_URL)
            currentPlaying = intent.getBooleanExtra(QuranAudioPlaybackService.EXTRA_IS_PLAYING, false)
            currentAudioPositionMs = intent.getIntExtra(QuranAudioPlaybackService.EXTRA_POSITION_MS, 0)
            currentAudioDurationMs = intent.getIntExtra(QuranAudioPlaybackService.EXTRA_DURATION_MS, 0)
            val active = intent.getBooleanExtra(QuranAudioPlaybackService.EXTRA_IS_ACTIVE, false)
            if (!active) {
                activeAudioIndex = null
                currentAudioPositionMs = 0
                currentAudioDurationMs = 0
            } else {
                activeAudioIndex = currentAudioAyahs.indexOfFirst { it.audioUrl == currentPlayingUrl }
                    .takeIf { it >= 0 }
            }
            if (isAdded) {
                renderAudioAyahList?.invoke(activeAudioIndex)
                renderAudioModeList?.invoke()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applySharedUiPreferences(view, requireContext())
        val readButton = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_read_quran)
        val audioButton = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_audio_quran)
        val allahNamesButton = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_allah_names)
        val modeTitle = view.findViewById<TextView>(R.id.quran_mode_title)
        val modeHint = view.findViewById<TextView>(R.id.quran_mode_hint)
        val offlineControls = view.findViewById<View>(R.id.quran_audio_offline_controls)
        val filterAllButton = view.findViewById<Button>(R.id.quran_audio_filter_all)
        val filterDownloadedButton = view.findViewById<Button>(R.id.quran_audio_filter_downloaded)
        val downloadProgressContainer = view.findViewById<View>(R.id.quran_download_progress_container)
        val downloadProgressTitle = view.findViewById<TextView>(R.id.quran_download_progress_title)
        val downloadProgressBar = view.findViewById<ProgressBar>(R.id.quran_download_progress_bar)
        val downloadProgressLabelView = view.findViewById<TextView>(R.id.quran_download_progress_label)
        val backToSurasButton = view.findViewById<Button>(R.id.quran_back_to_suras)
        val readNavigation = view.findViewById<View>(R.id.quran_read_navigation)
        val previousSuraButton = view.findViewById<Button>(R.id.quran_prev_sura)
        val nextSuraButton = view.findViewById<Button>(R.id.quran_next_sura)
        val fontControls = view.findViewById<View>(R.id.quran_font_controls)
        val fontDecreaseButton = view.findViewById<Button>(R.id.quran_font_decrease)
        val fontIncreaseButton = view.findViewById<Button>(R.id.quran_font_increase)
        val audioControls = view.findViewById<View>(R.id.quran_audio_controls)
        val audioNowPlayingTitle = view.findViewById<TextView>(R.id.quran_audio_now_playing_title)
        val audioNowPlayingSubtitle = view.findViewById<TextView>(R.id.quran_audio_now_playing_subtitle)
        val audioProgress = view.findViewById<ProgressBar>(R.id.quran_audio_progress)
        val audioProgressLabel = view.findViewById<TextView>(R.id.quran_audio_progress_label)
        val audioPlayButton = view.findViewById<ImageButton>(R.id.quran_audio_play)
        val audioPauseButton = view.findViewById<ImageButton>(R.id.quran_audio_pause)
        val audioStopButton = view.findViewById<ImageButton>(R.id.quran_audio_stop)
        val contentList = view.findViewById<ListView>(R.id.quran_content_list)
        val scrollRoot = view.findViewById<ScrollView>(R.id.quran_scroll_root)
        val scrollToTopButton = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.quran_scroll_to_top)
        val contentCard = view.findViewById<View>(R.id.quran_content_card)
        val appFontScale = PrayerPreferences.appFontScale(requireContext())
        val suras = quranSuras()
        var currentAudioSuras: List<AudioSuraItem> = emptyList()
        var currentAudioSuraTitle: String? = null
        var currentReadSura: QuranSura? = null
        var currentReadVerses: List<QuranVerse> = emptyList()
        var currentReadFontSp = PrayerPreferences.getQuranReadFontSizeSp(requireContext())
        lateinit var playAudioAyah: (Int) -> Unit
        lateinit var renderAudioAyahs: (AudioSuraItem) -> Unit
        lateinit var renderReadVerses: () -> Unit
        var audioSurasLoading = false
        val audioDownloadProgressBySura = mutableMapOf<Int, Pair<Long, Long>>()
        var mobileDataApprovedSuraNumber: Int? = null
        var audioFilterMode = AudioFilterMode.ALL

        fun audioOfflineSummaryLabel(): String? {
            val totalBytes = QuranAudioOfflineRepository.totalDownloadedBytes(requireContext())
            if (totalBytes <= 0L) return null
            return getString(
                R.string.quran_audio_offline_summary,
                QuranAudioOfflineRepository.formatSize(requireContext(), totalBytes),
            )
        }

        fun filteredAudioSuras(source: List<AudioSuraItem>): List<AudioSuraItem> {
            if (audioFilterMode == AudioFilterMode.ALL) return source
            val downloaded = QuranAudioOfflineRepository.downloadedSuraNumbers(requireContext())
            return source.filter { downloaded.contains(it.suraNumber) }
        }

        fun setModeHeaderVisible(visible: Boolean) {
            val state = if (visible) View.VISIBLE else View.GONE
            modeTitle.visibility = state
            modeHint.visibility = state
        }

        fun updateAudioOfflineControls() {
            val downloadedCount = QuranAudioOfflineRepository.downloadedSuraNumbers(requireContext()).size
            val inAudioMode = modeTitle.text.toString() == getString(R.string.quran_audio_tab)
            offlineControls.visibility = if (inAudioMode) View.VISIBLE else View.GONE
            filterAllButton.backgroundTintList = null
            filterDownloadedButton.backgroundTintList = null
            filterAllButton.background = AppCompatResources.getDrawable(
                requireContext(),
                if (audioFilterMode == AudioFilterMode.ALL) R.drawable.bg_quran_filter_active else R.drawable.bg_quran_filter_inactive,
            )
            filterAllButton.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (audioFilterMode == AudioFilterMode.ALL) R.color.white else R.color.text_primary,
                ),
            )
            filterDownloadedButton.background = AppCompatResources.getDrawable(
                requireContext(),
                if (audioFilterMode == AudioFilterMode.DOWNLOADED) R.drawable.bg_quran_filter_active else R.drawable.bg_quran_filter_inactive,
            )
            filterDownloadedButton.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (audioFilterMode == AudioFilterMode.DOWNLOADED) R.color.white else R.color.text_primary,
                ),
            )
            val alpha = if (downloadedCount > 0 || audioFilterMode == AudioFilterMode.ALL) 1f else 0.55f
            filterDownloadedButton.alpha = alpha
        }

        fun applyOfflineAyahsIfAvailable(suraNumber: Int, ayahs: List<AudioAyahItem>): List<AudioAyahItem> {
            return QuranAudioOfflineRepository.resolvePlaybackAyahs(requireContext(), suraNumber, ayahs)
        }

        fun isWifiConnected(): Boolean {
            val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        fun confirmStreamingIfNeeded(suraNumber: Int, onContinue: () -> Unit) {
            val offlineStatus = QuranAudioOfflineRepository.status(requireContext(), suraNumber)
            if (offlineStatus.isDownloaded || isWifiConnected()) {
                mobileDataApprovedSuraNumber = suraNumber
                onContinue()
                return
            }
            if (mobileDataApprovedSuraNumber == suraNumber) {
                onContinue()
                return
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.quran_audio_mobile_data_warning_title)
                .setMessage(R.string.quran_audio_mobile_data_warning_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.quran_audio_mobile_data_continue) { _, _ ->
                    mobileDataApprovedSuraNumber = suraNumber
                    onContinue()
                }
                .show()
        }

        fun downloadProgressLabel(suraNumber: Int): String? {
            val (downloaded, total) = audioDownloadProgressBySura[suraNumber] ?: return null
            if (total <= 0L) return QuranAudioOfflineRepository.formatSize(requireContext(), downloaded)
            return getString(
                R.string.quran_audio_download_progress_template,
                QuranAudioOfflineRepository.formatSize(requireContext(), downloaded),
                QuranAudioOfflineRepository.formatSize(requireContext(), total),
            )
        }

        fun hideDownloadProgressUi() {
            downloadProgressContainer.visibility = View.GONE
            downloadProgressBar.isIndeterminate = false
            downloadProgressBar.progress = 0
            downloadProgressTitle.text = getString(R.string.quran_audio_download_in_progress)
            downloadProgressLabelView.text = ""
        }

        fun showDownloadProgressUi(sura: AudioSuraItem) {
            val (downloaded, total) = audioDownloadProgressBySura[sura.suraNumber] ?: (0L to 0L)
            downloadProgressContainer.visibility = View.VISIBLE
            downloadProgressTitle.text = getString(R.string.quran_audio_download_progress_title, sura.nameLatin)
            if (total > 0L) {
                val percent = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                downloadProgressBar.isIndeterminate = false
                downloadProgressBar.progress = percent
                downloadProgressLabelView.text = "${getString(R.string.quran_audio_download_progress_percent, percent)} • ${
                    getString(
                        R.string.quran_audio_download_progress_template,
                        QuranAudioOfflineRepository.formatSize(requireContext(), downloaded),
                        QuranAudioOfflineRepository.formatSize(requireContext(), total),
                    )
                }"
            } else {
                downloadProgressBar.isIndeterminate = true
                downloadProgressLabelView.text = QuranAudioOfflineRepository.formatSize(requireContext(), downloaded)
            }
        }

        fun updateDownloadHintForSura(sura: AudioSuraItem) {
            val progressLabel = downloadProgressLabel(sura.suraNumber) ?: return
            modeHint.text = "${getString(R.string.quran_audio_download_in_progress)} • ${sura.nameLatin} • $progressLabel"
            showDownloadProgressUi(sura)
        }

        fun refreshAudioSelectionAfterOfflineChange(suraNumber: Int, keepPlayingSelection: Boolean = true) {
            val isCurrentSura = currentAudioSuraTitle?.startsWith("$suraNumber.") == true
            if (!isCurrentSura || currentAudioAyahs.isEmpty()) return
            if (keepPlayingSelection && currentPlayingUrl != null && currentPlaying) return
            currentAudioAyahs = applyOfflineAyahsIfAvailable(suraNumber, currentAudioAyahs)
            activeAudioIndex = currentPlayingUrl?.let { currentUrl ->
                currentAudioAyahs.indexOfFirst { it.audioUrl == currentUrl }.takeIf { it >= 0 }
            } ?: activeAudioIndex
        }

        fun styleCard(button: com.google.android.material.card.MaterialCardView, selected: Boolean) {
            button.strokeWidth = if (selected) dpToPx(3) else dpToPx(1)
            button.strokeColor = ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.accent_primary else R.color.imsak_card_stroke,
            )
            button.alpha = if (selected) 1f else 0.96f
        }

        fun styleTabs(mode: QuranMode) {
            styleCard(readButton, mode == QuranMode.READ)
            styleCard(audioButton, mode == QuranMode.AUDIO)
            styleCard(allahNamesButton, mode == QuranMode.ALLAH_NAMES)
        }

        fun updateScrollToTopButton(scrollY: Int) {
            val shouldShow = scrollY > dpToPx(240)
            if (shouldShow && scrollToTopButton.visibility != View.VISIBLE) {
                scrollToTopButton.show()
            } else if (!shouldShow && scrollToTopButton.visibility == View.VISIBLE) {
                scrollToTopButton.hide()
            }
        }

        fun updateReadFontButtons() {
            fontDecreaseButton.isEnabled = currentReadFontSp > 18f
            fontIncreaseButton.isEnabled = currentReadFontSp < 34f
            fontDecreaseButton.alpha = if (fontDecreaseButton.isEnabled) 1f else 0.4f
            fontIncreaseButton.alpha = if (fontIncreaseButton.isEnabled) 1f else 0.4f
        }

        fun updateAudioControls() {
            val hasSelection = activeAudioIndex != null && currentAudioAyahs.isNotEmpty()
            val canStartFullPlayback = filteredAudioSuras(currentAudioSuras).isNotEmpty()
            audioPlayButton.isEnabled = hasSelection || canStartFullPlayback
            audioPauseButton.isEnabled = hasSelection && currentPlaying
            audioStopButton.isEnabled = hasSelection || currentPlayingUrl != null
            audioPlayButton.contentDescription = getString(
                if (audioFilterMode == AudioFilterMode.DOWNLOADED) {
                    R.string.quran_audio_play_button_downloaded
                } else {
                    R.string.quran_audio_play_button
                },
            )
            audioPlayButton.alpha = if (audioPlayButton.isEnabled) 1f else 0.4f
            audioPauseButton.alpha = if (audioPauseButton.isEnabled) 1f else 0.4f
            audioStopButton.alpha = if (audioStopButton.isEnabled) 1f else 0.4f
        }

        fun scrollToReadingContent() {
            scrollRoot.post {
                scrollRoot.smoothScrollTo(0, (contentCard.top - dpToPx(12)).coerceAtLeast(0))
            }
        }

        fun updateReadNavigation() {
            val sura = currentReadSura
            if (sura == null) {
                readNavigation.visibility = View.GONE
                return
            }
            val currentIndex = suras.indexOfFirst { it.number == sura.number }
            if (currentIndex == -1) {
                readNavigation.visibility = View.GONE
                return
            }
            readNavigation.visibility = View.VISIBLE
            previousSuraButton.isEnabled = currentIndex > 0
            nextSuraButton.isEnabled = currentIndex < suras.lastIndex
        }

        fun updateAudioPlayerCard() {
            val activeAyah = currentAudioAyahs.getOrNull(activeAudioIndex ?: -1)
            if (activeAyah == null) {
                audioNowPlayingTitle.text = getString(
                    if (audioFilterMode == AudioFilterMode.DOWNLOADED) {
                        R.string.quran_audio_now_playing_idle_downloaded
                    } else {
                        R.string.quran_audio_now_playing_idle
                    },
                )
                audioNowPlayingSubtitle.text = getString(
                    if (audioFilterMode == AudioFilterMode.DOWNLOADED) {
                        R.string.quran_audio_filter_downloaded
                    } else {
                        R.string.quran_audio_play_button
                    },
                )
                audioProgress.progress = 0
                audioProgressLabel.text = getString(R.string.quran_audio_progress_idle)
            } else {
                audioNowPlayingTitle.text = getString(
                    R.string.quran_audio_now_playing_label,
                    currentAudioSuraTitle ?: getString(R.string.quran_audio_tab),
                    activeAyah.ayahKey,
                )
                audioNowPlayingSubtitle.text = if (currentPlaying) {
                    getString(R.string.quran_audio_state_playing)
                } else {
                    getString(R.string.quran_audio_state_paused)
                }
                val duration = currentAudioDurationMs.coerceAtLeast(0)
                val position = currentAudioPositionMs.coerceAtLeast(0).coerceAtMost(if (duration > 0) duration else Int.MAX_VALUE)
                audioProgress.progress = if (duration > 0) ((position * 100) / duration).coerceIn(0, 100) else 0
                audioProgressLabel.text = getString(
                    R.string.quran_audio_progress_template,
                    formatTimeMs(position),
                    formatTimeMs(duration),
                )
            }
            updateAudioControls()
        }

        fun createReadVerseAdapter(verses: List<QuranVerse>): ArrayAdapter<String> {
            val inflater = LayoutInflater.from(requireContext())
            return object : ArrayAdapter<String>(requireContext(), R.layout.item_quran_verse, verses.map { it.title }) {
                override fun getCount(): Int = verses.size

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_quran_verse, parent, false)
                    val verse = verses[position]
                    itemView.findViewById<TextView>(R.id.verse_title).text = verse.title
                    itemView.findViewById<TextView>(R.id.verse_arabic).apply {
                        text = verse.arabic
                        setScaledTextSizeIgnoringSystemFont(this, currentReadFontSp + 4f, appFontScale)
                    }
                    itemView.findViewById<TextView>(R.id.verse_translation).apply {
                        text = verse.translation
                        setScaledTextSizeIgnoringSystemFont(this, currentReadFontSp, appFontScale)
                    }
                    return itemView
                }
            }
        }

        fun createSuraAdapter(items: List<Pair<Int, String>>, metaProvider: (Int) -> String): BaseAdapter {
            val inflater = LayoutInflater.from(requireContext())
            return object : BaseAdapter() {
                override fun getCount(): Int = items.size
                override fun getItem(position: Int): Any = items[position]
                override fun getItemId(position: Int): Long = items[position].first.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_quran_sura, parent, false)
                    val (number, title) = items[position]
                    itemView.findViewById<TextView>(R.id.sura_number).text = number.toString()
                    itemView.findViewById<TextView>(R.id.sura_name).text = title
                    itemView.findViewById<TextView>(R.id.sura_meta).text = metaProvider(position)
                    applyScaledTextTree(itemView, appFontScale)
                    itemView.findViewById<Button>(R.id.sura_action_button).visibility = View.GONE
                    return itemView
                }
            }
        }

        fun createReadSuraGridAdapter(items: List<QuranSura>, onOpen: (QuranSura) -> Unit): BaseAdapter {
            val inflater = LayoutInflater.from(requireContext())
            val rows = items.chunked(2)
            return object : BaseAdapter() {
                override fun getCount(): Int = rows.size
                override fun getItem(position: Int): Any = rows[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_quran_sura_grid_row, parent, false)
                    val rowItems = rows[position]
                    bindReadSuraCard(
                        root = itemView.findViewById(R.id.left_card_root),
                        numberView = itemView.findViewById(R.id.left_sura_number),
                        nameView = itemView.findViewById(R.id.left_sura_name),
                        sura = rowItems.getOrNull(0),
                        onOpen = onOpen,
                    )
                    bindReadSuraCard(
                        root = itemView.findViewById(R.id.right_card_root),
                        numberView = itemView.findViewById(R.id.right_sura_number),
                        nameView = itemView.findViewById(R.id.right_sura_name),
                        sura = rowItems.getOrNull(1),
                        onOpen = onOpen,
                    )
                    applyScaledTextTree(itemView, appFontScale)
                    return itemView
                }
            }
        }

        fun sendAudioServiceAction(action: String) {
            ContextCompat.startForegroundService(
                requireContext(),
                Intent(requireContext(), QuranAudioPlaybackService::class.java).apply {
                    this.action = action
                },
            )
        }

        fun loadAudioSurasIfNeeded(onReady: (() -> Unit)? = null) {
            if (currentAudioSuras.isNotEmpty()) {
                onReady?.invoke()
                return
            }
            if (audioSurasLoading) return
            audioSurasLoading = true
            thread {
                val result = QuranAudioBackendRepository.fetchSuras(requireContext())
                requireActivity().runOnUiThread {
                    audioSurasLoading = false
                    result.onSuccess { suras ->
                        currentAudioSuras = suras
                        if (styleForCurrentAudioTab(modeTitle.text.toString(), currentAudioSuraTitle)) {
                            modeHint.text = audioOfflineSummaryLabel() ?: if (suras.any { it.nameLatin.contains("(Demo)") }) {
                                getString(R.string.quran_audio_demo_mode)
                            } else {
                                getString(R.string.quran_audio_select_sura_hint)
                            }
                            renderAudioModeList?.invoke()
                            updateAudioPlayerCard()
                        }
                        onReady?.invoke()
                    }.onFailure {
                        if (styleForCurrentAudioTab(modeTitle.text.toString(), currentAudioSuraTitle)) {
                            modeHint.text = getString(R.string.quran_audio_backend_error)
                            contentList.adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                listOf(getString(R.string.quran_audio_backend_error)),
                            )
                            resizeListView(contentList)
                        }
                    }
                }
            }
        }

        fun createAudioAyahAdapter(activeIndex: Int?): BaseAdapter {
            val inflater = LayoutInflater.from(requireContext())
            return object : BaseAdapter() {
                override fun getCount(): Int = currentAudioAyahs.size
                override fun getItem(position: Int): Any = currentAudioAyahs[position]
                override fun getItemId(position: Int): Long = currentAudioAyahs[position].ayahNumber.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_quran_audio_ayah, parent, false)
                    val ayah = currentAudioAyahs[position]
                    val state = when {
                        activeIndex == position && currentPlaying -> getString(R.string.quran_audio_state_playing)
                        activeIndex == position -> getString(R.string.quran_audio_state_paused)
                        else -> ""
                    }
                    val stateView = itemView.findViewById<TextView>(R.id.audio_state)
                    val card = itemView.findViewById<View>(R.id.audio_card)
                    itemView.findViewById<TextView>(R.id.audio_ayah_key).text = ayah.ayahKey
                    itemView.findViewById<TextView>(R.id.audio_arabic).text = ayah.arabicText
                    itemView.findViewById<TextView>(R.id.audio_translation).text = ayah.translation
                    if (state.isBlank()) {
                        stateView.visibility = View.GONE
                    } else {
                        stateView.visibility = View.VISIBLE
                        stateView.text = state
                    }
                    card.background = AppCompatResources.getDrawable(
                        requireContext(),
                        if (activeIndex == position) R.drawable.bg_widget_action_pending else R.drawable.bg_widget_action,
                    )
                    applyScaledTextTree(itemView, appFontScale)
                    return itemView
                }
            }
        }

        fun createAudioSuraAdapter(suras: List<AudioSuraItem>): BaseAdapter {
            val inflater = LayoutInflater.from(requireContext())
            return object : BaseAdapter() {
                override fun getCount(): Int = suras.size
                override fun getItem(position: Int): Any = suras[position]
                override fun getItemId(position: Int): Long = suras[position].suraNumber.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_quran_sura, parent, false)
                    val sura = suras[position]
                    val numberView = itemView.findViewById<TextView>(R.id.sura_number)
                    val nameView = itemView.findViewById<TextView>(R.id.sura_name)
                    val metaView = itemView.findViewById<TextView>(R.id.sura_meta)
                    val actionButton = itemView.findViewById<Button>(R.id.sura_action_button)
                    val downloadButton = itemView.findViewById<Button>(R.id.sura_download_button)
                    val cardRoot = itemView.findViewById<View>(R.id.sura_card_root)

                    val isCurrentSura = currentAudioSuraTitle?.startsWith("${sura.suraNumber}.") == true && currentPlayingUrl != null
                    val offlineStatus = QuranAudioOfflineRepository.status(requireContext(), sura.suraNumber)
                    val isDownloading = QuranAudioOfflineRepository.isDownloading(sura.suraNumber)
                    val progressLabel = downloadProgressLabel(sura.suraNumber)
                    numberView.text = sura.suraNumber.toString()
                    nameView.text = sura.nameLatin
                    actionButton.visibility = View.VISIBLE
                    downloadButton.visibility = View.VISIBLE
                    val offlineMeta = when {
                        isDownloading -> progressLabel?.let {
                            "${getString(R.string.quran_audio_download_in_progress)} • $it"
                        } ?: getString(R.string.quran_audio_download_in_progress)
                        offlineStatus.isDownloaded -> "${getString(R.string.quran_audio_downloaded_label)} • ${
                            QuranAudioOfflineRepository.formatSize(requireContext(), offlineStatus.totalBytes)
                        }"
                        else -> sura.nameArabic.ifBlank { getString(R.string.quran_audio_stream_only) }
                    }
                    metaView.text = when {
                        isCurrentSura && currentPlaying -> "${getString(R.string.quran_audio_state_playing)}\n$offlineMeta"
                        isCurrentSura -> "${getString(R.string.quran_audio_state_paused)}\n$offlineMeta"
                        else -> offlineMeta
                    }
                    cardRoot.background = AppCompatResources.getDrawable(
                        requireContext(),
                        if (offlineStatus.isDownloaded) R.drawable.bg_quran_sura_card_downloaded else R.drawable.bg_quran_sura_card,
                    )

                    actionButton.text = if (isCurrentSura) {
                        getString(R.string.quran_audio_stop_button)
                    } else {
                        getString(R.string.quran_audio_listen_button)
                    }
                    actionButton.background = AppCompatResources.getDrawable(
                        requireContext(),
                        if (isCurrentSura) R.drawable.bg_audio_control_stop else R.drawable.bg_audio_control,
                    )
                    actionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    actionButton.setOnClickListener {
                        if (isCurrentSura) {
                            sendAudioServiceAction(QuranAudioPlaybackService.ACTION_STOP)
                            currentPlaying = false
                            currentPlayingUrl = null
                            activeAudioIndex = null
                            currentAudioPositionMs = 0
                            currentAudioDurationMs = 0
                            currentAudioSuraTitle = null
                            modeHint.text = getString(R.string.quran_audio_select_sura_hint)
                            updateAudioPlayerCard()
                            renderAudioModeList?.invoke()
                        } else {
                            confirmStreamingIfNeeded(sura.suraNumber) {
                                renderAudioAyahs(sura)
                            }
                        }
                    }

                    downloadButton.text = when {
                        isDownloading -> getString(R.string.quran_audio_download_in_progress)
                        offlineStatus.isDownloaded -> getString(R.string.quran_audio_delete_button)
                        else -> getString(R.string.quran_audio_download_button)
                    }
                    downloadButton.isEnabled = !isDownloading
                    downloadButton.backgroundTintList = null
                    downloadButton.background = AppCompatResources.getDrawable(
                        requireContext(),
                        if (offlineStatus.isDownloaded) R.drawable.bg_quran_delete_button else R.drawable.bg_quran_download_button,
                    )
                    downloadButton.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (offlineStatus.isDownloaded) R.color.white else R.color.white,
                        ),
                    )
                    downloadButton.setOnClickListener {
                        runCatching {
                            val currentlyDownloading = QuranAudioOfflineRepository.isDownloading(sura.suraNumber)
                            val currentOfflineStatus = QuranAudioOfflineRepository.status(requireContext(), sura.suraNumber)
                            val currentIsCurrentSura =
                                currentAudioSuraTitle?.startsWith("${sura.suraNumber}.") == true && currentPlayingUrl != null

                            if (currentlyDownloading) {
                                updateDownloadHintForSura(sura)
                                return@setOnClickListener
                            }

                            if (currentOfflineStatus.isDownloaded) {
                                ReminderDiagnosticsStore.record(
                                    requireContext().applicationContext,
                                    "quran_audio_delete_tapped",
                                    "sura=${sura.suraNumber}",
                                )
                                val deletingCurrentSura = currentAudioSuraTitle?.startsWith("${sura.suraNumber}.") == true
                                if (currentIsCurrentSura && currentPlayingUrl != null) {
                                    sendAudioServiceAction(QuranAudioPlaybackService.ACTION_STOP)
                                    currentPlaying = false
                                    currentPlayingUrl = null
                                    activeAudioIndex = null
                                    currentAudioPositionMs = 0
                                    currentAudioDurationMs = 0
                                }
                                QuranAudioOfflineRepository.deleteSura(requireContext(), sura.suraNumber)
                                modeHint.text = audioOfflineSummaryLabel() ?: getString(R.string.quran_audio_select_sura_hint)
                                hideDownloadProgressUi()
                                renderAudioModeList?.invoke()
                                updateAudioPlayerCard()
                                if (deletingCurrentSura) {
                                    val appContext = requireContext().applicationContext
                                    thread {
                                        val refreshed = QuranAudioBackendRepository.fetchAyahs(appContext, sura.suraNumber)
                                        activity?.runOnUiThread {
                                            if (!isAdded) return@runOnUiThread
                                            refreshed.onSuccess { remoteAyahs ->
                                                currentAudioAyahs = remoteAyahs
                                                activeAudioIndex = null
                                                renderAudioAyahList?.invoke(null)
                                            }
                                        }
                                    }
                                }
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.quran_audio_delete_done, sura.nameLatin),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@setOnClickListener
                            }

                            ReminderDiagnosticsStore.record(
                                requireContext().applicationContext,
                                "quran_audio_download_tapped",
                                "sura=${sura.suraNumber}",
                            )
                            modeHint.text = getString(R.string.quran_audio_download_started, sura.nameLatin)
                            audioDownloadProgressBySura[sura.suraNumber] = 0L to 0L
                            showDownloadProgressUi(sura)
                            updateDownloadHintForSura(sura)
                            val appContext = requireContext().applicationContext
                            thread {
                                val result = QuranAudioBackendRepository.fetchAyahs(appContext, sura.suraNumber).fold(
                                    onSuccess = { ayahs ->
                                        QuranAudioOfflineRepository.downloadSura(
                                                appContext,
                                                sura.suraNumber,
                                                ayahs,
                                            ) { downloadedBytes, totalBytes ->
                                                activity?.runOnUiThread {
                                                    if (!isAdded) return@runOnUiThread
                                                    audioDownloadProgressBySura[sura.suraNumber] = downloadedBytes to totalBytes
                                                    updateDownloadHintForSura(sura)
                                                }
                                            }
                                        },
                                        onFailure = { Result.failure(it) },
                                    )
                                activity?.runOnUiThread {
                                    if (!isAdded) return@runOnUiThread
                                    audioDownloadProgressBySura.remove(sura.suraNumber)
                                    result.onSuccess {
                                        ReminderDiagnosticsStore.record(
                                            requireContext().applicationContext,
                                            "quran_audio_download_success",
                                            "sura=${sura.suraNumber}",
                                        )
                                        refreshAudioSelectionAfterOfflineChange(sura.suraNumber)
                                        hideDownloadProgressUi()
                                        modeHint.text = audioOfflineSummaryLabel()
                                            ?: getString(R.string.quran_audio_download_done, sura.nameLatin)
                                        renderAudioModeList?.invoke()
                                        updateAudioPlayerCard()
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.quran_audio_download_done, sura.nameLatin),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }.onFailure { error ->
                                        ReminderDiagnosticsStore.record(
                                            requireContext().applicationContext,
                                            "quran_audio_download_failed",
                                            "sura=${sura.suraNumber} error=${error.message.orEmpty()}",
                                        )
                                        hideDownloadProgressUi()
                                        modeHint.text = getString(R.string.quran_audio_download_failed)
                                        renderAudioModeList?.invoke()
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.quran_audio_download_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        }.onFailure { error ->
                            ReminderDiagnosticsStore.record(
                                requireContext().applicationContext,
                                "quran_audio_download_click_crash",
                                "sura=${sura.suraNumber} error=${error.message.orEmpty()}",
                            )
                            hideDownloadProgressUi()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.quran_audio_download_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    itemView.setOnClickListener {
                        if (isCurrentSura) {
                            sendAudioServiceAction(QuranAudioPlaybackService.ACTION_STOP)
                            currentPlaying = false
                            currentPlayingUrl = null
                            activeAudioIndex = null
                            currentAudioPositionMs = 0
                            currentAudioDurationMs = 0
                            currentAudioSuraTitle = null
                            modeHint.text = getString(R.string.quran_audio_select_sura_hint)
                            updateAudioPlayerCard()
                            renderAudioModeList?.invoke()
                        } else {
                            confirmStreamingIfNeeded(sura.suraNumber) {
                                renderAudioAyahs(sura)
                            }
                        }
                    }

                    applyScaledTextTree(itemView, appFontScale)
                    return itemView
                }
            }
        }

        fun createAllahNamesAdapter(names: List<AllahName>): BaseAdapter {
            val inflater = LayoutInflater.from(requireContext())
            val rows = names.chunked(2)
            return object : BaseAdapter() {
                override fun getCount(): Int = rows.size
                override fun getItem(position: Int): Any = rows[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: inflater.inflate(R.layout.item_allah_name_grid_row, parent, false)
                    val rowItems = rows[position]
                    bindAllahNameCard(
                        root = itemView.findViewById(R.id.left_card_root),
                        numberView = itemView.findViewById(R.id.left_allah_number),
                        titleView = itemView.findViewById(R.id.left_allah_title),
                        transliterationView = itemView.findViewById(R.id.left_allah_transliteration),
                        arabicView = itemView.findViewById(R.id.left_allah_arabic),
                        name = rowItems.getOrNull(0),
                    )
                    bindAllahNameCard(
                        root = itemView.findViewById(R.id.right_card_root),
                        numberView = itemView.findViewById(R.id.right_allah_number),
                        titleView = itemView.findViewById(R.id.right_allah_title),
                        transliterationView = itemView.findViewById(R.id.right_allah_transliteration),
                        arabicView = itemView.findViewById(R.id.right_allah_arabic),
                        name = rowItems.getOrNull(1),
                    )
                    applyScaledTextTree(itemView, appFontScale)
                    return itemView
                }
            }
        }

        playAudioAyah = playAudioAyah@{ index ->
            val ayah = currentAudioAyahs.getOrNull(index) ?: return@playAudioAyah
            val suraNumber = ayah.ayahKey.substringBefore(":").toIntOrNull() ?: -1
            val offlineStatus = QuranAudioOfflineRepository.status(requireContext(), suraNumber)
            val isStreamingUrl = ayah.audioUrl.startsWith("http://") || ayah.audioUrl.startsWith("https://")
            if (isStreamingUrl && !offlineStatus.isDownloaded && !isWifiConnected() && mobileDataApprovedSuraNumber != suraNumber) {
                confirmStreamingIfNeeded(suraNumber) {
                    playAudioAyah(index)
                }
                return@playAudioAyah
            }
            activeAudioIndex = index
            if (currentPlayingUrl == ayah.audioUrl) {
                val action = if (currentPlaying) {
                    QuranAudioPlaybackService.ACTION_PAUSE
                } else {
                    QuranAudioPlaybackService.ACTION_RESUME
                }
                sendAudioServiceAction(action)
                currentPlaying = !currentPlaying
                updateAudioPlayerCard()
                return@playAudioAyah
            }

            modeHint.text = getString(R.string.quran_audio_buffering, ayah.ayahKey)
            ContextCompat.startForegroundService(
                requireContext(),
                Intent(requireContext(), QuranAudioPlaybackService::class.java).apply {
                    action = QuranAudioPlaybackService.ACTION_PLAY
                    putStringArrayListExtra(
                        QuranAudioPlaybackService.EXTRA_URLS,
                        ArrayList(currentAudioAyahs.map { it.audioUrl }),
                    )
                    putStringArrayListExtra(
                        QuranAudioPlaybackService.EXTRA_AYAH_KEYS,
                        ArrayList(currentAudioAyahs.map { it.ayahKey }),
                    )
                    putExtra(QuranAudioPlaybackService.EXTRA_INDEX, index)
                    putExtra(QuranAudioPlaybackService.EXTRA_SURA_TITLE, currentAudioSuraTitle)
                    putExtra(
                        QuranAudioPlaybackService.EXTRA_SURA_NUMBER,
                        ayah.ayahKey.substringBefore(":").toIntOrNull() ?: -1,
                    )
                },
            )
            currentPlayingUrl = ayah.audioUrl
            currentPlaying = true
            renderAudioAyahList?.invoke(index)
            updateAudioPlayerCard()
        }

        renderAudioAyahs = { sura ->
            mobileDataApprovedSuraNumber = null
            currentAudioSuraTitle = "${sura.suraNumber}. ${sura.nameLatin}"
            modeTitle.text = getString(R.string.quran_audio_tab)
            modeHint.text = getString(R.string.quran_audio_loading)
            setModeHeaderVisible(false)
            backToSurasButton.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.VISIBLE
            styleTabs(QuranMode.AUDIO)
            contentList.setOnItemClickListener(null)
            renderAudioModeList?.invoke()

            thread {
                val result = QuranAudioBackendRepository.fetchAyahs(requireContext(), sura.suraNumber)
                requireActivity().runOnUiThread {
                    result.onSuccess { ayahs ->
                        currentAudioAyahs = applyOfflineAyahsIfAvailable(sura.suraNumber, ayahs)
                        val startIndex = rememberedAudioAyahKey?.let { rememberedKey ->
                            currentAudioAyahs.indexOfFirst { it.ayahKey == rememberedKey }.takeIf { it >= 0 }
                        } ?: 0
                        activeAudioIndex = startIndex
                        val offlineStatus = QuranAudioOfflineRepository.status(requireContext(), sura.suraNumber)
                        if (offlineStatus.isDownloaded) {
                            modeHint.text = getString(
                                R.string.quran_audio_download_ready,
                                QuranAudioOfflineRepository.formatSize(requireContext(), offlineStatus.totalBytes),
                            )
                        } else if (sura.nameLatin.contains("(Demo)")) {
                            modeHint.text = getString(R.string.quran_audio_demo_mode)
                        } else {
                            modeHint.text = getString(R.string.quran_audio_select_sura_hint)
                        }
                        renderAudioModeList?.invoke()
                        if (currentAudioAyahs.isNotEmpty()) {
                            playAudioAyah(startIndex)
                        } else {
                            updateAudioPlayerCard()
                        }
                    }.onFailure {
                        modeHint.text = getString(R.string.quran_audio_backend_error)
                        renderAudioModeList?.invoke()
                    }
                }
            }
        }

        renderAudioAyahList = { activeIndex ->
            modeTitle.text = getString(R.string.quran_audio_tab)
            modeHint.text = getString(R.string.quran_audio_ready_hint)
            setModeHeaderVisible(false)
            backToSurasButton.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.VISIBLE
            styleTabs(QuranMode.AUDIO)
            renderAudioModeList?.invoke()
            updateAudioPlayerCard()
        }

        fun renderAudioMode() {
            mobileDataApprovedSuraNumber = null
            activeAudioIndex = null
            modeTitle.text = getString(R.string.quran_audio_tab)
            modeHint.text = audioOfflineSummaryLabel() ?: getString(R.string.quran_audio_loading)
            setModeHeaderVisible(false)
            if (audioDownloadProgressBySura.isEmpty()) hideDownloadProgressUi()
            backToSurasButton.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.VISIBLE
            styleTabs(QuranMode.AUDIO)
            contentList.setOnItemClickListener(null)
            renderAudioModeList?.invoke()
            updateAudioOfflineControls()
            loadAudioSurasIfNeeded()
        }

        renderAudioModeList = {
            if (currentAudioAyahs.isEmpty()) {
                modeHint.text = when {
                    audioFilterMode == AudioFilterMode.DOWNLOADED && filteredAudioSuras(currentAudioSuras).isEmpty() ->
                        getString(R.string.quran_audio_offline_manage_empty_hint)
                    else -> audioOfflineSummaryLabel() ?: getString(R.string.quran_audio_select_sura_hint)
                }
            }
            val visibleSuras = filteredAudioSuras(currentAudioSuras)
            if (currentAudioSuras.isEmpty()) {
                contentList.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    listOf(getString(R.string.quran_audio_loading)),
                )
            } else if (visibleSuras.isEmpty()) {
                contentList.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    listOf(getString(R.string.quran_audio_offline_manage_empty)),
                )
            } else {
                contentList.adapter = createAudioSuraAdapter(visibleSuras)
            }
            contentList.setOnItemClickListener(null)
            updateAudioOfflineControls()
            resizeListView(contentList)
        }

        fun renderAllahNames(names: List<AllahName>) {
            modeTitle.text = getString(R.string.quran_allah_names_tab)
            modeHint.text = getString(R.string.quran_allah_names_hint)
            setModeHeaderVisible(true)
            offlineControls.visibility = View.GONE
            hideDownloadProgressUi()
            backToSurasButton.visibility = View.GONE
            fontControls.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.GONE
            styleTabs(QuranMode.ALLAH_NAMES)
            contentList.adapter = createAllahNamesAdapter(names)
            contentList.setOnItemClickListener(null)
            resizeListView(contentList)
        }

        fun renderAllahNamesMode() {
            modeTitle.text = getString(R.string.quran_allah_names_tab)
            modeHint.text = getString(R.string.quran_allah_names_loading)
            setModeHeaderVisible(true)
            offlineControls.visibility = View.GONE
            hideDownloadProgressUi()
            backToSurasButton.visibility = View.GONE
            fontControls.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.GONE
            styleTabs(QuranMode.ALLAH_NAMES)
            contentList.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                listOf(getString(R.string.quran_allah_names_loading)),
            )
            contentList.setOnItemClickListener(null)
            resizeListView(contentList)

            thread {
                val result = AsmaUlHusnaRepository.fetchAll()
                requireActivity().runOnUiThread {
                    result.onSuccess(::renderAllahNames).onFailure {
                        modeHint.text = getString(R.string.quran_allah_names_error)
                        contentList.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            listOf(getString(R.string.quran_allah_names_error)),
                        )
                        resizeListView(contentList)
                    }
                }
            }
        }

        renderReadVerses = readVerseRenderer@{
            val sura = currentReadSura
            if (sura == null) {
                return@readVerseRenderer
            }
            modeTitle.text = "${sura.number}. ${sura.name}"
            modeHint.text = getString(R.string.quran_read_hint)
            setModeHeaderVisible(true)
            offlineControls.visibility = View.GONE
            hideDownloadProgressUi()
            backToSurasButton.visibility = View.VISIBLE
            fontControls.visibility = View.VISIBLE
            updateReadNavigation()
            updateReadFontButtons()
            styleTabs(QuranMode.READ)
            contentList.adapter = createReadVerseAdapter(currentReadVerses)
            contentList.setOnItemClickListener(null)
            resizeListView(contentList)
            scrollToReadingContent()
        }

        fun renderSuraDetail(sura: QuranSura) {
            currentReadSura = sura
            modeTitle.text = "${sura.number}. ${sura.name}"
            modeHint.text = getString(R.string.quran_sura_loading)
            setModeHeaderVisible(true)
            offlineControls.visibility = View.GONE
            hideDownloadProgressUi()
            backToSurasButton.visibility = View.VISIBLE
            fontControls.visibility = View.VISIBLE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.GONE
            updateReadFontButtons()
            styleTabs(QuranMode.READ)
            contentList.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                listOf(getString(R.string.quran_sura_loading)),
            )
            contentList.setOnItemClickListener(null)
            resizeListView(contentList)

            thread {
                val result = QuranEncRepository.fetchSura(requireContext(), QuranEncRepository.AZERBAIJANI_TRANSLATION_KEY, sura.number)
                requireActivity().runOnUiThread {
                    result.onSuccess { verses ->
                        currentReadVerses = verses
                        renderReadVerses()
                    }.onFailure {
                        modeHint.text = getString(R.string.quran_sura_error)
                        contentList.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            listOf(getString(R.string.quran_sura_error)),
                        )
                        resizeListView(contentList)
                    }
                }
            }
        }

        fun renderReadMode() {
            currentReadSura = null
            currentReadVerses = emptyList()
            modeTitle.text = getString(R.string.quran_read_card_title)
            modeHint.text = getString(R.string.quran_sura_list_hint)
            setModeHeaderVisible(true)
            offlineControls.visibility = View.GONE
            hideDownloadProgressUi()
            backToSurasButton.visibility = View.GONE
            fontControls.visibility = View.GONE
            readNavigation.visibility = View.GONE
            audioControls.visibility = View.GONE
            styleTabs(QuranMode.READ)
            contentList.adapter = createReadSuraGridAdapter(suras, ::renderSuraDetail)
            contentList.setOnItemClickListener(null)
            resizeListView(contentList)
        }

        fontDecreaseButton.setOnClickListener {
            currentReadFontSp = (currentReadFontSp - 2f).coerceAtLeast(18f)
            PrayerPreferences.setQuranReadFontSizeSp(requireContext(), currentReadFontSp)
            if (currentReadVerses.isNotEmpty()) {
                renderReadVerses()
            } else {
                updateReadFontButtons()
            }
        }

        fontIncreaseButton.setOnClickListener {
            currentReadFontSp = (currentReadFontSp + 2f).coerceAtMost(34f)
            PrayerPreferences.setQuranReadFontSizeSp(requireContext(), currentReadFontSp)
            if (currentReadVerses.isNotEmpty()) {
                renderReadVerses()
            } else {
                updateReadFontButtons()
            }
        }

        previousSuraButton.setOnClickListener {
            val current = currentReadSura ?: return@setOnClickListener
            val currentIndex = suras.indexOfFirst { it.number == current.number }
            if (currentIndex > 0) {
                renderSuraDetail(suras[currentIndex - 1])
            }
        }

        nextSuraButton.setOnClickListener {
            val current = currentReadSura ?: return@setOnClickListener
            val currentIndex = suras.indexOfFirst { it.number == current.number }
            if (currentIndex in 0 until suras.lastIndex) {
                renderSuraDetail(suras[currentIndex + 1])
            }
        }

        audioPlayButton.setOnClickListener {
            when {
                activeAudioIndex != null && currentPlayingUrl != null && !currentPlaying -> {
                    sendAudioServiceAction(QuranAudioPlaybackService.ACTION_RESUME)
                    currentPlaying = true
                    modeHint.text = getString(R.string.quran_audio_state_playing)
                    renderAudioAyahList?.invoke(activeAudioIndex)
                    renderAudioModeList?.invoke()
                    updateAudioPlayerCard()
                }
                activeAudioIndex == null && currentAudioSuras.isNotEmpty() -> {
                    val playableSuras = filteredAudioSuras(currentAudioSuras)
                    if (playableSuras.isEmpty()) return@setOnClickListener
                    modeHint.text = getString(R.string.quran_audio_state_playing)
                    val firstSura = playableSuras.first()
                    confirmStreamingIfNeeded(firstSura.suraNumber) {
                        renderAudioAyahs(firstSura)
                    }
                }
                activeAudioIndex != null && currentAudioAyahs.isNotEmpty() -> {
                    playAudioAyah(0)
                }
            }
        }

        audioPauseButton.setOnClickListener {
            if (activeAudioIndex != null && currentPlayingUrl != null && currentPlaying) {
                sendAudioServiceAction(QuranAudioPlaybackService.ACTION_PAUSE)
                currentPlaying = false
                renderAudioAyahList?.invoke(activeAudioIndex)
                renderAudioModeList?.invoke()
                updateAudioPlayerCard()
            }
        }

        audioStopButton.setOnClickListener {
            if (currentPlayingUrl == null) return@setOnClickListener
            sendAudioServiceAction(QuranAudioPlaybackService.ACTION_STOP)
            currentPlaying = false
            currentPlayingUrl = null
            activeAudioIndex = null
            currentAudioPositionMs = 0
            currentAudioDurationMs = 0
            currentAudioSuraTitle = null
            modeHint.text = getString(R.string.quran_audio_select_sura_hint)
            renderAudioAyahList?.invoke(null)
            renderAudioModeList?.invoke()
            updateAudioPlayerCard()
        }

        readButton.setOnClickListener { renderReadMode() }
        audioButton.setOnClickListener { renderAudioMode() }
        allahNamesButton.setOnClickListener { renderAllahNamesMode() }
        backToSurasButton.setOnClickListener {
            when {
                modeTitle.text.toString() == getString(R.string.quran_allah_names_tab) -> renderAllahNamesMode()
                else -> renderReadMode()
            }
        }
        filterAllButton.setOnClickListener {
            audioFilterMode = AudioFilterMode.ALL
            renderAudioModeList?.invoke()
            updateAudioPlayerCard()
        }
        filterDownloadedButton.setOnClickListener {
            audioFilterMode = AudioFilterMode.DOWNLOADED
            renderAudioModeList?.invoke()
            updateAudioPlayerCard()
        }
        scrollToTopButton.setOnClickListener {
            scrollRoot.post { scrollRoot.smoothScrollTo(0, 0) }
        }
        scrollRoot.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            updateScrollToTopButton(scrollY)
        }
        scrollRoot.post {
            updateScrollToTopButton(scrollRoot.scrollY)
        }

        val remembered = PrayerPreferences.loadLastQuranAudioPlayback(requireContext())
        currentPlayingUrl = remembered.first
        rememberedAudioAyahKey = remembered.second
        rememberedAudioSuraTitle = remembered.third
        currentAudioPositionMs = QuranAudioPlaybackService.currentPositionMs
        currentAudioDurationMs = QuranAudioPlaybackService.currentDurationMs
        loadAudioSurasIfNeeded()
        renderReadMode()
        updateAudioPlayerCard()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            quranAudioStateReceiver,
            IntentFilter(QuranAudioPlaybackService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val remembered = PrayerPreferences.loadLastQuranAudioPlayback(requireContext())
        currentPlayingUrl = QuranAudioPlaybackService.currentTrackUrl ?: remembered.first
        currentPlaying = QuranAudioPlaybackService.isPlayingNow
        rememberedAudioAyahKey = remembered.second
        rememberedAudioSuraTitle = remembered.third
        currentAudioPositionMs = QuranAudioPlaybackService.currentPositionMs
        currentAudioDurationMs = QuranAudioPlaybackService.currentDurationMs
        activeAudioIndex = currentAudioAyahs.indexOfFirst { it.audioUrl == currentPlayingUrl }
            .takeIf { it >= 0 }
        renderAudioAyahList?.invoke(activeAudioIndex)
        renderAudioModeList?.invoke()
    }

    override fun onDestroyView() {
        runCatching {
            requireContext().unregisterReceiver(quranAudioStateReceiver)
        }
        super.onDestroyView()
    }

    private fun dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun styleForCurrentAudioTab(currentTitle: String, currentAudioSuraTitle: String?): Boolean {
        return currentTitle == getString(R.string.quran_audio_tab) || currentTitle == currentAudioSuraTitle
    }

    private enum class QuranMode {
        READ,
        AUDIO,
        ALLAH_NAMES,
    }

    private enum class AudioFilterMode {
        ALL,
        DOWNLOADED,
    }

    private fun resizeListView(listView: ListView) {
        val adapter = listView.adapter ?: return
        var totalHeight = 0
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.width.coerceAtLeast(1), View.MeasureSpec.AT_MOST)
        for (index in 0 until adapter.count) {
            val item = adapter.getView(index, null, listView)
            item.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += item.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (adapter.count - 1)).coerceAtLeast(0) + listView.paddingTop + listView.paddingBottom
        listView.layoutParams = params
        listView.requestLayout()
    }

    private fun bindReadSuraCard(
        root: View,
        numberView: TextView,
        nameView: TextView,
        sura: QuranSura?,
        onOpen: (QuranSura) -> Unit,
    ) {
        if (sura == null) {
            root.visibility = View.INVISIBLE
            root.isClickable = false
            root.setOnClickListener(null)
            return
        }
        root.visibility = View.VISIBLE
        numberView.text = sura.number.toString()
        nameView.text = sura.name
        root.isClickable = true
        root.isFocusable = true
        root.setOnClickListener { onOpen(sura) }
    }

    private fun bindAllahNameCard(
        root: View,
        numberView: TextView,
        titleView: TextView,
        transliterationView: TextView,
        arabicView: TextView,
        name: AllahName?,
    ) {
        if (name == null) {
            root.visibility = View.INVISIBLE
            return
        }
        root.visibility = View.VISIBLE
        numberView.text = name.number.toString()
        titleView.text = allahNameAzerbaijaniTitle(name.number)
        transliterationView.text = name.transliteration
        arabicView.text = name.arabic
    }

    private fun formatTimeMs(value: Int): String {
        val totalSeconds = (value / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

class DuaFragment : Fragment(R.layout.fragment_dua), SensorEventListener {
    private enum class QiblaViewMode { COMPASS, MAP }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var qiblaArrow: View? = null
    private var qiblaRotatingLayer: View? = null
    private var qiblaCompassContainer: View? = null
    private var qiblaMapContainer: View? = null
    private var qiblaMapRouteLayer: View? = null
    private var qiblaCard: MaterialCardView? = null
    private var qiblaStatus: TextView? = null
    private var qiblaDegreeValue: TextView? = null
    private var qiblaAccuracyBadge: TextView? = null
    private var qiblaPreciseHint: TextView? = null
    private var qiblaMapHint: TextView? = null
    private var qiblaMapDistance: TextView? = null
    private var qiblaCalibrationBadge: TextView? = null
    private var qiblaAlignmentHalo: View? = null
    private var qiblaModeCompass: TextView? = null
    private var qiblaModeMap: TextView? = null
    private var currentQiblaBearing: Float? = null
    private var currentDeclination: Float = 0f
    private var accelerometerValues = FloatArray(3)
    private var magneticValues = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagneticReading = false
    private var smoothedAzimuth: Float? = null
    private var cancellationTokenSource: CancellationTokenSource? = null
    private var qiblaViewMode = QiblaViewMode.COMPASS
    private var isAligned = false

    private enum class QiblaAccuracyState {
        SEARCHING,
        CALIBRATION_NEEDED,
        SENSOR_WEAK,
        ALIGNED,
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applySharedUiPreferences(view, requireContext())
        qiblaArrow = view.findViewById(R.id.qibla_arrow)
        qiblaRotatingLayer = view.findViewById(R.id.qibla_rotating_layer)
        qiblaCompassContainer = view.findViewById(R.id.qibla_compass_container)
        qiblaMapContainer = view.findViewById(R.id.qibla_map_container)
        qiblaMapRouteLayer = view.findViewById(R.id.qibla_map_route_layer)
        qiblaCard = view.findViewById(R.id.qibla_card)
        qiblaStatus = view.findViewById(R.id.qibla_status)
        qiblaDegreeValue = view.findViewById(R.id.qibla_degree_value)
        qiblaAccuracyBadge = view.findViewById(R.id.qibla_accuracy_badge)
        qiblaPreciseHint = view.findViewById(R.id.qibla_precise_hint)
        qiblaMapHint = view.findViewById(R.id.qibla_map_hint)
        qiblaMapDistance = view.findViewById(R.id.qibla_map_distance)
        qiblaCalibrationBadge = view.findViewById(R.id.qibla_calibration_badge)
        qiblaAlignmentHalo = view.findViewById(R.id.qibla_alignment_halo)
        qiblaModeCompass = view.findViewById(R.id.qibla_mode_compass)
        qiblaModeMap = view.findViewById(R.id.qibla_mode_map)
        qiblaModeCompass?.setOnClickListener { setQiblaViewMode(QiblaViewMode.COMPASS) }
        qiblaModeMap?.setOnClickListener { setQiblaViewMode(QiblaViewMode.MAP) }
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        qiblaStatus?.text = getString(R.string.qibla_loading)
        qiblaDegreeValue?.text = getString(R.string.qibla_degree_placeholder)
        qiblaAccuracyBadge?.text = getString(R.string.qibla_status_searching)
        qiblaPreciseHint?.text = getString(R.string.qibla_precise_hint)
        setQiblaViewMode(QiblaViewMode.COMPASS)
    }

    override fun onResume() {
        super.onResume()
        val accel = accelerometer
        val magnet = magnetometer
        if (accel == null || magnet == null) {
            qiblaStatus?.text = getString(R.string.qibla_sensor_missing)
            return
        }
        sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this, magnet, SensorManager.SENSOR_DELAY_GAME)
        fetchPreciseLocation()
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        cancellationTokenSource?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val bearing = currentQiblaBearing ?: return
        val values = event?.values ?: return
        when (event.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = values.copyOf()
                hasAccelerometerReading = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues = values.copyOf()
                hasMagneticReading = true
            }
            else -> return
        }
        if (!hasAccelerometerReading || !hasMagneticReading) return
        val rotationMatrix = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues)) return
        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val magneticAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val trueAzimuth = normalizeDegrees(magneticAzimuth + currentDeclination)
        val stabilizedAzimuth = smoothAngle(smoothedAzimuth, trueAzimuth)
        smoothedAzimuth = stabilizedAzimuth
        val ringRotation = stabilizedAzimuth - bearing
        val difference = angularDifference(stabilizedAzimuth, bearing)
        val signedDelta = signedAngularDelta(stabilizedAzimuth, bearing)
        qiblaArrow?.rotation = 0f
        qiblaRotatingLayer?.rotation = ringRotation
        qiblaMapRouteLayer?.rotation = bearing
        qiblaDegreeValue?.text = getString(R.string.qibla_degree_value_template, bearing.roundToInt())
        qiblaStatus?.text =
            if (difference <= 5f) {
                getString(R.string.qibla_aligned_direction_template, bearing.roundToInt())
            } else {
                getString(
                    if (signedDelta >= 0f) R.string.qibla_direction_right_template else R.string.qibla_direction_left_template,
                    difference.roundToInt(),
                )
            }
        qiblaPreciseHint?.text =
            if (difference <= 5f) {
                getString(R.string.qibla_aligned_hint)
            } else {
                getString(R.string.qibla_precise_hint)
            }
        if (qiblaCalibrationBadge?.visibility == View.VISIBLE) {
            setQiblaAccuracyStatus(QiblaAccuracyState.CALIBRATION_NEEDED)
        } else if (difference <= 5f) {
            setQiblaAccuracyStatus(QiblaAccuracyState.ALIGNED)
        } else {
            setQiblaAccuracyStatus(QiblaAccuracyState.SEARCHING)
        }
        updateAlignedState(difference <= 5f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            qiblaPreciseHint?.text = getString(R.string.qibla_calibration_hint)
            qiblaCalibrationBadge?.text = getString(R.string.qibla_calibration_badge)
            qiblaCalibrationBadge?.visibility = View.VISIBLE
        } else if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy > SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            qiblaCalibrationBadge?.visibility = View.GONE
        }
    }

    private fun fetchPreciseLocation() {
        val context = requireContext()
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
       val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            qiblaStatus?.text = getString(R.string.qibla_permission_needed)
            setQiblaAccuracyStatus(QiblaAccuracyState.SENSOR_WEAK)
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource?.token)
            .addOnSuccessListener { location ->
                val resolved = location ?: resolveLastKnownLocation(context)
                if (resolved == null) {
                    qiblaStatus?.text = getString(R.string.qibla_location_needed)
                    qiblaPreciseHint?.text = getString(R.string.qibla_location_refresh_hint)
                    setQiblaAccuracyStatus(QiblaAccuracyState.SENSOR_WEAK)
                    return@addOnSuccessListener
                }
                applyQiblaLocation(resolved)
            }
            .addOnFailureListener {
                val fallback = resolveLastKnownLocation(context)
                if (fallback == null) {
                    qiblaStatus?.text = getString(R.string.qibla_location_needed)
                    qiblaPreciseHint?.text = getString(R.string.qibla_location_refresh_hint)
                    setQiblaAccuracyStatus(QiblaAccuracyState.SENSOR_WEAK)
                    return@addOnFailureListener
                }
                applyQiblaLocation(fallback)
            }
    }

    private fun applyQiblaLocation(location: Location) {
        currentDeclination = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination
        val bearing = calculateQiblaBearing(location.latitude, location.longitude)
        currentQiblaBearing = bearing
        qiblaMapRouteLayer?.rotation = bearing
        qiblaDegreeValue?.text = getString(R.string.qibla_degree_value_template, bearing.roundToInt())
        qiblaStatus?.text = getString(R.string.qibla_direction_initial_template, bearing.roundToInt())
        qiblaPreciseHint?.text = getString(R.string.qibla_location_accuracy_template, location.accuracy.roundToInt())
        qiblaMapDistance?.text = getString(R.string.qibla_distance_template, calculateKaabaDistanceKm(location))
        setQiblaAccuracyStatus(if (isAligned) QiblaAccuracyState.ALIGNED else QiblaAccuracyState.SEARCHING)
    }

    private fun setQiblaViewMode(mode: QiblaViewMode) {
        qiblaViewMode = mode
        val isCompass = mode == QiblaViewMode.COMPASS
        qiblaCompassContainer?.visibility = if (isCompass) View.VISIBLE else View.GONE
        qiblaMapContainer?.visibility = if (isCompass) View.GONE else View.VISIBLE
        qiblaMapHint?.visibility = if (isCompass) View.GONE else View.VISIBLE
        qiblaMapDistance?.visibility = if (isCompass) View.GONE else View.VISIBLE
        qiblaModeCompass?.background = AppCompatResources.getDrawable(
            requireContext(),
            if (isCompass) R.drawable.bg_qibla_mode_active else R.drawable.bg_qibla_mode_inactive,
        )
        qiblaModeMap?.background = AppCompatResources.getDrawable(
            requireContext(),
            if (isCompass) R.drawable.bg_qibla_mode_inactive else R.drawable.bg_qibla_mode_active,
        )
        qiblaModeCompass?.setTextColor(
            ContextCompat.getColor(requireContext(), if (isCompass) R.color.white else R.color.text_primary),
        )
        qiblaModeMap?.setTextColor(
            ContextCompat.getColor(requireContext(), if (isCompass) R.color.text_primary else R.color.white),
        )
    }

    private fun updateAlignedState(alignedNow: Boolean) {
        val context = context ?: return
        qiblaCard?.strokeWidth = if (alignedNow) 4 else 1
        qiblaCard?.setStrokeColor(
            ContextCompat.getColor(
                context,
                if (alignedNow) R.color.qibla_aligned_stroke else R.color.surface_soft_sand,
            ),
        )
        qiblaCard?.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                if (alignedNow) R.color.qibla_aligned_surface else R.color.surface_soft_green,
            ),
        )
        qiblaStatus?.setTextColor(
            ContextCompat.getColor(
                context,
                if (alignedNow) R.color.qibla_aligned_stroke else R.color.text_primary,
            ),
        )
        qiblaDegreeValue?.setTextColor(
            ContextCompat.getColor(
                context,
                if (alignedNow) R.color.qibla_aligned_stroke else R.color.text_primary,
            ),
        )
        qiblaArrow?.alpha = if (alignedNow) 1f else 0.92f
        qiblaArrow?.scaleX = if (alignedNow) 1.08f else 1f
        qiblaArrow?.scaleY = if (alignedNow) 1.08f else 1f
        qiblaAlignmentHalo?.animate()?.cancel()
        qiblaAlignmentHalo?.animate()
            ?.alpha(if (alignedNow) 1f else 0f)
            ?.scaleX(if (alignedNow) 1.08f else 0.94f)
            ?.scaleY(if (alignedNow) 1.08f else 0.94f)
            ?.setDuration(220)
            ?.start()
        if (alignedNow && !isAligned) {
            qiblaCard?.animate()?.cancel()
            qiblaCard?.animate()?.scaleX(1.02f)?.scaleY(1.02f)?.setDuration(180)?.withEndAction {
                qiblaCard?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(220)?.start()
            }?.start()
            performAlignmentHaptic()
        } else if (!alignedNow && isAligned) {
            qiblaCard?.animate()?.cancel()
            qiblaCard?.scaleX = 1f
            qiblaCard?.scaleY = 1f
        }
        isAligned = alignedNow
    }

    private fun setQiblaAccuracyStatus(state: QiblaAccuracyState) {
        val context = context ?: return
        val badge = qiblaAccuracyBadge ?: return
        when (state) {
            QiblaAccuracyState.SEARCHING -> {
                badge.text = getString(R.string.qibla_status_searching)
                badge.background = AppCompatResources.getDrawable(context, R.drawable.bg_qibla_status_neutral)
                badge.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            QiblaAccuracyState.CALIBRATION_NEEDED -> {
                badge.text = getString(R.string.qibla_status_calibration)
                badge.background = AppCompatResources.getDrawable(context, R.drawable.bg_qibla_status_warning)
                badge.setTextColor(ContextCompat.getColor(context, R.color.qibla_calibration_text))
            }
            QiblaAccuracyState.SENSOR_WEAK -> {
                badge.text = getString(R.string.qibla_status_sensor_weak)
                badge.background = AppCompatResources.getDrawable(context, R.drawable.bg_qibla_status_warning)
                badge.setTextColor(ContextCompat.getColor(context, R.color.qibla_calibration_text))
            }
            QiblaAccuracyState.ALIGNED -> {
                badge.text = getString(R.string.qibla_status_aligned)
                badge.background = AppCompatResources.getDrawable(context, R.drawable.bg_qibla_status_success)
                badge.setTextColor(ContextCompat.getColor(context, R.color.qibla_aligned_stroke))
            }
        }
    }

    private fun calculateKaabaDistanceKm(location: Location): Int {
        val result = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, 21.4225, 39.8262, result)
        return result[0].div(1000f).roundToInt()
    }

    private fun performAlignmentHaptic() {
        val context = context ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun resolveLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers.mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLon = Math.toRadians(39.8262)
        val userLat = Math.toRadians(latitude)
        val userLon = Math.toRadians(longitude)
        val deltaLon = kaabaLon - userLon
        val y = sin(deltaLon)
        val x = cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(deltaLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }

    private fun normalizeDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f

    private fun angularDifference(first: Float, second: Float): Float {
        val diff = kotlin.math.abs(first - second) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    private fun signedAngularDelta(current: Float, target: Float): Float {
        return (((target - current + 540f) % 360f) - 180f)
    }

    private fun smoothAngle(previous: Float?, current: Float): Float {
        if (previous == null) return current
        val delta = (((current - previous + 540f) % 360f) - 180f)
        return normalizeDegrees(previous + (delta * 0.18f))
    }
}

class TimerFragment : Fragment(R.layout.fragment_timer) {
    companion object {
        private const val AUTO_LOCATION_DEBOUNCE_MS = 5_000L
        private const val AUTO_LOCATION_CACHE_MS = 2 * 60 * 1000L
    }

    private val reminderSwitchIds = listOf(
        R.id.switch_fajr,
        R.id.switch_sunrise,
        R.id.switch_dhuhr,
        R.id.switch_asr,
        R.id.switch_maghrib,
        R.id.switch_isha,
    )
    private var locationSummaryView: TextView? = null
    private var locationModeBadgeView: TextView? = null
    private var prayerSourceSummaryView: TextView? = null
    private var soundSettingsSummaryView: TextView? = null
    private var profileSettingsSummaryView: TextView? = null
    private var profileQuickActionTextView: TextView? = null
    private var dialogLocationSummaryView: TextView? = null
    private var dialogLocationModeBadgeView: TextView? = null
    private var dialogCityInput: Spinner? = null
    private var dialogCountryInput: Spinner? = null
    private var dialogUseDetectedButton: Button? = null
    private var dialogAutoLocationSwitch: SwitchCompat? = null
    private var dialogAutoLocationHelpText: TextView? = null
    private var dialogLocationPrayerSourceSpinner: Spinner? = null
    private var dialogLocationPrayerSourceHelpText: TextView? = null
    private var dialogCountryOptions: List<ManualCountryOption> = emptyList()
    private var dialogCityOptions: List<ManualCityOption> = emptyList()
    private var suppressLocationSpinnerCallbacks = false
    private var locationDialog: AlertDialog? = null
    private var prayerSourceDialog: AlertDialog? = null
    private var profileDialog: AlertDialog? = null
    private var setupChecklistDialog: AlertDialog? = null
    private var dialogPrayerSourceSpinner: Spinner? = null
    private var dialogPrayerSourceHelpText: TextView? = null
    private var dialogPrayerSourceSummaryView: TextView? = null
    private var soundDialog: AlertDialog? = null
    private var appearanceDialog: AlertDialog? = null
    private var appearanceSettingsSummaryView: TextView? = null
    private val customAzanPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val context = context ?: return@registerForActivityResult
        if (uri == null) {
            Toast.makeText(context, getString(R.string.azan_sound_add_failed), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val label = resolveAudioDisplayName(uri).ifBlank { uri.lastPathSegment ?: "audio" }
            PrayerPreferences.setCustomAzan(context, uri.toString(), label)
            val options = azanSoundOptions(context)
            updateSoundSettingsSummary(PrayerPreferences.CUSTOM_AZAN_RAW_NAME, options)
            soundDialog?.dismiss()
            soundDialog = null
            showSoundSettingsDialog()
            Toast.makeText(context, getString(R.string.azan_sound_added), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, getString(R.string.azan_sound_add_failed), Toast.LENGTH_SHORT).show()
        }
    }
    private val signalTonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val context = context ?: return@registerForActivityResult
        if (result.resultCode != AppCompatActivity.RESULT_OK) return@registerForActivityResult
        val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        val label = pickedUri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
            ?: getString(R.string.signal_tone_default)
        persistSelectedTone(context, pickedUri, label)
        updateSoundSettingsSummary(PrayerPreferences.getSelectedAzanRawName(context), azanSoundOptions(context))
        soundDialog?.dismiss()
        soundDialog = null
        showSoundSettingsDialog()
        Toast.makeText(context, getString(R.string.signal_tone_selected, label), Toast.LENGTH_SHORT).show()
    }
    private var suppressLocationDialogAutoSave = false
    private var suppressPrayerSourceDialogAutoSave = false
    private var suppressNotificationDialogAutoSave = false
    private var suppressAppearanceDialogAutoSave = false
    private val autoLocationFlowState = AutoLocationFlowState()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectAndApplyCurrentLocation(autoSave = true)
        } else {
            context?.let {
                Toast.makeText(it, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        runCatching {
        applySharedUiPreferences(view, requireContext())
        val text = view.findViewById<TextView>(R.id.timer_label)
        val infoCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.info_card)
        val profileQuickAction = view.findViewById<View>(R.id.profile_quick_action)
        val locationSettingsCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.location_settings_card)
        val prayerSourceSettingsCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.prayer_source_settings_card)
        val soundSettingsCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.sound_settings_card)
        val locationSummary = view.findViewById<TextView>(R.id.settings_location_summary)
        val profileQuickActionText = view.findViewById<TextView>(R.id.profile_quick_action_text)
        val locationModeBadge = view.findViewById<TextView>(R.id.settings_location_mode_badge)
        val prayerSourceSummary = view.findViewById<TextView>(R.id.prayer_source_summary)
        val soundSettingsSummary = view.findViewById<TextView>(R.id.sound_settings_summary)
        val notificationSettingsCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.notification_settings_card)
        val notificationSettingsSummary = view.findViewById<TextView>(R.id.notification_settings_summary)
        val setupBanner = view.findViewById<TextView>(R.id.setup_banner)
        val languages = supportedLanguages(requireContext())
        val azanSounds = azanSoundOptions(requireContext())
        val suggestedLocation = PrayerPreferences.suggestedLocation(requireContext())
        val selectedAzanRawName = PrayerPreferences.getSelectedAzanRawName(requireContext())
        text.text = getString(R.string.settings_intro)
        locationSummaryView = locationSummary
        locationModeBadgeView = locationModeBadge
        prayerSourceSummaryView = prayerSourceSummary
        soundSettingsSummaryView = soundSettingsSummary
        profileSettingsSummaryView = null
        profileQuickActionTextView = profileQuickActionText
        appearanceSettingsSummaryView = null
        val versionName = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull() ?: "1.0"
        setupBanner.visibility = View.GONE
        infoCard.setOnClickListener {
            showInfoDialog(versionName)
        }
        profileQuickAction.setOnClickListener {
            showProfileSettingsDialog()
        }
        updateSoundSettingsSummary(selectedAzanRawName, azanSounds)
        updateProfileSettingsSummary()

        val savedLocation = PrayerPreferences.loadSelectedLocation(requireContext()) ?: suggestedLocation
        updateLocationCardSummary(
            savedLocation.city,
            savedLocation.country,
            PrayerPreferences.getSelectedPrayerSource(requireContext()),
        )
        updatePrayerSourceSummary(PrayerPreferences.getSelectedPrayerSource(requireContext()))
        updateLocationModeBadge(PrayerPreferences.isAutoLocationEnabled(requireContext()))
        locationSettingsCard.setOnClickListener {
            showLocationSettingsDialog()
        }
        prayerSourceSettingsCard.visibility = View.GONE
        soundSettingsCard.setOnClickListener {
            showSoundSettingsDialog()
        }
        notificationSettingsCard.setOnClickListener {
            showNotificationSettingsDialog()
        }
        updateReminderSummaries()
        updateAppearanceSettingsSummary()
        finalizeSetupIfReady()
        }.onFailure { throwable ->
            Log.e("TimerFragment", "Settings init failed", throwable)
            view.findViewById<TextView>(R.id.timer_label)?.text = getString(R.string.settings_safe_mode_message)
            view.findViewById<TextView>(R.id.setup_banner)?.visibility = View.GONE
            Toast.makeText(requireContext(), getString(R.string.settings_safe_mode_message), Toast.LENGTH_LONG).show()
        }

    }

    override fun onDestroyView() {
        locationDialog?.dismiss()
        locationDialog = null
        prayerSourceDialog?.dismiss()
        prayerSourceDialog = null
        soundDialog?.dismiss()
        soundDialog = null
        profileDialog?.dismiss()
        profileDialog = null
        setupChecklistDialog?.dismiss()
        setupChecklistDialog = null
        appearanceDialog?.dismiss()
        appearanceDialog = null
        locationSummaryView = null
        locationModeBadgeView = null
        prayerSourceSummaryView = null
        soundSettingsSummaryView = null
        profileSettingsSummaryView = null
        profileQuickActionTextView = null
        appearanceSettingsSummaryView = null
        dialogLocationSummaryView = null
        dialogLocationModeBadgeView = null
        dialogCityInput = null
        dialogCountryInput = null
        dialogPrayerSourceSpinner = null
        dialogPrayerSourceHelpText = null
        dialogPrayerSourceSummaryView = null
        dialogUseDetectedButton = null
        dialogAutoLocationSwitch = null
        dialogAutoLocationHelpText = null
        dialogLocationPrayerSourceSpinner = null
        dialogLocationPrayerSourceHelpText = null
        super.onDestroyView()
    }

    private fun requestCurrentLocation() {
        val context = requireContext()
        val now = System.currentTimeMillis()
        if (!shouldStartAutoLocationRequest(autoLocationFlowState, now, AUTO_LOCATION_DEBOUNCE_MS)) return
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            detectAndApplyCurrentLocation(autoSave = true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun detectAndApplyCurrentLocation(autoSave: Boolean) {
        val context = context ?: return
        detectCurrentAppLocation(
            context = context,
            state = autoLocationFlowState,
            cacheMs = AUTO_LOCATION_CACHE_MS,
            fallbackLocation = { PrayerPreferences.suggestedLocation(context) },
            postToUi = { action ->
                activity?.runOnUiThread(action)
            },
            onResolved = { resolved ->
                if (!isAdded) return@detectCurrentAppLocation
                if (dialogAutoLocationSwitch?.isChecked == false) return@detectCurrentAppLocation
                applyDetectedLocation(resolved, autoSave)
            },
        )
    }

    private fun applyDetectedLocation(location: AppLocation, autoSave: Boolean) {
        if (!isAdded) return
        if (dialogAutoLocationSwitch?.isChecked == false) return
        val countryOption = ManualLocationCatalog.findCountry(location.country)
        if (countryOption != null) {
            selectCountryOption(countryOption, cityApiValue = location.city)
        }
        val selectedSource = PrayerPreferences.getSelectedPrayerSource(requireContext())
        updateLocationCardSummary(location.city, location.country, selectedSource)
        updateDialogLocationSummary(location.city, location.country, selectedSource)
        if (autoSave) {
            persistDetectedLocation(location)
        } else {
            updateReminderSummaries()
        }
    }

    private fun persistDetectedLocation(location: AppLocation) {
        fetchAndPersistLocation(location, isAutoDetected = true)
    }

    private fun showLocationSettingsDialog() {
        val context = requireContext()
        val suggestedLocation = PrayerPreferences.suggestedLocation(context)
        val savedLocation = PrayerPreferences.loadSelectedLocation(context) ?: suggestedLocation
        val prayerSources = prayerSourceOptions(context)
        val selectedPrayerSource = PrayerPreferences.getSelectedPrayerSource(context)
        val selectedPrayerSourceIndex = prayerSources.indexOfFirst { it.first == selectedPrayerSource }.takeIf { it >= 0 } ?: 0
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_settings, null)
        val bindings = LocationDialogSupport.collectBindings(dialogView)

        dialogLocationSummaryView = bindings.summaryView
        dialogLocationModeBadgeView = bindings.modeBadgeView
        dialogCityInput = bindings.citySpinner
        dialogCountryInput = bindings.countrySpinner
        dialogUseDetectedButton = bindings.useDetectedButton
        dialogAutoLocationSwitch = bindings.autoLocationSwitch
        dialogAutoLocationHelpText = bindings.autoLocationHelpText
        dialogLocationPrayerSourceSpinner = dialogView.findViewById(R.id.location_prayer_source_spinner)
        dialogLocationPrayerSourceHelpText = dialogView.findViewById(R.id.location_prayer_source_help_text)

        dialogCountryOptions = ManualLocationCatalog.countries()
        suppressLocationSpinnerCallbacks = true
        val isAutoLocation = PrayerPreferences.isAutoLocationEnabled(context)

        suppressLocationDialogAutoSave = true
        dialogLocationPrayerSourceSpinner?.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            prayerSources.map { it.second },
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dialogLocationPrayerSourceSpinner?.setSelection(selectedPrayerSourceIndex, false)
        updatePrayerSourceHelp(dialogLocationPrayerSourceHelpText, selectedPrayerSource)
        LocationDialogSupport.bindInitialState(
            context = context,
            bindings = bindings,
            savedLocation = savedLocation,
            isAutoLocation = isAutoLocation,
            countryOptions = dialogCountryOptions,
            bindCountrySpinner = { location -> bindCountrySpinner(location) },
            updateDialogLocationSummary = { city, country -> updateDialogLocationSummary(city, country, selectedPrayerSource) },
            updateLocationModeBadge = { enabled -> updateLocationModeBadge(enabled) },
            updateLocationDialogModeUi = { enabled -> updateLocationDialogModeUi(enabled) },
        )
        suppressLocationDialogAutoSave = false
        suppressLocationSpinnerCallbacks = false

        fun persistLocationSettings(manualLocationChanged: Boolean = false) {
            runCatching {
                if (suppressLocationDialogAutoSave || view == null) return
                val autoEnabled = dialogAutoLocationSwitch?.isChecked == true
                val chosenSource = prayerSources.getOrElse(
                    dialogLocationPrayerSourceSpinner?.selectedItemPosition ?: 0,
                ) { prayerSources.first() }.first
                val sourceChanged = chosenSource != PrayerPreferences.getSelectedPrayerSource(context)
                updateLocationModeBadge(autoEnabled)
                updatePrayerSourceHelp(dialogLocationPrayerSourceHelpText, chosenSource)
                if (sourceChanged) {
                    PrayerPreferences.setSelectedPrayerSource(context, chosenSource)
                    updatePrayerSourceSummary(chosenSource)
                }

                if (!autoEnabled) {
                    val selected = LocationDialogSupport.manualSelectionOrFallback(
                        countryOptions = dialogCountryOptions,
                        cityOptions = dialogCityOptions,
                        bindings = bindings,
                        fallbackLocation = suggestedLocation,
                    ) ?: return
                    val (country, city) = selected
                    val location = AppLocation(city.apiValue, country.apiValue)
                    persistLocationSelection(
                        context = context,
                        selection = LocationSettingsSelection(
                            autoEnabled = false,
                            manualLocation = location,
                        ),
                        currentPrayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context),
                        scheduleReminderSet = { times -> (activity as? MainActivity)?.scheduleReminderSet(times) ?: Unit },
                    )
                    updateLocationCardSummary(city.label, country.label, chosenSource)
                    updateDialogLocationSummary(city.label, country.label, chosenSource)
                    if (manualLocationChanged || sourceChanged) {
                        PrayerPreferences.clearSelectedPrayerTimes(context)
                        fetchAndPersistLocation(location, isAutoDetected = false)
                    }
                } else {
                    PrayerPreferences.clearSelectedPrayerTimes(context)
                    persistLocationSelection(
                        context = context,
                        selection = LocationSettingsSelection(
                            autoEnabled = true,
                            manualLocation = null,
                        ),
                        currentPrayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context),
                        scheduleReminderSet = { times -> (activity as? MainActivity)?.scheduleReminderSet(times) ?: Unit },
                    )
                    requestCurrentLocation()
                }
                updateReminderSummaries()
                finalizeSetupIfReady()
            }.onFailure { throwable ->
                Log.e("TimerFragment", "Location settings update failed", throwable)
                Toast.makeText(context, getString(R.string.settings_safe_mode_message), Toast.LENGTH_SHORT).show()
            }
        }

        bindings.useDetectedButton.setOnClickListener {
            PrayerPreferences.setAutoLocationEnabled(context, true)
            bindings.autoLocationSwitch.isChecked = true
            requestCurrentLocation()
        }
        bindings.autoLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLocationDialogModeUi(isChecked)
            updateLocationModeBadge(isChecked)
            if (isChecked) {
                persistLocationSelection(
                    context = context,
                    selection = LocationSettingsSelection(
                        autoEnabled = true,
                        manualLocation = null,
                    ),
                    currentPrayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context),
                    scheduleReminderSet = { times -> (activity as? MainActivity)?.scheduleReminderSet(times) ?: Unit },
                )
                requestCurrentLocation()
            } else {
                val selected = LocationDialogSupport.manualSelectionOrFallback(
                    countryOptions = dialogCountryOptions,
                    cityOptions = dialogCityOptions,
                    bindings = bindings,
                    fallbackLocation = suggestedLocation,
                )
                if (selected != null) {
                    val (country, city) = selected
                    val chosenSource = prayerSources.getOrElse(
                        dialogLocationPrayerSourceSpinner?.selectedItemPosition ?: 0,
                    ) { prayerSources.first() }.first
                    persistLocationSelection(
                        context = context,
                        selection = LocationSettingsSelection(
                            autoEnabled = false,
                            manualLocation = AppLocation(city.apiValue, country.apiValue),
                        ),
                        currentPrayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context),
                        scheduleReminderSet = { times -> (activity as? MainActivity)?.scheduleReminderSet(times) ?: Unit },
                    )
                    updateLocationCardSummary(city.label, country.label, chosenSource)
                    updateDialogLocationSummary(city.label, country.label, chosenSource)
                }
            }
            updateReminderSummaries()
            finalizeSetupIfReady()
        }

        dialogLocationPrayerSourceSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosenSource = prayerSources.getOrElse(position) { prayerSources.first() }.first
                updatePrayerSourceHelp(dialogLocationPrayerSourceHelpText, chosenSource)
                val preview = if (bindings.autoLocationSwitch.isChecked) {
                    PrayerPreferences.loadSelectedLocation(context) ?: suggestedLocation
                } else {
                    LocationDialogSupport.manualSelectionOrFallback(
                        countryOptions = dialogCountryOptions,
                        cityOptions = dialogCityOptions,
                        bindings = bindings,
                        fallbackLocation = suggestedLocation,
                    )?.let { (country, city) -> AppLocation(city.label, country.label) }
                        ?: (PrayerPreferences.loadSelectedLocation(context) ?: suggestedLocation)
                }
                updateDialogLocationSummary(preview.city, preview.country, chosenSource)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        LocationDialogSupport.attachManualSelectionListeners(
            bindings = bindings,
            countryOptions = dialogCountryOptions,
            cityOptionsProvider = { dialogCityOptions },
            suggestedLocation = suggestedLocation,
            isSuppressed = { suppressLocationSpinnerCallbacks },
            setSuppressed = { value -> suppressLocationSpinnerCallbacks = value },
            bindCitySpinner = { country, city -> bindCitySpinner(country, preselectedCity = city) },
            updateDialogLocationSummary = { city, country ->
                val chosenSource = prayerSources.getOrElse(
                    dialogLocationPrayerSourceSpinner?.selectedItemPosition ?: 0,
                ) { prayerSources.first() }.first
                updateDialogLocationSummary(city, country, chosenSource)
            },
        )

        locationDialog = DialogSupport.createOkDialog(
            context = context,
            titleRes = R.string.settings_location_section,
            view = dialogView,
            onPositive = {
                PrayerPreferences.setLocationSetupCompleted(context, true)
                persistLocationSettings(manualLocationChanged = !bindings.autoLocationSwitch.isChecked)
                finalizeSetupIfReady()
            },
            onDismiss = {
                LocationDialogSupport.clearFragmentDialogRefs {
                    dialogLocationSummaryView = null
                    dialogLocationModeBadgeView = null
                    dialogCityInput = null
                    dialogCountryInput = null
                    dialogUseDetectedButton = null
                    dialogAutoLocationSwitch = null
                    dialogCountryOptions = emptyList()
                    dialogCityOptions = emptyList()
                    dialogAutoLocationHelpText = null
                    dialogLocationPrayerSourceSpinner = null
                    dialogLocationPrayerSourceHelpText = null
                    suppressLocationSpinnerCallbacks = false
                    locationDialog = null
                }
            },
        )
        locationDialog?.show()
    }

    private fun showPrayerSourceSettingsDialog() {
        val context = requireContext()
        val prayerSources = prayerSourceOptions(context)
        val selectedPrayerSource = PrayerPreferences.getSelectedPrayerSource(context)
        val selectedPrayerSourceIndex = prayerSources.indexOfFirst { it.first == selectedPrayerSource }.takeIf { it >= 0 } ?: 0
        val dialogView = layoutInflater.inflate(R.layout.dialog_prayer_source_settings, null)

        dialogPrayerSourceSummaryView = dialogView.findViewById(R.id.prayer_source_dialog_summary)
        dialogPrayerSourceSpinner = dialogView.findViewById(R.id.prayer_source_spinner)
        dialogPrayerSourceHelpText = dialogView.findViewById(R.id.prayer_source_help_text)
        dialogPrayerSourceSpinner?.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            prayerSources.map { it.second },
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        suppressPrayerSourceDialogAutoSave = true
        dialogPrayerSourceSpinner?.setSelection(selectedPrayerSourceIndex, false)
        updatePrayerSourceSummary(selectedPrayerSource)
        dialogPrayerSourceSummaryView?.text = prayerSources.getOrElse(selectedPrayerSourceIndex) { prayerSources.first() }.second
        updatePrayerSourceHelp(dialogPrayerSourceHelpText, selectedPrayerSource)
        suppressPrayerSourceDialogAutoSave = false

        fun persistPrayerSourceSetting() {
            if (suppressPrayerSourceDialogAutoSave || view == null) return
            runCatching {
                val selectedSource = prayerSources.getOrElse(dialogPrayerSourceSpinner?.selectedItemPosition ?: 0) { prayerSources.first() }.first
                if (selectedSource == PrayerPreferences.getSelectedPrayerSource(context)) return
                PrayerPreferences.setSelectedPrayerSource(context, selectedSource)
                PrayerPreferences.clearSelectedPrayerTimes(context)
                updatePrayerSourceSummary(selectedSource)
                dialogPrayerSourceSummaryView?.text = prayerSources.first { it.first == selectedSource }.second
                updatePrayerSourceHelp(dialogPrayerSourceHelpText, selectedSource)

                val location = PrayerPreferences.loadSelectedLocation(context) ?: PrayerPreferences.suggestedLocation(context)
                if (PrayerPreferences.isAutoLocationEnabled(context)) {
                    requestCurrentLocation()
                } else {
                    fetchAndPersistLocation(location, isAutoDetected = false)
                }
                updateReminderSummaries()
                finalizeSetupIfReady()
            }.onFailure { throwable ->
                Log.e("TimerFragment", "Prayer source change failed", throwable)
                Toast.makeText(context, getString(R.string.settings_safe_mode_message), Toast.LENGTH_SHORT).show()
            }
        }

        dialogPrayerSourceSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressPrayerSourceDialogAutoSave) return
                persistPrayerSourceSetting()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        prayerSourceDialog = DialogSupport.createOkDialog(
            context = context,
            titleRes = R.string.settings_prayer_source_label,
            view = dialogView,
            onDismiss = {
                dialogPrayerSourceSpinner = null
                dialogPrayerSourceHelpText = null
                dialogPrayerSourceSummaryView = null
                prayerSourceDialog = null
            },
        )
        prayerSourceDialog?.show()
    }

    private fun showSoundSettingsDialog() {
        val context = requireContext()
        val azanSounds = azanSoundOptions(context)
        val selectedAzanRawName = PrayerPreferences.getSelectedAzanRawName(context)
        val selectedToneLabel = resolveSelectedToneLabel(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_sound_settings, null)
        val soundSummary = dialogView.findViewById<TextView>(R.id.sound_dialog_summary)
        val soundSelected = dialogView.findViewById<TextView>(R.id.sound_dialog_selected)
        val toneSelected = dialogView.findViewById<TextView>(R.id.sound_dialog_tone_selected)
        val azanSoundSpinner = dialogView.findViewById<Spinner>(R.id.azan_sound_spinner)
        val testAzanButton = dialogView.findViewById<ImageButton>(R.id.test_azan_button)
        val stopAzanButton = dialogView.findViewById<ImageButton>(R.id.stop_azan_button)
        val addCustomAzanButton = dialogView.findViewById<Button>(R.id.add_custom_azan_button)
        val chooseToneButton = dialogView.findViewById<Button>(R.id.choose_tone_button)
        val testToneButton = dialogView.findViewById<Button>(R.id.test_tone_button)
        val removeCustomAzanButton = dialogView.findViewById<Button>(R.id.remove_custom_azan_button)
        val selectedAzanIndex = selectedAzanIndex(selectedAzanRawName, azanSounds)
        bindLabelSpinner(context, azanSoundSpinner, azanSounds.map { it.label }, selectedAzanIndex)
        soundSummary.text = getString(R.string.settings_sound_help)
        soundSelected.text = getString(
            R.string.settings_sound_selected,
            azanSounds.getOrElse(selectedAzanIndex) { azanSounds.first() }.label,
        )
        toneSelected.text = getString(R.string.settings_tone_selected, selectedToneLabel)
        removeCustomAzanButton.visibility =
            if (PrayerPreferences.getCustomAzanUri(context).isNullOrBlank()) View.GONE else View.VISIBLE

        azanSoundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selectedView: View?, position: Int, id: Long) {
                runCatching {
                    val chosen = azanSounds.getOrElse(position) { azanSounds.first() }
                    if (chosen.rawName == PrayerPreferences.getSelectedAzanRawName(context)) return
                    persistSelectedAzan(context, chosen.rawName)
                    soundSelected.text = getString(R.string.settings_sound_selected, chosen.label)
                    updateSoundSettingsSummary(chosen.rawName, azanSounds)
                }.onFailure { throwable ->
                    Log.e("TimerFragment", "Sound selection failed", throwable)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        testAzanButton.setOnClickListener {
            val selectedRawName = azanSounds.getOrElse(azanSoundSpinner.selectedItemPosition) { azanSounds.first() }.rawName
            persistSelectedAzan(context, selectedRawName)
            updateSoundSettingsSummary(selectedRawName, azanSounds)
            stopToneTest()
            startAzanTest(context)
            Toast.makeText(context, getString(R.string.azan_test_started), Toast.LENGTH_SHORT).show()
        }
        stopAzanButton.setOnClickListener {
            stopAzanTest(context)
            stopToneTest()
        }
        testToneButton.setOnClickListener {
            stopAzanTest(context)
            startToneTest(context)
        }
        addCustomAzanButton.setOnClickListener {
            customAzanPickerLauncher.launch(arrayOf("audio/*"))
        }
        chooseToneButton.setOnClickListener {
            val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.signal_tone_picker_title))
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, resolveSelectedToneUri(context))
            }
            signalTonePickerLauncher.launch(pickerIntent)
        }
        removeCustomAzanButton.setOnClickListener {
            PrayerPreferences.clearCustomAzan(context)
            updateSoundSettingsSummary(
                PrayerPreferences.getSelectedAzanRawName(context),
                azanSoundOptions(context),
            )
            Toast.makeText(context, getString(R.string.azan_sound_removed), Toast.LENGTH_SHORT).show()
            soundDialog?.dismiss()
            soundDialog = null
            showSoundSettingsDialog()
        }

        soundDialog = DialogSupport.createOkDialog(
            context = context,
            titleRes = R.string.settings_sound_section,
            view = dialogView,
            onPositive = {
                PrayerPreferences.setSoundSetupCompleted(context, true)
                finalizeSetupIfReady()
            },
            onDismiss = {
                stopToneTest()
                soundDialog = null
            },
        )
        soundDialog?.show()
    }

    private fun showProfileSettingsDialog() {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_settings, null)
        val userNameInput = dialogView.findViewById<EditText>(R.id.settings_user_name_input)
        val genderSpinner = dialogView.findViewById<Spinner>(R.id.settings_user_gender_spinner)
        val languageButton = dialogView.findViewById<Button>(R.id.settings_user_language_button)
        val fontSpinner = dialogView.findViewById<Spinner>(R.id.settings_user_font_spinner)
        val themeSpinner = dialogView.findViewById<Spinner>(R.id.settings_user_theme_spinner)
        val elderSwitch = dialogView.findViewById<SwitchCompat>(R.id.settings_user_elder_switch)
        val birthDaySpinner = dialogView.findViewById<Spinner>(R.id.settings_birth_day_spinner)
        val birthMonthSpinner = dialogView.findViewById<Spinner>(R.id.settings_birth_month_spinner)
        val birthYearSpinner = dialogView.findViewById<Spinner>(R.id.settings_birth_year_spinner)
        userNameInput.setText(PrayerPreferences.getUserName(context))
        val languages = supportedLanguages(context)
        val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifBlank { Locale.getDefault().language }
            .split(",")
            .first()
            .substringBefore("-")
        val selectedLanguageIndex = languages.indexOfFirst { it.code == currentLanguage }.takeIf { it >= 0 } ?: 0
        languageButton.text = languages.getOrElse(selectedLanguageIndex) { languages.first() }.code.uppercase(Locale.getDefault())

        val genderOptions = genderOptions(context)
        bindLabelSpinner(
            context,
            genderSpinner,
            genderOptions.map { it.second },
            pairOptionIndex(genderOptions, PrayerPreferences.getUserGender(context)),
        )

        val dayOptions = birthDayOptions(context)
        val monthOptions = birthMonthOptions(context)
        val yearValues = birthYearValues()
        val yearOptions = birthYearOptions(context, yearValues)

        bindLabelSpinner(context, birthDaySpinner, dayOptions, PrayerPreferences.getBirthDay(context)?.coerceIn(1, 31) ?: 0)
        bindLabelSpinner(context, birthMonthSpinner, monthOptions, PrayerPreferences.getBirthMonth(context)?.coerceIn(1, 12) ?: 0)
        bindLabelSpinner(
            context,
            birthYearSpinner,
            yearOptions,
            PrayerPreferences.getBirthYear(context)?.let { yearValues.indexOf(it).takeIf { index -> index >= 0 }?.plus(1) } ?: 0,
        )
        val fontOptions = fontSizeOptions(context)
        val themeOptions = listOf(
            com.muslimtime.app.data.AppearancePreferences.THEME_SYSTEM to getString(R.string.settings_theme_system),
            com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT to getString(R.string.settings_theme_light),
            com.muslimtime.app.data.AppearancePreferences.THEME_DARK to getString(R.string.settings_theme_dark),
        )
        bindLabelSpinner(
            context,
            fontSpinner,
            fontOptions.map { it.second },
            pairOptionIndex(fontOptions, PrayerPreferences.getAppFontSize(context)),
        )
        bindLabelSpinner(
            context,
            themeSpinner,
            themeOptions.map { it.second },
            pairOptionIndex(themeOptions, PrayerPreferences.getThemeMode(context)),
        )
        elderSwitch.isChecked = PrayerPreferences.isElderModeEnabled(context)

        languageButton.setOnClickListener {
            val labels = languages.map { "${it.code.uppercase(Locale.getDefault())} - ${it.label}" }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.language_label))
                .setItems(labels) { _, which ->
                    val chosen = languages[which]
                    languageButton.text = chosen.code.uppercase(Locale.getDefault())
                    if (chosen.code != currentLanguage) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(chosen.code))
                    }
                }
                .show()
        }

        profileDialog = DialogSupport.createDialog(
            context = context,
            title = getString(R.string.settings_profile_section),
            view = dialogView,
            positiveText = getString(android.R.string.ok),
            onPositive = {
                runCatching {
                    persistProfileSettings(
                        context = context,
                        selection = ProfileSettingsSelection(
                            userName = userNameInput.text?.toString().orEmpty(),
                            gender = selectedPairValueOr(genderSpinner, genderOptions, genderOptions.first().first),
                            birthDay = birthDaySpinner.selectedItemPosition.takeIf { it > 0 },
                            birthMonth = birthMonthSpinner.selectedItemPosition.takeIf { it > 0 },
                            birthYear = birthYearSpinner.selectedItemPosition
                                .takeIf { it > 0 }
                                ?.let { yearValues.getOrNull(it - 1) },
                        ),
                    )
                    val appearanceSelection = AppearanceSettingsSelection(
                        fontSize = selectedPairValueOr(fontSpinner, fontOptions, fontOptions.first().first),
                        elderModeEnabled = elderSwitch.isChecked,
                        themeMode = selectedPairValueOr(themeSpinner, themeOptions, themeOptions.first().first),
                    )
                    val appearanceChanged = persistAppearanceSettings(context, appearanceSelection)
                    if (appearanceChanged) {
                        val nightMode = when (appearanceSelection.themeMode) {
                            com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                            com.muslimtime.app.data.AppearancePreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                        view?.let { root -> applySharedUiPreferences(root, context) }
                    }
                    updateProfileSettingsSummary()
                    updateAppearanceSettingsSummary()
                    finalizeSetupIfReady()
                }.onFailure { throwable ->
                    Log.e("TimerFragment", "Profile save failed", throwable)
                    Toast.makeText(context, getString(R.string.settings_safe_mode_message), Toast.LENGTH_SHORT).show()
                }
            },
            negativeText = getString(android.R.string.cancel),
            onDismiss = {
                profileDialog = null
            },
        )
        profileDialog?.show()
    }

    private fun showAppearanceSettingsDialog() {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_appearance_settings, null)
        val summaryView = dialogView.findViewById<TextView>(R.id.appearance_dialog_summary)
        val fontSpinner = dialogView.findViewById<Spinner>(R.id.app_font_size_spinner)
        val themeSpinner = dialogView.findViewById<Spinner>(R.id.app_theme_mode_spinner)
        val elderSwitch = dialogView.findViewById<SwitchCompat>(R.id.switch_elder_mode)

        val fontOptions = fontSizeOptions(context)
        val themeOptions = listOf(
            com.muslimtime.app.data.AppearancePreferences.THEME_SYSTEM to getString(R.string.settings_theme_system),
            com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT to getString(R.string.settings_theme_light),
            com.muslimtime.app.data.AppearancePreferences.THEME_DARK to getString(R.string.settings_theme_dark),
        )

        fun updateSummary() {
            val summary = appearanceDialogSummaryText(
                context = context,
                selection = AppearanceSettingsSelection(
                    fontSize = PrayerPreferences.getAppFontSize(context),
                    elderModeEnabled = PrayerPreferences.isElderModeEnabled(context),
                    themeMode = PrayerPreferences.getThemeMode(context),
                ),
                fontOptions = fontOptions,
                themeOptions = themeOptions,
            )
            summaryView.text = summary
            appearanceSettingsSummaryView?.text = summary
        }

        suppressAppearanceDialogAutoSave = true
        bindLabelSpinner(
            context,
            fontSpinner,
            fontOptions.map { it.second },
            pairOptionIndex(fontOptions, PrayerPreferences.getAppFontSize(context)),
        )
        bindLabelSpinner(
            context,
            themeSpinner,
            themeOptions.map { it.second },
            pairOptionIndex(themeOptions, PrayerPreferences.getThemeMode(context)),
        )
        elderSwitch.isChecked = PrayerPreferences.isElderModeEnabled(context)
        updateSummary()
        suppressAppearanceDialogAutoSave = false

        fun persistAppearance() {
            if (suppressAppearanceDialogAutoSave) return
            val selection = AppearanceSettingsSelection(
                fontSize = selectedPairValueOr(fontSpinner, fontOptions, fontOptions.first().first),
                elderModeEnabled = elderSwitch.isChecked,
                themeMode = selectedPairValueOr(themeSpinner, themeOptions, themeOptions.first().first),
            )
            val changed = persistAppearanceSettings(context, selection)
            if (!changed) {
                updateSummary()
                return
            }
            val nightMode = when (selection.themeMode) {
                com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                com.muslimtime.app.data.AppearancePreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            updateSummary()
            view?.let { root -> applySharedUiPreferences(root, context) }
        }

        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = persistAppearance()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = persistAppearance()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        elderSwitch.setOnCheckedChangeListener { _, _ -> persistAppearance() }

        appearanceDialog = DialogSupport.createOkDialog(
            context = context,
            titleRes = R.string.settings_appearance_section,
            view = dialogView,
            onDismiss = {
                suppressAppearanceDialogAutoSave = false
                appearanceDialog = null
            },
        )
        appearanceDialog?.show()
    }

    private fun updatePrayerSourceHelp(target: TextView?, source: String) {
        target?.text = getString(prayerSourceHelpRes(source))
    }

    private fun updateLocationCardSummary(city: String, country: String, source: String) {
        locationSummaryView?.text = locationSummaryText(requireContext(), city, country, source)
    }

    private fun updateDialogLocationSummary(city: String, country: String, source: String) {
        dialogLocationSummaryView?.text = locationSummaryText(requireContext(), city, country, source)
    }

    private fun updatePrayerSourceSummary(source: String) {
        prayerSourceSummaryView?.text = prayerSourceSummaryText(requireContext(), source)
    }

    private fun updateSoundSettingsSummary(selectedRawName: String, azanSounds: List<com.muslimtime.app.data.AppAzanSound>) {
        soundSettingsSummaryView?.text = soundSettingsSummaryText(requireContext(), selectedRawName, azanSounds)
    }

    private fun resolveAudioDisplayName(uri: Uri): String {
        val context = context ?: return ""
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex).orEmpty()
                } else {
                    ""
                }
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun updateProfileSettingsSummary() {
        val context = context ?: return
        profileSettingsSummaryView?.text = profileSettingsSummaryText(context)
        profileQuickActionTextView?.text = PrayerPreferences.personalizedAddress(context).ifBlank {
            getString(R.string.settings_profile_section)
        }
    }

    private fun updateAppearanceSettingsSummary() {
        val context = context ?: return
        appearanceSettingsSummaryView?.text = appearanceSettingsSummaryText(context)
    }

    private fun updateLocationModeBadge(isAuto: Boolean) {
        listOfNotNull(locationModeBadgeView, dialogLocationModeBadgeView).forEach { badge ->
            applyLocationModeBadge(requireContext(), badge, isAuto)
        }
    }

    private fun updateLocationDialogModeUi(isAuto: Boolean) {
        applyLocationDialogModeUi(
            context = requireContext(),
            citySpinner = dialogCityInput,
            countrySpinner = dialogCountryInput,
            useDetectedButton = dialogUseDetectedButton,
            helpText = dialogAutoLocationHelpText,
            isAuto = isAuto,
        )
    }

    private fun bindCountrySpinner(savedLocation: AppLocation) {
        val context = context ?: return
        bindCountrySpinner(context, dialogCountryInput, dialogCountryOptions)
        val country = ManualLocationCatalog.findCountry(savedLocation.country) ?: dialogCountryOptions.firstOrNull() ?: return
        selectCountryOption(country, cityApiValue = savedLocation.city)
    }

    private fun bindCitySpinner(country: ManualCountryOption, preselectedCity: ManualCityOption? = null) {
        val context = context ?: return
        dialogCityOptions = country.cities
        val selectedCity = bindCitySpinner(context, dialogCityInput, dialogCityOptions, preselectedCity)
        if (selectedCity != null) {
            val selectedSource = prayerSourceOptions(context).getOrElse(
                dialogLocationPrayerSourceSpinner?.selectedItemPosition ?: 0,
            ) { prayerSourceOptions(context).first() }.first
            updateLocationCardSummary(selectedCity.label, country.label, selectedSource)
            updateDialogLocationSummary(selectedCity.label, country.label, selectedSource)
        }
    }

    private fun selectCountryOption(country: ManualCountryOption, cityApiValue: String? = null) {
        suppressLocationSpinnerCallbacks = true
        val countryIndex = dialogCountryOptions.indexOfFirst { it.apiValue == country.apiValue }
        if (countryIndex >= 0) {
            dialogCountryInput?.setSelection(countryIndex, false)
        }
        val city = cityApiValue?.let { ManualLocationCatalog.findCity(country, it) } ?: country.cities.firstOrNull()
        bindCitySpinner(country, city)
        suppressLocationSpinnerCallbacks = false
    }

    private fun sourceLabelFor(country: String): String = resolvedPrayerSourceLabel(requireContext(), country)

    private fun fetchAndPersistLocation(location: AppLocation, isAutoDetected: Boolean) {
        val fragmentContext = context ?: return
        val selectedSource = PrayerPreferences.getSelectedPrayerSource(fragmentContext)
        updateLocationCardSummary(location.city, location.country, selectedSource)
        updateDialogLocationSummary(location.city, location.country, selectedSource)
        fetchAndPersistPrayerLocation(
            context = fragmentContext,
            location = location,
            isAutoDetected = isAutoDetected,
            postToUi = { action -> activity?.runOnUiThread(action) },
            scheduleReminderSet = { updated -> (activity as? MainActivity)?.scheduleReminderSet(updated) ?: Unit },
            onSuccess = { updated ->
                if (!isAdded) return@fetchAndPersistPrayerLocation
                updateReminderSummaries()
                finalizeSetupIfReady()
                if (isAutoDetected) {
                    Toast.makeText(fragmentContext, getString(R.string.location_detected_saved, updated.city, updated.country), Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                if (!isAdded) return@fetchAndPersistPrayerLocation
                updateReminderSummaries()
                finalizeSetupIfReady()
                if (isAutoDetected) {
                    Toast.makeText(fragmentContext, getString(R.string.location_detected_saved, location.city, location.country), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    fun refreshAutoLocationIfEnabled() {
        if (PrayerPreferences.isAutoLocationEnabled(requireContext())) {
            requestCurrentLocation()
        }
    }

    private fun finalizeSetupIfReady() {
        val root = view ?: return
        val context = requireContext()
        val setupBanner = root.findViewById<TextView>(R.id.setup_banner)
        PrayerPreferences.setInitialSetupComplete(context, true)
        setupBanner.visibility = View.GONE
        setupChecklistDialog?.dismiss()
        setupChecklistDialog = null
        (activity as? MainActivity)?.applySetupMode(false, navigateToPrayer = false)
    }

    private fun missingSetupItems(context: Context): List<String> {
        val items = mutableListOf<String>()
        if (!PrayerPreferences.isProfileConfigured(context) || !PrayerPreferences.isProfileSetupCompleted(context)) {
            items += getString(R.string.setup_missing_profile)
        }
        if (!PrayerPreferences.isNotificationSetupCompleted(context) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        ) {
            items += getString(R.string.setup_missing_notifications)
        }
        val locationReady = if (PrayerPreferences.isAutoLocationEnabled(context)) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            PrayerPreferences.loadSelectedLocation(context) != null
        }
        if (!PrayerPreferences.isLocationSetupCompleted(context) || !locationReady) {
            items += getString(R.string.setup_missing_location)
        }
        if (!PrayerPreferences.isSoundSetupCompleted(context)) {
            items += getString(R.string.setup_missing_sound)
        }
        return items
    }

    private fun showSetupChecklistDialog() {
        val context = context ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_initial_setup, null)
        val messageView = dialogView.findViewById<TextView>(R.id.setup_dialog_message)
        val progressView = dialogView.findViewById<TextView>(R.id.setup_dialog_progress)
        val profileButton = dialogView.findViewById<Button>(R.id.setup_profile_button)
        val notificationButton = dialogView.findViewById<Button>(R.id.setup_notification_button)
        val locationButton = dialogView.findViewById<Button>(R.id.setup_location_button)
        val soundButton = dialogView.findViewById<Button>(R.id.setup_sound_button)

        fun refreshProgress() {
            val missing = missingSetupItems(context)
            progressView.text = if (missing.isEmpty()) {
                getString(R.string.setup_dialog_progress_ready)
            } else {
                getString(R.string.setup_dialog_progress_missing, missing.joinToString(", "))
            }
        }

        messageView.text = getString(R.string.setup_dialog_message)
        profileButton.setOnClickListener {
            showProfileSettingsDialog()
            refreshProgress()
        }
        notificationButton.setOnClickListener {
            showNotificationSettingsDialog()
            refreshProgress()
        }
        locationButton.setOnClickListener {
            showLocationSettingsDialog()
            refreshProgress()
        }
        soundButton.setOnClickListener {
            showSoundSettingsDialog()
            refreshProgress()
        }
        refreshProgress()

        setupChecklistDialog?.dismiss()
        setupChecklistDialog = DialogSupport.createDialog(
            context = context,
            title = getString(R.string.setup_required_title),
            view = dialogView,
            positiveText = getString(R.string.setup_finish_button),
            onPositive = {
                if (!PrayerPreferences.isInitialSetupSatisfied(context)) {
                    Toast.makeText(context, getString(R.string.setup_finish_missing), Toast.LENGTH_SHORT).show()
                    view?.post { showSetupChecklistDialog() }
                } else {
                    finalizeSetupIfReady()
                }
            },
            cancelable = false,
            onDismiss = {
                setupChecklistDialog = null
            },
        )
        setupChecklistDialog?.show()
    }

    private fun updateReminderSummaries() {
        val root = view ?: return
        val context = requireContext()
        val notificationSummary = root.findViewById<TextView>(R.id.notification_settings_summary)
        val masterEnabled = PrayerPreferences.areRemindersGloballyEnabled(context)
        val enabledStates = reminderSwitchIds.mapIndexed { index, _ ->
            PrayerPreferences.isReminderEnabled(context, index)
        }
        notificationSummary.text = reminderSummaryText(context, masterEnabled, enabledStates)
    }

    private fun simpleSelectionListener(onSelected: () -> Unit): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun collectReminderStatusLines(
        context: Context,
        masterEnabled: Boolean,
        enabledStates: List<Boolean>,
    ): List<String> {
        val nextPrayerStatus = nextReminderStatusText(masterEnabled, enabledStates)
        return reminderStatusLines(context, masterEnabled, enabledStates, nextPrayerStatus)
    }

    private fun showReminderStatusDialog() {
        val context = context ?: return
        runCatching {
            val masterEnabled = PrayerPreferences.areRemindersGloballyEnabled(context)
            val enabledStates = reminderSwitchIds.mapIndexed { index, _ ->
                PrayerPreferences.isReminderEnabled(context, index)
            }
            val statusBlock = collectReminderStatusLines(context, masterEnabled, enabledStates)
                .joinToString("\n\n")
            val diagnosticsBlock = reminderDiagnosticsBlock(context)
            val telemetryBlock = reminderTelemetryBlock(context)
            val historyBlock = reminderHistoryBlock(context)
            val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else null
            val alarmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else null
            val dialogView = layoutInflater.inflate(R.layout.dialog_reminder_status, null)
            dialogView.findViewById<TextView>(R.id.status_dialog_content)?.text =
                statusBlock + "\n\n" + telemetryBlock + "\n\n" + diagnosticsBlock + "\n\n" + historyBlock
            dialogView.findViewById<Button>(R.id.status_open_notifications)?.setOnClickListener {
                runCatching { startActivity(notificationIntent) }
            }
            val secondaryButton = dialogView.findViewById<Button>(R.id.status_open_secondary)
            secondaryButton?.text = getString(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    R.string.reminder_status_open_alarms
                } else {
                    R.string.reminder_status_open_battery
                },
            )
            secondaryButton?.setOnClickListener {
                val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmIntent else batteryIntent
                target?.let { intent -> runCatching { startActivity(intent) } }
            }
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.reminder_status_title))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }.onFailure { throwable ->
            Log.e("TimerFragment", "Reminder status dialog failed", throwable)
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.reminder_status_title))
                .setMessage(getString(R.string.settings_safe_mode_message))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun showNotificationSettingsDialog() {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_notification_settings, null)
        val boundState = NotificationSettingsDialogSupport.bindState(
            context = context,
            view = dialogView,
            preReminderOptions = preReminderOptions(context),
            bindLabelSpinner = { spinner, labels, index -> bindLabelSpinner(context, spinner, labels, index) },
            pairOptionIndexInt = { options, value -> pairOptionIndex(options, value) },
        )

        suppressNotificationDialogAutoSave = true

        fun persistReminderPreferences() {
            if (suppressNotificationDialogAutoSave || view == null) return
            runCatching {
                val selection = NotificationSettingsDialogSupport.buildSelection(
                    context = context,
                    bindings = boundState.bindings,
                    options = boundState.options,
                    selectedIntValue = { spinner, options, fallback -> selectedPairValueOr(spinner, options, fallback) },
                )
                NotificationSettingsDialogSupport.persistSelection(
                    context = context,
                    selection = selection,
                    currentPrayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context),
                    scheduleReminderSet = { times -> (activity as? MainActivity)?.scheduleReminderSet(times) ?: Unit },
                )
                updateReminderSummaries()
                finalizeSetupIfReady()
            }.onFailure { throwable ->
                Log.e("TimerFragment", "Reminder settings save failed", throwable)
                Toast.makeText(context, getString(R.string.settings_safe_mode_message), Toast.LENGTH_SHORT).show()
            }
        }

        NotificationSettingsDialogSupport.attachAutoSaveListeners(
            bindings = boundState.bindings,
            persist = { persistReminderPreferences() },
            selectionListenerFactory = { callback -> simpleSelectionListener { callback() } },
        )
        suppressNotificationDialogAutoSave = false
        val showInternalTestButtons = com.muslimtime.app.BuildConfig.DEBUG
        boundState.bindings.testReminderButton.visibility = if (showInternalTestButtons) View.VISIBLE else View.GONE
        boundState.bindings.testPreReminderButton.visibility = if (showInternalTestButtons) View.VISIBLE else View.GONE
        if (showInternalTestButtons) {
            NotificationSettingsDialogSupport.attachTestActions(context, boundState.bindings, boundState.options)
        }
        DialogSupport.createOkDialog(
            context = context,
            titleRes = R.string.settings_reminder_section,
            view = dialogView,
            onPositive = {
                PrayerPreferences.setNotificationSetupCompleted(context, true)
                finalizeSetupIfReady()
            },
        ).show()
    }

    private fun showPrivacyDialog() {
        openExternalInfoPage(getString(R.string.privacy_policy_url))
    }

    private fun showLegacyDialog() {
        openExternalInfoPage(getString(R.string.legacy_url))
    }

    private fun openExternalInfoPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(requireContext(), url, Toast.LENGTH_LONG).show()
            }
    }

    private fun showInfoDialog(versionName: String) {
        val message = listOf(
            getString(R.string.about_text),
            getString(R.string.about_version, versionName),
            getString(R.string.about_founder),
            getString(R.string.about_feedback_hint),
        ).joinToString("\n\n")
        val dialogView = layoutInflater.inflate(R.layout.dialog_about_info, null)
        val messageView = dialogView.findViewById<TextView>(R.id.about_dialog_message)
        val feedbackButton = dialogView.findViewById<Button>(R.id.about_feedback_button)
        val advancedButton = dialogView.findViewById<Button>(R.id.about_advanced_button)
        val privacyButton = dialogView.findViewById<Button>(R.id.about_privacy_button)
        val legacyButton = dialogView.findViewById<Button>(R.id.about_legacy_button)
        messageView.text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.about_title))
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        feedbackButton.setOnClickListener {
            dialog.dismiss()
            showFeedbackDialog(versionName)
        }
        advancedButton.setOnClickListener {
            dialog.dismiss()
            showReminderStatusDialog()
        }
        privacyButton.setOnClickListener {
            dialog.dismiss()
            showPrivacyDialog()
        }
        legacyButton.setOnClickListener {
            dialog.dismiss()
            showLegacyDialog()
        }

        dialog.show()
    }

    private fun showFeedbackDialog(versionName: String) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val contactInput = dialogView.findViewById<EditText>(R.id.feedback_contact_input)
        val messageInput = dialogView.findViewById<EditText>(R.id.feedback_message_input)
        val userName = PrayerPreferences.getUserName(context)
        if (userName.isNotBlank()) {
            contactInput.setText(userName)
        }
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.feedback_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.feedback_send_button), null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                        val contact = contactInput.text?.toString().orEmpty().trim()
                        val message = messageInput.text?.toString().orEmpty().trim()
                        if (message.isBlank()) {
                            messageInput.error = getString(R.string.feedback_message_required)
                            return@setOnClickListener
                        }
                        submitFeedback(versionName, contact, message, dialog)
                    }
                }
            }
            .show()
    }

    private fun submitFeedback(
        versionName: String,
        contact: String,
        message: String,
        dialog: AlertDialog,
    ) {
        val context = requireContext()
        val baseUrl = BuildConfig.TELEMETRY_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            Toast.makeText(context, getString(R.string.feedback_send_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = java.util.TimeZone.getDefault() }
            .format(java.util.Date())
        thread {
            val result = runCatching {
                val connection = (URL("$baseUrl/feedback").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = """
                    {
                      "appId":"${jsonEscape(BuildConfig.APPLICATION_ID)}",
                      "versionName":"${jsonEscape(versionName)}",
                      "versionCode":"${BuildConfig.VERSION_CODE}",
                      "device":"${jsonEscape(device)}",
                      "android":"${jsonEscape(android.os.Build.VERSION.RELEASE ?: "")}",
                      "contact":"${jsonEscape(contact)}",
                      "message":"${jsonEscape(message)}",
                      "timestamp":"${jsonEscape(timestamp)}"
                    }
                """.trimIndent()
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                val responseBody = runCatching {
                    val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                connection.disconnect()
                if (responseCode !in 200..299) {
                    error("HTTP $responseCode ${responseBody.take(120)}".trim())
                }
            }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                result.onSuccess {
                    dialog.dismiss()
                    Toast.makeText(context, getString(R.string.feedback_send_success), Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, getString(R.string.feedback_send_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun jsonEscape(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private fun nextReminderStatusText(masterEnabled: Boolean, enabledStates: List<Boolean>): String {
        val context = requireContext()
        if (!masterEnabled || enabledStates.none { it }) {
            return getString(R.string.reminder_status_next_disabled)
        }

        val prayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context)
            ?: return getString(R.string.reminder_status_next_empty)
        val now = Calendar.getInstance()

        val nextCandidate = prayerTimes.times.mapIndexedNotNull { index, prayerTime ->
            if (!enabledStates.getOrElse(index) { false }) return@mapIndexedNotNull null
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                set(Calendar.HOUR_OF_DAY, prayerTime.time.substringBefore(":").toIntOrNull() ?: 0)
                set(Calendar.MINUTE, prayerTime.time.substringAfter(":", "0").take(2).toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            candidate to prayerTime
        }.minByOrNull { it.first.timeInMillis } ?: return getString(R.string.reminder_status_next_empty)

        val dayLabel = if (
            nextCandidate.first.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            nextCandidate.first.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            getString(R.string.reminder_day_today)
        } else {
            getString(R.string.reminder_day_tomorrow)
        }

        return getString(
            R.string.reminder_status_next_time_with_day,
            nextCandidate.second.name,
            nextCandidate.second.time,
            dayLabel,
        )
    }
}
