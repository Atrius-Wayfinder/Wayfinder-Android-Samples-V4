package com.myco;

import com.locuslabs.sdk.llpublic.LLConfiguration;
import com.locuslabs.sdk.llpublic.LLDependencyInjector;

public final class SampleConfig {

    private SampleConfig() {
    }

    // LLConfiguration is a process-wide singleton shared by every sample, so each sample starts from the
    // SDK defaults (4.2.8) for every setting any sample changes, then applies its own settings
    public static void resetToDefaults() {
        LLConfiguration config = LLConfiguration.Companion.getSingleton();
        config.setHideMapControls(false);
        config.setHideDirectionsSummaryControls(false);
        config.setHideDirectionsSummaryAccessibilityControls(false);
        config.setPreventInitialPanAndZoom(false);
        config.setLlUIThemeDefaultFilePathRelativeToAssetsDir("locuslabs/lluithemes/default.json");
        config.setCustomActionsFilePathRelativeToAssetsDir("locuslabs/llplaces/defaultLLCustomActions.json");
    }

    // LLDependencyInjector is a process-wide singleton, so listeners left on it would keep a destroyed
    // Activity or Fragment alive and receive another sample's events. Clear them when the sample goes away
    public static void clearListeners() {
        LLDependencyInjector injector = LLDependencyInjector.Companion.getSingleton();
        injector.setOnInitializationProgressListener(null);
        injector.setOnLevelLoadingProgressListener(null);
        injector.setOnPOIURLClickedListener(null);
        injector.setOnPOIPhoneClickedListener(null);
        injector.setOnWarningListener(null);
        injector.setOnFailureListener(null);
        injector.setPoiExtraButtonHandler(null);
        injector.setCustomActionsProvider(null);
    }
}
