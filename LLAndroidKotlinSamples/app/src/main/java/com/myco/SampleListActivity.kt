package com.myco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class SampleListActivity : AppCompatActivity() {

    private val samples = listOf(
        SampleItem("Fullscreen Map", FullscreenMapActivity::class.java),
        SampleItem("Embedded Map", EmbeddedMapActivity::class.java),
        SampleItem("Embedded Map (Multiple)", MultipleEmbeddedMapsActivity::class.java),
        SampleItem("Bundled Map", BundledMapActivity::class.java),
        SampleItem("Custom Actions", CustomActionsActivity::class.java),
        SampleItem("Custom Theme", CustomThemeActivity::class.java),
        SampleItem("Directions Show", DirectionsShowActivity::class.java),
        SampleItem("Directions - Steps & ETA", DirectionsStepsETAActivity::class.java),
        SampleItem("External Location Data", ExternalLocationDataActivity::class.java),
        SampleItem("Headless Mode", HeadlessModeActivity::class.java),
        SampleItem("Map Basics", MapBasicsActivity::class.java),
        SampleItem("Markers", MarkersActivity::class.java),
        SampleItem("Multi Venue", MultiVenueActivity::class.java),
        SampleItem("POI Button", POIButtonActivity::class.java),
        SampleItem("POI Show", POIShowActivity::class.java),
        SampleItem("Search Auto Display", SearchAutoDisplayActivity::class.java),
        SampleItem("Search Categories", SearchCategoriesActivity::class.java),
        SampleItem("Search General", SearchGeneralActivity::class.java),
        SampleItem("Search MultiTerm", SearchMultiTermActivity::class.java),
        SampleItem("Search Proximity", SearchProximityActivity::class.java),
        SampleItem("Venue Data", VenueDataActivity::class.java)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_list)

        val samplesLV: ListView = findViewById(R.id.samplesLV)
        samplesLV.adapter = SamplesAdapter(this, samples)
        samplesLV.setOnItemClickListener { _, _, position, _ ->
            val sample = samples[position]
            startActivity(Intent(this, sample.activityClass))
        }
    }

    private data class SampleItem(
        val title: String,
        val activityClass: Class<out AppCompatActivity>
    )

    private class SamplesAdapter(
        context: Context,
        private val dataSource: List<SampleItem>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getCount(): Int = dataSource.size

        override fun getItem(position: Int): SampleItem = dataSource[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = convertView ?: inflater.inflate(R.layout.sample_row, parent, false).also {
                val titleTextView = it.findViewById<TextView>(R.id.title_text_view)
                it.tag = ViewHolder(titleTextView)
            }

            val holder = rowView.tag as ViewHolder
            holder.titleTextView.text = getItem(position).title
            return rowView
        }

        private data class ViewHolder(val titleTextView: TextView)
    }
}