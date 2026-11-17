package com.example.streamviewlite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var repo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        setContentView(R.layout.activity_main)

        val btnPick = findViewById<Button>(R.id.btnPickMedia)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val editUrl = findViewById<EditText>(R.id.editUrl)
        val editKey = findViewById<EditText>(R.id.editKey)

        // Load settings when activity starts
        CoroutineScope(Dispatchers.Main).launch {
            repo.settings.collect { 
                if (editUrl.text.isNullOrEmpty()) editUrl.setText(it.rtmpUrl)
                if (editKey.text.isNullOrEmpty()) editKey.setText(it.streamKey)
            }
        }

        btnPick.setOnClickListener {
            registerForActivityResult(ActivityResultContracts.OpenDocument()).launch(arrayOf("video/mp4"))
        }

        btnSave.setOnClickListener {
            val textUrl = editUrl.text.toString()
            val textKey = editKey.text.toString()
            if (textUrl.endsWith("/")) { editUrl.setText(textUrl.dropLast(1)) }
            CoroutineScope(Dispatchers.IO).launch { 
                repo.saveSettings(AppSettings(textUrl, textKey)) 
                runOnUiThread { Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show() }
            }
        }

        btnStart.setOnClickListener {
            val file = File(filesDir, "fake_cam.mp4")
            if (!file.exists()) { Toast.makeText(this, "Pick video first!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            startService(Intent(this, LoopStreamService::class.java))
        }

        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                Toast.makeText(this, "Video loaded: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
