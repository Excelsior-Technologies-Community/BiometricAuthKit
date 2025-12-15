package com.ext.biometric_auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*

/**
 * Utility class to check biometric capabilities of the device.
 * Determines what authentication methods are available and why.
 */
class BiometricCapabilityChecker(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Represents the capability status of biometric authentication
     */
    sealed class BiometricCapability {
        object Available : BiometricCapability()
        object NotAvailable : BiometricCapability()
        object NotEnrolled : BiometricCapability()
        object HardwareNotPresent : BiometricCapability()
        object SecurityUpdateRequired : BiometricCapability()
        data class Unknown(val errorCode: Int) : BiometricCapability()
    }

    /**
     * Check if biometric authentication is available
     * @return BiometricCapability indicating the status
     */
    fun checkBiometricCapability(): BiometricCapability {
        // Check for strong biometric authentication (Class 3)
        val result = biometricManager.canAuthenticate(BIOMETRIC_STRONG)

        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricCapability.HardwareNotPresent
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricCapability.NotAvailable
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricCapability.NotEnrolled
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricCapability.SecurityUpdateRequired
            }
            else -> {
                BiometricCapability.Unknown(result)
            }
        }
    }

    /**
     * Check if fingerprint is available
     */
    fun isFingerprintAvailable(): Boolean {
        return checkBiometricCapability() is BiometricCapability.Available
    }

    /**
     * Check if face authentication is available
     * Note: Face unlock uses the same BiometricPrompt API
     */
    fun isFaceAuthAvailable(): Boolean {
        return checkBiometricCapability() is BiometricCapability.Available
    }

    /**
     * Check if any biometric is enrolled
     */
    fun hasBiometricEnrolled(): Boolean {
        val capability = checkBiometricCapability()
        return capability is BiometricCapability.Available
    }

    /**
     * Get human-readable message for capability status
     */
    fun getCapabilityMessage(capability: BiometricCapability): String {
        return when (capability) {
            is BiometricCapability.Available -> {
                "Biometric authentication is available"
            }
            is BiometricCapability.NotAvailable -> {
                "Biometric hardware is temporarily unavailable"
            }
            is BiometricCapability.NotEnrolled -> {
                "No biometric credentials enrolled. Please set up fingerprint or face unlock in device settings"
            }
            is BiometricCapability.HardwareNotPresent -> {
                "This device does not support biometric authentication"
            }
            is BiometricCapability.SecurityUpdateRequired -> {
                "Security update required for biometric authentication"
            }
            is BiometricCapability.Unknown -> {
                "Biometric status unknown (Error code: ${capability.errorCode})"
            }
        }
    }

    /**
     * Check if device supports Class 3 (Strong) biometric
     */
    fun supportsStrongBiometric(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    /**
     * Check if device supports weak biometric (convenience)
     */
    fun supportsWeakBiometric(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            false
        }
    }

    /**
     * Get recommended authentication approach based on device capabilities
     */
    fun getRecommendedAuthenticator(): Int {
        return when {
            supportsStrongBiometric() -> BIOMETRIC_STRONG
            supportsWeakBiometric() -> BIOMETRIC_WEAK
            else -> DEVICE_CREDENTIAL // Fallback to PIN/Pattern/Password
        }
    }

    /**
     * Check if device supports device credentials (PIN, Pattern, Password)
     */
    fun supportsDeviceCredential(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            true // Assume most devices have some form of device credential
        }
    }
}

/**
 * Extension function for easy capability checking
 */
fun Context.getBiometricCapability(): BiometricCapabilityChecker.BiometricCapability {
    return BiometricCapabilityChecker(this).checkBiometricCapability()
}