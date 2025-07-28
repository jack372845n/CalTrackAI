package com.caltrackai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.caltrackai.databinding.ActivityMainBinding
import com.caltrackai.fragments.DashboardFragment
import com.caltrackai.fragments.ProfileFragment
import com.caltrackai.fragments.StatsFragment
import com.caltrackai.viewmodels.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val AUDIO_PERMISSION_REQUEST_CODE = 101
        private const val PERMISSIONS_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not signed in, redirect to onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "CalTrackAI"
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        // Request necessary permissions
        requestPermissions()
        
        // Setup FAB for camera
        binding.fabCamera.setOnClickListener {
            if (hasRequiredPermissions()) {
                startCameraActivity()
            } else {
                requestPermissions()
            }
        }
        
        // Observe ViewModel
        observeViewModel()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_stats -> {
                    loadFragment(StatsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissions granted! You can now use camera and voice features.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Camera and microphone permissions are required for food scanning and voice logging.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
    
    private fun observeViewModel() {
        viewModel.userProfile.observe(this) { profile ->
            // Update UI with user profile data
            supportActionBar?.subtitle = "Welcome, ${profile?.name ?: "User"}"
        }
        
        viewModel.dailyCalories.observe(this) { calories ->
            // Update daily calories display
            binding.dailyCaloriesText.text = "$calories cal"
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
            binding.progressBar.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_voice -> {
                if (hasRequiredPermissions()) {
                    startVoiceLogging()
                } else {
                    requestPermissions()
                }
                true
            }
            R.id.action_settings -> {
                // Open settings
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun startVoiceLogging() {
        // Start voice logging activity or show voice input dialog
        Toast.makeText(this, "Voice logging activated - say what you ate!", Toast.LENGTH_SHORT).show()
        // TODO: Implement voice logging functionality
    }
    
    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the app
        viewModel.refreshData()
    }
}

