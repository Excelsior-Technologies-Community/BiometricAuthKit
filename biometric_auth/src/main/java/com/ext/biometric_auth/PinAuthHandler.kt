package com.ext.biometric_auth

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Handles PIN fallback authentication when biometric fails or is unavailable.
 * Includes retry limits, lockout mechanism, and secure PIN handling.
 *
 * Why PIN Fallback is Required:
 * 1. Biometric sensors can fail (dirty sensor, lighting conditions)
 * 2. User may have injured finger or changed appearance
 * 3. Regulatory requirement for alternative authentication
 * 4. Device may temporarily lock out biometric after failed attempts
 *
 * When it is Triggered:
 * 1. User clicks "Use PIN Instead" button in biometric dialog
 * 2. Biometric is permanently locked out
 * 3. Biometric hardware is unavailable
 * 4. User preference for PIN over biometric
 */
class PinAuthHandler(
    private val context: Context,
    private val config: BiometricConfig,
    private val callback: BiometricCallback
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "biometric_auth_prefs",
        Context.MODE_PRIVATE
    )

    private var pinDialog: Dialog? = null
    private var currentPin: String = ""
    private var attemptsRemaining: Int = config.maxPinAttempts
    private var lockoutTimer: CountDownTimer? = null

    // Keys for SharedPreferences
    private companion object {
        const val KEY_STORED_PIN = "stored_pin_hash"
        const val KEY_ATTEMPTS_REMAINING = "attempts_remaining"
        const val KEY_LOCKOUT_UNTIL = "lockout_until"
        const val KEY_PIN_SALT = "pin_salt"
    }

    /**
     * Check if PIN is set up
     */
    fun isPinSetup(): Boolean {
        return prefs.contains(KEY_STORED_PIN)
    }

    /**
     * Set up a new PIN (should be called during onboarding/setup)
     * @param pin The PIN to store (will be hashed)
     */
    fun setupPin(pin: String): Boolean {
        if (pin.length != config.pinLength) {
            return false
        }

        // Generate random salt for this PIN
        val salt = generateRandomSalt()
        val hashedPin = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_STORED_PIN, hashedPin)
            .putString(KEY_PIN_SALT, salt)
            .putInt(KEY_ATTEMPTS_REMAINING, config.maxPinAttempts)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()

        return true
    }

    /**
     * Show PIN authentication dialog
     */
    fun showPinDialog() {
        // Check if currently locked out
        if (isLockedOut()) {
            val remainingTime = getRemainingLockoutTime()
            callback.onAuthenticationError(
                BiometricResult.BiometricLockout(remainingTime.toInt())
            )
            return
        }

        // Check if PIN is set up
        if (!isPinSetup()) {
            callback.onAuthenticationError(
                BiometricResult.BiometricNotAvailable("PIN not configured")
            )
            return
        }

        // Load current attempts
        attemptsRemaining = prefs.getInt(KEY_ATTEMPTS_REMAINING, config.maxPinAttempts)
        currentPin = ""

        createPinDialog()
    }

    /**
     * Create and configure PIN dialog
     */
    private fun createPinDialog() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pin_auth, null)
        dialog.setContentView(view)

        // Force the dialog window to be full width with controlled margins
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            decorView.setPadding(50, 0, 50, 0)
        }

        // Configure dialog UI
        val titleText: TextView = view.findViewById(R.id.pinDialogTitle)
        val statusText: TextView = view.findViewById(R.id.pinStatusText)
        val pinIndicators: List<View> = listOf(
            view.findViewById(R.id.pinIndicator1),
            view.findViewById(R.id.pinIndicator2),
            view.findViewById(R.id.pinIndicator3),
            view.findViewById(R.id.pinIndicator4)
        )
        val cancelButton: Button = view.findViewById(R.id.pinCancelButton)

        // Apply custom styling
        view.setBackgroundColor(config.dialogBackgroundColor)
        titleText.text = "Enter PIN"
        titleText.setTextColor(config.dialogTitleColor)
        statusText.text = "Attempts remaining: $attemptsRemaining"
        statusText.setTextColor(config.dialogSubtitleColor)
        cancelButton.text = config.cancelButtonText
        cancelButton.setTextColor(config.cancelButtonColor)

        // Number pad buttons
        val numberButtons = listOf(
            view.findViewById<Button>(R.id.btn0),
            view.findViewById<Button>(R.id.btn1),
            view.findViewById<Button>(R.id.btn2),
            view.findViewById<Button>(R.id.btn3),
            view.findViewById<Button>(R.id.btn4),
            view.findViewById<Button>(R.id.btn5),
            view.findViewById<Button>(R.id.btn6),
            view.findViewById<Button>(R.id.btn7),
            view.findViewById<Button>(R.id.btn8),
            view.findViewById<Button>(R.id.btn9)
        )

        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)

        // Set up number button listeners
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleNumberInput(index, pinIndicators, statusText)
            }
        }

        // Delete button
        deleteButton.setOnClickListener {
            handleDeleteInput(pinIndicators)
        }

        // Cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
            callback.onAuthenticationCancelled()
        }

        dialog.setOnDismissListener {
            pinDialog = null
        }

        pinDialog = dialog
        dialog.show()
    }

    /**
     * Handle number input
     */
    private fun handleNumberInput(number: Int, indicators: List<View>, statusText: TextView) {
        if (currentPin.length < config.pinLength) {
            currentPin += number.toString()
            updatePinIndicators(indicators)

            // Check PIN when complete
            if (currentPin.length == config.pinLength) {
                verifyPin(statusText, indicators)
            }
        }
    }

    /**
     * Handle delete input
     */
    private fun handleDeleteInput(indicators: List<View>) {
        if (currentPin.isNotEmpty()) {
            currentPin = currentPin.dropLast(1)
            updatePinIndicators(indicators)
        }
    }

    /**
     * Update PIN indicator dots
     */
    private fun updatePinIndicators(indicators: List<View>) {
        indicators.forEachIndexed { index, indicator ->
            val isActive = index < currentPin.length
            indicator.setBackgroundColor(
                if (isActive) config.successColor else config.neutralColor
            )
        }
    }

    /**
     * Verify entered PIN
     */
    private fun verifyPin(statusText: TextView, indicators: List<View>) {
        val storedHash = prefs.getString(KEY_STORED_PIN, "")
        val salt = prefs.getString(KEY_PIN_SALT, "")
        val enteredHash = hashPin(currentPin, salt ?: "")

        if (enteredHash == storedHash) {
            // PIN correct - success
            indicators.forEach { it.setBackgroundColor(config.successColor) }
            statusText.text = "PIN Correct!"
            statusText.setTextColor(config.successColor)

            // Reset attempts
            prefs.edit()
                .putInt(KEY_ATTEMPTS_REMAINING, config.maxPinAttempts)
                .apply()

            // Delay to show success state
            pinDialog?.window?.decorView?.postDelayed({
                pinDialog?.dismiss()
                callback.onAuthenticationSuccess(
                    BiometricResult.PinAuthenticationSucceeded(currentPin)
                )
            }, 500)

        } else {
            // PIN incorrect
            attemptsRemaining--
            prefs.edit()
                .putInt(KEY_ATTEMPTS_REMAINING, attemptsRemaining)
                .apply()

            indicators.forEach { it.setBackgroundColor(config.errorColor) }
            currentPin = ""

            if (attemptsRemaining <= 0) {
                // Lockout
                val lockoutUntil = System.currentTimeMillis() +
                        (config.pinLockoutSeconds * 1000L)
                prefs.edit()
                    .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                    .apply()

                statusText.text = "Too many attempts. Locked for ${config.pinLockoutSeconds}s"
                statusText.setTextColor(config.errorColor)

                pinDialog?.window?.decorView?.postDelayed({
                    pinDialog?.dismiss()
                    callback.onAuthenticationError(
                        BiometricResult.BiometricLockout(config.pinLockoutSeconds)
                    )
                }, 1500)

            } else {
                statusText.text = "Incorrect PIN. $attemptsRemaining attempts remaining"
                statusText.setTextColor(config.errorColor)

                // Reset indicators after delay
                pinDialog?.window?.decorView?.postDelayed({
                    indicators.forEach { it.setBackgroundColor(config.neutralColor) }
                }, 500)
            }
        }
    }

    /**
     * Check if PIN entry is currently locked out
     */
    private fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        return System.currentTimeMillis() < lockoutUntil
    }

    /**
     * Get remaining lockout time in seconds
     */
    private fun getRemainingLockoutTime(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        val remaining = (lockoutUntil - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }

    /**
     * Hash PIN with salt using SHA-256
     * Note: In production, use Android Keystore for better security
     */
    private fun hashPin(pin: String, salt: String): String {
        return try {
            val bytes = (pin + salt).toByteArray()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Generate random salt for PIN hashing
     */
    private fun generateRandomSalt(): String {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /**
     * Clear PIN (for testing or reset)
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_STORED_PIN)
            .remove(KEY_PIN_SALT)
            .remove(KEY_ATTEMPTS_REMAINING)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    /**
     * Dismiss PIN dialog if showing
     */
    fun dismiss() {
        lockoutTimer?.cancel()
        pinDialog?.dismiss()
        pinDialog = null
    }
}