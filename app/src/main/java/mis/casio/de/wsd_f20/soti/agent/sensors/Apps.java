package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

public class Apps {

    public static class PackageInfo {
        private String appName;
        private String packageName;
        private String versionName;
//        private int versionCode;
        private Drawable icon;
        private boolean systemApp;
        private boolean installed;
        private final Object globalLock = new Object();

        public PackageInfo() {
            this.setDefaults();
        }

        public PackageInfo(String packageName) {
            this.setDefaults();
            synchronized (globalLock) {
                this.packageName = packageName;
            }
        }

        public PackageInfo(PackageInfo packageInfo) {
            this.set(packageInfo);
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.appName = "";
                this.packageName = "";
                this.versionName = "";
//                this.versionCode = 0;
                this.icon = null;
                this.systemApp = false;
                this.installed = false;
            }
        }

        public void set(PackageInfo packageInfo) {
            synchronized (globalLock) {
                this.appName = packageInfo.appName;
                this.packageName = packageInfo.packageName;
                this.versionName = packageInfo.versionName;
//                this.versionCode = packageInfo.versionCode;
                this.icon = packageInfo.icon;
                this.systemApp = packageInfo.systemApp;
                this.installed = packageInfo.installed;
            }
        }

        public JSONObject getJsonObject() {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    if (!this.appName.equals(this.packageName)) jsonObject.put("appName", this.appName);
                    jsonObject.put("packageName", this.packageName);
                    jsonObject.put("versionName", this.versionName);
//                    jsonObject.put("versionCode", this.versionCode);
                    jsonObject.put("systemApp", this.systemApp);
                    jsonObject.put("installed", this.installed);
                 }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }
    }

    public static class AppParams {

        private ArrayList<PackageInfo> appsList;
        private final Object globalLock = new Object();

        private void initAppsList() {
            if (this.appsList == null) this.appsList = new ArrayList<>();
        }

        public AppParams() {
            this.setDefaults();
        }

        public AppParams(AppParams appParams) {
            this.set(appParams);
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.initAppsList();
            }
        }

        public void set(AppParams appParams) {
            logMethodEntranceExit(true);
            this.setAppsList(appParams.appsList);
            logMethodEntranceExit(false);
        }

        public void setAppsList(List<PackageInfo> appsList) {
            logMethodEntranceExit(true);
            synchronized (globalLock) {
                this.initAppsList();
                this.appsList.clear();
                this.appsList.addAll(appsList);
            }
            logMethodEntranceExit(false);
        }

        public List<PackageInfo> getAppsList() {
            return new ArrayList<>(this.appsList);
        }

        public PackageInfo get(String packageName, boolean createIfNull) {
            PackageInfo retVal = this.get(packageName);
            if (retVal == null && createIfNull) retVal = new PackageInfo(packageName);
            return retVal;
        }

        public PackageInfo get(String packageName) {
            try {
                synchronized (globalLock) {
                    for (int appsListIndex = 0; appsListIndex < this.appsList.size(); appsListIndex++) {
                        if (this.appsList.get(appsListIndex).packageName.equals(packageName)) {
                            return new PackageInfo(this.appsList.get(appsListIndex));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean put(PackageInfo packageInfo) {
            try {
                synchronized (globalLock) {
                    for (int appsListIndex = 0; appsListIndex < this.appsList.size(); appsListIndex++) {
                        if (this.appsList.get(appsListIndex).packageName.equals(packageInfo.packageName)) {
                            this.appsList.set(appsListIndex, packageInfo);
                            return true;
                        }
                    }
                    this.appsList.add(packageInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public boolean resetInstalledFlags() {
            try {
                synchronized (globalLock) {
                    for (PackageInfo p:this.appsList) {
                        p.installed = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public JSONObject getJsonObject() {
            return this.getJsonObject(true, true);
        }

        public JSONObject getJsonObject(boolean userApps, boolean systemApps) {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();
            Collection<JSONObject> apps = new ArrayList<JSONObject>();

            try {
                synchronized (globalLock) {
                    for (PackageInfo p:this.appsList) {
                        if (!p.installed) continue;
                        if (p.systemApp && !systemApps) continue;
                        if (!p.systemApp && !userApps) continue;
                        JSONObject jsonAppObject = new JSONObject();
                        if (!p.appName.equals(p.packageName)) jsonAppObject.put("appName", p.appName);
                        jsonAppObject.put("packageName", p.packageName);
                        jsonAppObject.put("versionName", p.versionName);
//                        jsonAppObject.put("versionCode", p.versionCode);
                        jsonAppObject.put("systemApp", p.systemApp);
                        ((ArrayList<JSONObject>) apps).add(jsonAppObject);
                    }
                    if (!apps.isEmpty()) {
                        JSONArray appsJsonArray = new JSONArray(apps);
                        jsonObject.put("installed", appsJsonArray);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }

        public JSONArray getJsonArray() {
            return this.getJsonArray(true, true);
        }

        public JSONArray getJsonArray(boolean userApps, boolean systemApps) {
            logMethodEntranceExit(true);
            Collection<JSONObject> apps = new ArrayList<JSONObject>();
            JSONArray appsJsonArray = new JSONArray();

            try {
                synchronized (globalLock) {
                    for (PackageInfo p:this.appsList) {
                        if (!p.installed) continue;
                        if (p.systemApp && !systemApps) continue;
                        if (!p.systemApp && !userApps) continue;
                        JSONObject jsonAppObject = new JSONObject();
                        if (!p.appName.equals(p.packageName)) jsonAppObject.put("appName", p.appName);
                        jsonAppObject.put("packageName", p.packageName);
                        jsonAppObject.put("versionName", p.versionName);
//                        jsonAppObject.put("versionCode", p.versionCode);
                        jsonAppObject.put("systemApp", p.systemApp);
                        ((ArrayList<JSONObject>) apps).add(jsonAppObject);
                    }
                    if (!apps.isEmpty()) {
                        appsJsonArray = new JSONArray(apps);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return appsJsonArray;
        }

    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x03FF;
    public static final int OBSERVE_APP_ADDED = 0x0001;
    public static final int OBSERVE_APP_REMOVED = 0x0002;
    public static final int OBSERVE_APP_FULLY_REMOVED = 0x0004;
    public static final int OBSERVE_APP_REPLACED = 0x0008;
    public static final int OBSERVE_APP_DATA_CLEARED = 0x0010;
    public static final int OBSERVE_APP_CHANGED = 0x0020;
    public static final int OBSERVE_APP_FIRST_LAUNCH = 0x0040;
    public static final int OBSERVE_APP_RESTARTED = 0x0080;
    public static final int OBSERVE_APP_NEEDS_VERIFICATION = 0x0100;
    public static final int OBSERVE_APP_VERIFIED = 0x0200;
    private static final String TAG = "MQTT (App Sensor)";
    private static final String MQTT_SOURCE = "Apps";

    private static Apps instance;

    private AppParams appParams;
    public AppParams getAppParams() {
        return new AppParams(this.appParams);
    }
    private Map<String, Integer> observationMaskMap = new ConcurrentHashMap<>();

    public Map<String, Integer> getObservationMaskMap() {
        return this.observationMaskMap;
    }

    public void setObservationMaskMap(Map<String, Integer> observationMaskMap) {
        this.observationMaskMap = observationMaskMap;
        this.autoUpdate(!this.observationMaskMap.isEmpty());
    }

    private AtomicBoolean autoUpdate = new AtomicBoolean(false);

    private static final boolean LOG_METHOD_ENTRANCE_EXIT = false;

    private static void logMethodEntranceExit(boolean entrance, String... addonTags) {
        if (!LOG_METHOD_ENTRANCE_EXIT) return;
        String nameofCurrMethod = Thread.currentThread()
                .getStackTrace()[3]
                .getMethodName();
        if (nameofCurrMethod.startsWith("access$")) { // Inner Class called this method!
            nameofCurrMethod = Thread.currentThread()
                    .getStackTrace()[4]
                    .getMethodName();
        }
        StringBuilder sb = new StringBuilder(addonTags.length);
        Arrays.stream(addonTags).forEach(addonTag -> sb.append(addonTag));

        Log.v(TAG, nameofCurrMethod + " " + sb.toString() + (entrance?" +":" -"));
    }

    public boolean requestData(String property, String originator, boolean get, boolean observe, long transactionId) {
        logMethodEntranceExit(true);
        Integer observationMaskInteger = observationMaskMap.get(originator);
        int observationMask = (observationMaskInteger == null)?OBSERVE_NONE:observationMaskInteger.intValue();
        boolean retVal = true;
        if (get | observe) this.getApps();
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:get?observationMask:OBSERVE_NONE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.appParams.getJsonArray(true, true));
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.appParams.getJsonArray(true, true));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingApps cancelled.");
                }
                break;
            case "User":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.appParams.getJsonArray(true, false));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "System":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.appParams.getJsonArray(false, true));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;

            case "Added":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_ADDED; else observationMask &= ~OBSERVE_APP_ADDED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "Removed":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_REMOVED; else observationMask &= ~OBSERVE_APP_REMOVED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "FullyRemoved":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_FULLY_REMOVED; else observationMask &= ~OBSERVE_APP_FULLY_REMOVED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "Replaced":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_REPLACED; else observationMask &= ~OBSERVE_APP_REPLACED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "DataCleared":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_DATA_CLEARED; else observationMask &= ~OBSERVE_APP_DATA_CLEARED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "Changed":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_CHANGED; else observationMask &= ~OBSERVE_APP_CHANGED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "FirstLaunch":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_FIRST_LAUNCH; else observationMask &= ~OBSERVE_APP_FIRST_LAUNCH;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "Restarted":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_RESTARTED; else observationMask &= ~OBSERVE_APP_RESTARTED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "NeedsVerification":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_NEEDS_VERIFICATION; else observationMask &= ~OBSERVE_APP_NEEDS_VERIFICATION;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            case "Verified":
                if (get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Get requested on non-gettable property: " + property);
                    logMethodEntranceExit(false, "Get requested on non-gettable property: " + property);
                    return false;
                }
                if (observe) observationMask |= OBSERVE_APP_VERIFIED; else observationMask &= ~OBSERVE_APP_VERIFIED;
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, (observe?"":"cancelled ") + "observing apps property: " + property);
                retVal = true;
                break;

            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown App Property requested: " + property);
                retVal = false;
        }
        if (observationMask == OBSERVE_NONE) {
            observationMaskMap.remove(originator);
        } else {
            observationMaskMap.put(originator, new Integer(observationMask));
        }
        this.autoUpdate(!observationMaskMap.isEmpty());

        logMethodEntranceExit(false);
        return retVal;
    }

    private Apps() {
        logMethodEntranceExit(true);
        this.appParams = new AppParams();
        this.getApps();
        logMethodEntranceExit(false);
    }

    public static Apps getInstance() {
        return Apps.instance;
    }

    private void autoUpdate(boolean enableAutoUpdate) {
        Log.d(TAG, "autoUpdate(" + enableAutoUpdate + ")");
        if (enableAutoUpdate) {
            try {
                if (this.autoUpdate.get()) return;
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_FIRST_LAUNCH);
                intentFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
                intentFilter.addAction(Intent.ACTION_PACKAGE_VERIFIED);
                intentFilter.addDataScheme("package");
                SotiAgentApplication.getAppContext().registerReceiver(this.packageReceiver, intentFilter);
                this.autoUpdate.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (!this.autoUpdate.get()) return;
                SotiAgentApplication.getAppContext().unregisterReceiver(this.packageReceiver);
                this.autoUpdate.set(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.autoUpdate.set(enableAutoUpdate);
    }

    public static boolean startAutoUpdate() {
        logMethodEntranceExit(true);
        if (getInstance() == null) {
            logMethodEntranceExit(false, "instance == null");
            return false;
        }
        if (!getInstance().observationMaskMap.isEmpty()) getInstance().autoUpdate(true);
        logMethodEntranceExit(false);
        return true;
    }

    public static boolean stopAutoUpdate() {
        logMethodEntranceExit(true);
        if (getInstance() == null) {
            logMethodEntranceExit(false, "instance == null");
            return false;
        }
        getInstance().autoUpdate(false);
        logMethodEntranceExit(false);
        return true;
    }


    private void getApps() {
        ArrayList<PackageInfo> apps = new ArrayList<PackageInfo>();
        PackageManager packageManager = SotiAgentApplication.getAppContext().getPackageManager();
        List<android.content.pm.PackageInfo> packs = packageManager.getInstalledPackages(0);
        this.appParams.resetInstalledFlags();
        for(android.content.pm.PackageInfo p:packs) {
            PackageInfo newInfo = new PackageInfo();
            newInfo.appName = p.applicationInfo.loadLabel(packageManager).toString();
            newInfo.packageName = p.packageName;
            newInfo.versionName = p.versionName;
//            newInfo.versionCode = p.versionCode;
            newInfo.icon = p.applicationInfo.loadIcon(packageManager);
            newInfo.systemApp = (p.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
            newInfo.installed = (p.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) > 0;
            this.appParams.put(newInfo);
        }

    }

    public class PackageMessageRunnable implements Runnable {

        private String packageName;
        private String action;
        private int mask;

        public PackageMessageRunnable(String packageName, String action, int mask) {
            this.packageName = packageName;
            this.action = action;
            this.mask = mask;
        }

        public void run() {
            Apps.this.getApps();
            final PackageInfo newInfo = Apps.this.appParams.get(packageName, true);
            try {
                Apps.this.observationMaskMap.forEach((key, value) -> {
                    int observationMask = Apps.this.observationMaskMap.get(key).intValue();
                    if ((observationMask & this.mask) > 0) {
                        MqttHelper.getInstance().doNotify(MQTT_SOURCE + action, key, newInfo.getJsonObject());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
        }
    }

    private void sendPackageMessage(String packageName, String action, int mask) {
        Runnable r = new PackageMessageRunnable(packageName, action, mask);
        new Thread(r).start();
    }

    private BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logMethodEntranceExit(true);
            try {
                Uri uri = intent.getData();
                String packageName = uri != null ? uri.getSchemeSpecificPart() : null;
                switch (intent.getAction()) {
                    case Intent.ACTION_PACKAGE_ADDED:
                        sendPackageMessage(packageName, "Added", OBSERVE_APP_ADDED);
                        break;
                    case Intent.ACTION_PACKAGE_REMOVED:
                        sendPackageMessage(packageName, "Removed", OBSERVE_APP_REMOVED);
                        break;
                    case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                        sendPackageMessage(packageName, "FullyRemoved", OBSERVE_APP_FULLY_REMOVED);
                        break;
                    case Intent.ACTION_PACKAGE_REPLACED:
                        sendPackageMessage(packageName, "Replaced", OBSERVE_APP_REPLACED);
                        break;
                    case Intent.ACTION_PACKAGE_DATA_CLEARED:
                        sendPackageMessage(packageName, "DataCleared", OBSERVE_APP_DATA_CLEARED);
                        break;
                    case Intent.ACTION_PACKAGE_CHANGED:
                        sendPackageMessage(packageName, "Changed", OBSERVE_APP_CHANGED);
                        break;
                    case Intent.ACTION_PACKAGE_FIRST_LAUNCH:
                        sendPackageMessage(packageName, "FirstLaunch", OBSERVE_APP_FIRST_LAUNCH);
                        break;
                    case Intent.ACTION_PACKAGE_RESTARTED:
                        sendPackageMessage(packageName, "Restarted", OBSERVE_APP_RESTARTED);
                        break;
                    case Intent.ACTION_PACKAGE_NEEDS_VERIFICATION:
                        sendPackageMessage(packageName, "NeedsVerification", OBSERVE_APP_NEEDS_VERIFICATION);
                        break;
                    case Intent.ACTION_PACKAGE_VERIFIED:
                        sendPackageMessage(packageName, "Verified", OBSERVE_APP_VERIFIED);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
        }
    };

    static {
        instance = new Apps();
    }
}
