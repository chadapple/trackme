package com.gmail.chadapple.trackme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements LocationService.LocationCallback {
  private GoogleMap mMap;
  private Marker mLocationMarker = null;
  static private final String TAG = "TrackMeMain";
  private boolean mBound = false;

  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      ((LocationService.LocationServiceBinder) binder).getService().registerCallback(MainActivity.this);
      mBound = true;
      Log.i(TAG, "Connected to LocationService");
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.i(TAG, "Disconnected to LocationService");
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    setUpMap();
    startService(new Intent(this, LocationService.class));
  }

  @Override
  protected void onStart() {
    super.onStart();
    // Bind to LocationService
    Intent intent = new Intent(this, LocationService.class);
    bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Unbind from the service
    if (mBound) {
      unbindService(mServiceConnection);
      mBound = false;
    }
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
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.058039, -84.136014), 10.0f));
    mMap.addMarker(new MarkerOptions().position(new LatLng(40.058039, -84.136014)).title("My house"));
  }

  @Override
  public void LocationChanged(Location location) {
    if (mLocationMarker != null) {
      mLocationMarker.remove();
    }
    else {
      // If this is the first update, zoom camera to location
      mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13.0f));
    }
    mLocationMarker = mMap.addMarker(new MarkerOptions().
        position(new LatLng(location.getLatitude(), location.getLongitude())).
        title("My Location").
        icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).
        draggable(false));
  }
}
