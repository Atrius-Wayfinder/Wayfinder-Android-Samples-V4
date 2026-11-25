package com.myco;

import static com.locuslabs.sdk.llpublic.LLConstantsKt.PROGRESS_BAR_FRACTION_FINISH;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.locuslabs.sdk.llpublic.LLConfiguration;
import com.locuslabs.sdk.llpublic.LLDependencyInjector;
import com.locuslabs.sdk.llpublic.LLLocusMapsFragment;
import com.locuslabs.sdk.llpublic.LLNavAccessibilityType;
import com.locuslabs.sdk.llpublic.LLNavigationPoint;
import com.locuslabs.sdk.llpublic.LLNavigationPointForPOI;
import com.locuslabs.sdk.llpublic.LLOnGetVenueListCallback;
import com.locuslabs.sdk.llpublic.LLOnProgressListener;
import com.locuslabs.sdk.llpublic.LLVenueDatabase;
import com.locuslabs.sdk.llpublic.LLVenueList;
import com.locuslabs.sdk.llpublic.LLVenueListEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultipleEmbeddedMapsActivity extends AppCompatActivity {

    private LLLocusMapsFragment llLocusMapsFragment1;
    private LLLocusMapsFragment llLocusMapsFragment2;
    private LLLocusMapsFragment llLocusMapsFragment3;
    private LLLocusMapsFragment llLocusMapsFragment4;
    private LLLocusMapsFragment llLocusMapsFragment5;

    private final Set<LLLocusMapsFragment> mapFragmentsShown = new HashSet<>();

    private String venueID = "lax";

    private int loadCount = 0;

    // Data APIs can run on a background thread (recommended)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiple_embedded_maps);

        SampleConfig.resetToDefaults();

        LLConfiguration.Companion.getSingleton().setHideMapControls(true);
        LLConfiguration.Companion.getSingleton().setHideDirectionsSummaryControls(true);
        LLConfiguration.Companion.getSingleton().setHideDirectionsSummaryAccessibilityControls(true);

        llLocusMapsFragment1 = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment1);
        llLocusMapsFragment2 = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment2);
        llLocusMapsFragment3 = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment3);
        llLocusMapsFragment4 = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment4);
        llLocusMapsFragment5 = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment5);

        initLocusMaps();
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to the LLLocusMapsFragments, each fragment handles them itself

    @Override
    protected void onDestroy() {

        backgroundExecutor.shutdown();
        SampleConfig.clearListeners();
        super.onDestroy();
    }

    private void initLocusMaps() {

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentStarted(fm, f);

                // Show the venue in each map once its fragment has started
                if (f instanceof LLLocusMapsFragment && mapFragmentsShown.add((LLLocusMapsFragment) f)) {

                    showVenue((LLLocusMapsFragment) f);
                }
            }
        }, false);

        LLDependencyInjector.Companion.getSingleton().setOnInitializationProgressListener(
                new LLOnProgressListener() {
                    @Override
                    public void onProgressUpdate(double fractionComplete, String progressDescription) {

                        // Map Ready
                        if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {

                           // The listener is shared by all maps, so count the maps that finished loading
                           loadCount++;
                           if (loadCount == 5) { // 5 is the number of embedded maps

                               mapsReady();
                           }
                        }
                    }
                }
        );
    }

    private void showVenue(LLLocusMapsFragment llLocusMapsFragment) {

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

    private void mapsReady() {

        Map<String, List<String>> securityQueueTypes = new HashMap<>();

        LLNavigationPoint startPoint =  new LLNavigationPointForPOI("1025");
        LLNavigationPoint endPoint =  new LLNavigationPointForPOI("566");

        llLocusMapsFragment1.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes);
        llLocusMapsFragment2.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes);
        llLocusMapsFragment3.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes);
        llLocusMapsFragment4.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes);
        llLocusMapsFragment5.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes);
    }
}
