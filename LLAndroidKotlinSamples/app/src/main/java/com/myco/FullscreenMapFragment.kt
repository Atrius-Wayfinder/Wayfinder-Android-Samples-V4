package com.myco

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.locuslabs.sdk.llpublic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenMapFragment : Fragment() {

    private lateinit var llLocusMapsFragment: LLLocusMapsFragment
    private lateinit var initializationAnimationViewBackground: View
    private lateinit var initializationAnimationView: ImageView
    private lateinit var initializationAnimationDrawable: AnimationDrawable
    private var showVenueCalled = false
    private var mapLifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fullscreen_map, container, false)

        // Reference views
        initializationAnimationViewBackground = rootView.findViewById(R.id.initializationAnimationViewBackground)
        initializationAnimationView = rootView.findViewById(R.id.initializationAnimationView)

        return rootView
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        SampleConfig.resetToDefaults()
        initLocusMaps()
        initBackHandling()
    }

    override fun onDestroyView() {

        // This fragment's view (and the map's view inside it) is destroyed, e.g. when the fragment is replaced and
        // added to the back stack. Reset the flag so showVenue is called again for the new view when the user returns
        mapLifecycleCallbacks?.let { childFragmentManager.unregisterFragmentLifecycleCallbacks(it) }
        mapLifecycleCallbacks = null
        showVenueCalled = false
        SampleConfig.clearListeners()
        super.onDestroyView()
    }

    fun initLocusMaps() {

        llLocusMapsFragment = childFragmentManager.findFragmentById(R.id.llLocusMapsFragment) as LLLocusMapsFragment

        initInitializationProgressIndicator()
        showInitializationProgressIndicator()

        val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)

                if (f == llLocusMapsFragment && !showVenueCalled) {

                    showVenueCalled = true
                    showVenue()
                }
            }
        }
        mapLifecycleCallbacks = callbacks
        childFragmentManager.registerFragmentLifecycleCallbacks(callbacks, false)

        LLDependencyInjector.singleton.onInitializationProgressListener = object : LLOnProgressListener {
            override fun onProgressUpdate(fractionComplete: Double, progressDescription: String) {
                if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {

                    hideInitializationProgressIndicator()
                    mapReady()
                }
            }
        }

        LLDependencyInjector.singleton.onLevelLoadingProgressListener = object : LLOnProgressListener {
            override fun onProgressUpdate(fractionComplete: Double, progressDescription: String) {
                updateLevelLoadingProgressIndicator(fractionComplete, progressDescription)
            }
        }

        LLDependencyInjector.singleton.onPOIURLClickedListener = object : LLOnPOIURLClickedListener {
            override fun onPOIURLClicked(url: String) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                this@FullscreenMapFragment.startActivity(intent)
            }
        }

        LLDependencyInjector.singleton.onPOIPhoneClickedListener = object : LLOnPOIPhoneClickedListener {
            override fun onPOIPhoneClicked(phone: String) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                this@FullscreenMapFragment.startActivity(intent)
            }
        }

        LLDependencyInjector.singleton.onWarningListener = object : LLOnWarningListener {

            override fun onWarning(throwable: Throwable) {

                // SDK warnings are sent here
            }
        }

        LLDependencyInjector.singleton.onFailureListener = object : LLOnFailureListener {
            override fun onFailure(throwable: Throwable) {

                Log.e("LOG", "stack trace: ${Log.getStackTraceString(throwable)}")
                Log.e("LOG", "stack trace cause: ${Log.getStackTraceString(throwable.cause)}")
            }
        }
    }

    private fun showVenue() {

        val venueID = "lax"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get the venue list on a background thread (recommended), then show the venue on the main thread
                val venueList = withContext(Dispatchers.IO) { LLVenueDatabase().getVenueList() }

                val venueListEntry = venueList[venueID]
                    ?: return@launch // The venue isn't available for this account

                llLocusMapsFragment.showVenue(venueID, venueListEntry.assetVersion, venueListEntry.files)
            } catch (throwable: Throwable) {

                // Failed to get the venue list
                Log.e("LOG", "Failed to get the venue list", throwable)
            }
        }
    }

    private fun initInitializationProgressIndicator() {

        initializationAnimationView.setBackgroundResource(R.drawable.ll_navigation_loading_animation)
        initializationAnimationDrawable = initializationAnimationView.background as AnimationDrawable
        initializationAnimationDrawable.start()
        initializationAnimationDrawable.setVisible(false, false)
    }

    private fun showInitializationProgressIndicator() {

        initializationAnimationViewBackground.visibility = View.VISIBLE
        initializationAnimationView.visibility = View.VISIBLE
        initializationAnimationDrawable.setVisible(true, true)
    }

    private fun hideInitializationProgressIndicator() {

        initializationAnimationViewBackground.visibility = View.GONE
        initializationAnimationView.visibility = View.GONE
        initializationAnimationDrawable.setVisible(false, false)
    }

    private fun updateLevelLoadingProgressIndicator(fractionComplete: Double, progressDescription: String) {

        // Use this section to implement a loading progress indicator if desired
        val percentComplete = (fractionComplete * FRACTION_TO_PERCENT_CONVERSION_RATIO).toInt()
        Log.d(
                "LOG",
                "LocusMaps Android SDK Loading Progress: ${percentComplete}%\t${progressDescription}"
        )

        if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {
            // Load complete
        }
    }

    private fun initBackHandling() {

        // Let the map close its own screens (POI view, search, directions...) before the activity handles back.
        // Registered with viewLifecycleOwner so the callback is removed when this fragment's view is destroyed
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (llLocusMapsFragment.hasBackStackItems()) {
                llLocusMapsFragment.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    private fun mapReady() {

    }
}