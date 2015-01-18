package com.gmail.chadapple.trackme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileDescriptor;

public class LocationService extends Service {
  /////////////////////////////////////////////////////////////
  static private final String TAG = "LocationService";
  private final IBinder mBinder = new LocationServiceBinder();
  private LocationCallback mCallback = null;

  public interface LocationCallback {
    void LocationChanged(Location location);
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
    // Acquire a reference to the system Location Manager
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Change = " + location.toString());
        if(mCallback != null) { mCallback.LocationChanged(location); }
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

    // Register the listener with the Location Manager to receive location updates
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15 * 1000, 0, locationListener);
    //locationManager.removeUpdates(locationListener);
  }
}
