package com.ext.biometricauth

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ext.biometric_auth.*

/**
 * Main Activity demonstrating usage of Biometric Auth Library
 *
 * This example shows:
 * - How to configure the library via XML
 * - How to initialize BiometricAuthManager
 * - How to handle authentication callbacks
 * - How to setup PIN for fallback
 * - Different authentication scenarios
 */
class MainActivity : AppCompatActivity() {

    lateinit var authManager: BiometricAuthManager
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)

        // Initialize the library with custom configuration
        initializeBiometricAuth()

        // Setup buttons
        setupButtons()

        // Setup PIN if not already done (for demo purposes)
        setupPinIfNeeded()
    }

    /**
     * Initialize BiometricAuthManager with configuration
     * Configuration can come from XML attributes or programmatically
     */
    private fun initializeBiometricAuth() {
        // Option 1: Use default configuration
        // val config = BiometricConfig.default(this)

        // Option 2: Build configuration programmatically
        val config = BiometricConfigBuilder(this)
            .setDialogTitle("Login to Your Account")
            .setDialogSubtitle("Verify your identity to continue")
            .setDialogDescription("Use fingerprint or face to authenticate")
            .setDialogBackgroundColor(Color.WHITE)
            .setSuccessColor(Color.parseColor("#4CAF50"))
            .setErrorColor(Color.parseColor("#F44336"))
            .setEnableFingerprint(true)
            .setEnableFaceAuth(true)
            .setEnablePinFallback(true)
            .setPinLength(4)
            .setMaxPinAttempts(3)
            .setEnableAnimations(true)
            .build()

        authManager = BiometricAuthManager(this, config)
    }

    /**
     * Setup PIN for first time (in real app, do this during onboarding)
     */
    private fun setupPinIfNeeded() {
        if (!authManager.isPinSetup()) {
            // For demo: Set default PIN as "1234"
            authManager.setupPin("1234") { success ->
                if (success) {
                    updateStatus("PIN setup complete (1234)", Color.parseColor("#4CAF50"))
                } else {
                    updateStatus("PIN setup failed", Color.parseColor("#F44336"))
                }
            }
        }
    }

    /**
     * Setup button click listeners
     */
    private fun setupButtons() {
        // Biometric authentication button
        findViewById<Button>(R.id.btnAuthenticate).setOnClickListener {
            authenticateWithBiometric()
        }

        // Reset PIN button (for testing)
        findViewById<Button>(R.id.btnResetPin).setOnClickListener {
            authManager.clearPin()
            setupPinIfNeeded()
        }

        // Check capability button
        findViewById<Button>(R.id.btnCheckCapability).setOnClickListener {
            checkBiometricCapability()
        }
    }

    /**
     * Start biometric authentication with callback handling
     */
    private fun authenticateWithBiometric() {
        updateStatus("Starting authentication...", Color.GRAY)

        // Create callback to handle authentication results
        val callback = object : BiometricCallback {

            override fun onAuthenticationSuccess(result: BiometricResult) {
                when (result) {
                    is BiometricResult.AuthenticationSucceeded -> {
                        val message = "✓ Biometric authentication successful!"
                        updateStatus(message, Color.parseColor("#4CAF50"))

                        // Proceed with secure action (e.g., login, payment)
                        proceedToSecureArea()
                    }

                    is BiometricResult.PinAuthenticationSucceeded -> {
                        val message = "✓ PIN authentication successful!"
                        updateStatus(message, Color.parseColor("#4CAF50"))

                        // Proceed with secure action
                        proceedToSecureArea()
                    }

                    else -> {
                        // Should not reach here for success
                    }
                }
            }

            override fun onAuthenticationError(result: BiometricResult) {
                when (result) {
                    is BiometricResult.AuthenticationFailed -> {
                        val message = "✗ Authentication failed: ${result.errorMessage}"
                        updateStatus(message, Color.parseColor("#F44336"))
//                        showToast(message)
                    }

                    is BiometricResult.BiometricNotAvailable -> {
                        val message = "⚠ Biometric not available: ${result.reason}"
                        updateStatus(message, Color.parseColor("#FF9800"))
//                        showToast(message)
                    }

                    is BiometricResult.BiometricNotEnrolled -> {
                        val message = "⚠ No biometric enrolled. Please set up in device settings."
                        updateStatus(message, Color.parseColor("#FF9800"))
//                        showToast(message)
                    }

                    is BiometricResult.BiometricLockout -> {
                        val message =
                            "⚠ Too many attempts. Locked for ${result.lockoutDurationSeconds}s"
                        updateStatus(message, Color.parseColor("#F44336"))
//                        showToast(message)
                    }

                    is BiometricResult.PinAuthenticationFailed -> {
                        val message =
                            "✗ PIN incorrect. ${result.attemptsRemaining} attempts remaining"
                        updateStatus(message, Color.parseColor("#F44336"))
//                        showToast(message)
                    }

                    else -> {
                        val message = "✗ Authentication error"
                        updateStatus(message, Color.parseColor("#F44336"))
//                        showToast(message)
                    }
                }
            }

            override fun onAuthenticationCancelled() {
                val message = "Authentication cancelled by user"
                updateStatus(message, Color.GRAY)
            }

            override fun onAuthenticationHelp(helpMessage: String) {
                // Show help message to user (e.g., "Sensor dirty, please clean")
                showToast("Help: $helpMessage")
            }
        }

        // Start authentication
        authManager.authenticate(callback)
    }

    /**
     * Check device biometric capability
     */
    private fun checkBiometricCapability() {
        val checker = BiometricCapabilityChecker(this)
        val capability = checker.checkBiometricCapability()
        val message = checker.getCapabilityMessage(capability)

        updateStatus(message, Color.parseColor("#2196F3"))
    }

    /**
     * Proceed to secure area after successful authentication
     */
    private fun proceedToSecureArea() {
        // In real app: Navigate to secure screen, process payment, etc.
        showToast("Access granted!")
    }

    /**
     * Update status text
     */
    private fun updateStatus(message: String, color: Int) {
        statusTextView.text = message
        statusTextView.setTextColor(color)
    }

    /**
     * Show toast message
     */
    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        authManager.cleanup()
    }
}