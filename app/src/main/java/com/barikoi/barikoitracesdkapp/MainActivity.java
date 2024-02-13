package com.barikoi.barikoitracesdkapp;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;


import com.barikoi.barikoitrace.BarikoiTrace;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {


	private SwitchCompat switchService;

	private Spinner spinnertype;
	private FloatingActionButton tagloc;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		BarikoiTrace.initialize(this, "BARIKOI_API_KEY");
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
		tagloc=findViewById(R.id.fab);
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
						Toast.makeText(MainActivity.this, barikoiError.getMessage(), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onSuccess(BarikoiTraceUser traceUser) {
						Toast.makeText(MainActivity.this, "user set: " +traceUser.getName(), Toast.LENGTH_SHORT).show();
						tv_username.setText(traceUser.getPhone());
						BarikoiTrace.syncTripstate(new BarikoiTraceTripStateCallback() {

							@Override
							public void onSuccess() {
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
						public void onSuccess() {
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
				Toast.makeText(MainActivity.this,"Location Tagged", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(BarikoiTraceError barikoiError) {

			}
		}));
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
				if (ui > 0) tb.setUpdateInterval(ui);
				if (df > 0) tb.setDistancefilter(df);
				if (af > 0) tb.setAccuracyFilter(af);

				if (!spinnertype.getSelectedItem().equals("NONE")) {
					if (spinnertype.getSelectedItem().equals("ACTIVE")) mode = TraceMode.ACTIVE;
					if (spinnertype.getSelectedItem().equals("REACTIVE"))
						mode = TraceMode.REACTIVE;
					if (spinnertype.getSelectedItem().equals("PASSIVE"))
						mode = TraceMode.PASSIVE;
				}

				if (BarikoiTrace.isOnTrip() || BarikoiTrace.isLocationTracking()) {
					Log.d("locationupdate", "already running no need to start again");
					//System.out.println("already running no need to start again");
			 		Toast.makeText(getApplicationContext(), "trip already running!! no need to start again", Toast.LENGTH_SHORT).show();

				}  else {
					tb.setDebugModeOn();
					if (mode == null) mode = tb.build();
//					BarikoiTrace.startTracking(mode);
					BarikoiTrace.startTrip("test", mode, new BarikoiTraceTripStateCallback() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "trip started!!", Toast.LENGTH_SHORT).show();

						}

						@Override
						public void onFailure(BarikoiTraceError barikoiError) {
							Toast.makeText(getApplicationContext(), barikoiError.getMessage(), Toast.LENGTH_SHORT).show();

							switchService.setChecked(false);
						}
					});

				}
			}
			else {
//				BarikoiTrace.stopTracking();
					BarikoiTrace.endTrip(new BarikoiTraceTripStateCallback() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "trip stopped!!", Toast.LENGTH_SHORT).show();

						}

						@Override
						public void onFailure(BarikoiTraceError barikoiError) {
							switchService.setChecked(true);
							Toast.makeText(getApplicationContext(), barikoiError.getMessage(), Toast.LENGTH_SHORT).show();

						}
					});
					//if (!BarikoiTrace.isOnTrip()) {
						//Toast.makeText(getApplicationContext(), "trip stopped!!", Toast.LENGTH_SHORT).show();
					//}
				}
		});

	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.top_menu, menu);
		return true;
	}


}
