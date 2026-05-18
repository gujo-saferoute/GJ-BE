package com.example.safe_route_project.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.safe_route_project.MainActivity
import com.example.safe_route_project.R
import com.example.safe_route_project.settings.AppThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainScreenController(
    private val activity: AppCompatActivity,
    private val bottomNav: BottomNavigationView,
    private val homeLayout: View,
    private val mapScreen: View,
    private val settingsLayout: View,
    private val btnMyLocation: FloatingActionButton,
    private val switchDarkMode: SwitchCompat,
) {
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun bind(
        savedInstanceState: Bundle?,
        onMapSelected: () -> Unit
    ) {
        requestNotificationPermissionIfNeeded()
        AppThemeManager.bind(activity, switchDarkMode)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {
                    showHome()
                    true
                }

                R.id.tab_map -> {
                    showMap()
                    onMapSelected()
                    true
                }

                R.id.tab_settings -> {
                    showSettings()
                    true
                }

                else -> false
            }
        }

        val initialTab = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: R.id.tab_home
        bottomNav.selectedItemId = initialTab
        handleIntent(activity.intent)
    }

    fun handleNewIntent(intent: Intent?) {
        handleIntent(intent)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, bottomNav.selectedItemId)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra(MainActivity.EXTRA_OPEN_TAB)) {
            MainActivity.TAB_HOME -> bottomNav.selectedItemId = R.id.tab_home
            MainActivity.TAB_MAP -> bottomNav.selectedItemId = R.id.tab_map
        }
    }

    private fun showHome() {
        homeLayout.visibility = View.VISIBLE
        mapScreen.visibility = View.GONE
        settingsLayout.visibility = View.GONE
        btnMyLocation.visibility = View.GONE
    }

    private fun showMap() {
        homeLayout.visibility = View.GONE
        mapScreen.visibility = View.VISIBLE
        settingsLayout.visibility = View.GONE
        btnMyLocation.visibility = View.VISIBLE
    }

    private fun showSettings() {
        homeLayout.visibility = View.GONE
        mapScreen.visibility = View.GONE
        settingsLayout.visibility = View.VISIBLE
        btnMyLocation.visibility = View.GONE
    }

    companion object {
        private const val KEY_SELECTED_TAB = "key_selected_tab"
    }
}