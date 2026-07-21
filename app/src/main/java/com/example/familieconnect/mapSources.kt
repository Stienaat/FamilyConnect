package com.example.familieconnect

import android.content.Context
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

object MapSources {

    const val PREF_KEY = "map_provider"

    const val ESRI_STREETS = "esri_streets"
    const val OPENSTREETMAP = "openstreetmap"
    const val OPENTOPOMAP = "opentopomap"
    const val CARTO_VOYAGER = "carto_voyager"

    data class Provider(
        val key: String,
        val label: String,
        val attribution: String
    )

    val PROVIDERS = listOf(
        Provider(ESRI_STREETS, "Esri Streets", "Tiles © Esri"),
        Provider(OPENSTREETMAP, "OpenStreetMap", "© OpenStreetMap contributors"),
        Provider(
            OPENTOPOMAP,
            "OpenTopoMap",
            "© OpenStreetMap contributors, SRTM | © OpenTopoMap"
        ),
        Provider(
            CARTO_VOYAGER,
            "Carto Voyager",
            "© OpenStreetMap contributors | © CARTO"
        )
    )

    fun getSelectedKey(context: Context): String {
        return context
            .getSharedPreferences("tracker", Context.MODE_PRIVATE)
            .getString(PREF_KEY, ESRI_STREETS)
            ?: ESRI_STREETS
    }

    fun get(context: Context): ITileSource {
        return when (getSelectedKey(context)) {
            OPENSTREETMAP -> TileSourceFactory.MAPNIK
            OPENTOPOMAP -> openTopoMap()
            CARTO_VOYAGER -> cartoVoyager()
            else -> esriStreets()
        }
    }

    fun getAttribution(context: Context): String {
        val selected = getSelectedKey(context)
        return PROVIDERS.firstOrNull { it.key == selected }?.attribution
            ?: PROVIDERS.first().attribution
    }

    private fun openTopoMap(): ITileSource {
        return XYTileSource(
            "OpenTopoMap",
            0,
            17,
            256,
            ".png",
            arrayOf(
                "https://a.tile.opentopomap.org/",
                "https://b.tile.opentopomap.org/",
                "https://c.tile.opentopomap.org/"
            )
        )
    }

    private fun cartoVoyager(): ITileSource {
        return XYTileSource(
            "CartoVoyager",
            0,
            20,
            256,
            ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
            )
        )
    }

    private fun esriStreets(): ITileSource {
        return object : OnlineTileSourceBase(
            "EsriWorldStreetMap",
            0,
            19,
            256,
            ".jpg",
            arrayOf(
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"
            )
        ) {
            override fun getTileURLString(tileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(tileIndex)
                val x = MapTileIndex.getX(tileIndex)
                val y = MapTileIndex.getY(tileIndex)

                return baseUrl + "$zoom/$y/$x"
            }
        }
    }
}