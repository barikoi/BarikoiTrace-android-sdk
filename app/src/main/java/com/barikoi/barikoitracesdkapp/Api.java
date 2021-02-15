package com.barikoi.barikoitracesdkapp;

/**
 * Created by Sakib on 7/16/2017.
 */

public class Api {
    public static final String base_url="https://backend.barikoi.com:8888/api/v1/";

    public static final String NAME="name",PHONE="phone",EMAIL="email",TOKEN="token", ENROLLED="enrolled", PATH="file_path", URI_PATH="uri_path",USER_ID="user_id",POINTS="points",SPENT_POINTS="redeemed_points",ISREFFERED="isReferred",REFCODE="ref_code";

    public static String logouturl=base_url+"logout";
    public static final String usercheckurl=base_url+"auth/user";
    //loginactivity
    public static final String  loginurl=base_url+"login";
    public static final String signupurl=base_url+"register";
    public static final String companyListurl=base_url+"companies";
    public static final String companyEnrollurl=base_url+"company-user/";


}
