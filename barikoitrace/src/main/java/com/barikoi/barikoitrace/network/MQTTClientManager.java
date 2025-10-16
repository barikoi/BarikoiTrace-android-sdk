package com.barikoi.barikoitrace.network;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;


import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

/**
 * MQTT Client Manager for handling MQTT connections in Android
 * This class handles connection management, reconnection logic,
 * and publishing messages to MQTT broker.
 */
public class MQTTClientManager {
    private static final String TAG = "MQTTClientManager";

    // MQTT configuration
    private final String serverUri;
    private final String clientId;
    private final String username;
    private final String password;
    private final String deviceId;
    private final String companyId;
    private final String locationTopic;
    private final String channelTopic;

    // Internal state
    private MqttAndroidClient mqttClient;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final long RECONNECT_INTERVAL_MS = 5000; // 5 seconds

    // Context and callbacks
    private final Context context;
    private final Handler handler;
    private final MqttStatusCallback statusCallback;
    private MqttCallbackExtended mqttCallback;
    /**
     * Interface for notifying status changes
     */
    public interface MqttStatusCallback {
        void onConnectionStatusChanged(boolean connected, String message);
        void onMessageDelivered(String topic);
        void onMessageReceived(String topic, String message);
    }

    /**
     * Constructor for MQTTClientManager
     */
    public MQTTClientManager(Context context, String serverUri, String id, String company, String groupId, String uuid, MqttStatusCallback callback) {
        this.context = context.getApplicationContext();
        this.serverUri = serverUri;
        this.statusCallback = callback;
        this.clientId = "AndroidClient-" + id+ "-"+uuid;
        this.deviceId = id;
        this.companyId = company;
        this.locationTopic = "device/" + id + "/location";
        this.channelTopic = "company/"+company+ "/"+groupId+"/"+id +"/location";
        this.username = "rilus"; // Set your username if needed
        this.password = "r1lu5"; // Set your password if needed
        this.handler = new Handler(Looper.getMainLooper());

        
        this.mqttCallback=new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                isConnected = true;
                isConnecting =false;
                Log.i(TAG, "Connected to MQTT broker");
                statusCallback.onConnectionStatusChanged(true, reconnect? "reonnected": "connected");
            }

            @Override
            public void connectionLost(Throwable cause) {
                isConnected = false;
                isConnecting =false;
                Log.e(TAG, "Connection lost", cause);
                statusCallback.onConnectionStatusChanged(false, "Connection lost: " +( (cause == null) ? "Unknown Error": cause.getMessage()));
                scheduleReconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Handle incoming messages
                String payload = new String(message.getPayload());
                Log.d(TAG, "Message arrived on topic: " + topic + " - " + payload);
                statusCallback.onMessageReceived(topic, payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    String[] topics = token.getTopics();
                    if (topics != null && topics.length > 0) {
                        Log.d(TAG, "Message delivered to topic: " + topics[0]);
                        statusCallback.onMessageDelivered(topics[0]);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in delivery complete", e);
                }
            }
        };
        // Set up heartbeat mechanism
//        startHeartbeatCheck();
    }

    /**
     * Initializes and connects the MQTT client
     */
    public void connect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
        }

//        try {
            // Use file persistence to maintain messages during restarts
            String persistenceDir = context.getFilesDir().getAbsolutePath();
            MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistenceDir);

            // Create MQTT client
            if( mqttClient==null) mqttClient = new MqttAndroidClient(context,serverUri, clientId, Ack.AUTO_ACK, persistence, false, 100);

            // Set callback for connection events
            mqttClient.setCallback(mqttCallback);

            // Configure connection options
            MqttConnectOptions options = createConnectionOptions();

            // Connect to broker
            Log.d(TAG, "Attempting to connect to MQTT broker: " + serverUri);
            statusCallback.onConnectionStatusChanged(false, "Connecting...");

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = true;
                    reconnectAttempts = 0;
                    Log.i(TAG, "Connected to MQTT broker");
                    statusCallback.onConnectionStatusChanged(true, "Connected");

                    // Subscribe to topics if needed
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    isConnected = false;
                    statusCallback.onConnectionStatusChanged(false, "Connection failed: " + exception.getMessage());
//                    scheduleReconnect();
                }
            });
//        } catch (MqttException e) {
//            Log.e(TAG, "Error setting up MQTT client", e);
//            statusCallback.onConnectionStatusChanged(false, "Setup error: " + e.getMessage());
////            scheduleReconnect();
//        }
    }

    /**
     * Creates MQTT connection options
     */
    private MqttConnectOptions createConnectionOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setKeepAliveInterval(60); // 60 seconds keep alive
        options.setServerURIs(new String[]{ serverUri });
        // Set Last Will and Testament (LWT)
        try {
            options.setWill("device/" + deviceId + "/status",
                    "offline".getBytes(),
                    1, // QoS 1 - at least once delivery
                    true); // Retained message
        } catch (Exception e) {
            Log.e(TAG, "Error setting LWT", e);
        }

        // Set credentials if provided
        if (username != null && password != null) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        return options;
    }

    /**
     * Schedule reconnection with exponential backoff
     */
    private void scheduleReconnect() {
        if(isConnecting==true) return;
        isConnecting=true;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts=0;
            return;
        }

        long delay = RECONNECT_INTERVAL_MS * (long) Math.pow(2, reconnectAttempts);
        reconnectAttempts++;

        Log.d(TAG, "Scheduling reconnect in " + delay + "ms, attempt " + reconnectAttempts);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isConnected) {
                    connect();
                }
            }
        }, delay);
    }

    /**
     * Subscribe to necessary topics
     */
    private void subscribeToTopics() {
        if (mqttClient != null && mqttClient.isConnected()) {
            // Example: subscribe to command topic
            String commandTopic = "device/" + deviceId + "/command";
            mqttClient.subscribe(commandTopic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to: " + commandTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to subscribe to: " + commandTopic, exception);
                }
            });

            // Publish online status
//            publishMessage("device/" + deviceId + "/status", "online", 1, true);

        }
    }

    /**
     * Publish location data to MQTT broker
     */
    public void publishLocation(Location location) {
        try {
            JSONObject locationData = new JSONObject();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("gpx_time", location.getTime());
            locationData.put("user_id", deviceId);
            locationData.put("company_id", companyId);
            locationData.put("speed", location.getSpeed());
            locationData.put("bearing",location.getBearing());
            locationData.put("altitude", location.getAltitude());
            locationData.put("accuracy", location.getAccuracy());

//            publishMessage(locationTopic, locationData.toString(), 1, false);
            publishMessage(channelTopic, locationData.toString(), 1, false);
        } catch (Exception e) {
            Log.e(TAG, "Error creating location JSON", e);
        }
    }

    /**
     * Generic method to publish a message to a topic
     */
    public void publishMessage(String topic, String payload, int qos, boolean retained) {
        if (mqttClient != null && isConnected) {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);

            mqttClient.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Published to topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to publish to topic: " + topic, exception);
                }
            });
        } else {
            Log.w(TAG, "Cannot publish - not connected");
//            statusCallback.onConnectionStatusChanged(false, "Not connected");
            // Optionally queue messages for later delivery
//            scheduleReconnect();

        }
    }

    /**
     * Start heartbeat mechanism to keep connection alive
     */
    private void startHeartbeatCheck() {
        final int HEARTBEAT_INTERVAL = 30000; // 1 minute

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mqttClient != null && isConnected) {
                    // Send heartbeat
                    publishMessage("device/" + deviceId + "/heartbeat",
                            String.valueOf(System.currentTimeMillis()),
                            0, false);
                }
                // Schedule next heartbeat
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }, HEARTBEAT_INTERVAL);
    }

    /**
     * Check if MQTT client is connected
     */
    public boolean isConnected() {
        return isConnected && mqttClient != null && mqttClient.isConnected();
    }

    /**
     * Disconnect MQTT client
     */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            // Publish offline status before disconnecting
            publishMessage("device/" + deviceId + "/status", "offline", 1, true);

            mqttClient.removeCallback(mqttCallback);
            // Disconnect
            mqttClient.disconnect(0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = false;
                    mqttClient.close();
                    mqttClient = null;
                    Log.i(TAG, "Disconnected from MQTT broker");
//                    statusCallback.onConnectionStatusChanged(false, "Disconnected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to disconnect", exception);
                }
            });
        }
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        handler.removeCallbacksAndMessages(null);

        if (mqttClient != null) {
            disconnect();
        }
    }

    /**
     * Handle system power changes
     */
    public void handlePowerSaveMode(boolean isPowerSaveMode) {
        if (mqttClient != null && mqttClient.isConnected()) {
            // Adjust MQTT parameters based on power mode
            MqttConnectOptions options = createConnectionOptions();

            if (isPowerSaveMode) {
                // Less frequent heartbeats in power save mode
                options.setKeepAliveInterval(900); // 15 minutes
            } else {
                // Regular heartbeat interval
                options.setKeepAliveInterval(60); // 1 minute
            }

            // Update connection parameters
            mqttClient.disconnect();
            mqttClient.connect(options);

        }
    }
}