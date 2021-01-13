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
import android.widget.EditText;
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


import static com.barikoi.barikoitraceapp.Api.ENROLLED;
import static com.barikoi.barikoitraceapp.Api.companyListurl;

public class SignupActivity extends AppCompatActivity {


    protected  String USER_ID="user_id",PASSWORD="password",TOKEN="token",EMAIL="email",NAME="name";

    private String name,email,password,number, cpassword, company_name;
    private Boolean enrolled;
    private EditText nameText,emailText,passwordText,numberText, cpasswordText;
    private Button signupButton,skip;
    private TextView loginLink;
    ProgressDialog pd;
    private RequestQueue queue;
    private String player_id, token;
    private Spinner spinnerList;
    private ArrayList<Company> companyList;
    private Boolean isSelected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        nameText=(EditText)findViewById(R.id.input_name);
        emailText=(EditText)findViewById(R.id.input_email);
        numberText=(EditText)findViewById(R.id.input_phone);
        passwordText=(EditText)findViewById(R.id.input_password);
        cpasswordText=(EditText)findViewById(R.id.confirm_password);
        spinnerList = findViewById(R.id.spinnerList);

		companyList = new ArrayList<>();
        getCompanyList();
//		companyList.add("Maya");
//		companyList.add("Sheba");
//		companyList.add("HungryNaki");
//		companyList.add("ShopUp");
//		SpinnerAdapter spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, companyList);
//		//spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//		spinnerList.setAdapter(spinnerAdapter);






        signupButton = (Button)findViewById(R.id.btn_signup);
        loginLink=(TextView)findViewById(R.id.link_login);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return
                // to the Login activity
                Intent loginpage= new Intent(SignupActivity.this,LoginActivity.class);
                startActivity(loginpage);
                finish();
            }
        });
    }

    public void signup() {

        if (!validate()) {
            //onSignupFailed();
            return;
        }

        signupButton.setEnabled(false);

        pd = new ProgressDialog(this);
        pd.setMessage("Signing up...");
        pd.show();

        RequestQueue queue= RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        StringRequest request=new StringRequest(Request.Method.POST,Api.signupurl,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response){
                        pd.dismiss();
                        onSignupSuccess();
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
                                        onSignupFailed(new String(error.networkResponse.data,"UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                            }
                        }
                        else    onSignupFailed(getString(R.string.couldnot_connect));
                    }
                }
        ){
            @Override

            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                //parameters.put("id", id.getText().toString());
                parameters.put("name",name);
                parameters.put("email",email);
                parameters.put("password",password);
                parameters.put("phone",number);
				parameters.put("company_name",company_name);
                parameters.put("userType","2");
                return parameters;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
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

                                    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item){

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
                                    adapter.add("Choose Company Name");

                                    spinnerList.setAdapter(adapter);
                                    spinnerList.setSelection(adapter.getCount()); //display hint

                                    spinnerList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                                            if (pos != adapter.getCount()) {
                                                company_name = adapter.getItem(pos);
                                                isSelected = true;
                                                Log.d("Login", "name: " + company_name);
                                            }
                                        }

                                        @Override
                                        public void onNothingSelected(AdapterView<?> adapterView) {

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
    public void onSignupSuccess() {
        pd = new ProgressDialog(this);
        pd.setMessage("authenticating...");
        pd.show();
        signupButton.setEnabled(true);
        queue= RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        StringRequest request=new StringRequest(Request.Method.POST,Api.loginurl,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response){
                        try {
                            JSONObject responsedata= new JSONObject(response.toString());
//                            JSONObject tokendata= responsedata.getJSONObject("data");
//                            String token= tokendata.getString("token");
                            Log.d("Login", "response: " +responsedata);
                            if(responsedata.has("data")) {
                                token = responsedata.getString("data");
                                enrolled= responsedata.getBoolean("enrolled");
                                Log.d("Login", "token: " + token);
                                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(EMAIL, email);
                                editor.putString(TOKEN, token);
                                editor.putBoolean(ENROLLED,enrolled);
                                editor.commit();
                                pd.dismiss();

                                onLoginDone();
                                // onLoginFailed();
                            }else{
                                Toast.makeText(getApplicationContext(), "Invalid Email or Password", Toast.LENGTH_LONG).show();
                                //onLoginFailed("Invalid Email or Password");
                            }

                        } catch (JSONException e) {
                            pd.dismiss();
                            Toast.makeText(SignupActivity.this, "problem problem or change in server, please wait and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pd.dismiss();
                        Toast.makeText(SignupActivity.this, "Problem connecting, please wait and try again.", Toast.LENGTH_LONG).show();

                    }
                }
        ){
            @Override

            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                //parameters.put("id", id.getText().toString());
                parameters.put("email",email);
                parameters.put("password",password);
                if (player_id != null) parameters.put("device_ID",player_id);
                return parameters;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);

    }

    public void onSignupFailed(String s) {
        if(!s.equals("")) {
            try {
                JSONObject data=new JSONObject(s).getJSONObject("message");
                Log.d("Signup", "error: " +data);
                if(data.has("email")){
                    emailText.setError(data.getJSONArray("email").getString(0));
                    Toast.makeText(getApplicationContext(),  data.getJSONArray("email").getString(0) , Toast.LENGTH_LONG).show();
                }
                if(data.has("phone")){
                    Log.d("Signup", "error phone: " +data.getJSONArray("phone").getString(0));
                    numberText.setError(data.getJSONArray("phone").getString(0));
                    Toast.makeText(getApplicationContext(),  data.getJSONArray("phone").getString(0) , Toast.LENGTH_LONG).show();
                }
                if(data.has("password")){
                    passwordText.setError(data.getJSONArray("password").getString(0));
                    Toast.makeText(getApplicationContext(),  data.getJSONArray("password").getString(0) , Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),  s, Toast.LENGTH_LONG).show();
            }
        }
        signupButton.setEnabled(true);
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
                                Log.d("MainActivity", "user data: " +user);
                                Log.d("MainActivity", "user data: " +user.getString("id"));
                                String user_id = user.getString("id");
                                String user_email = user.getString("email");

                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(Api.TOKEN, token);
                                editor.putBoolean(ENROLLED,enrolled);
                                editor.putString(Api.USER_ID, user.getString("id"));
                                editor.putString(Api.EMAIL, user.getString("email"));
                                editor.putString(Api.NAME, user.getString("name"));
                                editor.putString(Api.PHONE, user.getString("phone"));
                                editor.putString(Api.POINTS, user.getString("total_points"));
                                editor.putString(Api.SPENT_POINTS, user.getString("redeemed_points"));
                                editor.putString(Api.ISREFFERED,user.getString(Api.ISREFFERED));
                                editor.putString(Api.REFCODE,user.getString(Api.REFCODE));
                                editor.apply();

                                finish();
                                Intent i=new Intent(SignupActivity.this,MainActivity.class);
                                startActivity(i);

                            } catch (JSONException e){
                                Toast.makeText(SignupActivity.this, getString(R.string.prob_change_server), Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            NetworkResponse response = error.networkResponse;
                            if(error instanceof AuthFailureError){
                                Toast.makeText(SignupActivity.this, "authentication problem, redirecting to login", Toast.LENGTH_SHORT).show();

                            }
                            else if (response != null && response.data != null) {
                                switch(response.statusCode) {
                                    case 401:{
                                        Toast.makeText(SignupActivity.this, "not logged in, redirecting to login", Toast.LENGTH_SHORT).show();

                                    }
                                    default:

                                }
                            } else {
                                Toast.makeText(SignupActivity.this, getString(R.string.could_not_con_acc), Toast.LENGTH_LONG).show();

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
    public void onLoginDone() {
//        Intent welcome=new Intent(this,MainActivity.class);
//
//        welcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(welcome);
        proceedusercheck();
        /*Toast.makeText(getBaseContext(),  "done", Toast.LENGTH_LONG).show();
        ViewDialog newaccdone= new ViewDialog();
        newaccdone.changePassDialog(SignupActivity.this,"Welcome "+name,"Thank you for joining, you got 10 points for signing up! hurray!!");
        *//*Intent i=new Intent(this,MainActivity.class);
        startActivity(i);*/
    }
    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public boolean validate() {
        boolean valid = true;
        name = nameText.getText().toString();
        email = emailText.getText().toString();
        password = passwordText.getText().toString();
        number = numberText.getText().toString();
        cpassword=cpasswordText.getText().toString();


        if (name.equals("")) {
            valid = false;
            nameText.setError(getString(R.string.this_field_is_required));
        }
        if (email.equals("")) {
            valid = false;
            emailText.setError(getString(R.string.this_field_is_required));
        }
        if (password.equals("")) {
            valid = false;
            passwordText.setError(getString(R.string.this_field_is_required));
        }
        if (number.equals("")) {
            valid = false;
            numberText.setError(getString(R.string.this_field_is_required));
        }
        if (!isSelected){
            valid = false;
            Toast.makeText(getApplicationContext(), "Need to select company name", Toast.LENGTH_LONG).show();
        }
        else {

            if (name.length() < 3) {
                nameText.setError("at least 3 characters");
                valid = false;
            } else {
                nameText.setError(null);
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailText.setError("enter a valid email address");
                valid = false;
            } else {
                emailText.setError(null);
            }
            if (number.length() < 11) {
                numberText.setError("enter a valid mobile number");
                valid = false;
            } else {
                numberText.setError(null);
            }

            if (password.length() < 6) {
                passwordText.setError("More than 4 alphanumeric characters");
                valid = false;
            } else {
                passwordText.setError(null);
            }

            if (!password.equals(cpassword)) {
                cpasswordText.setError("Password Does not Match");
                valid = false;
            } else {
                cpasswordText.setError(null);
            }

        }
        return valid;
    }
}
