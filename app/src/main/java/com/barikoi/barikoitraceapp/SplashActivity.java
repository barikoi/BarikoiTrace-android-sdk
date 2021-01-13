package com.barikoi.barikoitraceapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.barikoi.barikoitraceapp.Api.ENROLLED;
import static com.barikoi.barikoitraceapp.Api.usercheckurl;

public class SplashActivity extends AppCompatActivity {

    private String token="";
    private boolean isFirst=true;
    private Boolean enrolled;
    private RequestQueue queue;
    public static final int MULTIPLE_PERMISSIONS = 10;
    private double userLat, userLng;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        token = prefs.getString("token", "");
        enrolled = prefs.getBoolean("enrolled", false);
        isFirst = prefs.getBoolean("isFirst", true);

        proceedusercheck();

    }


    private void proceedusercheck() {
        if (token != "") {
            queue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
            StringRequest request = new StringRequest(Request.Method.GET, usercheckurl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject data = new JSONObject(response.toString());
                                JSONObject user = data.getJSONObject("data");
                                String user_id = user.getString("id");
                                String user_email = user.getString("email");

                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(Api.TOKEN, token);
                                editor.putBoolean(ENROLLED,enrolled);
                                editor.putString(Api.USER_ID, user.getString("id"));
                                editor.putString(Api.EMAIL, user.getString("email"));
//                                OneSignal.setEmail(user.getString("email"));
//                                OneSignal.sendTag(Api.EMAIL,user.getString("email"));
                                editor.putString(Api.NAME, user.getString("name"));
                                editor.apply();

                                finish();
//                                Intent i=new Intent(LoginActivity.this,MainActivity.class);
//                                startActivity(i);
                                Log.d("Login", "enrolled splash: " +enrolled);
                                if (enrolled) {
                                    routeToAppropriatePage(2);
                                }else {
                                    //routeToAppropriatePage(3);
                                    Toast.makeText(getApplicationContext(), "You have to enroll company to continue. Contact Admin panel", Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e){
                                Toast.makeText(getApplicationContext(), "Problem or change in server, please wait and try again", Toast.LENGTH_LONG).show();
                                routeToAppropriatePage(1);
                            }catch (Exception e) {
								Log.d("SplashActivity", "exception: " +e);

							}
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
							try {

                                if(error instanceof NoConnectionError){

                                    FrameLayout layout = findViewById(R.id.layoutView);
                                    Snackbar snackbar = Snackbar.make(layout,"Turn on your internet connection and Try again", Snackbar.LENGTH_INDEFINITE);
                                    snackbar.setBackgroundTint(getResources().getColor(R.color.colorRed));
                                    snackbar.setTextColor(getResources().getColor(R.color.white));
                                    snackbar.setAction(R.string.try_again, new View.OnClickListener(){
                                        @Override
                                        public void onClick(View view) {
                                            //snackbar.dismiss();
                                            proceedusercheck();
                                        }
                                    }).show();


                                    View snackbarView = snackbar.getView();

                                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                                    params.gravity = Gravity.TOP;
                                    snackbarView.setLayoutParams(params);
                                    //snackbarView.startAnimation(AnimationUtils.loadAnimation(host, R.anim.slide_in_snack_bar));


                                }
								NetworkResponse response = error.networkResponse;
								if (error instanceof AuthFailureError) {
									Toast.makeText(SplashActivity.this, "authentication problem, redirecting to login", Toast.LENGTH_SHORT).show();

								} else if (response != null && response.data != null) {
									switch (response.statusCode) {
										case 401: {
											Toast.makeText(SplashActivity.this, "not logged in, redirecting to login", Toast.LENGTH_SHORT).show();

										}
										default:

									}
								} else {
									Toast.makeText(SplashActivity.this, "Could not connect to account", Toast.LENGTH_LONG).show();

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
                    if (!token.equals(""))
                        params.put("Authorization", "bearer " + token);
                    return params;
                }

            };
            request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(request);
        }else {
            if (isFirst) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isFirst", false);
                editor.commit();
                //routeToAppropriatePage(0);
                routeToAppropriatePage(1);

            } else {
                Toast.makeText(getApplicationContext(), "no auth token", Toast.LENGTH_SHORT).show();

                routeToAppropriatePage(1);
            }

        }
    }

    private void routeToAppropriatePage(int routeopt) {
        // Example routing
        switch (routeopt){
            case 0: {
                Intent i = new Intent(this, SignupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                break;
            }
            case 1: {
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
                finish();
                break;
            }
            case 2: {
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                finish();
                break;
            }
            case 3: {
                Intent i = new Intent(this, CompanyNameActivity.class);
                startActivity(i);
                finish();
                break;
            }

        }
    }

}
