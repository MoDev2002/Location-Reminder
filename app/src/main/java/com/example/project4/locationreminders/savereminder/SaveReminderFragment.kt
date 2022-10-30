package com.example.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.project4.BuildConfig
import com.example.project4.R
import com.example.project4.databinding.FragmentSaveReminderBinding
import com.example.project4.base.BaseFragment
import com.example.project4.base.NavigationCommand
import com.example.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.example.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.ACTION_GEOFENCE_EVENT
import com.example.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService
import com.example.project4.locationreminders.reminderslist.ReminderDataItem
import com.example.project4.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class SaveReminderFragment : BaseFragment() {

    companion object {
        // permission codes to activate location permissions
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 34
        private const val REQUEST_FOREGROUND_ONLY_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_BACKGROUND_ONLY_PERMISSION_RESULT_CODE = 36
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 35
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

        // geofence circular radius in meters
        private const val GEOFENCE_RADIUS = 500f

        private const val TAG = "SaveReminderFragment"
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    // geofencing client used to add the geofence request
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var reminderData: ReminderDataItem

    // Determine if the device is running on version Q or above
    private val runningQOrLater =
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q

    // PendingIntent for broadcast receiver to handle geofence request
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this.context as Activity, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            this.context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(this.requireActivity())

        requestForegroundAndBackgroundLocationPermission()
        requestBackgroundLocationPermission()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderData = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminderData)) {
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    requestForegroundAndBackgroundLocationPermission()
                    requestBackgroundLocationPermission()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    // Check if foreground and background permissions are approved
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundApproved = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ))

        val backgroundApproved =
            if (runningQOrLater) {
                (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this.requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ))
            } else {
                true
            }
        return foregroundApproved && backgroundApproved
    }

    // request foreground and background permissions from user
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermission() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }

        var permissionsArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val requestCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSION_RESULT_CODE
        }

        ActivityCompat.requestPermissions(
            this.requireActivity(),
            permissionsArray,
            requestCode
        )
    }

    // check if permissions are granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            _viewModel.showSnackBarInt.value = R.string.permission_denied_explanation
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    // if permission are granted start location services and add geofence
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this.requireActivity())
        val locationSettingsRequestTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsRequestTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        this.requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings: " + e.message)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsRequestTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
            if (foregroundAndBackgroundLocationPermissionApproved() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this.requireActivity(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                ) {
                    AlertDialog.Builder(this.requireContext())
                        .setTitle("Background Permissions Needed")
                        .setMessage("Background location needed to use geofence Choose \"Allow All The Time\"")
                        .setPositiveButton("OK") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this.requireActivity(),
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                REQUEST_BACKGROUND_ONLY_PERMISSION_RESULT_CODE
                            )
                        }
                        .setNegativeButton("CANCEL") { dialog, which ->
                            _viewModel.showSnackBar.value = "Please Allow the permissions"
                        }
                        .create().show()
                }
            } else {
                requestForegroundAndBackgroundLocationPermission()
            }
    }

    // add geofence
    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(reminderData.id)
            .setCircularRegion(reminderData.latitude!!, reminderData.longitude!!, GEOFENCE_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminderData)
            }
            addOnFailureListener {
                it.printStackTrace()
                _viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }
}
