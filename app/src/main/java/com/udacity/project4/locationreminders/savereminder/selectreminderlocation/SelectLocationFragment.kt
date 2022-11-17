package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // Map and Permission ID
    private lateinit var map : GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private val TAG = SelectLocationFragment::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Initialize the map and call onMapReady()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveLocationBtn.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    // save the location info and navigate back to the reminders screen
    private fun onLocationSelected() {
        if(_viewModel.validateLocation()) {
            findNavController().navigateUp()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // check user permissions and get user current location
        enableLocation()
        seMapStyle(map)
        setPoiClick(map)
        setMapClick(map)
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )

            _viewModel.saveSelectedLocation(getString(R.string.dropped_pin), latLng.latitude, latLng.longitude)
        }
    }

    // place a marker when the user selects a POI and save it's data to the viewModel
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker =map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            )
            poiMarker?.showInfoWindow()
            _viewModel.saveSelectedLocation(poi.name, poi.latLng.latitude, poi.latLng.longitude)
        }
    }

    // set custom map style
    private fun seMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this.requireContext(),
                    R.raw.map_style
                )
            )

            if(!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e : Resources.NotFoundException) {
            Log.e(TAG, "Can't find style error :", e)
        }
    }

    // get user location and move the camera to it
    @SuppressLint("MissingPermission")
    private fun addUserLocation() {
        // fusedLocationProvider gets the current user location
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        val locationTask = fusedLocationProviderClient.lastLocation

        // camera zoom level
        val zoomLevel = 20f

        locationTask.addOnCompleteListener(this.requireActivity()) { task ->
            if(task.isSuccessful) {
                val result = task.result
                // inside run block so the app waits for the process to finish
                result.run {
                    val userLatLng = LatLng(latitude, longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel))
                }
            }
        }
    }

    // Check that the location permission is granted
    private fun isPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(
        this.requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // enable the phone location permission
    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            addUserLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            _viewModel.showSnackBar.value = "Please accept location permissions to use the app"
        }
    }

    // CHeck the the permission result is granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_LOCATION_PERMISSION) {
            if(grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableLocation()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    // Change map type upon user selection
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
