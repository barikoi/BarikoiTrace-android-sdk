package com.barikoi.barikoitraceapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.barikoi.barikoitrace.network.Api.TOKEN;
import static com.barikoi.barikoitraceapp.Api.ENROLLED;
import static com.barikoi.barikoitraceapp.Api.companyListurl;


public class CompanyNameActivity extends AppCompatActivity {

	private Spinner spinnerList;
	private Button submit;
	private RequestQueue queue;
	private ArrayList<Company> companyList;
	private String company_name;
	private int company_id;
	ProgressDialog pd;
	private String token;
	private Boolean isSelected = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_company_name);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		spinnerList = findViewById(R.id.spinnerList);
		submit = findViewById(R.id.btn_submit);
		token = prefs.getString(TOKEN, "");
		companyList = new ArrayList<>();
		getCompanyList();

		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isSelected) companyEnroll(company_id);
				else Toast.makeText(getApplicationContext(), "Need to select company name", Toast.LENGTH_LONG).show();
			}
		});

	}

	private void getCompanyList() {

//        itemList.clear();
		queue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
		StringRequest request = new StringRequest(Request.Method.GET, companyListurl,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						try {
							JSONObject data = new JSONObject(response.toString());
							JSONArray companyArray = data.getJSONArray("companies");
//                                Log.d("MainActivity", "data: " +data);
//                                Log.d("MainActivity", "Company array: " +companyArray);
							if (companyArray.length()>0){

								for (int i=0; i<companyArray.length(); i++) {
									JSONObject list = companyArray.getJSONObject(i);
									int id = list.getInt("id");
									String name = list.getString("name");
									Company comp = new Company(id, name);
									companyList.add(comp);
								}
								Log.d("MainActivity", "data: " +companyList);

								ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item){

									@Override
									public View getView(int position, View convertView, ViewGroup parent) {

										View v = super.getView(position, convertView, parent);
										if (position == getCount()) {
											((TextView)v.findViewById(android.R.id.text1)).setText("");
											((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount())); //"Hint to be displayed"
										}

										return v;
									}

									@Override
									public int getCount() {
										return super.getCount()-1; // you dont display last item. It is used as hint.
									}
								};

								adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
								//		adapter.add("Maya");
								//companyList = getCompanyList();

								Log.d("MainActivity", "Company array: " +companyList);
								for (int i =0; i<companyList.size(); i++) {
									adapter.add(companyList.get(i).getCompanyName());
								}

								adapter.add("Company Name");

								spinnerList.setAdapter(adapter);
								spinnerList.setSelection(adapter.getCount()); //display hint

								spinnerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
									@Override
									public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
										if (pos != adapter.getCount()) {
											company_name = adapter.getItem(pos);

											submit.setEnabled(true);
											for (Company c: companyList) {
												if (c.getCompanyName().equals(company_name)){
													company_id = c.getId();
													isSelected = true;
													Log.d("Company", "name: " + company_name);
													Log.d("Company", "id: " + company_id);
												}

											}

										}
									}

									@Override
									public void onNothingSelected(AdapterView<?> adapterView) {
										submit.setEnabled(false);
									}
								});
							}

						} catch (JSONException e){
							Toast.makeText(getApplicationContext(), "Problem or change in server, please wait and try again", Toast.LENGTH_LONG).show();
						}catch (Exception e) {
							Log.d("SplashActivity", "exception: " +e);

						}
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						try {
							NetworkResponse response = error.networkResponse;
							if (error instanceof AuthFailureError) {
								Toast.makeText(getApplicationContext(), "authentication problem, redirecting to login", Toast.LENGTH_SHORT).show();

							} else if (response != null && response.data != null) {
								switch (response.statusCode) {
									case 401: {
										Toast.makeText(getApplicationContext(), "not logged in, redirecting to login", Toast.LENGTH_SHORT).show();

									}
									default:

								}
							} else {
								Toast.makeText(getApplicationContext(), "Could not connect to account", Toast.LENGTH_LONG).show();

							}
							throw new Exception(String.valueOf(error.networkResponse.statusCode));
						}catch (Exception e) {
							Log.d("SplashActivity", "exception: " +e);

						}
					}
				}
		) {

			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> params = new HashMap<String, String>();
//                    if (!token.equals(""))
//                        params.put("Authorization", "bearer " + token);
				return params;
			}

		};
		request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		queue.add(request);

		Log.d("MainActivity", "itemList: " +companyList);
		//return itemList;
	}

	public void companyEnroll(final int company_id) {

		submit.setEnabled(false);

		pd = new ProgressDialog(this);
		pd.setMessage("Submitting...");
		pd.show();

		RequestQueue queue= RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
		StringRequest request=new StringRequest(Request.Method.POST,Api.companyEnrollurl+company_id,
				new Response.Listener<String>(){
					@Override
					public void onResponse(String response){
						pd.dismiss();

						Log.d("Company", "response: " +response);

						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean(ENROLLED, true);
						editor.commit();

						Intent i=new Intent(CompanyNameActivity.this, MainActivity.class);
						startActivity(i);
						finish();
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						pd.dismiss();
						//NetworkcallUtils.handleResponse(error, getApplicationContext());
						if(error != null && error.networkResponse != null){
							switch(error.networkResponse.statusCode){
								default:
									try {
										JSONObject data=new JSONObject(new String(error.networkResponse.data,"UTF-8"));
										Toast.makeText(getApplicationContext(),  data.getString("message") , Toast.LENGTH_LONG).show();
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									} catch (JSONException e) {
										e.printStackTrace();
									}
							}
						}
						else {
							Toast.makeText(getApplicationContext(), getString(R.string.couldnot_connect), Toast.LENGTH_SHORT).show();
						}
					}
				}
		){
			@Override

			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> params = new HashMap<String, String>();
                    if (!token.equals(""))
                        params.put("Authorization", "bearer " + token);
				return params;
			}
		};
		request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		queue.add(request);
	}
}
