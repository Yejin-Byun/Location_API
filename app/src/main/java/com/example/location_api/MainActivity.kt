package com.example.location_api

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener

// MainActivity에서 OnMapReadyCallBack 을 상속받음 (override 메소드로 onMapReady() 가 필수적으로 들어가야 함)
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mGoogleMap: GoogleMap

    //위치 서비스가 gps를 사용해서 위치를 확인
    lateinit var fusedLocationClient: FusedLocationProviderClient

    //위치 정보 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback

    lateinit var locationPermission: ActivityResultLauncher<Array<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        locationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){ results ->
            // 퍼미션이 있는지 확인
            if(results.all{it.value}){
                // 프래그먼트의 mapView 와 연결을 시킴 ( getMapAsync() )
                (supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment)!!.getMapAsync(this)
            }else{ //문제가 발생했을 때
                Toast.makeText(this,"권한 승인이 필요합니다.",Toast.LENGTH_LONG).show()
            }
        }

        // 위치 권한 요청
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }


    // 지도 객체를 이용할 수 있는 상황이 될 때의 콜백 함수
    override fun onMapReady(p0: GoogleMap) {

        // MapReady가 될 시 올리는 마커 (seoul)
        val seoul = LatLng(37.566610, 126.978403)
        mGoogleMap = p0
        mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL // default 노말 생략 가능
        mGoogleMap.apply {
            val markerOptions = MarkerOptions()
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            markerOptions.position(seoul)
            markerOptions.title("서울시청")
            markerOptions.snippet("Tel:01-120")
            addMarker(markerOptions)
        }

        // getFusedLocationProviderClient() = 위치 정보를 받아야 하기 때문에 불리는 메소드
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        updateLocation()
    }

    // updateLocation() = 위치를 계속 받아오는 부분
    fun updateLocation(){

        // 몇 초당 한 번씩 사용자 위치를 요청할 것인지 설정
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // 콜백 함수는 1초당 한 번씩 LocationResult로 들어오게 됨
        locationCallback = object : LocationCallback(){
            //1초에 한번씩 변경된 위치 정보가 onLocationResult 으로 전달된다.
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult?.let{
                    for (location in it.locations){
                        Log.d("위치정보",  "위도: ${location.latitude} 경도: ${location.longitude}") // 1초에 한 번씩 현재 위치가 log로 찍힘
                        setLastLocation(location) //계속 실시간으로 위치를 받아오고 있기 때문에 맵을 확대해도 다시 줄어든다.
                    }
                }
            }
        }

        //권한 처리
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // requestLocationUpdates() = 생성한 request 조건과 콜백 함수를 등록하여 위치가 변경될 시 초당 한 번씩 불림
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
            Looper.myLooper()!!
        )
    }

    // 위치가 변경될 시 호출되는 메소드
    fun setLastLocation(lastLocation: Location){
        val LATLNG = LatLng(lastLocation.latitude,lastLocation.longitude)

        val makerOptions = MarkerOptions().position(LATLNG).title("나 여기 있어용~") // 마커 생성
        val cameraPosition = CameraPosition.Builder().target(LATLNG).zoom(15.0f).build() // 카메라 이동

        mGoogleMap.addMarker(makerOptions)
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
}