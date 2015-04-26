package com.gmail.chadapple.trackme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MainActivity extends FragmentActivity implements LocationService.LocationCallback {
  private GoogleMap mMap;
  private Marker mLocationMarker = null;
  static private final String TAG = "TrackMeMain";
  private boolean mBound = false;
  private PolylineOptions mLineOptions = new PolylineOptions();
  private Polyline mPolyline = null;

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
    // Polyline will be used to draw a history of locations
    mLineOptions.color(Color.BLUE);
    mPolyline = mMap.addPolyline(mLineOptions);
  }

  @Override
  public void LocationChanged(Location location) {
    LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
    if(mLocationMarker == null) {
      // If this is the first update, zoom camera to location
      mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 13.0f));
    }
    List<LatLng> points = mLineOptions.getPoints();
    float[] distance = new float[] {10000.0f};
    if(!points.isEmpty()) {
      LatLng lastPoint = points.get(points.size()-1);
      Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, ll.latitude, ll.longitude, distance);
    }
    // If moved a significant amount of distance, add a polyline and move the marker
    if(distance[0] > 100.0f) {
      if (mLocationMarker != null) {
        mLocationMarker.remove();
      }
      mLineOptions.add(ll);
      mPolyline.setPoints(mLineOptions.getPoints());
      mLocationMarker = mMap.addMarker(new MarkerOptions().
              position(ll).
              title("Current Location").
              icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).
              draggable(false));
    }
  }
}
