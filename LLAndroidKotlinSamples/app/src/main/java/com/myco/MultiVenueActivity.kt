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
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.locuslabs.sdk.llpublic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MultiVenueActivity : AppCompatActivity() {

    private lateinit var venueListRecyclerView: RecyclerView
    private lateinit var llLocusMapsFragment: LLLocusMapsFragment
    private lateinit var initializationAnimationViewBackground: View
    private lateinit var initializationAnimationView: ImageView
    private lateinit var initializationAnimationDrawable: AnimationDrawable
    private lateinit var mapContainer: ConstraintLayout
    private var mapFragmentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_venue)

        SampleConfig.resetToDefaults()

        mapContainer = findViewById(R.id.map_container)

        venueListRecyclerView = findViewById(R.id.recycler_view)
        val dividerItemDecoration = DividerItemDecoration(this,
            RecyclerView.VERTICAL)
        venueListRecyclerView.addItemDecoration(dividerItemDecoration)

        getVenueList()
        initLocusMaps()
        initBackHandling()
    }

    // Note: there is no need to forward lifecycle events (onStart, onResume, onPause, onStop,
    // onDestroy, onLowMemory) to LLLocusMapsFragment, the fragment handles them itself

    override fun onDestroy() {

        SampleConfig.clearListeners()
        super.onDestroy()
    }

    private fun getVenueList() {

        lifecycleScope.launch {
            try {
                // Get the venue list on a background thread (recommended), then update the UI on the main thread
                val venueList = withContext(Dispatchers.IO) { LLVenueDatabase().getVenueList() }

                val venueListEntries = ArrayList(venueList.values)
                venueListRecyclerView.adapter = VenueListAdapter(venueListEntries)
            } catch (throwable: Throwable) {

                // Failed to get the venue list
                Log.d("Error", "Failed to get venues:" + throwable.localizedMessage)
            }
        }
    }

    private fun initLocusMaps() {

        initializationAnimationViewBackground = findViewById(R.id.initializationAnimationViewBackground)
        initializationAnimationView = findViewById(R.id.initializationAnimationView)
        initInitializationProgressIndicator()

        llLocusMapsFragment = supportFragmentManager.findFragmentById(R.id.llLocusMapsFragment) as LLLocusMapsFragment

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)

                if (f == llLocusMapsFragment && !mapFragmentReady) {

                    mapFragmentReady = true
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
                startActivity(intent)
            }
        }

        LLDependencyInjector.singleton.onPOIPhoneClickedListener = object : LLOnPOIPhoneClickedListener {
            override fun onPOIPhoneClicked(phone: String) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                startActivity(intent)
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

    public fun showVenue(venueListEntry: LLVenueListEntry) {

        if (!mapFragmentReady) return
        showInitializationProgressIndicator()
        mapContainer.visibility = View.VISIBLE
        val llVenueAssetVersion = venueListEntry.assetVersion
        val llVenueFiles = venueListEntry.files
        llLocusMapsFragment.showVenue(venueListEntry.id!!, llVenueAssetVersion, llVenueFiles)
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

        onBackPressedDispatcher.addCallback(this) {
            if (llLocusMapsFragment.hasBackStackItems()) {

                // Let the map close its own screens (POI view, search, directions...) first
                llLocusMapsFragment.popBackStack()
            }
            else if (mapContainer.visibility == View.VISIBLE) {

                // Then go back from the map to the venue list
                getVenueList()
                mapContainer.visibility = View.INVISIBLE
            }
            else {

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    private fun mapReady() {

    }

    inner class VenueListAdapter(private val venueListEntries: ArrayList<LLVenueListEntry>) : RecyclerView.Adapter<VenueListAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView

            init {
                textView = view.findViewById<View>(R.id.title_text_view) as TextView
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.sample_row, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val venueListEntry = venueListEntries[position]
            viewHolder.textView.text = venueListEntry.name
            viewHolder.itemView.setOnClickListener {
                this@MultiVenueActivity.showVenue(venueListEntry)
            }
        }

        override fun getItemCount(): Int {
            return venueListEntries.size
        }
    }
}