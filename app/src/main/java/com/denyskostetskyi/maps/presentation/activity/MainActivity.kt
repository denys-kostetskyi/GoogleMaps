package com.denyskostetskyi.maps.presentation.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.denyskostetskyi.maps.R
import com.denyskostetskyi.maps.databinding.ActivityMainBinding
import com.denyskostetskyi.maps.presentation.utils.MarkerWithRadius.Companion.addMarkerWithRadius
import com.denyskostetskyi.maps.presentation.utils.PermissionUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    OnRequestPermissionsResultCallback,
    OnMapLongClickListener {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding ?: throw RuntimeException("ActivityMainBinding is null")

    private var permissionDenied = false
    private var isEditModeEnabled = false

    private lateinit var map: GoogleMap

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
        initMap()
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.setOnMapLongClickListener(this)
        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (!PermissionUtils.isLocationPermissionGranted(this)) {
            PermissionUtils.requestLocationPermissions(
                this,
                LOCATION_PERMISSION_REQUEST_CODE,
                true
            )
        } else {
            enableMyLocation()
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
                isEditModeEnabled = item.isChecked
                if (isEditModeEnabled) {
                    Toast.makeText(
                        this,
                        getString(R.string.tap_to_remove_marker),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 135
        private const val PERMISSION_DENIED_DIALOG_TAG = "dialog"
    }
}
