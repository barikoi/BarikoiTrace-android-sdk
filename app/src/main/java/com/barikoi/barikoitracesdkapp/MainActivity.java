package com.barikoi.barikoitracesdkapp;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;


import com.barikoi.barikoitrace.BarikoiTrace;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceLocationInfo;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;
import com.barikoi.barikoitrace.service.BarikoiTreaceEventCallback;
import com.barikoi.barikoitrace.service.BarikoiTraceReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {


	private SwitchCompat switchService;

	private Spinner spinnertype;

	private BarikoiTraceReceiver receiver = new BarikoiTraceReceiver();

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		BarikoiTrace.initialize(this, "MjA1NDo4MjBSTUxLTEs5");
		BarikoiTrace.setBroadcastingEnabled(true);
		receiver.setEventCallback(new BarikoiTreaceEventCallback() {
			@Override
			public void onError(BarikoiTraceError barikoiError) {
				Toast.makeText(MainActivity.this, barikoiError.getMessage(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onLocationReceived(BarikoiTraceLocationInfo barikoiLocationInfo) {
				Toast.makeText(MainActivity.this, "Location received", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onLocationUpdated(Location location) {
				Toast.makeText(MainActivity.this, "Location updated: "+location.getLatitude()+ " "+ location.getLongitude(), Toast.LENGTH_SHORT).show();
			}
		});
		BarikoiTrace.requestNotificationPermission(this);
		if (!BarikoiTrace.isLocationPermissionsGranted()) {
			BarikoiTrace.requestLocationPermissions(MainActivity.this);
		}
		if (!BarikoiTrace.isLocationSettingsOn()) {
			BarikoiTrace.requestLocationServices(MainActivity.this);
		}
		//set UI components
		EditText tv_username = findViewById(R.id.tvUserName);
		switchService = findViewById(R.id.switchService);
        FloatingActionButton tagloc = findViewById(R.id.fab);
		Button setuser=findViewById(R.id.button_set_user);
		setuser.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				if(BarikoiTrace.isOnTrip()){
					Toast.makeText(MainActivity.this, "cannot change user mid journey!", Toast.LENGTH_SHORT).show();
					return;
				}
				BarikoiTrace.setOrCreateUser("sakib 4",null,tv_username.getText().toString(), new BarikoiTraceUserCallback() {
					@Override
					public void onFailure(BarikoiTraceError barikoiError) {
						//if user not fetched from online, alternatively check if user already saved in device storage for trace
						if(BarikoiTrace.getUser().getPhone().equals("01676529696")){
							Toast.makeText(MainActivity.this, "user not found in online , found in local storage",Toast.LENGTH_SHORT).show();
							//user found offline, prceed to call same operations as in onSuccess method
						}else{
							Toast.makeText(MainActivity.this, barikoiError.getMessage(), Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void onSuccess(BarikoiTraceUser traceUser) {
						Toast.makeText(MainActivity.this, "user set: " +traceUser.getName(), Toast.LENGTH_SHORT).show();
						tv_username.setText(traceUser.getPhone());
						BarikoiTrace.syncTripstate(new BarikoiTraceTripStateCallback() {

							@Override
							public void onSuccess(Trip trip) {
								switchService.setChecked(BarikoiTrace.isOnTrip());

							}

							@Override
							public void onFailure(BarikoiTraceError barikoiError) {
								Log.e("tripstate", barikoiError.getMessage());
							}
						});
					}
				});
			}
		});
		SwitchCompat switchupdate = findViewById(R.id.switchBroadcast);
		switchupdate.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b) {
				BarikoiTrace.setBroadcastingEnabled(true);

			} else {
				BarikoiTrace.setBroadcastingEnabled(false);
			}
		});
		//set user
		if(BarikoiTrace.getUserId()==null){
			BarikoiTrace.setOrCreateUser("sakib 5",null,"01111111124", new BarikoiTraceUserCallback() {
				@Override
				public void onFailure(BarikoiTraceError barikoiError) {
					Toast.makeText(MainActivity.this, barikoiError.getMessage(), Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onSuccess(BarikoiTraceUser traceUser) {
					Toast.makeText(MainActivity.this, "user set: " +traceUser.getName(), Toast.LENGTH_SHORT).show();
					tv_username.setText(traceUser.getPhone());
					BarikoiTrace.syncTripstate(new BarikoiTraceTripStateCallback() {

						@Override
						public void onSuccess(Trip trip) {
							switchService.setChecked(BarikoiTrace.isOnTrip());

						}

						@Override
						public void onFailure(BarikoiTraceError barikoiError) {
							Log.e("tripstate", barikoiError.getMessage());
						}
					});
				}
			});
		}
		else{
			tv_username.setText(BarikoiTrace.getUser().getPhone());
		}




		BarikoiTrace.setOfflineTracking(true);
		//username = prefs.getString(Api.NAME, "");

		tagloc.setOnClickListener(view ->
				BarikoiTrace.updateCurrentLocation(new BarikoiTraceLocationUpdateCallback() {
					@Override
					public void onlocationUpdate(Location location) {
						Toast.makeText(MainActivity.this,"Location Tagged, service running:"+BarikoiTrace.isLocationTracking(), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(BarikoiTraceError barikoiError) {

					}
				})
		);

		String[] types = new String[]{"NONE", "ACTIVE", "REACTIVE", "PASSIVE"};
		spinnertype = findViewById(R.id.spinnerType);
		ArrayAdapter<String> aa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnertype.setAdapter(aa);

		if (BarikoiTrace.isOnTrip()) {
			Log.d("locationupdate","already running no need to start again");
			switchService.setChecked(true);
		}
		BarikoiTrace.checkAppServicePermission(this);
//		BarikoiTrace.startTracking(new TraceMode.Builder().setUpdateInterval(7).setPingSyncInterval(21).build());
		//BarikoiTrace.openAutostartsettings(this);
		switchService.setOnCheckedChangeListener((compoundButton, b) -> {
			if (!compoundButton.isPressed()) return;
			/*TraceMode mode = new TraceMode.Builder().setUpdateInterval(5).build();
			if(b) BarikoiTrace.startTracking(mode);
			else BarikoiTrace.stopTracking();*/
			if (b) {

				TraceMode mode = null;
				EditText uitext =  findViewById(R.id.input_updateinterval);
				EditText dftext =  findViewById(R.id.input_distancefilter);
				EditText aftext =  findViewById(R.id.input_accuracy);
				int ui = Integer.parseInt(uitext.getText().toString());
				int df = Integer.parseInt(dftext.getText().toString());
				int af = Integer.parseInt(aftext.getText().toString());
				TraceMode.Builder tb = new TraceMode.Builder();
				if (ui > 0) {
					tb.setUpdateInterval(ui);
					tb.setPingSyncInterval(ui * 3);
				}

				if (df > 0) tb.setDistancefilter(df);
				if (af > 0) tb.setAccuracyFilter(af);

				if (!spinnertype.getSelectedItem().equals("NONE")) {
					if (spinnertype.getSelectedItem().equals("ACTIVE")) mode = TraceMode.ACTIVE;
					if (spinnertype.getSelectedItem().equals("REACTIVE"))
						mode = TraceMode.REACTIVE;
					if (spinnertype.getSelectedItem().equals("PASSIVE"))
						mode = TraceMode.PASSIVE;
					if (mode == null) mode = tb.build();
					BarikoiTrace.startTracking(mode);
				}

				if (BarikoiTrace.isOnTrip() || BarikoiTrace.isLocationTracking()) {
					Log.d("locationupdate", "already running no need to start again"+ BarikoiTrace.isOnTrip() + " "+BarikoiTrace.isLocationTracking());
			 		Toast.makeText(getApplicationContext(), "trip already running!! no need to start again", Toast.LENGTH_SHORT).show();

				}  else {
					tb.setDebugModeOn();
					if (mode == null) mode = tb.build();
					TraceMode finalMode = mode;
					if(BarikoiTrace.isLocationPermissionsGranted()){
						BarikoiTrace.startTracking(finalMode);
					}else{
						switchService.setChecked(false);
						Toast.makeText(this, "location permission denied", Toast.LENGTH_SHORT).show();
					}
					
					/*BarikoiTrace.startTrip("test", mode, new BarikoiTraceTripStateCallback() {
						@Override
						public void onSuccess(Trip trip) {
							Toast.makeText(getApplicationContext(), "trip started!! id: "+ trip.getTrip_id(), Toast.LENGTH_SHORT).show();

						}

						@Override
						public void onFailure(BarikoiTraceError barikoiError) {
							Toast.makeText(getApplicationContext(), barikoiError.getMessage(), Toast.LENGTH_SHORT).show();

							switchService.setChecked(false);
						}
					});*/

				}
			}
			else {
				BarikoiTrace.stopTracking();
					/*BarikoiTrace.endTrip(new BarikoiTraceTripStateCallback() {
						@Override
						public void onSuccess(Trip trip) {
							Toast.makeText(getApplicationContext(), "trip stopped!!: id" +trip.getTrip_id(), Toast.LENGTH_SHORT).show();

						}

						@Override
						public void onFailure(BarikoiTraceError barikoiError) {
							switchService.setChecked(true);
							Toast.makeText(getApplicationContext(), barikoiError.getMessage(), Toast.LENGTH_SHORT).show();

						}
					});*/
					//if (!BarikoiTrace.isOnTrip()) {
						//Toast.makeText(getApplicationContext(), "trip stopped!!", Toast.LENGTH_SHORT).show();
					//}
				}
		});

	}



	@Override
	protected void onResume() {
		super.onResume();
		BarikoiTrace.registerlocationupdate(receiver);
//		BarikoiTrace.refreshtracking();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.top_menu, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		BarikoiTrace.unregisterLocationUpdate(receiver);
	}
}
