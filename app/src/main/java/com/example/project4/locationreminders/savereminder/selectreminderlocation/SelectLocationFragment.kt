package com.example.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.project4.R
import com.example.project4.databinding.FragmentSelectLocationBinding
import com.example.project4.base.BaseFragment
import com.example.project4.locationreminders.savereminder.SaveReminderViewModel
import com.example.project4.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // Map and Permission ID
    private lateinit var map : GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private val TAG = SelectLocationFragment::class.java.simpleName

    // fused location provider to get the user location


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // TODO: add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // TODO: zoom to the user location after taking his permission




        // TODO: add style to the map
        // TODO: put a marker to location that the user selected
        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableLocation()

        // set zoom level
        val zoomLevel = 15f

        var userLatLng = LatLng(0.0, 0.0)

        Log.i(TAG, "latitude = ${userLatLng.latitude}, long = ${userLatLng.longitude}")
        // move camera to user location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel))

    }




    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
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
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun addUserLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        val locationTask = fusedLocationProviderClient.lastLocation

        val zoomLevel = 20f

        locationTask.addOnCompleteListener(this.requireActivity()) { task ->
            if(task.isSuccessful) {
                val result = task.result
                result.run {
                    val userLatLng = LatLng(latitude, longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel))
                }
            }
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


}
