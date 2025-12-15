package com.ext.biometric_auth

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Main entry point for biometric authentication library.
 * Orchestrates capability checking, biometric prompts, custom dialogs, and PIN fallback.
 *
 * Authentication Flow:
 * 1. Check device capabilities (BiometricCapabilityChecker)
 * 2. Show custom biometric dialog (BiometricDialogHandler)
 * 3. Initiate BiometricPrompt authentication
 * 4. Handle success/failure/error/cancel states
 * 5. Offer PIN fallback on failure or user request (PinAuthHandler)
 * 6. Return result through callback
 *
 * Usage:
 * val manager = BiometricAuthManager(activity, config)
 * manager.authenticate(callback)
 */
class BiometricAuthManager(
    private val activity: FragmentActivity,
    private val config: BiometricConfig
) {
    private val context: Context = activity.applicationContext
    private val capabilityChecker = BiometricCapabilityChecker(context)
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var pinHandler: PinAuthHandler? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var currentCallback: BiometricCallback? = null

    /**
     * Start biometric authentication
     * @param callback Callback to receive authentication results
     */
    fun authenticate(callback: BiometricCallback) {
        currentCallback = callback

        // Step 1: Check device capabilities
        val capability = capabilityChecker.checkBiometricCapability()

        when (capability) {
            is BiometricCapabilityChecker.BiometricCapability.Available -> {
                // Biometric available, proceed with authentication
                startBiometricAuthentication()
            }

            is BiometricCapabilityChecker.BiometricCapability.NotEnrolled -> {
                // No biometric enrolled - offer PIN if enabled
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    callback.onAuthenticationError(
                        BiometricResult.BiometricNotEnrolled
                    )
                }
            }

            is BiometricCapabilityChecker.BiometricCapability.HardwareNotPresent,
            is BiometricCapabilityChecker.BiometricCapability.NotAvailable -> {
                // Hardware issue - offer PIN if enabled
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    val message = capabilityChecker.getCapabilityMessage(capability)
                    callback.onAuthenticationError(
                        BiometricResult.BiometricNotAvailable(message)
                    )
                }
            }

            else -> {
                val message = capabilityChecker.getCapabilityMessage(capability)
                callback.onAuthenticationError(
                    BiometricResult.BiometricNotAvailable(message)
                )
            }
        }
    }

    /**
     * Start biometric authentication with custom dialog
     */
    private fun startBiometricAuthentication() {
        // Create BiometricPrompt
        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                handleAuthenticationSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                handleAuthenticationError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                handleAuthenticationFailed()
            }
        }

        biometricPrompt = BiometricPrompt(activity, executor, promptCallback)

        // Build prompt info
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.dialogTitle)
            .setSubtitle(config.dialogSubtitle)
            .setDescription(config.dialogDescription)
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(if (config.enablePinFallback) config.pinButtonText else config.cancelButtonText)
            .setConfirmationRequired(false) // Faster authentication
            .build()

        // Authenticate
        try {
            biometricPrompt?.authenticate(promptInfo)
        } catch (e: Exception) {
            currentCallback?.onAuthenticationError(
                BiometricResult.AuthenticationFailed(
                    errorCode = -1,
                    errorMessage = "Failed to start authentication: ${e.message}"
                )
            )
        }
    }

    /**
     * Handle successful authentication
     */
    private fun handleAuthenticationSuccess() {
        currentCallback?.onAuthenticationSuccess(
                    BiometricResult.AuthenticationSucceeded(
                        authenticationType = "BIOMETRIC"
                    )
                )
    }

    /**
     * Handle authentication error
     */
    private fun handleAuthenticationError(errorCode: Int, errorMessage: String) {
        when (errorCode) {
            BiometricErrorCodes.ERROR_CANCELED,
            BiometricErrorCodes.ERROR_USER_CANCELED -> {
                currentCallback?.onAuthenticationCancelled()
            }
            BiometricErrorCodes.ERROR_NEGATIVE_BUTTON -> {
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    currentCallback?.onAuthenticationCancelled()
                }
            }
            BiometricErrorCodes.ERROR_LOCKOUT,
            BiometricErrorCodes.ERROR_LOCKOUT_PERMANENT -> {
                val lockoutSeconds = if (errorCode == BiometricErrorCodes.ERROR_LOCKOUT) 30 else 300
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    currentCallback?.onAuthenticationError(BiometricResult.BiometricLockout(lockoutSeconds))
                }
            }
            BiometricErrorCodes.ERROR_NO_BIOMETRICS -> {
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    currentCallback?.onAuthenticationError(BiometricResult.BiometricNotEnrolled)
                }
            }
            BiometricErrorCodes.ERROR_HW_NOT_PRESENT,
            BiometricErrorCodes.ERROR_HW_UNAVAILABLE -> {
                if (config.enablePinFallback) {
                    showPinFallback()
                } else {
                    currentCallback?.onAuthenticationError(BiometricResult.BiometricNotAvailable(errorMessage))
                }
            }
            else -> {
                currentCallback?.onAuthenticationError(BiometricResult.AuthenticationFailed(errorCode, errorMessage))
            }
        }
    }

    /**
     * Handle authentication failed (biometric not recognized but can retry)
     */
    private fun handleAuthenticationFailed() {
        currentCallback?.onAuthenticationHelp("Not recognized. Try again")
    }

    /**
     * Show PIN fallback authentication
     */
    private fun showPinFallback() {
        pinHandler = PinAuthHandler(
            context = activity,
            config = config,
            callback = object : BiometricCallback {
                override fun onAuthenticationSuccess(result: BiometricResult) {
                    currentCallback?.onAuthenticationSuccess(result)
                }

                override fun onAuthenticationError(result: BiometricResult) {
                    currentCallback?.onAuthenticationError(result)
                }

                override fun onAuthenticationCancelled() {
                    currentCallback?.onAuthenticationCancelled()
                }

                override fun onAuthenticationHelp(helpMessage: String) {
                    currentCallback?.onAuthenticationHelp(helpMessage)
                }
            }
        )

        pinHandler?.showPinDialog()
    }

    /**
     * Cancel ongoing authentication
     */
    private fun cancelAuthentication() {
        biometricPrompt?.cancelAuthentication()
        pinHandler?.dismiss()
        currentCallback?.onAuthenticationCancelled()
    }

    /**
     * Setup PIN for fallback authentication
     * Should be called during app setup/onboarding
     */
    fun setupPin(pin: String, callback: (Boolean) -> Unit) {
        val handler = PinAuthHandler(activity, config, object : BiometricCallback {
            override fun onAuthenticationSuccess(result: BiometricResult) {}
            override fun onAuthenticationError(result: BiometricResult) {}
            override fun onAuthenticationCancelled() {}
        })

        val success = handler.setupPin(pin)
        callback(success)
    }

    /**
     * Check if PIN is already set up
     */
    fun isPinSetup(): Boolean {
        val handler = PinAuthHandler(activity, config, object : BiometricCallback {
            override fun onAuthenticationSuccess(result: BiometricResult) {}
            override fun onAuthenticationError(result: BiometricResult) {}
            override fun onAuthenticationCancelled() {}
        })
        return handler.isPinSetup()
    }

    /**
     * Clear/reset PIN (for testing or user reset)
     */
    fun clearPin() {
        val handler = PinAuthHandler(activity, config, object : BiometricCallback {
            override fun onAuthenticationSuccess(result: BiometricResult) {}
            override fun onAuthenticationError(result: BiometricResult) {}
            override fun onAuthenticationCancelled() {}
        })
        handler.clearPin()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        biometricPrompt?.cancelAuthentication()
        pinHandler?.dismiss()
        pinHandler = null
        biometricPrompt = null
        currentCallback = null
    }
}