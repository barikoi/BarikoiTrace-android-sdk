package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class AuthenticateResponse {
    @SerializedName("user")
    private UserResponse user;

    public UserResponse getUser() {
        return user;
    }

    public static class UserResponse {
        @SerializedName("_id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        @SerializedName("companies")
        private Company[] companies;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Company[] getCompanies() {
            return companies;
        }
    }

    public static class Company {
        @SerializedName("company_id")
        private String companyId;

        @SerializedName("group_id")
        private String groupId;

        public String getCompanyId() {
            return companyId;
        }

        public String getGroupId() {
            return groupId;
        }
    }
}