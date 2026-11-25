package com.myco

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.locuslabs.sdk.llpublic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MultipleEmbeddedMapsActivity : AppCompatActivity() {

    private var llLocusMapsFragment1: LLLocusMapsFragment? = null
    private var llLocusMapsFragment2: LLLocusMapsFragment? = null
    private var llLocusMapsFragment3: LLLocusMapsFragment? = null
    private var llLocusMapsFragment4: LLLocusMapsFragment? = null
    private var llLocusMapsFragment5: LLLocusMapsFragment? = null

    private val mapFragmentsShown = mutableSetOf<LLLocusMapsFragment>()

    private var venueID = "lax"
    private var loadCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.multiple_embedded_maps)

        SampleConfig.resetToDefaults()

        LLConfiguration.singleton.hideMapControls = true
        LLConfiguration.singleton.hideDirectionsSummaryControls = true
        LLConfiguration.singleton.hideDirectionsSummaryAccessibilityControls = true

        llLocusMapsFragment1 = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment1) as LLLocusMapsFragment?
        llLocusMapsFragment2 = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment2) as LLLocusMapsFragment?
        llLocusMapsFragment3 = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment3) as LLLocusMapsFragment?
        llLocusMapsFragment4 = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment4) as LLLocusMapsFragment?
        llLocusMapsFragment5 = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment5) as LLLocusMapsFragment?

        initLocusMaps()
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to the LLLocusMapsFragments, each fragment handles them itself

    override fun onDestroy() {

        SampleConfig.clearListeners()
        super.onDestroy()
    }

    private fun initLocusMaps() {

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)

                // Show the venue in each map once its fragment has started
                if (f is LLLocusMapsFragment && mapFragmentsShown.add(f)) {

                    showVenue(f)
                }
            }
        }, false)

        LLDependencyInjector.singleton.onInitializationProgressListener = object :
            LLOnProgressListener {
            override fun onProgressUpdate(fractionComplete: Double, progressDescription: String) {
                if (PROGRESS_BAR_FRACTION_FINISH == fractionComplete) {

                    // The listener is shared by all maps, so count the maps that finished loading
                    loadCount++

                    if (loadCount == 5) { // 5 is the number of embedded maps

                        mapsReady()
                    }
                }
            }
        }
    }

    private fun showVenue(llLocusMapsFragment: LLLocusMapsFragment) {

        lifecycleScope.launch {
            try {
                // Get the venue list on a background thread (recommended), then show the venue on the main thread
                val venueList = withContext(Dispatchers.IO) { LLVenueDatabase().getVenueList() }

                val venueListEntry = venueList[venueID]
                if (venueListEntry == null) {
                    Log.e("error", "venueListEntry null")
                    return@launch
                }

                llLocusMapsFragment.showVenue(venueID, venueListEntry.assetVersion, venueListEntry.files)
            } catch (throwable: Throwable) {
                Log.d("error", "getVenueList failed $throwable")
            }
        }
    }

    private fun mapsReady() {

        val securityQueueTypes: Map<String, List<String>> = HashMap()
        val startPoint: LLNavigationPoint = LLNavigationPointForPOI("1025")
        val endPoint: LLNavigationPoint = LLNavigationPointForPOI("566")

        llLocusMapsFragment1!!.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes)
        llLocusMapsFragment2!!.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes)
        llLocusMapsFragment3!!.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes)
        llLocusMapsFragment4!!.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes)
        llLocusMapsFragment5!!.showDirectionsSummary(startPoint, endPoint, LLNavAccessibilityType.Direct, securityQueueTypes)
    }
}
