package com.myco

import com.locuslabs.sdk.llpublic.LLConfiguration
import com.locuslabs.sdk.llpublic.LLDependencyInjector

object SampleConfig {

    // LLConfiguration is a process-wide singleton shared by every sample, so each sample starts from the
    // SDK defaults (4.2.8) for every setting any sample changes, then applies its own settings
    fun resetToDefaults() {
        val config = LLConfiguration.singleton
        config.hideMapControls = false
        config.hideDirectionsSummaryControls = false
        config.hideDirectionsSummaryAccessibilityControls = false
        config.preventInitialPanAndZoom = false
        config.llUIThemeDefaultFilePathRelativeToAssetsDir = "locuslabs/lluithemes/default.json"
        config.customActionsFilePathRelativeToAssetsDir = "locuslabs/llplaces/defaultLLCustomActions.json"
    }

    // LLDependencyInjector is a process-wide singleton, so listeners left on it would keep a destroyed
    // Activity or Fragment alive and receive another sample's events. Clear them when the sample goes away
    fun clearListeners() {
        val injector = LLDependencyInjector.singleton
        injector.onInitializationProgressListener = null
        injector.onLevelLoadingProgressListener = null
        injector.onPOIURLClickedListener = null
        injector.onPOIPhoneClickedListener = null
        injector.onWarningListener = null
        injector.onFailureListener = null
        injector.poiExtraButtonHandler = null
        injector.customActionsProvider = null
    }
}
