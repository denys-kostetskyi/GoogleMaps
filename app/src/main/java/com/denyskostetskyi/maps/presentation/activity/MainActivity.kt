package com.denyskostetskyi.maps.presentation.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.denyskostetskyi.maps.BackgroundTaskHandler
import com.denyskostetskyi.maps.R
import com.denyskostetskyi.maps.databinding.ActivityMainBinding
import com.denyskostetskyi.maps.model.MarkerData
import com.denyskostetskyi.maps.MarkerWithRadius
import com.denyskostetskyi.maps.MarkerWithRadius.Companion.addMarkerWithRadius
import com.denyskostetskyi.maps.utils.PermissionUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    OnRequestPermissionsResultCallback,
    OnMapLongClickListener {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding ?: throw RuntimeException("ActivityMainBinding is null")

    private var permissionDenied = false
    private var isEditModeEnabled = false
    private var shouldRestoreState = false

    private var savedCameraPosition: CameraPosition? = null
    private var savedMarkers: List<MarkerData>? = null

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundTaskHandler: BackgroundTaskHandler
    private lateinit var map: GoogleMap

    private val exportFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(FILE_JSON)) { uri: Uri? ->
            uri?.let {
                backgroundTaskHandler.postSaveMarkersToFile(it)
            }
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                backgroundTaskHandler.postLoadMarkersFromFile(it) { markerDataList ->
                    runOnUiThread {
                        addMarkersFromList(markerDataList)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initBackgroundTaskHandler()
        initMap()
    }

    private fun initBackgroundTaskHandler() {
        backgroundThread = HandlerThread(BACKGROUND_THREAD_NAME).apply { start() }
        backgroundTaskHandler = BackgroundTaskHandler(backgroundThread.looper, this.contentResolver)
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.setOnMapLongClickListener(this)
        restoreMapStateIfNeeded()
        checkLocationPermissions()
    }

    private fun restoreMapStateIfNeeded() {
        savedCameraPosition?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(it)) }
        savedMarkers?.let { addMarkersFromList(it) }
    }

    private fun checkLocationPermissions() {
        if (PermissionUtils.isLocationPermissionGranted(this)) {
            enableMyLocation()
        } else {
            PermissionUtils.requestLocationPermissions(
                this,
                LOCATION_PERMISSION_REQUEST_CODE,
                true
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
    }

    override fun onMapLongClick(position: LatLng) {
        addMarker(position)
    }

    private fun addMarker(position: LatLng) {
        map.addMarkerWithRadius(position) {
            isEditModeEnabled
        }
    }

    private fun addMarkersFromList(markerDataList: List<MarkerData>) {
        markerDataList.forEach { data ->
            addMarker(LatLng(data.latitude, data.longitude))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        if (PermissionUtils.isLocationPermissionGranted(this)) {
            enableMyLocation()
        } else {
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            PermissionUtils.PermissionDeniedDialog.newInstance(true)
                .show(supportFragmentManager, PERMISSION_DENIED_DIALOG_TAG)
            permissionDenied = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_mode -> {
                item.isChecked = !item.isChecked
                switchEditMode(item.isChecked)
            }

            R.id.action_restore_from_file -> importMarkers()
            R.id.action_save_to_file -> exportMarkers()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun switchEditMode(isEnabled: Boolean) {
        isEditModeEnabled = isEnabled
        if (isEditModeEnabled) {
            Toast.makeText(
                this,
                getString(R.string.tap_to_remove_marker),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun importMarkers() {
        importFileLauncher.launch(arrayOf(FILE_JSON))
    }

    private fun exportMarkers() {
        exportFileLauncher.launch(SUGGESTED_FILE_NAME)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val cameraPosition = map.cameraPosition
        val markerDataList = ArrayList<MarkerData>(MarkerWithRadius.markersData)
        with(outState) {
            putBoolean(KEY_EDIT_MODE, isEditModeEnabled)
            putParcelable(KEY_CAMERA_POSITION, cameraPosition)
            putParcelableArrayList(KEY_MARKERS, markerDataList)
        }
        MarkerWithRadius.reset()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        with(savedInstanceState) {
            isEditModeEnabled = getBoolean(KEY_EDIT_MODE)
            savedCameraPosition = getParcelable(KEY_CAMERA_POSITION, CameraPosition::class.java)
            savedMarkers = getParcelableArrayList(KEY_MARKERS, MarkerData::class.java)
        }
        shouldRestoreState = true
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundThread.quitSafely()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 135
        private const val PERMISSION_DENIED_DIALOG_TAG = "dialog"
        private const val BACKGROUND_THREAD_NAME = "BackgroundThread"
        private const val FILE_JSON = "application/json"
        private const val SUGGESTED_FILE_NAME = "markers"
        private const val KEY_EDIT_MODE = "edit_mode"
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_MARKERS = "markers"
    }
}
