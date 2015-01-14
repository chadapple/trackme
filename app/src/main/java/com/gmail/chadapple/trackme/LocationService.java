package com.gmail.chadapple.trackme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service {
  static private final String TAG = "LocationService";
  public LocationService() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    // If we get killed, after returning from here, restart
    //return START_STICKY;
    return START_NOT_STICKY;
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate");

    // Acquire a reference to the system Location Manager
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location location) { Log.i(TAG, "Location Change = " + location.toString()); }
      public void onStatusChanged(String provider, int status, Bundle extras) { Log.i(TAG, "Status Change"); }
      public void onProviderEnabled(String provider) { Log.i(TAG, "Provider Enable"); }
      public void onProviderDisabled(String provider) { Log.i(TAG, "Provider Disable"); }
    };

    // Register the listener with the Location Manager to receive location updates
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 0, locationListener);
    //locationManager.removeUpdates(locationListener);
  }
}
