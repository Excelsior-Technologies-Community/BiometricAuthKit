package com.ext.biometric_auth

/**
 * Sealed class representing all possible biometric authentication results.
 * This provides type-safe result handling for the library consumers.
 */
sealed class BiometricResult {

    /**
     * Authentication succeeded using biometric (fingerprint or face)
     * @param authenticationType Type of biometric used ("FINGERPRINT" or "FACE")
     */
    data class AuthenticationSucceeded(
        val authenticationType: String
    ) : BiometricResult()

    /**
     * Authentication succeeded using PIN fallback
     * @param pin The entered PIN (for verification if needed)
     */
    data class PinAuthenticationSucceeded(
        val pin: String
    ) : BiometricResult()

    /**
     * Authentication failed with an error
     * @param errorCode Error code from BiometricPrompt
     * @param errorMessage Human-readable error message
     */
    data class AuthenticationFailed(
        val errorCode: Int,
        val errorMessage: String
    ) : BiometricResult()

    /**
     * User cancelled the authentication
     */
    object AuthenticationCancelled : BiometricResult()

    /**
     * Biometric authentication is not available on this device
     * @param reason Specific reason why it's not available
     */
    data class BiometricNotAvailable(
        val reason: String
    ) : BiometricResult()

    /**
     * Biometric sensor is temporarily locked due to too many attempts
     * @param lockoutDurationSeconds How long the lockout will last
     */
    data class BiometricLockout(
        val lockoutDurationSeconds: Int
    ) : BiometricResult()

    /**
     * User has not enrolled any biometrics on the device
     */
    object BiometricNotEnrolled : BiometricResult()

    /**
     * PIN authentication failed
     * @param attemptsRemaining Number of attempts left before lockout
     * @param message Error message to show user
     */
    data class PinAuthenticationFailed(
        val attemptsRemaining: Int,
        val message: String
    ) : BiometricResult()
}

/**
 * Error codes for biometric authentication failures
 */
object BiometricErrorCodes {
    const val ERROR_CANCELED = 5
    const val ERROR_USER_CANCELED = 10
    const val ERROR_LOCKOUT = 7
    const val ERROR_LOCKOUT_PERMANENT = 9
    const val ERROR_NO_BIOMETRICS = 11
    const val ERROR_HW_NOT_PRESENT = 12
    const val ERROR_HW_UNAVAILABLE = 1
    const val ERROR_UNABLE_TO_PROCESS = 2
    const val ERROR_TIMEOUT = 3
    const val ERROR_NO_SPACE = 4
    const val ERROR_VENDOR = 8
    const val ERROR_NEGATIVE_BUTTON = 13
}