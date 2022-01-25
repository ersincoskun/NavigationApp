package com.example.navigationapp

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.navigationapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType.ROUND
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var startPoint: LatLng
    private lateinit var endPoint: LatLng
    private var firstBluePolyLine: Polyline? = null
    private var firstRedPolyLine: Polyline? = null
    private var secondBluePolyLine: Polyline? = null
    private var secondRedPolyLine: Polyline? = null
    private var thirdBluePolyLine: Polyline? = null
    private var thirdRedPolyLine: Polyline? = null
    private val firstPolylineList = mutableListOf<LatLng>()
    private val secondPolylineList = mutableListOf<LatLng>()
    private val thirdPolylineList = mutableListOf<LatLng>()
    private var isCanDrawable = true
    private var animationCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        registerLauncher()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                Snackbar.make(
                    binding.root,
                    "Permission needed for location",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Give Permission") {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        } else {
            //permission granted
            fusedLocationClient.lastLocation
                .addOnSuccessListener(
                    this
                ) { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                userLocation,
                                15f
                            )
                        )
                    }
                }
        }

    }

    private fun registerLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //permission granted
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener(
                                this
                            ) { location ->
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    val userLocation = LatLng(location.latitude, location.longitude)

                                    mMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            userLocation,
                                            15f
                                        )
                                    )
                                }
                            }
                    }

                } else {
                    //permission denied
                    Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onMapLongClick(p0: LatLng) {
        if (isCanDrawable) {
            mMap.addMarker(MarkerOptions().position(p0))
            animationCounter++
            if (animationCounter % 2 == 0) {
                endPoint = p0
                val URL = getDirectionURL(startPoint, endPoint)
                GetDirection(URL).execute()
                if (animationCounter == 6) {
                    isCanDrawable = false
                }
            } else {
                startPoint = p0
            }
        } else {
            Toast.makeText(this, "You can create max 3 route", Toast.LENGTH_LONG).show()
        }
    }

    fun getDirectionURL(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=driving&key=${
            getString(
                R.string.google_maps_key
            )
        }"
    }

    private inner class GetDirection(
        val url: String
    ) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body()!!.string()
            Log.d("GoogleMap", " data : $data")
            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)

                val path = ArrayList<LatLng>()

                for (i in 0..(respObj.routes[0].legs[0].steps.size - 1)) {
//                    val startLatLng = LatLng(respObj.routes[0].legs[0].steps[i].start_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].start_location.lng.toDouble())
//                    path.add(startLatLng)
//                    val endLatLng = LatLng(respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble()
//                            ,respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble())
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
                firstPolylineList
                when {
                    animationCounter <= 2 -> {
                        firstPolylineList.addAll(path)
                    }

                    animationCounter in 3..4 -> {
                        secondPolylineList.addAll(path)
                    }

                    animationCounter in 5..6 -> {
                        thirdPolylineList.addAll(path)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val blueOptions = PolylineOptions()
            val redOptions = PolylineOptions()
            for (i in result.indices) {
                blueOptions.apply {
                    addAll(result[i])
                    width(10f)
                    color(Color.BLUE)
                    geodesic(true)
                    jointType(ROUND)
                }
            }

            for (i in result.indices) {
                redOptions.apply {
                    addAll(result[i])
                    width(10f)
                    color(Color.RED)
                    geodesic(true)
                    jointType(ROUND)
                }
            }
            when {
                animationCounter <= 2 -> {
                    firstBluePolyLine = mMap.addPolyline(blueOptions)
                    firstRedPolyLine = mMap.addPolyline(redOptions)
                    animatePolyLine(firstRedPolyLine!!, firstBluePolyLine!!, firstPolylineList)
                }

                animationCounter in 3..4 -> {
                    secondBluePolyLine = mMap.addPolyline(blueOptions)
                    secondRedPolyLine = mMap.addPolyline(redOptions)
                    animatePolyLine(secondRedPolyLine!!, secondBluePolyLine!!, secondPolylineList)
                }

                animationCounter in 5..6 -> {
                    thirdBluePolyLine = mMap.addPolyline(blueOptions)
                    thirdRedPolyLine = mMap.addPolyline(redOptions)
                    animatePolyLine(thirdRedPolyLine!!, thirdBluePolyLine!!, thirdPolylineList)
                }
            }

            //animatePolyLine()
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }


    private fun animatePolyLine(
        foregroundPolyline: Polyline,
        backgroundPolyline: Polyline,
        polylineList: List<LatLng>
    ) {
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 1000
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { paramAnimator ->
            val latLngList: MutableList<LatLng> = backgroundPolyline.points
            val initialPointSize = latLngList.size
            val animatedValue = paramAnimator.animatedValue as Int
            val newPoints: Int = animatedValue * polylineList.size / 100
            if (initialPointSize < newPoints) {
                latLngList.addAll(polylineList.subList(initialPointSize, newPoints))
                backgroundPolyline.points = latLngList
            }
        }
        val polyLineAnimationListener: Animator.AnimatorListener =
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                }

                override fun onAnimationEnd(animator: Animator) {
                    val blackLatLng: MutableList<LatLng> = backgroundPolyline.points
                    val greyLatLng: MutableList<LatLng> = foregroundPolyline.points
                    greyLatLng.clear()
                    greyLatLng.addAll(blackLatLng)
                    blackLatLng.clear()
                    backgroundPolyline.points = blackLatLng
                    foregroundPolyline.points = greyLatLng
                    backgroundPolyline.zIndex = 2f
                    animator.start()
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            }
        animator.addListener(polyLineAnimationListener)
        animator.start()
    }


}



