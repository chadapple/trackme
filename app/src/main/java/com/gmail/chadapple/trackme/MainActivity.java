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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
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

  private class GetRoutes extends AsyncTask<String, String, String> {
    InputStream inputStream = null;
    String result = null;
    protected String doInBackground(String... str) {
//      String url_select = "http://capple.no-ip.org/trackme/route.php";
      String url_select = "http://192.168.1.50/trackme/route.php";

      try {
        URL url = new URL(url_select);
        URLConnection urlconn = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                urlconn.getInputStream()));
        StringBuilder sBuilder = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null) {
          sBuilder.append(line + "\n");
        }
        result = sBuilder.toString();
        in.close();
      } catch (IOException e1) {
        Log.e("GetRoutes", e1.toString());
        e1.printStackTrace();
      }
      return result;
    }
    protected void onProgressUpdate(String... progress) {
    }
    protected void onPostExecute(String result) {
      //parse JSON data
      try {
        JSONArray points = new JSONArray(result);
        ArrayList<String> stringArrayList = new ArrayList<String>();

        // looping through All Contacts
        for (int i = 0; i < points.length(); i++) {
          JSONObject c = points.getJSONObject(i);
          stringArrayList.add(c.getString("name"));
        }
        setupControls(stringArrayList.toArray(new String[stringArrayList.size()]));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    setUpMap();
    new GetRoutes().execute(new String("test"));
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

  private void setupControls(String[] choices) {
    // Selection of the spinner
    Spinner spinner = (Spinner) findViewById(R.id.spinner);

    // Application of the Array to the Spinner
    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, choices);
    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
    spinner.setAdapter(spinnerArrayAdapter);
  }

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
