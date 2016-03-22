package com.hungdh.gcmdemo;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import static com.hungdh.gcmdemo.QuickstartPreferences.EMAIL;
import static com.hungdh.gcmdemo.QuickstartPreferences.NAME;
import static com.hungdh.gcmdemo.QuickstartPreferences.REGID;
import static com.hungdh.gcmdemo.QuickstartPreferences.REGISTRATION_COMPLETE;
import static com.hungdh.gcmdemo.QuickstartPreferences.SENT_TOKEN_TO_SERVER;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private static final String URL_REGISTER = "http://10.0.3.2/gcm/register.php";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Bundle bundle = intent.getBundleExtra("bundle");
        String name = bundle.getString(NAME);
        String email = bundle.getString(EMAIL);
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            boolean isSentToServer = sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);

            if (!isSentToServer) {
                isSentToServer = sendRegistrationToServer(name, email, token);
                sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, isSentToServer).apply();
            }
            // [END register_for_gcm]

            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    private boolean sendRegistrationToServer(String name, String email, String token) {
        JSONObject params = new JSONObject();
        try {
            params.put(NAME, name);
            params.put(EMAIL, email);
            params.put(REGID, token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        URL url = null;
        try {
            url = new URL(URL_REGISTER);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);

            // HTTP request header
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");
            con.setUseCaches(false);
            con.connect();

            OutputStream os = con.getOutputStream();
            os.write(params.toString().getBytes("UTF-8"));
            os.close();

            // Read the response into a string
            InputStream is = con.getInputStream();
            String responseString = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
            Log.d(TAG, responseString);
            is.close();

            // Parse the JSON string and return the notification key
            JSONObject response = new JSONObject(responseString);
            if (response.has("status")) {
                String status = response.getString("status");
                if (status.equals("ok"))
                    return true;
            }
            return false;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + URL_REGISTER);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
