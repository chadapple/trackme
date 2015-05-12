package com.gmail.chadapple.trackme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements LocationService.LocationCallback {
  static private final String TAG = "TrackMeMain";
  static private final String SERVER = "http://capple.no-ip.org";
//  static private final String SERVER = "http://192.168.1.50";
  static private final String SERVER_ROUTE_URL = SERVER + "/trackme/route.php";
  private GoogleMap mMap;
  private Marker mLocationMarker = null;
  private boolean mBound = false;
  private PolylineOptions mLineOptions = new PolylineOptions();
  private Polyline mPolyline = null;
  private LocationService.LocationServiceBinder mLocationServiceBinder = null;

  // ServiceConnection used for binding to LocationService
  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      mLocationServiceBinder = ((LocationService.LocationServiceBinder) binder);
      mLocationServiceBinder.getService().registerCallback(MainActivity.this);
      mLocationServiceBinder.getService().setServer(SERVER);
      mBound = true;
      // Wait until we're bound to the service to get the routes
      // After we get the routes we'll call setupControls() which depends on this service being bound
      new GetRoutes().execute();
      Log.i(TAG, "Connected to LocationService");
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
      mLocationServiceBinder = null;
      mBound = false;
      Log.i(TAG, "Disconnected to LocationService");
    }
  };

  // Background async task to get route names
  private class GetRoutes extends AsyncTask<Void, Void, String> {
    protected String doInBackground(Void...params) {
      StringBuilder sBuilder = new StringBuilder();
      String line;
      try {
        URL url = new URL(SERVER_ROUTE_URL);
        URLConnection urlconnection = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                urlconnection.getInputStream()));
        // Read all lines from the URL stream
        while ((line = in.readLine()) != null) {
          sBuilder.append(line + "\n");
        }
        in.close();
      } catch (IOException e) {
        Log.e(TAG, e.toString());
        e.printStackTrace();
      }
      // String will get passed into onPostExecute
      return sBuilder.toString();
    }
    protected void onProgressUpdate(Void...params) {
    }
    protected void onPostExecute(String result) {
      // Parse JSON data
      try {
        JSONArray routes = new JSONArray(result);
        ArrayList<String> stringArrayList = new ArrayList<String>();

        // looping through routes, saving each name
        for (int i = 0; i < routes.length(); i++) {
          JSONObject c = routes.getJSONObject(i);
          stringArrayList.add(c.getString("name"));
        }
        // Time to setup the spinner and toggle button controls
        setupControls(stringArrayList.toArray(new String[stringArrayList.size()]));
      } catch (JSONException e) {
        Log.e(TAG, e.toString());
        e.printStackTrace();
      }
    }
  }

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
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    setUpMap();
  }

  private void setupControls(String[] choices) {
    // Selection of the spinner
    Spinner spinner = (Spinner) findViewById(R.id.spinner);

    // Application of the Array to the Spinner
    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, choices);
    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
    spinner.setAdapter(spinnerArrayAdapter);

    // Setup the toggle button
    final ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);
    button.setText("Monitor");
    button.setTextOff("Monitor");
    button.setTextOn("Track");
    // attach an OnClickListener
    button.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if(mLocationServiceBinder != null) {
          clearMap();
          if (button.isChecked()) {
            mLocationServiceBinder.getService().setMode(LocationService.LocationServiceMode.TRACK);
          } else {
            mLocationServiceBinder.getService().setMode(LocationService.LocationServiceMode.MONITOR);
          }
        }
        else {
          Log.i(TAG, "onClick, but not bound to location service");
        }
      }
    });
  }

  private void setUpMap() {
    if (mMap != null) {
      return;
    }
    mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
    if (mMap == null) {
      Log.e(TAG, "Error getting map fragment");
      return;
    }
    // Initialize map options
    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.058039, -84.136014), 10.0f));
//    mMap.addMarker(new MarkerOptions().position(new LatLng(40.058039, -84.136014)).title("My house"));
    // Polyline will be used to draw a history of locations
    mLineOptions.color(Color.BLUE);
    mPolyline = mMap.addPolyline(mLineOptions);
  }

  private void clearMap() {
    mMap.clear();
    if (mLocationMarker != null) {
      mLocationMarker.remove();
      mLocationMarker = null;
    }
    mLineOptions = new PolylineOptions();
    mPolyline = mMap.addPolyline(mLineOptions);
    mPolyline.setPoints(mLineOptions.getPoints());
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
