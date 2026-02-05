package com.example.safespark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safespark.auth.ParentAuthActivity
import com.example.safespark.auth.ParentAuthManager
import androidx.core.app.Person

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var authManager: ParentAuthManager

    private val notificationPermissionHandler = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            Log.d(TAG, "✅ Notification Permission granted")
        } else {
            Log.w(TAG, "⚠️ Notification Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = ParentAuthManager(this)

        if (!checkAuthAndConsent()) {
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestNotificationPermission()
        setupDynamicShareShortcut()
        wireUpDemoButton()

        Log.d(TAG, "✅ MainActivity initialized in Share Target mode")
    }

    private fun wireUpDemoButton() {
        val demoTrigger = findViewById<Button>(R.id.btnDemoCheck)
        demoTrigger.setOnClickListener {
            val testIntent = Intent(this, CheckItActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Bist du allein zu Hause?")
            }
            startActivity(testIntent)
        }
    }

    private fun setupDynamicShareShortcut() {
        try {
            val shareShortcut = ShortcutInfoCompat.Builder(this, "spark_share_target")
                .setShortLabel("Spark ⚡")
                .setLongLabel("An Spark weiterleiten")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(Intent.ACTION_SEND).apply {
                    type = "text/*"
                    setClass(this@MainActivity, CheckItActivity::class.java)
                })
                .setCategories(setOf("com.example.safespark.category.TEXT_SHARE_TARGET"))
                .setPerson(Person.Builder().setName("Spark ⚡").build())
                .build()
            
            ShortcutManagerCompat.pushDynamicShortcut(this, shareShortcut)
            Log.d(TAG, "✅ Dynamic share shortcut created")
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to create share shortcut: ${ex.message}")
        }
    }

    private fun checkAuthAndConsent(): Boolean {
        when {
            !authManager.isPinSet() -> {
                Log.d(TAG, "⚠️ No PIN configured -> ParentAuthActivity")
                startActivity(Intent(this, ParentAuthActivity::class.java))
                finish()
                return false
            }
            !authManager.isOnboardingCompleted() || !authManager.isConsentGiven() -> {
                Log.d(TAG, "⚠️ Onboarding/Consent missing")
                startActivity(Intent(this, com.example.safespark.consent.OnboardingActivity::class.java))
                finish()
                return false
            }
            else -> {
                Log.d(TAG, "✅ Auth & Consent validated")
                return true
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "✅ Notification Permission already granted")
                }
                else -> {
                    Log.d(TAG, "🔔 Requesting Notification Permission...")
                    notificationPermissionHandler.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "ℹ️ Android < 13 - No notification permission needed")
        }
    }
}
