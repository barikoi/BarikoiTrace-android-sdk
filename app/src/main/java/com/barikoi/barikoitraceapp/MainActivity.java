package com.barikoi.barikoitraceapp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;

import com.barikoi.barikoitrace.BarikoiTrace;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Telemetry;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import java.util.List;

import static com.barikoi.barikoitraceapp.Api.USER_ID;
import static com.barikoi.barikoitraceapp.NetworkcallUtils.logout;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
		PermissionsListener {

	private AppCompatButton btnStart, btnStop;
	private String token, username;
	private TextView tv_username;
	public int REQUEST_CHECK_PERMISSION = 0x1;
	private int LOCATION_SETTING_REQUEST = 999;
	private SwitchCompat switchService;
	private MapboxMap mMap = null;
	private MapView mapView = null;
	private LocationEngine locationEngine = null;
	private LocationLayerPlugin locationPlugin= null;
	private FloatingActionButton fab;
	private PermissionsManager permissionsManager =null;
	private final static String TAG = "MainActivity";
	private BarikoiTrace barikoiTrace;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//btnStart = findViewById(R.id.btn_start);
		//btnStop = findViewById(R.id.btn_stop);
		fab = findViewById(R.id.fab);
		Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
		Telemetry.disableOnUserRequest();
		mapView = findViewById(R.id.mapView);
		mapView.setStyleUrl(getString(R.string.map_view_styleUrl));
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		/*locationTask = new LocationTask(MainActivity.this,new LocationTaskListener(){

			@Override
			public void onLocationAvailability(boolean available) {

			}

			@Override
			public void oneEndTask() {

			}

			@Override
			public void OnLocationChanged(Location location) {

			}
		});
		locationTask.displayLocation();*/
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		token = prefs.getString("token", "");
		String userid=prefs.getString(USER_ID,"");
		BarikoiTrace.initialize(this,getString(R.string.barikoi_api_key));
		BarikoiTrace.setEmail("sadmansakib69@yahoo.com", new BarikoiTraceUserCallback() {
			@Override
			public void onFailure(BarikoiTraceError barikoiError) {

			}

			@Override
			public void onSuccess(BarikoiTraceUser traceUser) {

			}
		});
		username = prefs.getString(Api.NAME, "");
		tv_username = findViewById(R.id.tvUserName);
		switchService = findViewById(R.id.switchService);

		tv_username.setText(username);


		if (BarikoiTrace.isLocationTracking()) {
			//Log.d("locationupdate","already running no need to start again");
			switchService.setChecked(true);
		}
		switchService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

				if (b) {

					if (BarikoiTrace.isLocationTracking()) {
						Log.d("locationupdate","already running no need to start again");
						//System.out.println("already running no need to start again");
						Toast.makeText(getApplicationContext(), "Service already running!! no need to start again", Toast.LENGTH_SHORT).show();
						switchService.setChecked(false);
					}else if(BarikoiTrace.isBatteryOptimizationEnabled()){
						BarikoiTrace.disableBatteryOptimization();
						switchService.setChecked(false);
					}else if(!BarikoiTrace.isLocationPermissionsGranted()){
						BarikoiTrace.requestLocationPermissions(MainActivity.this);
					}else if(!BarikoiTrace.isLocationSettingsOn()){
						BarikoiTrace.requestLocationServices(MainActivity.this);
					}
					else {

						BarikoiTrace.startTracking(new TraceMode.Builder().setUpdateInterval(3).build());
						if (BarikoiTrace.isLocationTracking()) {
							Toast.makeText(getApplicationContext(), "Service started!!", Toast.LENGTH_SHORT).show();
						}
					}
				}else{
					switchService.setChecked(false);
					if (BarikoiTrace.isLocationTracking()) {
						BarikoiTrace.stopTracking();
						if (!BarikoiTrace.isLocationTracking()) {
							Toast.makeText(getApplicationContext(), "Service stopped!!", Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		});

		//displayLocation();

		//showEnableLocationSetting();


		//locationTask.showEnableLocationSetting();

//		btnStart.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//
//				if (isMyServiceRunning(getApplicationContext(), TimerService.class)) {
//					Log.d("locationupdate","already running no need to start again");
//					//System.out.println("already running no need to start again");
//					Toast.makeText(getApplicationContext(), "Service already running!! no need to start again", Toast.LENGTH_SHORT).show();
//				} else {
//					startservice();
//					if (isMyServiceRunning(getApplicationContext(), TimerService.class)) {
//						Toast.makeText(getApplicationContext(), "Service started!!", Toast.LENGTH_SHORT).show();
//					}
//				}
//
//			}
//		});
//
//		btnStop.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//
//				if (isMyServiceRunning(getApplicationContext(), TimerService.class)) {
//					stopService(new Intent(MainActivity.this, TimerService.class));
//					if (!isMyServiceRunning(getApplicationContext(), TimerService.class)) {
//						Toast.makeText(getApplicationContext(), "Service stopped!!", Toast.LENGTH_SHORT).show();
//					}
//				}
//
//
//			}
//		});

	}

	@Override
	public void onMapReady(MapboxMap mapboxMap) {
		//Log.d(TAG,"map ready");
		mMap = mapboxMap;
		enableLocation();

		mMap.getUiSettings().setCompassEnabled(false);

//        val getLocationTask = GetLocationTask(this)
//        getLocationTask.displayLocation()
//        mapboxMap.setStyle(Style.Builder().fromUrl(getString(R.string.map_view_styleUrl))) {
//
//            // Custom map style has been loaded and map is now ready
//            enableLocationComponent()
//
//        }

		//mapView!!.setStyleUrl(getString(R.string.map_view_styleUrl))


//        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
//            enableLocationComponent()
//
//        }
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (locationEngine != null) {
					@SuppressLint("MissingPermission")
					Location lastLocation = locationEngine.getLastLocation();
					if (lastLocation != null) {
						setCameraPosition(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 13.0);
					} else {
						locationEngine.requestLocationUpdates();
					}
				} else {
					enableLocation();
				}
			}
		});

	}

	private void enableLocation() {
		if (PermissionsManager.areLocationPermissionsGranted(this)) {
			// Create an instance of LOST location engine
			Log.d(TAG, "enableLocation: " +locationPlugin);
			initializeLocationEngine();
		} else {
			permissionsManager = new PermissionsManager(this);
			permissionsManager.requestLocationPermissions(this);
		}
	}

	private void initializeLocationEngine() {
		LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
		locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
		locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
		locationEngine.activate();
		Log.d("Search", "location Engine: " +locationEngine);
		Log.d(TAG, "locationPlugin: " +locationPlugin);

		if (locationPlugin == null) {
			Log.d(TAG, "locationPlugin 2: " +locationPlugin);
			locationPlugin = new LocationLayerPlugin(mapView, mMap, locationEngine, LocationLayerOptions.builder(this).maxZoom(25.0).build());
			locationPlugin.setLocationLayerEnabled(true);
			locationPlugin.setCameraMode(CameraMode.TRACKING);
			locationPlugin.setRenderMode(RenderMode.COMPASS);
			mMap.moveCamera(CameraUpdateFactory.zoomTo(13.0));
			//map!!.animateCamera(CameraUpdateFactory.zoomTo(17.0))
		}
		// currentLocation.displayLocation();

		locationEngine.addLocationEngineListener(this);
		locationEngine.requestLocationUpdates();
		Location lastLocation = locationEngine.getLastLocation();
		if (lastLocation != null) {
			setCameraPosition(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 13.0);
			locationEngine.removeLocationUpdates();
		} else {
			locationEngine.addLocationEngineListener(this);
		}

	}

	private void setCameraPosition(LatLng location, Double zoom) {
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new
				LatLng(location.getLatitude(), location.getLongitude()), zoom));
	}

	private void startservice() {


	}

	@Override
	protected void onStart() {
		super.onStart();
		if (locationEngine != null) {
			locationEngine.requestLocationUpdates();
		}
		if (locationPlugin != null) {
			locationPlugin.onStart();
		}

		mapView.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (locationEngine != null) {
			locationEngine.removeLocationUpdates();
		}
		if (locationPlugin != null) {
			locationPlugin.onStop();
		}
		mapView.onStop();

	}

	@Override
	protected void onDestroy() {

		if (locationEngine != null) {
			locationEngine.deactivate();
		}
		mapView.onDestroy();

//		Log.d("MainActivity", "onDestroy");
//		Intent broadcastIntent = new Intent();
//		broadcastIntent.setAction("restartservice");
//		broadcastIntent.setClass(this, Restarter.class);
//		this.sendBroadcast(broadcastIntent);

		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onConnected() {
		locationEngine.requestLocationUpdates();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("OnLoc", "Onlocchanged");
		if (location != null) {
			if (mMap != null)

				locationEngine.removeLocationEngineListener(this);
		} else {
			locationEngine.requestLocationUpdates();
		}

	}

	@Override
	public void onExplanationNeeded(List<String> permissionsToExplain) {

	}

	@Override
	public void onPermissionResult(boolean granted) {


		enableLocation();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.top_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {

		switch (item.getItemId()){
			case R.id.menu_settings:
				break;
			case R.id.menu_logout:
				new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog)
						.setTitle(R.string.logout)
						.setMessage(R.string.sure_log_out)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								 logout(MainActivity.this);
							}
						})
						.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						})
					// do nothing
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
