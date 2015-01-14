package com.gmail.chadapple.trackme;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity {
  private GoogleMap mMap;
  private Marker mLocationMarker = null;
  static private final String TAG = "TrackMeMain";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    setUpMap();
    startService(new Intent(this, LocationService.class));
  }

  @Override
  protected void onResume() {
    super.onResume();
    setUpMap();
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // setUpMap()
  ////////////////////////////////////////////////////////////////////////////////////////
  private void setUpMap() {
    if (mMap != null) {
      return;
    }
    mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
    if (mMap == null) {
      return;
    }
    // Initialize map options
    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.058039, -84.136014), 14.0f));
    mMap.addMarker(new MarkerOptions().position(new LatLng(40.058039, -84.136014)).title("My house"));

    // Acquire a reference to the system Location Manager
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Change = " + location.toString());
        makeUseOfNewLocation(location);
      }
      public void onStatusChanged(String provider, int status, Bundle extras) { Log.i(TAG, "Status Change"); }
      public void onProviderEnabled(String provider) {
        Log.i(TAG, "Provider Enable");
      }
      public void onProviderDisabled(String provider) {
        Log.i(TAG, "Provider Disable");
      }
    };

    // Register the listener with the Location Manager to receive location updates
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15 * 1000, 0, locationListener);
    //locationManager.removeUpdates(locationListener);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // makeUseOfNewLocation()
  ////////////////////////////////////////////////////////////////////////////////////////
  void makeUseOfNewLocation(Location location) {
    if (mLocationMarker != null) {
      mLocationMarker.remove();
    }
    mLocationMarker = mMap.addMarker(new MarkerOptions().
        position(new LatLng(location.getLatitude(), location.getLongitude())).
        title("Current Location").
        icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).
        draggable(false));
  }
}
