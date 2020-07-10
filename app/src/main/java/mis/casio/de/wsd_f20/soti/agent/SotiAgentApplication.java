package mis.casio.de.wsd_f20.soti.agent;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;
import mis.casio.de.wsd_f20.soti.agent.sensors.Apps;
import mis.casio.de.wsd_f20.soti.agent.sensors.Battery;
import mis.casio.de.wsd_f20.soti.agent.sensors.Bluetooth;
import mis.casio.de.wsd_f20.soti.agent.sensors.Gps;
import mis.casio.de.wsd_f20.soti.agent.sensors.System;
import mis.casio.de.wsd_f20.soti.agent.sensors.Wifi;
import mis.casio.de.wsd_f20.soti.agent.service.MqttService;

public class SotiAgentApplication extends Application {
    private static Context context = null;
    private static final String TAG = "MQTT (SotiAgentApplication)";

    private static final String USE_TLS_KEY = "useTLS";
    private static final String SERVER_ADDRESS_KEY = "serverAddress";
    private static final String SERVER_PORT_KEY = "serverPort";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

    private static boolean useTLS = false;
    private static String serverAddress = "";
    private static int serverPort = 0;
    private static String username = "";
    private static String password = "";

    public static boolean isUseTLS() {
        return useTLS;
    }

    public static String getServerAddress() {
        return serverAddress;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }


    public static boolean readAppSetting() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences preferences = EncryptedSharedPreferences.create(
                    "mqtt_server_prefs",
                    masterKeyAlias,
                    getAppContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
//            if (!preferences.contains(USE_TLS_KEY)) return false;
            if (!preferences.contains(SERVER_ADDRESS_KEY)) return false;
            if (!preferences.contains(SERVER_PORT_KEY)) return false;
//            if (!preferences.contains(USERNAME_KEY)) return false;
//            if (!preferences.contains(PASSWORD_KEY)) return false;
            if (!preferences.contains(USE_TLS_KEY))
                SotiAgentApplication.useTLS = false;
            else
                SotiAgentApplication.useTLS = preferences.getBoolean(USE_TLS_KEY, false);
            SotiAgentApplication.serverAddress = preferences.getString(SERVER_ADDRESS_KEY, "");
            SotiAgentApplication.serverPort = preferences.getInt(SERVER_PORT_KEY, 0);
            if (!preferences.contains(USERNAME_KEY))
                SotiAgentApplication.username = "";
            else
                SotiAgentApplication.username = preferences.getString(USERNAME_KEY, "");
            if (!preferences.contains(PASSWORD_KEY))
                SotiAgentApplication.password = "";
            else
                SotiAgentApplication.password = preferences.getString(PASSWORD_KEY, "");
            return true;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveAppSetting(boolean useTLS, String serverAddress, int serverPort, String username, String password) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences preferences = EncryptedSharedPreferences.create(
                    "mqtt_server_prefs",
                    masterKeyAlias,
                    getAppContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            if (serverAddress.length() == 0) return false;
            if (serverPort == 0) return false;
//            if (username.length() == 0) return false;
//            if (password.length() == 0) return false;

            // use the shared preferences and editor as you normally would
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(USE_TLS_KEY, useTLS);
            editor.putString(SERVER_ADDRESS_KEY, serverAddress);
            editor.putInt(SERVER_PORT_KEY, serverPort);
            editor.putString(USERNAME_KEY, username);
            editor.putString(PASSWORD_KEY, password);
            editor.commit();
            SotiAgentApplication.useTLS = useTLS;
            SotiAgentApplication.serverAddress = serverAddress;
            SotiAgentApplication.serverPort = serverPort;
            SotiAgentApplication.username = username;
            SotiAgentApplication.password = password;

            if (MqttHelper.getInstance() != null && !MqttHelper.getInstance().isConnected()) MqttHelper.getInstance().connect();

            MqttService.start(getAppContext());

            return true;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void onCreate() {
        super.onCreate();
        SotiAgentApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        if (SotiAgentApplication.context == null) {
            try {
                Application application = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null, (Object[]) null);
                SotiAgentApplication.context = application.getApplicationContext();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return SotiAgentApplication.context;
    }

    public static void saveSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getAppContext());
        SharedPreferences.Editor editor = preferences.edit();
        saveSettings(editor, "System", System.getInstance().getObservationMaskMap());
        saveSettings(editor, "Battery", Battery.getInstance().getObservationMaskMap());
        saveSettings(editor, "Wifi", Wifi.getInstance().getObservationMaskMap());
        saveSettings(editor, "Bluetooth", Bluetooth.getInstance().getObservationMaskMap());
        saveSettings(editor, "Gps", Gps.getInstance().getObservationMaskMap());
        saveSettings(editor, "Apps", Apps.getInstance().getObservationMaskMap());
        editor.commit();
    }

    private static void saveSettings(SharedPreferences.Editor editor, String module, Map<String, Integer> observationMaskMap) {
        Set<String> observationMaskMapKeys = new HashSet<>();
        for(Map.Entry<String, Integer> observationMaskEntry : observationMaskMap.entrySet()) {
            String key = observationMaskEntry.getKey();
            Integer value = observationMaskEntry.getValue();
            observationMaskMapKeys.add(key);
            editor.putInt("Observe" + module + key, value);
//            Log.d(TAG, "saveSettings Observe" + module + key + "=" + value);
        }
        editor.putStringSet("Observe" + module, observationMaskMapKeys);
    }

    public static void restoreSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getAppContext());
        System.getInstance().setObservationMaskMap(restoreSettings(preferences, "System"));
        Battery.getInstance().setObservationMaskMap(restoreSettings(preferences, "Battery"));
        Wifi.getInstance().setObservationMaskMap(restoreSettings(preferences, "Wifi"));
        Bluetooth.getInstance().setObservationMaskMap(restoreSettings(preferences, "Bluetooth"));
        Gps.getInstance().setObservationMaskMap(restoreSettings(preferences, "Gps"));
        Apps.getInstance().setObservationMaskMap(restoreSettings(preferences, "Apps"));
    }

    private static Map<String, Integer> restoreSettings(SharedPreferences preferences, String module) {
        Map<String, Integer> observationMaskMap = new ConcurrentHashMap<>();
        Set<String> observationMaskMapKeys = new HashSet<>();
        observationMaskMapKeys = preferences.getStringSet("Observe" + module, observationMaskMapKeys);

        for (String observationMaskMapKey: observationMaskMapKeys) {
            int observationMask = preferences.getInt("Observe" + module + observationMaskMapKey, 0);
//            Log.d(TAG, "restoreSettings Observe" + module + observationMaskMapKey + "=" + observationMask);
            if (observationMask != 0) observationMaskMap.put(observationMaskMapKey, observationMask);
        }

        return observationMaskMap;
    }

}
