package com.ext.biometric_auth

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Configuration class for customizing biometric authentication dialog.
 * All properties can be set via XML attributes or programmatically.
 */
data class BiometricConfig(
    // Dialog appearance
    @ColorInt val dialogBackgroundColor: Int,
    val dialogTitle: String,
    @ColorInt val dialogTitleColor: Int,
    val dialogSubtitle: String,
    @ColorInt val dialogSubtitleColor: Int,
    val dialogDescription: String,
    @ColorInt val dialogDescriptionColor: Int,

    // Icons
    @DrawableRes val fingerprintIcon: Int,
    @DrawableRes val faceIcon: Int,
    val appIcon: Drawable?,

    // Status colors
    @ColorInt val successColor: Int,
    @ColorInt val errorColor: Int,
    @ColorInt val neutralColor: Int,

    // Button texts and colors
    val cancelButtonText: String,
    @ColorInt val cancelButtonColor: Int,
    val pinButtonText: String,
    @ColorInt val pinButtonColor: Int,

    // Authentication settings
    val enableFingerprint: Boolean,
    val enableFaceAuth: Boolean,
    val enablePinFallback: Boolean,
    val pinLength: Int,
    val maxPinAttempts: Int,

    // Animation settings
    val enableAnimations: Boolean,

    // Timeouts
    val biometricTimeoutSeconds: Int,
    val pinLockoutSeconds: Int
) {

    companion object {
        /**
         * Creates BiometricConfig from XML attributes
         */
        fun fromAttributes(context: Context, attrs: AttributeSet?): BiometricConfig {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BiometricAuthView)

            try {
                return BiometricConfig(
                    dialogBackgroundColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_dialogBackgroundColor,
                        ContextCompat.getColor(context, R.color.default_dialog_background)
                    ),
                    dialogTitle = typedArray.getString(
                        R.styleable.BiometricAuthView_dialogTitle
                    ) ?: context.getString(R.string.default_dialog_title),
                    dialogTitleColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_dialogTitleColor,
                        ContextCompat.getColor(context, R.color.default_title_color)
                    ),
                    dialogSubtitle = typedArray.getString(
                        R.styleable.BiometricAuthView_dialogSubtitle
                    ) ?: context.getString(R.string.default_dialog_subtitle),
                    dialogSubtitleColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_dialogSubtitleColor,
                        ContextCompat.getColor(context, R.color.default_subtitle_color)
                    ),
                    dialogDescription = typedArray.getString(
                        R.styleable.BiometricAuthView_dialogDescription
                    ) ?: context.getString(R.string.default_dialog_description),
                    dialogDescriptionColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_dialogDescriptionColor,
                        ContextCompat.getColor(context, R.color.default_description_color)
                    ),
                    fingerprintIcon = typedArray.getResourceId(
                        R.styleable.BiometricAuthView_fingerprintIcon,
                        R.drawable.ic_fingerprint
                    ),
                    faceIcon = typedArray.getResourceId(
                        R.styleable.BiometricAuthView_faceIcon,
                        R.drawable.ic_face
                    ),
                    appIcon = typedArray.getDrawable(R.styleable.BiometricAuthView_appIcon),
                    successColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_successColor,
                        ContextCompat.getColor(context, R.color.default_success_color)
                    ),
                    errorColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_errorColor,
                        ContextCompat.getColor(context, R.color.default_error_color)
                    ),
                    neutralColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_neutralColor,
                        ContextCompat.getColor(context, R.color.default_neutral_color)
                    ),
                    cancelButtonText = typedArray.getString(
                        R.styleable.BiometricAuthView_cancelButtonText
                    ) ?: context.getString(R.string.default_cancel_button),
                    cancelButtonColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_cancelButtonColor,
                        ContextCompat.getColor(context, R.color.default_button_color)
                    ),
                    pinButtonText = typedArray.getString(
                        R.styleable.BiometricAuthView_pinButtonText
                    ) ?: context.getString(R.string.default_pin_button),
                    pinButtonColor = typedArray.getColor(
                        R.styleable.BiometricAuthView_pinButtonColor,
                        ContextCompat.getColor(context, R.color.default_button_color)
                    ),
                    enableFingerprint = typedArray.getBoolean(
                        R.styleable.BiometricAuthView_enableFingerprint,
                        true
                    ),
                    enableFaceAuth = typedArray.getBoolean(
                        R.styleable.BiometricAuthView_enableFaceAuth,
                        true
                    ),
                    enablePinFallback = typedArray.getBoolean(
                        R.styleable.BiometricAuthView_enablePinFallback,
                        true
                    ),
                    pinLength = typedArray.getInteger(
                        R.styleable.BiometricAuthView_pinLength,
                        4
                    ),
                    maxPinAttempts = typedArray.getInteger(
                        R.styleable.BiometricAuthView_maxPinAttempts,
                        3
                    ),
                    enableAnimations = typedArray.getBoolean(
                        R.styleable.BiometricAuthView_enableAnimations,
                        true
                    ),
                    biometricTimeoutSeconds = typedArray.getInteger(
                        R.styleable.BiometricAuthView_biometricTimeoutSeconds,
                        30
                    ),
                    pinLockoutSeconds = typedArray.getInteger(
                        R.styleable.BiometricAuthView_pinLockoutSeconds,
                        30
                    )
                )
            } finally {
                typedArray.recycle()
            }
        }

        /**
         * Creates default configuration
         */
        fun default(context: Context): BiometricConfig {
            return BiometricConfig(
                dialogBackgroundColor = ContextCompat.getColor(context, R.color.default_dialog_background),
                dialogTitle = context.getString(R.string.default_dialog_title),
                dialogTitleColor = ContextCompat.getColor(context, R.color.default_title_color),
                dialogSubtitle = context.getString(R.string.default_dialog_subtitle),
                dialogSubtitleColor = ContextCompat.getColor(context, R.color.default_subtitle_color),
                dialogDescription = context.getString(R.string.default_dialog_description),
                dialogDescriptionColor = ContextCompat.getColor(context, R.color.default_description_color),
                fingerprintIcon = R.drawable.ic_fingerprint,
                faceIcon = R.drawable.ic_face,
                appIcon = null,
                successColor = ContextCompat.getColor(context, R.color.default_success_color),
                errorColor = ContextCompat.getColor(context, R.color.default_error_color),
                neutralColor = ContextCompat.getColor(context, R.color.default_neutral_color),
                cancelButtonText = context.getString(R.string.default_cancel_button),
                cancelButtonColor = ContextCompat.getColor(context, R.color.default_button_color),
                pinButtonText = context.getString(R.string.default_pin_button),
                pinButtonColor = ContextCompat.getColor(context, R.color.default_button_color),
                enableFingerprint = true,
                enableFaceAuth = true,
                enablePinFallback = true,
                pinLength = 4,
                maxPinAttempts = 3,
                enableAnimations = true,
                biometricTimeoutSeconds = 30,
                pinLockoutSeconds = 30
            )
        }
    }
}

/**
 * Builder for programmatic configuration
 */
class BiometricConfigBuilder(private val context: Context) {
    private var config = BiometricConfig.default(context)

    fun setDialogBackgroundColor(@ColorInt color: Int) = apply {
        config = config.copy(dialogBackgroundColor = color)
    }

    fun setDialogTitle(title: String) = apply {
        config = config.copy(dialogTitle = title)
    }

    fun setDialogTitleColor(@ColorInt color: Int) = apply {
        config = config.copy(dialogTitleColor = color)
    }

    fun setDialogSubtitle(subtitle: String) = apply {
        config = config.copy(dialogSubtitle = subtitle)
    }

    fun setDialogSubtitleColor(@ColorInt color: Int) = apply {
        config = config.copy(dialogSubtitleColor = color)
    }

    fun setDialogDescription(description: String) = apply {
        config = config.copy(dialogDescription = description)
    }

    fun setSuccessColor(@ColorInt color: Int) = apply {
        config = config.copy(successColor = color)
    }

    fun setErrorColor(@ColorInt color: Int) = apply {
        config = config.copy(errorColor = color)
    }

    fun setEnableFingerprint(enable: Boolean) = apply {
        config = config.copy(enableFingerprint = enable)
    }

    fun setEnableFaceAuth(enable: Boolean) = apply {
        config = config.copy(enableFaceAuth = enable)
    }

    fun setEnablePinFallback(enable: Boolean) = apply {
        config = config.copy(enablePinFallback = enable)
    }

    fun setPinLength(length: Int) = apply {
        config = config.copy(pinLength = length)
    }

    fun setMaxPinAttempts(attempts: Int) = apply {
        config = config.copy(maxPinAttempts = attempts)
    }

    fun setEnableAnimations(enable: Boolean) = apply {
        config = config.copy(enableAnimations = enable)
    }

    fun build(): BiometricConfig = config
}