package com.myco

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.locuslabs.sdk.llpublic.*
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectionsStepsETAActivity : AppCompatActivity() {

    private lateinit var llLocusMapsFragment: LLLocusMapsFragment
    private lateinit var initializationAnimationViewBackground: View
    private lateinit var initializationAnimationView: ImageView
    private lateinit var initializationAnimationDrawable: AnimationDrawable
    private var showVenueCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fullscreen_map)

        SampleConfig.resetToDefaults()

        // Reference views
        initializationAnimationViewBackground = findViewById(R.id.initializationAnimationViewBackground)
        initializationAnimationView = findViewById(R.id.initializationAnimationView)

        initLocusMaps()
        initBackHandling()
        initInitializationProgressIndicator()
        showInitializationProgressIndicator()
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    override fun onDestroy() {

        SampleConfig.clearListeners()
        super.onDestroy()
    }

    private fun initLocusMaps() {

        llLocusMapsFragment = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment) as LLLocusMapsFragment

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
                FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)

                if (f == llLocusMapsFragment && !showVenueCalled) {

                    showVenueCalled = true
                    showVenue()
                }
            }

        }, false)

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
                this@DirectionsStepsETAActivity.startActivity(intent)
            }
        }

        LLDependencyInjector.singleton.onPOIPhoneClickedListener = object : LLOnPOIPhoneClickedListener {
            override fun onPOIPhoneClicked(phone: String) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                this@DirectionsStepsETAActivity.startActivity(intent)
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

        lifecycleScope.launch {
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

        // Let the map close its own screens (POI view, search, directions...) before leaving this screen
        onBackPressedDispatcher.addCallback(this) {
            if (llLocusMapsFragment.hasBackStackItems()) {
                llLocusMapsFragment.popBackStack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    private fun mapReady() {

        val venueID = "lax"
        val accessibilityType = LLNavAccessibilityType.Direct
        val securityQueueTypes = mutableMapOf<String, List<String>>()

        // Optional - specify a security lane
        // val laneTypes = ArrayList<String>()
        // laneTypes.add("general") // other options include, tsapre or clear
        // securityQueueTypes["SecurityLane"] = laneTypes

        // Points can also be LLNavigationPointForCurrentLocation or any other LLNavigationPoint
        // Note passing only a single point in waypoints is equivalent to a traditional single origin and single destination route
        val startPoint = LLNavigationPointForPOI("1025") // Club 42 Tequila Bar & Bites
        val waypoints = listOf(
            LLNavigationPointForPOI("870"), // Starbucks near Gate 60
            LLNavigationPointForPOI("566")  // Gate 77
        )

        // Fetch the route to report its steps and ETA. Data APIs can run on a background thread (recommended),
        // then UI work is posted back to the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            LLNavigationDatabase().getMultipointDirections(venueID, startPoint, waypoints, accessibilityType, securityQueueTypes, object : LLOnGetDirectionsCallback {
                override fun successCallback(navPath: LLNavPath) {
                    val steps = navPath.segments(Locale.getDefault()).joinToString("\n\n") { segment ->
                        "%s %s (%.0f m, %.1f min)".format(segment.instruction, segment.destination, segment.distanceMeters, segment.estimatedTimeMinutes)
                    }
                    runOnUiThread { showDialog("Obtained Directions", "ETA: %.1f min\n\nSteps:\n\n%s".format(navPath.transitTime(), steps)) }
                }

                override fun failureCallback(throwable: Throwable) {
                    runOnUiThread { showDialog("Directions Error", throwable.localizedMessage.orEmpty()) }
                }
            })
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}