package com.gmail.chadapple.trackme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {
  /////////////////////////////////////////////////////////////
  static private final String TAG = "LocationService";
  private final IBinder mBinder = new LocationServiceBinder();
  private LocationCallback mCallback = null;
  private LocationServiceMode mMode = LocationServiceMode.NONE;
  private String mCurrentRouteName = null;
  private String mServer = null;
  private Handler mHandler = new Handler();
  private final String SERVER_POINTS_URL = "/trackme/insertPoint.php";
  private final String SERVER_ROUTE_URL = "/trackme/route.php";
  private final String POINT_NAME = "name";
  private final String POINT_LAT = "lat";
  private final String POINT_LONG = "long";
  private final String POINT_SPEED = "speed";

  // Periodic task to update the map when in Monitor mode
  private Runnable mUpdateMonitor = new Runnable() {
    @Override
    public void run() {
      if(mMode == LocationServiceMode.MONITOR) {
        if(mCallback != null && mMode == LocationServiceMode.MONITOR)
        {
          mCallback.clearMap();
          new GetPoints().execute();
        }
      }
      mHandler.postDelayed(mUpdateMonitor, 60000);  // every 1 minute
    }
  };

  // Define a listener that responds to location updates
  private LocationListener mLocationListener = new LocationListener() {
    public void onLocationChanged(Location location) {
      Log.i(TAG, "Location Change = " + location.toString());
      // Only call callback LocationChanged if we are in tracking mode
      // Otherwise, the main activity will be waiting on location updates from the server
      if(mCallback != null && mMode == LocationServiceMode.TRACK)
      {
        mCallback.LocationChanged(location);
        new PushServer().execute(location);
      }
    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.i(TAG, "Status Change");
    }
    public void onProviderEnabled(String provider) {
      Log.i(TAG, "Provider Enable");
    }
    public void onProviderDisabled(String provider) {
      Log.i(TAG, "Provider Disable");
    }
  };

  public enum LocationServiceMode
  {
    NONE,
    MONITOR,
    TRACK
  }

  public interface LocationCallback {
    void LocationChanged(Location location);
    void clearMap();
  }
  public class LocationServiceBinder extends Binder {
    LocationService getService() { return LocationService.this; }
  }

  /**
   * LocationService()
   */
  public LocationService() {
  }

  /**
   * onBind()
   * @param intent
   * @return
   */
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  void registerCallback(LocationCallback callback) {
   mCallback = callback;
  }

  /**
   * onStartCommand()
   * @param intent
   * @param flags
   * @param startId
   * @return
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    // If we get killed, after returning from here, restart
    //return START_STICKY;
    return START_NOT_STICKY;
  }

  /**
   * onCreate()
   */
  @Override
  public void onCreate() {
    mUpdateMonitor.run();
  }

  public LocationServiceMode getMode()
  {
    return mMode;
  }

  public void setMode(LocationServiceMode m, String routeName)
  {
    mMode = m;
    mCurrentRouteName = routeName;

    // Acquire a reference to the system Location Manager
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    if(mMode == LocationServiceMode.TRACK)
    {
      // Register the listener with the Location Manager to receive location updates
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 0, mLocationListener);
    }
    else if(mMode == LocationServiceMode.MONITOR)
    {
      locationManager.removeUpdates(mLocationListener);
      new GetPoints().execute();
    }
  }

  public void setServer(String server)
  {
    mServer = server;
  }

  // Background async task to push location data to server
  private class PushServer extends AsyncTask<Location, Void, Void> {
    protected Void doInBackground(Location...locations) {
      StringBuilder sBuilder = new StringBuilder();
      String line;
      try {
        String urlName = mServer + SERVER_POINTS_URL + "?" + POINT_NAME + "=" + mCurrentRouteName + "&" +
                POINT_LAT + "=" + new Double(locations[0].getLatitude()).toString() + "&" +
                POINT_LONG + "=" + new Double(locations[0].getLongitude()).toString() + "&" +
                POINT_SPEED + "=" + new Double(locations[0].getSpeed()).toString();
        URL url = new URL(urlName);
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
      String result = sBuilder.toString();
      if(result.startsWith("Success"))
      {
        Log.i(TAG, "Sent location to server");
      }
      else
      {
        Log.i(TAG, "Error sending location to server");
      }
      return null;
    }
    protected void onProgressUpdate(Void...params) {
    }
    protected void onPostExecute(Void...params) {
    }
  }

  // Background async task to get route names
  private class GetPoints extends AsyncTask<Void, Void, String> {
    protected String doInBackground(Void...params) {
      StringBuilder sBuilder = new StringBuilder();
      String line;
      try {
        String urlName = mServer + SERVER_ROUTE_URL + "?" + POINT_NAME + "=" + mCurrentRouteName;
        URL url = new URL(urlName);
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
      return sBuilder.toString();
    }
    protected void onProgressUpdate(Void...params) {
    }
    protected void onPostExecute(String result) {
      try {
        JSONArray points = new JSONArray(result);

        // looping through points on route
        for (int i = 0; i < points.length(); i++) {
          JSONObject c = points.getJSONObject(i);
          Location l = new Location("");  // provider is unnecessary
          l.setLatitude(Double.parseDouble(c.getString("latitude")));
          l.setLongitude(Double.parseDouble(c.getString("longitude")));
          l.setSpeed(Float.parseFloat(c.getString("speed")));
          if (mCallback != null && mMode == LocationServiceMode.MONITOR) {
            mCallback.LocationChanged(l);
          }
        }
      }
      catch (JSONException e) {
        Log.e(TAG, e.toString());
        e.printStackTrace();
      }
    }
  }
}
