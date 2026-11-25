package com.myco;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.locuslabs.sdk.llpublic.LLDependencyInjector;
import com.locuslabs.sdk.llpublic.LLLocusMapsFragment;
import com.locuslabs.sdk.llpublic.LLOnFailureListener;
import com.locuslabs.sdk.llpublic.LLOnGetVenueListCallback;
import com.locuslabs.sdk.llpublic.LLOnPOIPhoneClickedListener;
import com.locuslabs.sdk.llpublic.LLOnPOIURLClickedListener;
import com.locuslabs.sdk.llpublic.LLOnProgressListener;
import com.locuslabs.sdk.llpublic.LLOnWarningListener;
import com.locuslabs.sdk.llpublic.LLVenueDatabase;
import com.locuslabs.sdk.llpublic.LLVenueList;
import com.locuslabs.sdk.llpublic.LLVenueListEntry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.locuslabs.sdk.llpublic.LLConstantsKt.FRACTION_TO_PERCENT_CONVERSION_RATIO;
import static com.locuslabs.sdk.llpublic.LLConstantsKt.PROGRESS_BAR_FRACTION_FINISH;

public class FullscreenMapFragment extends Fragment {

    private LLLocusMapsFragment llLocusMapsFragment;
    private View initializationAnimationViewBackground;
    private ImageView initializationAnimationView;
    private AnimationDrawable initializationAnimationDrawable;
    private boolean showVenueCalled;
    private FragmentManager.FragmentLifecycleCallbacks mapLifecycleCallbacks;

    // Data APIs can run on a background thread (recommended)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_fullscreen_map, container, false);

        initializationAnimationViewBackground = rootView.findViewById(R.id.initializationAnimationViewBackground);
        initializationAnimationView = rootView.findViewById(R.id.initializationAnimationView);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        SampleConfig.resetToDefaults();
        initLocusMaps();
        initBackHandling();
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    @Override
    public void onDestroyView() {

        // This fragment's view (and the map's view inside it) is destroyed, e.g. when the fragment is replaced and
        // added to the back stack. Reset the flag so showVenue is called again for the new view when the user returns
        if (mapLifecycleCallbacks != null) {
            getChildFragmentManager().unregisterFragmentLifecycleCallbacks(mapLifecycleCallbacks);
            mapLifecycleCallbacks = null;
        }
        showVenueCalled = false;
        SampleConfig.clearListeners();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private void initLocusMaps() {

        llLocusMapsFragment = (LLLocusMapsFragment)getChildFragmentManager().findFragmentById(R.id.llLocusMapsFragment);

        initInitializationProgressIndicator();
        showInitializationProgressIndicator();

        mapLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentStarted(fm, f);

                if (f == llLocusMapsFragment && !showVenueCalled) {

                    showVenueCalled = true;
                    showVenue();
                }
            }
        };
        getChildFragmentManager().registerFragmentLifecycleCallbacks(mapLifecycleCallbacks, false);

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

                mainHandler.post(() -> {
                    if (getView() == null) return; // this fragment's view was destroyed in the meantime
                    llLocusMapsFragment.showVenue(venueID, venueListEntry.getAssetVersion(), venueListEntry.getFiles());
                });
            }

            @Override
            public void failureCallback(Throwable throwable) {

                // Failed to get the venue list
                Log.e("LOG", "Failed to get the venue list", throwable);
            }
        }));
    }

    private void initBackHandling() {

        // Let the map close its own screens (POI view, search, directions...) before the activity handles back.
        // Registered with getViewLifecycleOwner() so the callback is removed when this fragment's view is destroyed
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (llLocusMapsFragment.hasBackStackItems()) {
                    llLocusMapsFragment.popBackStack();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
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

    private void mapReady() {

        // This is the appropriate place to take most actions that affect the map
    }
}
