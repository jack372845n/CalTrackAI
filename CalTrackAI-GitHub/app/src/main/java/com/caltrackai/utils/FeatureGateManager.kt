package com.caltrackai.utils

import android.content.Context
import android.util.Log
import com.caltrackai.services.BetaTesterService
import com.caltrackai.services.PremiumFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FeatureGateManager handles access control for premium features,
 * ensuring beta testers get full access while regular users see
 * appropriate subscription prompts.
 * 
 * This manager provides a centralized way to check feature access
 * and handle the different user types (beta testers vs regular users)
 * seamlessly throughout the application.
 */
class FeatureGateManager(private val context: Context) {
    
    private val betaTesterService = BetaTesterService(context)
    
    companion object {
        private const val TAG = "FeatureGateManager"
        
        @Volatile
        private var INSTANCE: FeatureGateManager? = null
        
        fun getInstance(context: Context): FeatureGateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeatureGateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Check if user has access to a specific premium feature
     * Returns true for beta testers, checks subscription for regular users
     */
    fun hasFeatureAccess(feature: PremiumFeature): Boolean {
        return betaTesterService.hasFeatureAccess(feature)
    }
    
    /**
     * Check if user has general premium access
     * Returns true for beta testers and active subscribers
     */
    fun hasPremiumAccess(): Boolean {
        return betaTesterService.hasPremiumAccess()
    }
    
    /**
     * Execute code block only if user has access to specified feature
     * For beta testers: executes immediately
     * For regular users: shows subscription prompt if no access
     */
    inline fun withFeatureAccess(
        feature: PremiumFeature,
        onAccessGranted: () -> Unit,
        onAccessDenied: (() -> Unit)? = null
    ) {
        if (hasFeatureAccess(feature)) {
            Log.d(TAG, "Feature access granted for: ${feature.name}")
            onAccessGranted()
        } else {
            Log.d(TAG, "Feature access denied for: ${feature.name}")
            onAccessDenied?.invoke() ?: showSubscriptionPrompt(feature)
        }
    }
    
    /**
     * Multi-AI Food Recognition Access Control
     * Beta testers: Full access with 99.5% accuracy
     * Regular users: Limited scans then subscription prompt
     */
    fun canUseMultiAIRecognition(): Boolean {
        return hasFeatureAccess(PremiumFeature.MULTI_AI_RECOGNITION)
    }
    
    fun executeMultiAIRecognition(
        onSuccess: () -> Unit,
        onSubscriptionRequired: (() -> Unit)? = null
    ) {
        withFeatureAccess(
            feature = PremiumFeature.MULTI_AI_RECOGNITION,
            onAccessGranted = {
                Log.i(TAG, "Multi-AI recognition access granted")
                onSuccess()
            },
            onAccessDenied = {
                Log.i(TAG, "Multi-AI recognition requires subscription")
                onSubscriptionRequired?.invoke() ?: showMultiAISubscriptionPrompt()
            }
        )
    }
    
    /**
     * Voice Assistant Access Control
     * Beta testers: Full 20+ language support
     * Regular users: English only, then subscription prompt for more languages
     */
    fun canUseVoiceAssistant(language: String = "en"): Boolean {
        if (language == "en") {
            return true // Basic English voice is free
        }
        return hasFeatureAccess(PremiumFeature.ALL_LANGUAGES)
    }
    
    fun executeVoiceLogging(
        language: String = "en",
        onSuccess: () -> Unit,
        onSubscriptionRequired: (() -> Unit)? = null
    ) {
        if (canUseVoiceAssistant(language)) {
            Log.i(TAG, "Voice assistant access granted for language: $language")
            onSuccess()
        } else {
            Log.i(TAG, "Voice assistant requires subscription for language: $language")
            onSubscriptionRequired?.invoke() ?: showVoiceSubscriptionPrompt(language)
        }
    }
    
    /**
     * Advanced AI Coaching Access Control
     * Beta testers: Full context-aware coaching
     * Regular users: Basic recommendations, then subscription prompt
     */
    fun canUseAdvancedCoaching(): Boolean {
        return hasFeatureAccess(PremiumFeature.ADVANCED_COACHING)
    }
    
    fun executeAdvancedCoaching(
        onSuccess: () -> Unit,
        onSubscriptionRequired: (() -> Unit)? = null
    ) {
        withFeatureAccess(
            feature = PremiumFeature.ADVANCED_COACHING,
            onAccessGranted = {
                Log.i(TAG, "Advanced coaching access granted")
                onSuccess()
            },
            onAccessDenied = {
                Log.i(TAG, "Advanced coaching requires subscription")
                onSubscriptionRequired?.invoke() ?: showCoachingSubscriptionPrompt()
            }
        )
    }
    
    /**
     * Advanced Analytics Access Control
     * Beta testers: Full analytics dashboard
     * Regular users: Basic stats, then subscription prompt
     */
    fun canUseAdvancedAnalytics(): Boolean {
        return hasFeatureAccess(PremiumFeature.ADVANCED_ANALYTICS)
    }
    
    fun executeAdvancedAnalytics(
        onSuccess: () -> Unit,
        onSubscriptionRequired: (() -> Unit)? = null
    ) {
        withFeatureAccess(
            feature = PremiumFeature.ADVANCED_ANALYTICS,
            onAccessGranted = {
                Log.i(TAG, "Advanced analytics access granted")
                onSuccess()
            },
            onAccessDenied = {
                Log.i(TAG, "Advanced analytics requires subscription")
                onSubscriptionRequired?.invoke() ?: showAnalyticsSubscriptionPrompt()
            }
        )
    }
    
    /**
     * Unlimited Scanning Access Control
     * Beta testers: Unlimited food scans
     * Regular users: Limited scans per day, then subscription prompt
     */
    fun canScanUnlimited(): Boolean {
        return hasFeatureAccess(PremiumFeature.UNLIMITED_SCANNING)
    }
    
    fun executeFoodScan(
        onSuccess: () -> Unit,
        onLimitReached: (() -> Unit)? = null
    ) {
        if (canScanUnlimited()) {
            Log.i(TAG, "Unlimited scanning access granted")
            onSuccess()
        } else {
            // Check daily scan limit for regular users
            if (hasRemainingScans()) {
                Log.i(TAG, "Scan allowed within daily limit")
                incrementScanCount()
                onSuccess()
            } else {
                Log.i(TAG, "Daily scan limit reached, subscription required")
                onLimitReached?.invoke() ?: showScanLimitPrompt()
            }
        }
    }
    
    /**
     * Priority Support Access Control
     * Beta testers: Direct access to development team
     * Regular users: Standard support channels
     */
    fun canUsePrioritySupport(): Boolean {
        return hasFeatureAccess(PremiumFeature.PRIORITY_SUPPORT)
    }
    
    /**
     * Show subscription prompts for different features
     * Beta testers never see these prompts
     */
    private fun showSubscriptionPrompt(feature: PremiumFeature) {
        if (betaTesterService.hasPremiumAccess()) {
            // Beta testers should never see subscription prompts
            Log.w(TAG, "Beta tester seeing subscription prompt - this should not happen")
            return
        }
        
        when (feature) {
            PremiumFeature.MULTI_AI_RECOGNITION -> showMultiAISubscriptionPrompt()
            PremiumFeature.VOICE_ASSISTANT -> showVoiceSubscriptionPrompt()
            PremiumFeature.ADVANCED_COACHING -> showCoachingSubscriptionPrompt()
            PremiumFeature.UNLIMITED_SCANNING -> showScanLimitPrompt()
            PremiumFeature.ADVANCED_ANALYTICS -> showAnalyticsSubscriptionPrompt()
            PremiumFeature.PRIORITY_SUPPORT -> showSupportSubscriptionPrompt()
            PremiumFeature.ALL_LANGUAGES -> showLanguageSubscriptionPrompt()
        }
    }
    
    private fun showMultiAISubscriptionPrompt() {
        Log.d(TAG, "Showing Multi-AI subscription prompt")
        // Show dialog highlighting 99.5% accuracy advantage
        // "Upgrade to CalTrackAI Premium for 99.5% AI accuracy - 50% more accurate than competitors!"
    }
    
    private fun showVoiceSubscriptionPrompt(language: String = "") {
        Log.d(TAG, "Showing Voice subscription prompt for language: $language")
        // Show dialog highlighting 20+ language support
        // "Unlock voice logging in 20+ languages - unique to CalTrackAI!"
    }
    
    private fun showCoachingSubscriptionPrompt() {
        Log.d(TAG, "Showing Coaching subscription prompt")
        // Show dialog highlighting context-aware coaching
        // "Get AI coaching that adapts to your lifestyle - smarter than static meal plans!"
    }
    
    private fun showAnalyticsSubscriptionPrompt() {
        Log.d(TAG, "Showing Analytics subscription prompt")
        // Show dialog highlighting advanced insights
        // "Unlock detailed nutrition insights and progress tracking!"
    }
    
    private fun showScanLimitPrompt() {
        Log.d(TAG, "Showing Scan limit prompt")
        // Show dialog highlighting unlimited scanning
        // "Upgrade for unlimited food scanning - only $4.99/month vs competitors' $8.99-$12.99!"
    }
    
    private fun showSupportSubscriptionPrompt() {
        Log.d(TAG, "Showing Support subscription prompt")
        // Show dialog highlighting priority support
        // "Get priority support and direct access to our team!"
    }
    
    private fun showLanguageSubscriptionPrompt() {
        Log.d(TAG, "Showing Language subscription prompt")
        // Show dialog highlighting multilingual support
        // "Unlock voice logging in your native language - 20+ languages supported!"
    }
    
    /**
     * Daily scan limit management for regular users
     * Beta testers bypass this entirely
     */
    private fun hasRemainingScans(): Boolean {
        if (betaTesterService.hasPremiumAccess()) {
            return true // Beta testers have unlimited scans
        }
        
        val prefs = context.getSharedPreferences("CalTrackAI_Limits", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val todayScans = prefs.getInt("scans_$today", 0)
        
        return todayScans < 5 // Regular users get 5 free scans per day
    }
    
    private fun incrementScanCount() {
        if (betaTesterService.hasPremiumAccess()) {
            return // Beta testers don't need scan counting
        }
        
        val prefs = context.getSharedPreferences("CalTrackAI_Limits", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val todayScans = prefs.getInt("scans_$today", 0)
        
        prefs.edit()
            .putInt("scans_$today", todayScans + 1)
            .apply()
    }
    
    /**
     * Get remaining scans for display purposes
     */
    fun getRemainingScans(): Int {
        if (betaTesterService.hasPremiumAccess()) {
            return Int.MAX_VALUE // Beta testers have unlimited
        }
        
        val prefs = context.getSharedPreferences("CalTrackAI_Limits", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val todayScans = prefs.getInt("scans_$today", 0)
        
        return maxOf(0, 5 - todayScans)
    }
    
    /**
     * Initialize feature gates and verify beta tester status
     * Should be called during app startup
     */
    fun initializeFeatureGates() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Initializing feature gates...")
                val betaStatus = betaTesterService.detectAndVerifyBetaTester()
                Log.i(TAG, "Beta tester status: $betaStatus")
                
                if (betaStatus == com.caltrackai.services.BetaTesterStatus.BETA_TESTER_CONFIRMED) {
                    Log.i(TAG, "Beta tester confirmed - all premium features enabled")
                } else {
                    Log.d(TAG, "Regular user - feature gates active")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing feature gates", e)
            }
        }
    }
    
    /**
     * Get feature access summary for debugging/display
     */
    fun getFeatureAccessSummary(): Map<String, Boolean> {
        return mapOf(
            "Premium Access" to hasPremiumAccess(),
            "Multi-AI Recognition" to hasFeatureAccess(PremiumFeature.MULTI_AI_RECOGNITION),
            "Voice Assistant" to hasFeatureAccess(PremiumFeature.VOICE_ASSISTANT),
            "Advanced Coaching" to hasFeatureAccess(PremiumFeature.ADVANCED_COACHING),
            "Unlimited Scanning" to hasFeatureAccess(PremiumFeature.UNLIMITED_SCANNING),
            "Advanced Analytics" to hasFeatureAccess(PremiumFeature.ADVANCED_ANALYTICS),
            "Priority Support" to hasFeatureAccess(PremiumFeature.PRIORITY_SUPPORT),
            "All Languages" to hasFeatureAccess(PremiumFeature.ALL_LANGUAGES)
        )
    }
    
    /**
     * Force refresh of feature access status
     * Useful after subscription changes or beta status updates
     */
    fun refreshFeatureAccess() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Refreshing feature access status...")
                betaTesterService.detectAndVerifyBetaTester()
                Log.i(TAG, "Feature access status refreshed")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing feature access", e)
            }
        }
    }
}

