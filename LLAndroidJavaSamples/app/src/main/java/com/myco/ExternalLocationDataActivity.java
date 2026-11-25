package com.myco;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.locuslabs.sdk.llpublic.LLDependencyInjector;
import com.locuslabs.sdk.llpublic.LLLatLng;
import com.locuslabs.sdk.llpublic.LLLocusMapsFragment;
import com.locuslabs.sdk.llpublic.LLOnFailureListener;
import com.locuslabs.sdk.llpublic.LLOnGetVenueListCallback;
import com.locuslabs.sdk.llpublic.LLOnPOIPhoneClickedListener;
import com.locuslabs.sdk.llpublic.LLOnPOIURLClickedListener;
import com.locuslabs.sdk.llpublic.LLOnProgressListener;
import com.locuslabs.sdk.llpublic.LLOnWarningListener;
import com.locuslabs.sdk.llpublic.LLVenueDatabase;
import com.locuslabs.sdk.llpublic.LLVenueFiles;
import com.locuslabs.sdk.llpublic.LLVenueList;
import com.locuslabs.sdk.llpublic.LLVenueListEntry;
import androidx.activity.OnBackPressedCallback;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.locuslabs.sdk.llpublic.LLConstantsKt.FRACTION_TO_PERCENT_CONVERSION_RATIO;
import static com.locuslabs.sdk.llpublic.LLConstantsKt.PROGRESS_BAR_FRACTION_FINISH;

public class ExternalLocationDataActivity extends AppCompatActivity {

    private LLLocusMapsFragment llLocusMapsFragment;
    private View initializationAnimationViewBackground;
    private ImageView initializationAnimationView;
    private AnimationDrawable initializationAnimationDrawable;
    private boolean showVenueCalled;
    
    // Data APIs can run on a background thread (recommended)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_map);

        SampleConfig.resetToDefaults();

        // Reference views
        initializationAnimationViewBackground = findViewById(R.id.initializationAnimationViewBackground);
        initializationAnimationView = findViewById(R.id.initializationAnimationView);

        initLocusMaps();
        initBackHandling();
        initInitializationProgressIndicator();
        showInitializationProgressIndicator();
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    @Override
    protected void onDestroy() {

        backgroundExecutor.shutdown();
        SampleConfig.clearListeners();
        super.onDestroy();
    }

    private void initLocusMaps() {

        llLocusMapsFragment = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentStarted(fm, f);

                if (f == llLocusMapsFragment && !showVenueCalled) {

                    showVenueCalled = true;
                    showVenue();
                }
            }
        }, false);

        LLDependencyInjector.Companion.getSingleton().setOnInitializationProgressListener(
                new LLOnProgressListener() {
                    @Override
                    public void onProgressUpdate(double fractionComplete, String progressDescription) {

                        // Map Ready
                        if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {

                            hideInitializationProgressIndicator();
                            mapReady();
                        }
                    }
                }
        );

        LLDependencyInjector.Companion.getSingleton().setOnLevelLoadingProgressListener(
                new LLOnProgressListener() {
                    @Override
                    public void onProgressUpdate(double fractionComplete, String progressDescription) {
                        updateLevelLoadingProgressIndicator(fractionComplete, progressDescription);
                    }
                }
        );

        LLDependencyInjector.Companion.getSingleton().setOnPOIURLClickedListener(
                new LLOnPOIURLClickedListener() {
                    @Override
                    public void onPOIURLClicked(String url) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                }
        );

        LLDependencyInjector.Companion.getSingleton().setOnPOIPhoneClickedListener(
                new LLOnPOIPhoneClickedListener() {
                    @Override
                    public void onPOIPhoneClicked(String phone) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    }
                }
        );

        LLDependencyInjector.Companion.getSingleton().setOnWarningListener(new LLOnWarningListener() {
            @Override
            public void onWarning(Throwable throwable) {

                // SDK warnings are sent here
            }
        });

        LLDependencyInjector.Companion.getSingleton().setOnFailureListener(new LLOnFailureListener() {
            @Override
            public void onFailure(Throwable throwable) {

                // SDK fatal errors are sent here
                Log.e("LOG", "stack trace: " + Log.getStackTraceString(throwable));
                Log.e("LOG", "stack trace cause: " + Log.getStackTraceString(throwable.getCause()));
            }
        });
    }

    private void showVenue() {

        String venueID = "lax";

        // Get the venue list on a background thread (recommended), then show the venue on the main thread
        backgroundExecutor.execute(() -> new LLVenueDatabase().getVenueList(new LLOnGetVenueListCallback() {
            @Override
            public void successCallback(LLVenueList llVenueList) {

                LLVenueListEntry venueListEntry = llVenueList.get(venueID);
                if (venueListEntry == null)  {

                    // The venue isn't available for this account
                    return;
                }

                runOnUiThread(() -> llLocusMapsFragment.showVenue(venueID, venueListEntry.getAssetVersion(), venueListEntry.getFiles()));
            }

            @Override
            public void failureCallback(Throwable throwable) {

                // Failed to get the venue list
                Log.e("LOG", "Failed to get the venue list", throwable);
            }
        }));
    }

    private void initInitializationProgressIndicator() {

        initializationAnimationView.setBackgroundResource(R.drawable.ll_navigation_loading_animation);
        initializationAnimationDrawable = (AnimationDrawable) initializationAnimationView.getBackground();
        initializationAnimationDrawable.start();
        initializationAnimationDrawable.setVisible(false, false);
    }

    private void showInitializationProgressIndicator() {

        initializationAnimationViewBackground.setVisibility(View.VISIBLE);
        initializationAnimationView.setVisibility(View.VISIBLE);
        initializationAnimationDrawable.setVisible(true, true);
    }

    private void hideInitializationProgressIndicator() {

        initializationAnimationViewBackground.setVisibility(View.GONE);
        initializationAnimationView.setVisibility(View.GONE);
        initializationAnimationDrawable.setVisible(false, false);
    }

    private void updateLevelLoadingProgressIndicator(double fractionComplete, String progressDescription) {

        // Use this section to implement a loading progress indicator if desired
        int percentComplete = (int) (fractionComplete * FRACTION_TO_PERCENT_CONVERSION_RATIO);

        Log.d("LOG", "LocusMaps Android SDK Level Loading Progress: " + percentComplete +"\t" + progressDescription);

        if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {

            // Load complete
        }
    }

    private void initBackHandling() {

        // Let the map close its own screens (POI view, search, directions...) before leaving this screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (llLocusMapsFragment.hasBackStackItems()) {
                    llLocusMapsFragment.popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void mapReady() {

        // Start mocking external location
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                mockExternalLocationData();
            }
        }, 5000);
    }

    private void externalLocationUpdateReceived(double lat, double lon, String floorID, double accuracy, double heading) {

        LLLatLng latLng = new LLLatLng(lat, lon);
        String locusLabsFloorID = locusLabsFloorIDForExternalFloorID(floorID);

        // Note: accuracy and heading are not used. In SDK 4.2.8 setCurrentLocation takes only a position and a
        // Wayfinder level ID (an overload taking horizontalAccuracyMeters is coming in 4.2.9)
        llLocusMapsFragment.setCurrentLocation(latLng, locusLabsFloorID);
    }

    private String locusLabsFloorIDForExternalFloorID(String floorID) {

        // If you are not able to compile this mapping table yourself, please send us a list (help@atrius.com) of
        // the building names and associated floor ids as provided by your external mapping provider and we will compile the mapping table

        String locusLabsFloorID = null;

        if (floorID.equals("T48L3")) locusLabsFloorID = "lax-terminal6-departures";

        return (locusLabsFloorID != null ? locusLabsFloorID : "");
    }

    private void hideBlueDot() {

        llLocusMapsFragment.setCurrentLocation(null, null);
    }

    private void mockExternalLocationData() {

        // Position 1 (Initial - DFS Duty Free)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                externalLocationUpdateReceived(33.941485, -118.40195, "T48L3", 0, 0);
            }
        }, 1000);

        // Position 2 (2 secs later)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                externalLocationUpdateReceived(33.941398, -118.401916, "T48L3", 0, 0);
            }
        }, 3000);

        // Position 3 (4 secs later)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                externalLocationUpdateReceived(33.941283, -118.401863, "T48L3", 0, 0);
            }
        }, 5000);

        // Position 4 (6 secs later)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                externalLocationUpdateReceived(33.941102, -118.401902, "T48L3", 0, 0);
            }
        }, 7000);

        // Position 5 (8 secs later - Destination - Gate 64B)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                externalLocationUpdateReceived(33.940908, -118.40177, "T48L3", 0, 0);
            }
        }, 9000);
    }
}