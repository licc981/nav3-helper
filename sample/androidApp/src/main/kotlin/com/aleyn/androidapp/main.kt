package com.aleyn.androidapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aleyn.navigation.core.route.NavCenter
import sample.app.App
import sample.app.initNavigation

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initNavigation()
        NavCenter.setRouteNotFoundHandler { url ->
            Toast.makeText(this, "没找到:$url", Toast.LENGTH_SHORT).show()
            return@setRouteNotFoundHandler null
        }
        setContent { App() }
    }
}
