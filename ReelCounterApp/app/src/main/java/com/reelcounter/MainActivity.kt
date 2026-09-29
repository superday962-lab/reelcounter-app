package com.reelcounter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var countTv: TextView
    private lateinit var statusTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(ReelAccessibilityService.PREFS, MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 120, 48, 48)
        }

        countTv = TextView(this).apply { textSize = 72f; gravity = Gravity.CENTER }
        statusTv = TextView(this).apply { textSize = 15f; gravity = Gravity.CENTER; setPadding(0, 16, 0, 32) }

        val permBtn = Button(this).apply {
            text = "Permission do (Accessibility kholo)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val strict = Switch(this).apply {
            text = "Sirf Reels screen me count karo"
            isChecked = prefs.getBoolean(ReelAccessibilityService.KEY_STRICT, true)
            setPadding(0, 32, 0, 32)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean(ReelAccessibilityService.KEY_STRICT, on).apply()
            }
        }

        val resetBtn = Button(this).apply {
            text = "Count reset"
            setOnClickListener {
                prefs.edit().putInt(ReelAccessibilityService.KEY_COUNT, 0).apply()
                refresh()
            }
        }

        root.addView(countTv); root.addView(statusTv)
        root.addView(permBtn); root.addView(strict); root.addView(resetBtn)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val prefs = getSharedPreferences(ReelAccessibilityService.PREFS, MODE_PRIVATE)
        countTv.text = prefs.getInt(ReelAccessibilityService.KEY_COUNT, 0).toString()
        val on = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(packageName) == true
        statusTv.text = if (on) "✅ Chalu hai. Instagram Reels kholo." else "❌ Permission abhi off hai."
    }
}
