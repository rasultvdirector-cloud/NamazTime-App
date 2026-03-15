package com.muslimtime.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.muslimtime.app.notifications.AzanPlaybackService
import androidx.appcompat.app.AppCompatActivity
import com.muslimtime.app.R
import com.muslimtime.app.notifications.PrayerActionReceiver

class PrayerReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prayer_reminder)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: getString(R.string.nav_prayer)
        val notificationId = intent.getIntExtra(EXTRA_ID, 0)
        val reminderTitle = intent.getStringExtra(EXTRA_TITLE)
            ?: getString(R.string.reminder_fullscreen_title, prayerName)
        val reminderBody = intent.getStringExtra(EXTRA_BODY)
            ?: getString(R.string.reminder_fullscreen_body)
        val simpleMode = intent.getBooleanExtra(EXTRA_SIMPLE_MODE, false)

        val headerView = findViewById<TextView>(R.id.reminder_bismillah)
        val titleView = findViewById<TextView>(R.id.reminder_prayer_name)
        val bodyView = findViewById<TextView>(R.id.reminder_prayer_body)
        val stopButton = findViewById<Button>(R.id.button_done)
        val openButton = findViewById<Button>(R.id.button_later)

        titleView.text = reminderTitle
        bodyView.text = if (simpleMode) getString(R.string.reminder_fullscreen_simple_body) else reminderBody

        if (simpleMode) {
            headerView.textSize = 18f
            titleView.textSize = 40f
            bodyView.textSize = 18f
            openButton.textSize = 26f
            stopButton.textSize = 30f
        }

        stopButton.setOnClickListener {
            dispatchAction(PrayerActionReceiver.ACTION_STOP_REMINDER, notificationId)
        }

        openButton.setOnClickListener {
            stopService(Intent(this, AzanPlaybackService::class.java))
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            )
            finish()
        }
    }

    private fun dispatchAction(action: String, notificationId: Int) {
        sendBroadcast(
            Intent(this, PrayerActionReceiver::class.java).apply {
                this.action = action
                putExtra(PrayerActionReceiver.EXTRA_ID, notificationId)
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SIMPLE_MODE = "extra_simple_mode"
    }
}
