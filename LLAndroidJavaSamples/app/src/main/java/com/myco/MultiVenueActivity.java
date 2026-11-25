package com.myco;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.locuslabs.sdk.llpublic.LLDependencyInjector;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.locuslabs.sdk.llpublic.LLConstantsKt.FRACTION_TO_PERCENT_CONVERSION_RATIO;
import static com.locuslabs.sdk.llpublic.LLConstantsKt.PROGRESS_BAR_FRACTION_FINISH;

public class MultiVenueActivity extends AppCompatActivity {
    private RecyclerView venueListRecyclerView;
    private LLLocusMapsFragment llLocusMapsFragment;
    private View initializationAnimationViewBackground;
    private ImageView initializationAnimationView;
    private AnimationDrawable initializationAnimationDrawable;
    private boolean mapFragmentReady;
    private ConstraintLayout mapContainer;

    // Data APIs can run on a background thread (recommended)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_venue);

        SampleConfig.resetToDefaults();

        mapContainer = findViewById(R.id.map_container);

        venueListRecyclerView = findViewById(R.id.recycler_view);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                RecyclerView.VERTICAL);
        venueListRecyclerView.addItemDecoration(dividerItemDecoration);

        getVenueList();
        initLocusMaps();
        initBackHandling();
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    @Override
    protected void onDestroy() {

        backgroundExecutor.shutdown();
        SampleConfig.clearListeners();
        super.onDestroy();
    }

    private void getVenueList() {

        // Get the venue list on a background thread (recommended), then update the UI on the main thread
        backgroundExecutor.execute(() -> new LLVenueDatabase().getVenueList(new LLOnGetVenueListCallback() {
            @Override
            public void successCallback(LLVenueList llVenueList) {

                ArrayList<LLVenueListEntry> venueListEntries = new ArrayList<>(llVenueList.values());

                runOnUiThread(() -> venueListRecyclerView.setAdapter(new VenueListAdapter(venueListEntries)));
            }

            @Override
            public void failureCallback(Throwable throwable) {

                // Failed to get the venue list
                Log.d("Error", "Failed to get venues:" +throwable.getLocalizedMessage());
            }
        }));
    }

    private void initLocusMaps() {

        initializationAnimationViewBackground = findViewById(R.id.initializationAnimationViewBackground);
        initializationAnimationView = findViewById(R.id.initializationAnimationView);
        initInitializationProgressIndicator();

        llLocusMapsFragment = (LLLocusMapsFragment) getSupportFragmentManager().findFragmentById(R.id.llLocusMapsFragment);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentStarted(fm, f);

                if (f == llLocusMapsFragment && !mapFragmentReady) {

                    mapFragmentReady = true;
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

    private void showVenue(LLVenueListEntry venueListEntry) {

        if (!mapFragmentReady) return;

        showInitializationProgressIndicator();
        mapContainer.setVisibility(View.VISIBLE);

        String llVenueAssetVersion = venueListEntry.getAssetVersion();
        LLVenueFiles llVenueFiles = venueListEntry.getFiles();

        llLocusMapsFragment.showVenue(venueListEntry.getId(), llVenueAssetVersion, llVenueFiles);
    }

    private void mapReady() {

        // This is the appropriate place to take most actions that affect the map
    }

    private void initBackHandling() {

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (llLocusMapsFragment.hasBackStackItems()) {

                    // Let the map close its own screens (POI view, search, directions...) first
                    llLocusMapsFragment.popBackStack();
                }
                else if (mapContainer.getVisibility() == View.VISIBLE) {

                    // Then go back from the map to the venue list
                    getVenueList();
                    mapContainer.setVisibility(View.INVISIBLE);
                }
                else {

                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private class VenueListAdapter extends RecyclerView.Adapter<VenueListAdapter.ViewHolder> {

        private ArrayList<LLVenueListEntry> venueListEntries;

        public class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView textView;

            public ViewHolder(View view) {

                super(view);
                textView = (TextView) view.findViewById(R.id.title_text_view);
            }

            public TextView getTextView() {

                return textView;
            }
        }

        public VenueListAdapter(ArrayList<LLVenueListEntry> venueListEntries) {

            this.venueListEntries = venueListEntries;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.sample_row, viewGroup, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {

            LLVenueListEntry venueListEntry = venueListEntries.get(position);
            viewHolder.getTextView().setText(venueListEntry.getName());

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    LLVenueListEntry venueListEntry = venueListEntries.get(position);
                    showVenue(venueListEntry);
                }
            });

        }

        @Override
        public int getItemCount() {

            return venueListEntries.size();
        }
    }
}
