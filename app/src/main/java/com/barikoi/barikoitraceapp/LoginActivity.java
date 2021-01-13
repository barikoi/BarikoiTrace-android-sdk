package com.barikoi.barikoitraceapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.barikoi.barikoitraceapp.Api.loginurl;

public class LoginActivity extends AppCompatActivity {



    protected  String USER_ID="user_id",PASSWORD="password",TOKEN="token",EMAIL="email",NAME="name", ENROLLED="enrolled";
    EditText emailText,passText;
    TextView signin_link,resetpass;
    Button login,skip;
    ProgressDialog pd;
    private String player_id;
    private RequestQueue queue;
    private String token;
    private Boolean enrolled=true;
    private boolean isFirst=true;
    //public static final String loginurl="https://admin.barikoi.xyz:8090/auth/login";
    //public static final String usercheckurl="https://admin.barikoi.xyz:8090/auth/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();

    }
    private void init(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        emailText=(EditText)findViewById(R.id.input_email);
        passText=(EditText)findViewById(R.id.input_password);
        //signin_link=(TextView)findViewById(R.id.link_signup);
        resetpass=(TextView)findViewById(R.id.textView_resetpass);
        resetpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewDialog resetdialog=new ViewDialog();
                resetdialog.showresetpassInputDialog(LoginActivity.this);
            }
        });


        login=(Button)findViewById(R.id.btn_login);

        token = prefs.getString(TOKEN, "");
        enrolled = prefs.getBoolean(ENROLLED, false);
        isFirst = prefs.getBoolean("isFirst", true);
        proceedusercheck();


        emailText.setText(prefs.getString(EMAIL,""));

//        signin_link.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent signup=new Intent(LoginActivity.this, SignupActivity.class);
//                startActivity(signup);
//                finish();
//            }
//        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login(){
        if (!validate()) {
            onLoginFailed();
            return;
        }
        final String email = emailText.getText().toString();
        final String password = passText.getText().toString();
        //login.setEnabled(false);

        pd = new ProgressDialog(this);
        pd.setMessage("Authenticating...");
        pd.show();

        RequestQueue queue= RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        StringRequest request=new StringRequest(Request.Method.POST,loginurl,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response){
                        try {
                            JSONObject responsedata= new JSONObject(response.toString());
                            //JSONObject tokendata= responsedata.getJSONObject("data");
                            token= responsedata.getString("data");
                            enrolled= responsedata.getBoolean("enrolled");
                            Log.d("Login", "enrolled: " +enrolled);
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(EMAIL,email);
                            editor.putString(TOKEN,token);
                            editor.putBoolean(ENROLLED,enrolled);
                            editor.commit();
                            //OneSignal.setEmail(email);
                            pd.dismiss();
                            onLoginSuccess();
                            // onLoginFailed();

                        } catch (JSONException e) {
                            pd.dismiss();
                            Toast.makeText(getApplicationContext(), "Problem or change in server, please wait and try again", Toast.LENGTH_LONG).show();
                        }
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
                                    onLoginFailed();
                            }
                        }
                    }
                }
        ){
            @Override

            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                //parameters.put("id", id.getText().toString());
                Log.d("MainActivity", "email: " +email);
                Log.d("MainActivity", "pass: " +password);
                parameters.put("email",email);
                parameters.put("password",password);
                //parameters.put("device_ID",player_id);
                return parameters;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        //login.setEnabled(true);
        /*PlaceDbAdapter db=new PlaceDbAdapter(this);
        db.open();
        db.deleteAllPlaces();
        db.close();*/
//        finish();
//        Intent i=new Intent(this,MainActivity.class);
//        startActivity(i);
        proceedusercheck();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed,check if email and password is correct", Toast.LENGTH_LONG).show();

       /* PlaceDbAdapter db=new PlaceDbAdapter(this);
        db.open();
        db.deleteAllPlaces();
        db.close();*/
        //login.setEnabled(true);
    }

    private boolean validate(){
        boolean valid = true;

        String email = emailText.getText().toString();
        String password = passText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("enter a valid email address");
            valid = false;
        } else {
            emailText.setError(null);
        }

        if (password.isEmpty()) {
            passText.setError("Password required");
            valid = false;
        } else {
            passText.setError(null);
        }

        return valid;
    }

    private void proceedusercheck() {
        if (token != "") {
            queue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
            StringRequest request = new StringRequest(Request.Method.GET, Api.usercheckurl,
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
                                editor.putString(TOKEN, token);
                                editor.putBoolean(ENROLLED,enrolled);
                                editor.putString(USER_ID, user.getString("id"));
                                editor.putString(EMAIL, user.getString("email"));
//                                OneSignal.setEmail(user.getString("email"));
//                                OneSignal.sendTag(Api.EMAIL,user.getString("email"));
                                editor.putString(NAME, user.getString("name"));
                                editor.apply();

                                finish();
//                                Intent i=new Intent(LoginActivity.this,MainActivity.class);
//                                startActivity(i);
                                if (enrolled) {
                                    routeToAppropriatePage(2);
                                }else {
                                    //routeToAppropriatePage(3);
                                    Toast.makeText(getApplicationContext(), "You have to enroll company to continue. Contact Admin panel", Toast.LENGTH_SHORT).show();
                                    //logout(LoginActivity.this);
                                    routeToAppropriatePage(1);
                                }

                            } catch (JSONException e){
                                Toast.makeText(getApplicationContext(), "Problem or change in server, please wait and try again", Toast.LENGTH_LONG).show();
                                routeToAppropriatePage(1);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(error instanceof AuthFailureError){
                                Toast.makeText(LoginActivity.this, "authentication problem, redirecting to login", Toast.LENGTH_SHORT).show();

                            }
                            else if (response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:{
                                        Toast.makeText(LoginActivity.this, "not logged in, redirecting to login", Toast.LENGTH_SHORT).show();

                                    }
                                    default:

                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Could not connect to account", Toast.LENGTH_LONG).show();

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
                Intent intent = getIntent();
                startActivity(intent);
				//finish();
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
