package com.example.ontariooutbacknavigator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.Polyline


class MainActivity : AppCompatActivity(), LocationListener, AddWaypointListener, CompassEventListener {

    private var mapView : MapView? = null

    private var locationService : GPSLocationService? = null
    private var compassService : CompassService? = null

    private var locationMarker : Marker? = null
    private var selectedMarker : Marker? = null

    private var activeLinkNode : Marker? = null

    private val links : MutableList<Polyline> = mutableListOf()

    private val markerToWaypoint = mutableMapOf<Marker, Waypoint>()

    private var trackingPathOverlay : Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "OntarioOutbackNavigator/1.0"
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (checkPermissions()) {
            initAll()
        }
    }

    private fun initAll() {
        initLocationService()
        initCompassService()
    }

    private fun resize(image: Drawable): Drawable {
        val b = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(b, 30, 45, false)
        return BitmapDrawable(resources, bitmapResized)
    }

    private fun initMap(startLocation : Location) {
        mapView = findViewById(R.id.mapView)
        val controller = mapView!!.controller
        controller.animateTo(GeoPoint(startLocation.latitude, startLocation.longitude))
        controller.setZoom(16.0)
        locationMarker = Marker(mapView)
        locationMarker!!.position = GeoPoint(startLocation.latitude, startLocation.longitude)
        locationMarker!!.icon = resize(ResourcesCompat.getDrawable(resources, R.drawable.user_icon, null)!!)
        locationMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        locationMarker!!.setOnMarkerClickListener { _, _ ->
            false
        }
        mapView!!.overlays.add(locationMarker)
        mapView!!.setMultiTouchControls(true)
        val mapTileOverlayProvider = MapTileProviderBasic(applicationContext)
        val preferences = getDefaultSharedPreferences(applicationContext)
        val tileserverIP = preferences.getString("tileserver-ip", "10.0.2.2")
        val tileserverPort = preferences.getString("tileserver-port", "8080")
        val baseUrl = "http://$tileserverIP:$tileserverPort/styles/klokantech-basic/"
        val tileOverlaySource = object: XYTileSource("tile_overlay", 6, 17, 256, ".png", arrayOf(baseUrl)) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + ".png"
            }
        }
        mapTileOverlayProvider.tileSource = tileOverlaySource
        val overlay = TilesOverlay(mapTileOverlayProvider, applicationContext)
        overlay.loadingBackgroundColor = Color.TRANSPARENT
        mapView!!.overlays.add(overlay)
        mapView!!.overlays.add(MapEventsOverlay(object: MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                openAddWaypointDialog(p!!.latitude, p.longitude)
                mapView!!.invalidate()
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }

        }))
        val db = DatabaseClient(applicationContext).getDB()
        lifecycleScope.launch {
            for (waypoint in db.waypointDao().getAll()) {
                val marker = addWaypointMarker(waypoint.latitude, waypoint.longitude, waypoint.description, waypoint.color)
                markerToWaypoint[marker] = waypoint
            }
            mapView!!.invalidate()
        }
    }

    private fun initLocationService() {
        locationService = GPSLocationService(this, this)
    }

    private fun initCompassService() {
        compassService = CompassService(applicationContext, this)
    }

    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAll()
            }
        }
    }

    private fun checkPermissions() : Boolean {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                0
            )
            return false
        }
        return true
    }

    override fun onLocationChanged(location: Location) {
        if (mapView == null) {
            initMap(location)
        }
        locationMarker!!.position = GeoPoint(location.latitude, location.longitude)
        if (trackingPathOverlay != null) {
            trackingPathOverlay!!.setPoints(mutableListOf(locationMarker!!.position, selectedMarker!!.position))
            val results = FloatArray(1)
            Location.distanceBetween(locationMarker!!.position.latitude, locationMarker!!.position.longitude, selectedMarker!!.position.latitude, selectedMarker!!.position.longitude, results)
            val distance = results[0] / 1000.0f
            trackingPathOverlay!!.title = "${"%.2f".format(distance)} km"
            mapView!!.invalidate()
        }
    }

    private fun openAddWaypointDialog(lat : Double?, lng : Double?) {
        val dialog = AddWaypointDialogFragment(lat, lng, this)
        dialog.show(supportFragmentManager, "AddWaypointDialogFragment")
    }

    fun onNewWaypointButtonClicked(view : View) {
        openAddWaypointDialog(locationMarker!!.position.latitude, locationMarker!!.position.longitude)
    }

    fun onSettingsButtonClicked(view : View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun onDeleteButtonClicked(view : View) {
        if (mapView != null) {
            if (selectedMarker != null && selectedMarker!!.isInfoWindowOpen) {
                selectedMarker!!.closeInfoWindow()
                removeWaypoint(selectedMarker!!)
                selectedMarker = null
            }
        }
    }

    fun onTrackButtonClicked(view : View) {
        if (mapView != null && selectedMarker != null && selectedMarker!!.isInfoWindowOpen) {
            selectedMarker!!.closeInfoWindow()
            if (trackingPathOverlay != null) {
                mapView!!.overlays.remove(trackingPathOverlay)
            }
            trackingPathOverlay = Polyline(mapView)
            trackingPathOverlay!!.outlinePaint.color = Color.BLUE
            trackingPathOverlay!!.outlinePaint.strokeWidth = 5f
            trackingPathOverlay!!.addPoint(locationMarker!!.position)
            trackingPathOverlay!!.addPoint(selectedMarker!!.position)
            val results = FloatArray(1)
            Location.distanceBetween(locationMarker!!.position.latitude, locationMarker!!.position.longitude, selectedMarker!!.position.latitude, selectedMarker!!.position.longitude, results)
            val distance = results[0] / 1000.0f
            trackingPathOverlay!!.title = "${"%.2f".format(distance)} km"
            mapView!!.overlays.add(trackingPathOverlay)
        }
        else if (mapView != null && trackingPathOverlay != null) {
            mapView!!.overlays.remove(trackingPathOverlay)
            trackingPathOverlay = null
        }
    }

    fun onLinkButtonClicked(view : View) {
        if (mapView != null && selectedMarker != null && selectedMarker!!.isInfoWindowOpen) {
            activeLinkNode = selectedMarker
        }
    }

    fun onCenterButtonClicked(view : View) {
        if (mapView != null) {
            mapView!!.controller.animateTo(locationMarker!!.position)
        }
    }

    fun onClearButtonClicked(view : View) {
        if (mapView != null) {
            for (link in links) {
                mapView!!.overlays.remove(link)
            }
            links.clear()
            mapView!!.invalidate()
        }
    }

    private fun addWaypointMarker(lat : Double, lng : Double, description : String, color : Int) : Marker {
        val marker = Marker(mapView)
        marker.position = GeoPoint(lat, lng)
        marker.title = description
        marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.waypoint, null)
        marker.icon.setTint(color)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.setOnMarkerClickListener { _, _ ->
            if (activeLinkNode != null) {
                val link = Polyline(mapView)
                link.outlinePaint.color = Color.CYAN
                link.outlinePaint.strokeWidth = 5f
                link.addPoint(activeLinkNode!!.position)
                link.addPoint(marker.position)
                val results = FloatArray(1)
                Location.distanceBetween(activeLinkNode!!.position.latitude, activeLinkNode!!.position.longitude, marker.position.latitude, marker.position.longitude, results)
                val distance = results[0] / 1000.0f
                link.title = "${"%.2f".format(distance)} km"
                links.add(link)
                activeLinkNode!!.closeInfoWindow()
                mapView!!.overlays.add(link)
                activeLinkNode = null
            }
            else {
                marker.showInfoWindow()
                selectedMarker = marker
            }
            true
        }
        mapView!!.overlays.add(marker)
        return marker
    }

    private fun removeWaypointMarker(marker : Marker) {
        mapView!!.overlays.remove(marker)
    }

    private fun addWaypointToDatabase(marker : Marker, lat : Double, lng : Double, description : String, color : Int) {
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            val waypoint = Waypoint(0, lat, lng, description, color)
            waypoint.id = db.waypointDao().insert(waypoint).toInt()
            markerToWaypoint[marker] = waypoint
        }
    }

    private fun removeWaypointFromDatabase(waypoint : Waypoint) {
        lifecycleScope.launch {
            val db = DatabaseClient(applicationContext).getDB()
            db.waypointDao().delete(waypoint)
        }
    }

    override fun addWaypoint(lat: Double, lng: Double, description: String, color: Int) {
        val marker = addWaypointMarker(lat, lng, description, color)
        addWaypointToDatabase(marker, lat, lng, description, color)
        mapView!!.invalidate()
    }

    private fun removeWaypoint(marker : Marker) {
        removeWaypointMarker(marker)
        if (markerToWaypoint.containsKey(marker)) {
            removeWaypointFromDatabase(markerToWaypoint[marker]!!)
            markerToWaypoint.remove(marker)
        }
        mapView!!.invalidate()
    }

    override fun onCompassChanged(azimuth: Float) {
        if (locationMarker != null) {
            locationMarker!!.rotation = Math.toDegrees(azimuth.toDouble()).toFloat()
            mapView!!.invalidate()
        }
    }
}