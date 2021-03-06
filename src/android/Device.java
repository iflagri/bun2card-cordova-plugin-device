/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class Device extends CordovaPlugin {
    public static final String TAG = "Device";

    public static String platform;                            // Device OS
    public static String uuid;                                // Device UUID

    private static final String USER_PROFILE = "USER_PROFILE";

    private static final String ANDROID_PLATFORM = "Android";
    private static final String AMAZON_PLATFORM = "amazon-fireos";
    private static final String AMAZON_DEVICE = "Amazon";

    private static final String KEY_UUID = "UUID";


    /**
     * Constructor.
     */
    public Device() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Device.uuid = getUuid();
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("getDeviceInfo".equals(action)) {
            JSONObject r = new JSONObject();
            if (args.length() > 0) {
                String newUuid = args.getString(0);
                if (newUuid != null && newUuid.length() > 10 && newUuid != Device.uuid) {
                    this.setUuid(newUuid);
                }
            }
            r.put("uuid", Device.uuid);
            r.put("version", this.getOSVersion());
            r.put("platform", this.getPlatform());
            r.put("model", this.getModel());
            r.put("manufacturer", this.getManufacturer());
	        r.put("isVirtual", this.isVirtual());
            r.put("serial", this.getSerialNumber());
            callbackContext.success(r);
        } else if ("setUuid".equals(action)) {
            if (args.length() > 0) {
                String uuid = args.getString(0);
                setUuid(uuid);
                callbackContext.success(uuid);
            }
        }
        else {
            return false;
        }
        return true;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Get the OS name.
     *
     * @return
     */
    public String getPlatform() {
        String platform;
        if (isAmazonDevice()) {
            platform = AMAZON_PLATFORM;
        } else {
            platform = ANDROID_PLATFORM;
        }
        return platform;
    }

    /**
     * Get the device's Universally Unique Identifier (UUID).
     *
     * @return
     */
    public String getUuid() {

        SharedPreferences preferences = cordova.getActivity().getSharedPreferences(USER_PROFILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor  = preferences.edit();
        String uuid = preferences.getString(KEY_UUID, "");
        if (uuid == null || "".equals(uuid)) {
            uuid = UUID.randomUUID().toString();
            editor.putString(KEY_UUID, uuid);
            editor.commit();
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        SharedPreferences preferences = cordova.getActivity().getSharedPreferences(USER_PROFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor  = preferences.edit();
        editor.putString(KEY_UUID, uuid);
        // editor.putString(KEY_UUID, guid);
        editor.commit();
        Device.uuid = uuid;
        // Device.uuid = guid;
    }

    protected String getStringResource(String group, String key) {
        Context context = this.cordova.getActivity().getApplicationContext();
        try {
            int id = context.getResources().getIdentifier(key, group, context.getPackageName());
            if (id == 0) {
                return "";
            }
            return context.getResources().getString(id);
        } catch (Exception e) {
            return "";
        }
    }

    protected String readUuidFromFile(File fileUUID) {
        if (!fileUUID.exists()) {
            return "";
        }
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(fileUUID, "r");

            byte[] buffer = new byte[(int)file.length()];
            file.readFully(buffer);
            return new String(buffer);
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            return "";
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }}
        }
    }

    protected void writeUuidFile(File fileUUID, String uuid) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileUUID);
            out.write(uuid.getBytes());
            out.flush();
            out.close();
            out = null;
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }

    public String getProductName() {
        String productname = android.os.Build.PRODUCT;
        return productname;
    }

    public String getManufacturer() {
        String manufacturer = android.os.Build.MANUFACTURER;
        return manufacturer;
    }

    public String getSerialNumber() {
        String serial = android.os.Build.SERIAL;
        return serial;
    }

    /**
     * Get the OS version.
     *
     * @return
     */
    public String getOSVersion() {
        String osversion = android.os.Build.VERSION.RELEASE;
        return osversion;
    }

    public String getSDKVersion() {
        @SuppressWarnings("deprecation")
        String sdkversion = android.os.Build.VERSION.SDK;
        return sdkversion;
    }

    public String getTimeZoneID() {
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    /**
     * Function to check if the device is manufactured by Amazon
     *
     * @return
     */
    public boolean isAmazonDevice() {
        if (android.os.Build.MANUFACTURER.equals(AMAZON_DEVICE)) {
            return true;
        }
        return false;
    }

    public boolean isVirtual() {
	return android.os.Build.FINGERPRINT.contains("generic") ||
	    android.os.Build.PRODUCT.contains("sdk");
    }

}
