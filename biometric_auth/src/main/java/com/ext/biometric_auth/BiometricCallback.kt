package com.ext.biometric_auth

/**
 * Callback interface for biometric authentication results.
 * Implement this interface to receive authentication results in your activity/fragment.
 */
interface BiometricCallback {

    /**
     * Called when authentication succeeds
     * @param result The success result (biometric or PIN)
     */
    fun onAuthenticationSuccess(result: BiometricResult)

    /**
     * Called when authentication fails
     * @param result The failure result with error details
     */
    fun onAuthenticationError(result: BiometricResult)

    /**
     * Called when user cancels authentication
     */
    fun onAuthenticationCancelled()

    /**
     * Called when authentication help message needs to be shown
     * @param helpMessage Message to guide the user (e.g., "Sensor dirty, clean it")
     */
    fun onAuthenticationHelp(helpMessage: String) {
        // Optional override - default implementation does nothing
    }
}

/**
 * Lambda-based callback builder for more concise usage
 */
class BiometricCallbackBuilder {
    private var onSuccess: ((BiometricResult) -> Unit)? = null
    private var onError: ((BiometricResult) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    private var onHelp: ((String) -> Unit)? = null

    fun onSuccess(callback: (BiometricResult) -> Unit) {
        onSuccess = callback
    }

    fun onError(callback: (BiometricResult) -> Unit) {
        onError = callback
    }

    fun onCancel(callback: () -> Unit) {
        onCancel = callback
    }

    fun onHelp(callback: (String) -> Unit) {
        onHelp = callback
    }

    internal fun build(): BiometricCallback {
        return object : BiometricCallback {
            override fun onAuthenticationSuccess(result: BiometricResult) {
                onSuccess?.invoke(result)
            }

            override fun onAuthenticationError(result: BiometricResult) {
                onError?.invoke(result)
            }

            override fun onAuthenticationCancelled() {
                onCancel?.invoke()
            }

            override fun onAuthenticationHelp(helpMessage: String) {
                onHelp?.invoke(helpMessage)
            }
        }
    }
}

/**
 * DSL function to create callbacks using lambda syntax
 */
fun biometricCallback(builder: BiometricCallbackBuilder.() -> Unit): BiometricCallback {
    return BiometricCallbackBuilder().apply(builder).build()
}