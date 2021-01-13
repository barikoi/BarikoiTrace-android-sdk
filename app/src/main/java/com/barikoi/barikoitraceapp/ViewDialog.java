package com.barikoi.barikoitraceapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sakib on 4/19/2017.
 */

public class ViewDialog {
    private ProgressDialog pd;
    //viewdialog
    public static final String resetpassUrl="https://admin.barikoi.xyz:8090/auth/password/reset";


//    public void showAuthAddDialog(final Activity activity, String point, String uCode){
//        final Dialog dialog = new Dialog(activity);
//
//        dialog.setContentView(R.layout.popup);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//
//        TextView points = (TextView) dialog.findViewById(R.id.textView_pointdialog);
//        TextView code=(TextView)dialog.findViewById(R.id.textView_codeback);
//
//        points.setText(point );
//        code.setText(uCode);
//
//        Button dialogButton = (Button) dialog.findViewById(R.id.buttonokay);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//                Intent i=new Intent(activity, MainActivity.class);
//                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                activity.startActivity(i);
//            }
//        });
//        dialog.show();
//    }
//
//    public void showAddDialog(final Activity activity, String uCode){
//        final Dialog dialog = new Dialog(activity);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setCancelable(false);
//        dialog.setContentView(R.layout.popup);
//        TextView points = (TextView) dialog.findViewById(R.id.textView_pointdialog);
//        TextView code=(TextView)dialog.findViewById(R.id.textView_codeback);
//
//        code.setText(uCode);
//        points.setText("Log in to get points by adding places");
//
//        Button dialogButton = (Button) dialog.findViewById(R.id.buttonokay);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//                Intent i=new Intent(activity, MainActivity.class);
//                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                activity.startActivity(i);
//            }
//        });
//        dialog.show();
//    }
//
//    public void showauthfailureDialog(final Activity activity){
//        final Dialog dialog = new Dialog(activity);
//
//        dialog.setContentView(R.layout.popup);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//
//        TextView points = (TextView) dialog.findViewById(R.id.textView_pointdialog);
//        TextView code=(TextView)dialog.findViewById(R.id.textView_codeback);
//        TextView congrats=(TextView)dialog.findViewById(R.id.textViewTitle);
//        TextView messageViw=(TextView) dialog.findViewById(R.id.textViewMessage);
//
//        messageViw.setText("Hello human! \n Somethng wrong happed!\n Login again!");
//        congrats.setText("Are you logged in?");
//        points.setVisibility(View.INVISIBLE);
//        code.setVisibility(View.INVISIBLE);
//
//        Button dialogButton = (Button) dialog.findViewById(R.id.buttonokay);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//                Intent i=new Intent(activity,LoginActivity.class);
//                activity.startActivity(i);
//            }
//        });
//
//        dialog.show();
//    }
//    public void showmessageDialog(final Activity activity,String title,String message){
//        final Dialog dialog = new Dialog(activity);
//
//        dialog.setContentView(R.layout.popup);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//
//        TextView points = (TextView) dialog.findViewById(R.id.textView_pointdialog);
//        TextView code=(TextView)dialog.findViewById(R.id.textView_codeback);
//        TextView congrats=(TextView)dialog.findViewById(R.id.textViewTitle);
//        TextView messageViw=(TextView) dialog.findViewById(R.id.textViewMessage);
//
//        messageViw.setText(message);
//        congrats.setText(title);
//        points.setVisibility(View.INVISIBLE);
//        code.setVisibility(View.INVISIBLE);
//
//        Button dialogButton = (Button) dialog.findViewById(R.id.buttonokay);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//            }
//        });
//        dialog.show();
//    }

    public void showresetpassInputDialog(final Activity activity){
        final Dialog dialog = new Dialog(activity);

        dialog.setContentView(R.layout.inputtextpopup);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        final EditText emailview=(EditText) dialog.findViewById(R.id.editText_input);
        Button submit=(Button)dialog.findViewById(R.id.buttonokay);
        Button cancel=(Button)dialog.findViewById(R.id.button_cancel);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd = new ProgressDialog(activity);
                pd.setMessage("Processing Request");
                pd.show();
                final String email= emailview.getText().toString();
                if(email.length()>0 ){
                    RequestQueue queue= RequestQueueSingleton.getInstance(activity.getApplicationContext()).getRequestQueue();
                    StringRequest request=new StringRequest(Request.Method.POST,resetpassUrl,
                            new Response.Listener<String>(){
                                @Override
                                public void onResponse(String response){
                                    try {
                                        JSONObject responsedata= new JSONObject(response.toString());
                                        String tokendata= responsedata.getString("message");
                                        if(tokendata.contains("We have sent a temporary password to")){
                                            dialog.dismiss();
                                            pd.dismiss();
                                            Toast.makeText(activity,tokendata,Toast.LENGTH_LONG).show();
                                        }
                                        else{
                                            pd.dismiss();
                                            Toast.makeText(activity,"Reseting passsword failed, email not sent",Toast.LENGTH_LONG).show();
                                        }
                                        // onLoginFailed();

                                    } catch (JSONException e) {
                                        pd.dismiss();
                                        Toast.makeText(activity, "problem or change in server, please wait and try again.", Toast.LENGTH_LONG).show();
                                    }
                                }

                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    pd.dismiss();
                                    Toast.makeText(activity,"problem sending email",Toast.LENGTH_LONG).show();
                                }
                            }
                    ){
                        @Override

                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> parameters = new HashMap<String, String>();
                            //parameters.put("id", id.getText().toString());
                            parameters.put("email",email);
                            return parameters;
                        }
                    };
                    request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    queue.add(request);
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

//    public void changePassDialog(Context container, String title, String message){
//        final Context activity=container;
//        final Dialog dialog = new Dialog(activity);
//
//        dialog.setContentView(R.layout.popup);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//
//        TextView points = (TextView) dialog.findViewById(R.id.textView_pointdialog);
//        TextView code=(TextView)dialog.findViewById(R.id.textView_codeback);
//        TextView congrats=(TextView)dialog.findViewById(R.id.textViewTitle);
//        TextView messageViw=(TextView) dialog.findViewById(R.id.textViewMessage);
//
//        messageViw.setText(message);
//        congrats.setText(title);
//        points.setVisibility(View.INVISIBLE);
//        code.setVisibility(View.INVISIBLE);
//
//        final Button dialogButton = (Button) dialog.findViewById(R.id.buttonokay);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismissWithCheck(dialog);
//
//                Intent main=new Intent(activity,MainActivity.class);
//                activity.startActivity(main);
//
//            }
//        });
//        dialog.show();
//    }
    public void dismissWithCheck(Dialog dialog) {
        if (dialog != null) {
            if (dialog.isShowing()) {

                //get the Context object that was used to great the dialog
                Context context = ((ContextWrapper) dialog.getContext()).getBaseContext();

                // if the Context used here was an activity AND it hasn't been finished or destroyed
                // then dismiss it
                if (context instanceof Activity) {

                    // Api >=17
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        if (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed()) {
                            dismissWithTryCatch(dialog);
                        }
                    } else {

                        // Api < 17. Unfortunately cannot check for isDestroyed()
                        if (!((Activity) context).isFinishing()) {
                            dismissWithTryCatch(dialog);
                        }
                    }
                } else
                    // if the Context used wasn't an Activity, then dismiss it too
                    dismissWithTryCatch(dialog);
            }
            dialog = null;
        }
    }



    public void dismissWithTryCatch(Dialog dialog) {
        try {
            dialog.dismiss();
        } catch (final IllegalArgumentException e) {
            // Do nothing.
        } catch (final Exception e) {
            // Do nothing.
        } finally {
            dialog = null;
        }
    }


}