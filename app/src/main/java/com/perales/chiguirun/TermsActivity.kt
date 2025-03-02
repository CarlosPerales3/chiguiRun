package com.perales.chiguirun

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_terms)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.bar_title_terms)
    }
}