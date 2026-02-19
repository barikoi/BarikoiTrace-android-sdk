package com.barikoi.barikoitrace.network;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    // MEMORY_LEAK [LOW]: Static singleton holds Context reference. Uses ApplicationContext which is correct.
    private static volatile RetrofitClient instance = null;
    private BarikoiTraceApiService apiService;
    private final Context context;
    private String baseUrl;
    private OkHttpClient okHttpClient;

    private RetrofitClient(Context context) {
        this.context = context.getApplicationContext();
        this.baseUrl = Api.base_url;
        this.okHttpClient = createOkHttpClient();
        this.apiService = createApiService();
    }

    public static RetrofitClient getInstance(Context context) {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient(context);
                }
            }
        }
        return instance;
    }

    public static RetrofitClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RetrofitClient is not initialized, call getInstance(context) first");
        }
        return instance;
    }

    /**
     * FIX: Updates base URL and recreates the API service
     * @param url The new base URL to use
     */
    public synchronized void setBaseUrl(String url) {
        if (url != null && !url.equals(this.baseUrl)) {
            this.baseUrl = url;
            // Recreate API service with new URL
            this.apiService = createApiService();
        }
    }

    private OkHttpClient createOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Setup cache
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        Cache cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10 MB

        return new OkHttpClient.Builder()
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    private BarikoiTraceApiService createApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(BarikoiTraceApiService.class);
    }

    public BarikoiTraceApiService getApiService() {
        return apiService;
    }
}
