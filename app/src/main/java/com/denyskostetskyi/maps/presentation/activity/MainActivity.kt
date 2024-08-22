package com.denyskostetskyi.maps.presentation.activity

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMyLocationButtonClickListener,
    OnMapReadyCallback,
    OnRequestPermissionsResultCallback,
    OnMapLongClickListener {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding ?: throw RuntimeException("ActivityMainBinding is null")

    private var permissionDenied = false

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
        checkLocationPermissions()
        initMap()
    }

    private fun checkLocationPermissions() {
        if (!PermissionUtils.isLocationPermissionGranted(this)) {
            PermissionUtils.requestLocationPermissions(
                this,
                LOCATION_PERMISSION_REQUEST_CODE,
                true
            )
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMapLongClickListener(this)
    }

    override fun onMapLongClick(position: LatLng) {
        addMarker(position)
    }

    private fun addMarker(position: LatLng) {
        map.addMarkerWithRadius(position)
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        if (PermissionUtils.isLocationPermissionGranted(this)) {
            map.isMyLocationEnabled = true
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 135
        private const val PERMISSION_DENIED_DIALOG_TAG = "dialog"
    }
}
