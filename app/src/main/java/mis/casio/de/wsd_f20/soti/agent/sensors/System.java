package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

public class System {

    public static class SystemParams {

        private String manufacturer;
        private String model;
        private String name;
        private String serial;
        private String build;
        private String wear;
        private String homeApp;
        private String playServices;
        private String android;
        private String security;
        private String bootloader;
        private String board;
        private int sdk;
        private final Object globalLock = new Object();

        public String getBootloader() {
            return bootloader;
        }

        public void setBootloader(String bootloader) {
            this.bootloader = bootloader;
        }

        public String getBoard() {
            return board;
        }

        public void setBoard(String board) {
            this.board = board;
        }

        public int getSdk() {
            return sdk;
        }

        public void setSdk(int sdk) {
            this.sdk = sdk;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getBuild() {
            return build;
        }

        public void setBuild(String build) {
            this.build = build;
        }

        public String getWear() {
            return wear;
        }

        public void setWear(String wear) {
            synchronized (globalLock) {
                this.wear = wear;
            }
        }

        public String getHomeApp() {
            return homeApp;
        }

        public void setHomeApp(String homeApp) {
            synchronized (globalLock) {
                this.homeApp = homeApp;
            }
        }

        public String getPlayServices() {
            return playServices;
        }

        public void setPlayServices(String playServices) {
            synchronized (globalLock) {
                this.playServices = playServices;
            }
        }

        public String getAndroid() {
            return android;
        }

        public void setAndroid(String android) {
            this.android = android;
        }

        public String getSecurity() {
            return security;
        }

        public void setSecurity(String security) {
            this.security = security;
        }

        public SystemParams() {
            this.setDefaults();
        }

        public SystemParams(SystemParams systemParams) {
            this.set(systemParams);
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.manufacturer = "unknown";
                this.model = "unknown";
                this.name = "unknown";
                this.serial = "unknown";
                this.build = "unknown";
                this.wear = "unknown";
                this.homeApp = "unknown";
                this.playServices = "unknown";
                this.android = "unknown";
                this.security = "unknown";
                this.bootloader = "unknown";
                this.board = "unknown";
                this.sdk = 0;
            }
        }

        public void set(SystemParams systemParams) {
            logMethodEntranceExit(true);
            synchronized (globalLock) {
                this.manufacturer = systemParams.manufacturer;
                this.model = systemParams.model;
                this.name = systemParams.name;
                this.serial = systemParams.serial;
                this.build = systemParams.build;
                this.wear = systemParams.wear;
                this.homeApp = systemParams.homeApp;
                this.playServices = systemParams.playServices;
                this.android = systemParams.android;
                this.security = systemParams.security;
                this.bootloader = systemParams.bootloader;
                this.board = systemParams.board;
                this.sdk = systemParams.sdk;
            }
            logMethodEntranceExit(false);
        }

        public JSONObject getJsonObject() {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();
            JSONObject about = new JSONObject();
            JSONObject version = new JSONObject();
            JSONObject other = new JSONObject();

            try {
                synchronized (globalLock) {
                    about.put("manufacturer", this.manufacturer);
                    about.put("model", this.model);
                    about.put("name", this.name);
                    about.put("serial", this.serial);
                    about.put("build", this.build);
                    version.put("wear", this.wear);
                    version.put("homeApp", this.homeApp);
                    version.put("playServices", this.playServices);
                    version.put("android", this.android);
                    version.put("security", this.security);
                    other.put("bootloader", this.bootloader);
                    other.put("board", this.board);
                    other.put("sdk", this.sdk);
                    jsonObject.put("about", about);
                    jsonObject.put("version", version);
                    jsonObject.put("other", other);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }

        public JSONObject getJsonObject(int mask) {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();
            JSONObject about = new JSONObject();
            JSONObject version = new JSONObject();
            JSONObject other = new JSONObject();

            try {
                synchronized (globalLock) {
                    if ((mask & GET_ABOUT_MANUFACTURER) != 0)
                        about.put("manufacturer", this.manufacturer);
                    if ((mask & GET_ABOUT_MODEL) != 0) about.put("model", this.model);
                    if ((mask & GET_ABOUT_NAME) != 0) about.put("name", this.name);
                    if ((mask & GET_ABOUT_SERIAL) != 0) about.put("serial", this.serial);
                    if ((mask & GET_ABOUT_BUILD) != 0) about.put("build", this.build);
                    if ((mask & GET_VERSION_WEAR) != 0) version.put("wear", this.wear);
                    if ((mask & OBSERVE_VERSION_HOMEAPP) != 0) version.put("homeApp", this.homeApp);
                    if ((mask & OBSERVE_VERSION_PLAYSERVICES) != 0)
                        version.put("playServices", this.playServices);
                    if ((mask & GET_VERSION_ANDROID) != 0) version.put("android", this.android);
                    if ((mask & GET_VERSION_SECURITY) != 0) version.put("security", this.security);
                    if ((mask & GET_OTHER_BOOTLOADER) != 0)
                        other.put("bootloader", this.bootloader);
                    if ((mask & GET_OTHER_BOARD) != 0) other.put("board", this.board);
                    if ((mask & GET_OTHER_SDK) != 0) other.put("sdk", this.sdk);
                }
                if (about.length() > 0) jsonObject.put("about", about);
                if (version.length() > 0) jsonObject.put("version", version);
                if (other.length() > 0) jsonObject.put("other", other);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }

        public JSONObject getJsonObject(SystemParams systemParams, int mask) {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();
            JSONObject version = new JSONObject();

            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_VERSION_HOMEAPP) != 0 && !this.homeApp.equalsIgnoreCase(systemParams.homeApp))
                        version.put("homeApp", this.homeApp);
                    if ((mask & OBSERVE_VERSION_PLAYSERVICES) != 0 && !this.playServices.equalsIgnoreCase(systemParams.playServices))
                        version.put("playServices", this.playServices);
                }
                if (version.length() > 0) jsonObject.put("version", version);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }

        @Override
        public boolean equals(Object o) {
            logMethodEntranceExit(true);
            if (this == o) {
                logMethodEntranceExit(false, "equals called on same object reference");
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                logMethodEntranceExit(false, "equals called on different object class types");
                return false;
            }
            SystemParams systemParams = (SystemParams) o;

            logMethodEntranceExit(false);
            synchronized (globalLock) {
                if (this.sdk != systemParams.sdk) return false;
                if (!this.bootloader.equalsIgnoreCase(systemParams.bootloader)) return false;
                if (!this.security.equalsIgnoreCase(systemParams.security)) return false;
                if (!this.android.equalsIgnoreCase(systemParams.android)) return false;
                if (!this.playServices.equalsIgnoreCase(systemParams.playServices)) return false;
                if (!this.homeApp.equalsIgnoreCase(systemParams.homeApp)) return false;
                if (!this.model.equalsIgnoreCase(systemParams.model)) return false;
                if (!this.manufacturer.equalsIgnoreCase(systemParams.manufacturer)) return false;
                if (!this.board.equalsIgnoreCase(systemParams.board)) return false;
                if (!this.build.equalsIgnoreCase(systemParams.build)) return false;
                if (!this.name.equalsIgnoreCase(systemParams.name)) return false;
                if (!this.serial.equalsIgnoreCase(systemParams.serial)) return false;
                if (!this.wear.equalsIgnoreCase(systemParams.wear)) return false;
            }
            return true;
        }

        public boolean equalsMasked(SystemParams systemParams, int mask) {
            logMethodEntranceExit(true);
            logMethodEntranceExit(false);
            synchronized (globalLock) {
                if ((mask & OBSERVE_VERSION_PLAYSERVICES) != 0 && !this.playServices.equalsIgnoreCase(systemParams.playServices))
                    return false;
                if ((mask & OBSERVE_VERSION_HOMEAPP) != 0 && !this.homeApp.equalsIgnoreCase(systemParams.homeApp))
                    return false;
            }
            return true;
        }

    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x07FF;
    public static final int OBSERVE_VERSION_HOMEAPP = 0x0001;
    public static final int OBSERVE_VERSION_PLAYSERVICES = 0x0002;
    public static final int OBSERVE_VERSION_ALL= 0x0003;
    public static final int GET_ABOUT_MANUFACTURER = 0x00010000;
    public static final int GET_ABOUT_MODEL = 0x00020000;
    public static final int GET_ABOUT_NAME = 0x00040000;
    public static final int GET_ABOUT_SERIAL = 0x00080000;
    public static final int GET_ABOUT_BUILD = 0x00100000;
    public static final int GET_ABOUT_ALL = 0x001F0000;
    public static final int GET_VERSION_WEAR = 0x00200000;
    public static final int GET_VERSION_ANDROID = 0x00400000;
    public static final int GET_VERSION_SECURITY = 0x00800000;
    public static final int GET_VERSION_ALL = 0x00E00003;
    public static final int GET_OTHER_BOOTLOADER = 0x01000000;
    public static final int GET_OTHER_BOARD = 0x02000000;
    public static final int GET_OTHER_SDK = 0x04000000;
    public static final int GET_OTHER_ALL = 0x07000000;

    private static final String TAG = "MQTT (System Sensor)";
    private static final String MQTT_SOURCE = "System";

    private static final int INITIAL_DELAY = 10;
    private static final int REFRESH_TIMEOUT = 60;
    private static System instance;

    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture;

    private SystemParams systemParamsOld;
    private SystemParams systemParams;

    public SystemParams getSystemParams() {
        this.doUpdateSystemParams(this.systemParams);
        return new SystemParams(this.systemParams);
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
        if (get | observe) this.doUpdateSystemParams(this.systemParams);
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:get?observationMask:OBSERVE_NONE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.systemParams.getJsonObject(OBSERVE_ALL));
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getJsonObject());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingSystem cancelled.");
                }
                break;
            case "VersionHomeApp":
                if (observe) observationMask |= OBSERVE_VERSION_HOMEAPP; else if (!get) observationMask &= ~OBSERVE_VERSION_HOMEAPP;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.systemParams.getHomeApp());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getHomeApp());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingHome App Version cancelled.");
                }
                break;
            case "VersionPlayServices":
                if (observe) observationMask |= OBSERVE_VERSION_PLAYSERVICES; else if (!get) observationMask &= ~OBSERVE_VERSION_PLAYSERVICES;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.systemParams.getPlayServices());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getPlayServices());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingPlay Services Version cancelled.");
                }
                break;
            case "Version":
                if (observe) observationMask |= OBSERVE_VERSION_ALL; else if (!get) observationMask &= ~OBSERVE_VERSION_ALL;
                if (observe) {
                    JSONObject version = this.systemParams.getJsonObject(OBSERVE_VERSION_ALL);
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, version.optJSONObject("version"));
                } else if (get) {
                    JSONObject version = this.systemParams.getJsonObject(GET_VERSION_ALL);
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, version.optJSONObject("version"));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingSystem Version Information cancelled.");
                }
                break;
            case "AboutManufacturer":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getManufacturer());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
             case "AboutModel":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getModel());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "AboutName":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getName());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "AboutSerial":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getSerial());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "AboutBuild":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getBuild());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "About":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) {
                    JSONObject about = this.systemParams.getJsonObject(GET_ABOUT_ALL);
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, about.optJSONObject("about"));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "VersionWearOS":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getWear());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "VersionAndroid":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getAndroid());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "VersionSecurity":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getSecurity());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "OtherBootloader":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getBootloader());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "OtherBoard":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getBoard());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "OtherSdk":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.systemParams.getSdk());
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            case "Other":
                if (observe) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Observe requested on non-observeable property: " + property);
                    logMethodEntranceExit(false, "Observe requested on non-observeable property: " + property);
                    return false;
                }
                if (get) {
                    JSONObject other = this.systemParams.getJsonObject(GET_OTHER_ALL);
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, other.optJSONObject("other"));
                }
                if (!observe && !get) {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Cancelling Observation requested on non-observeable property: " + property);
                }
                break;
            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown System Property requested: " + property);
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

    public void autoUpdate(boolean enableAutoUpdate) {
        logMethodEntranceExit(true);
        Log.d(TAG, "autoUpdate(" + enableAutoUpdate + ")");
        if (enableAutoUpdate) {
            try {
                if (this.autoUpdate.get()) return;
                if (scheduledFuture != null && !scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
                Runnable updateSystemTask = () -> updateSystemAsync();
                scheduledFuture = scheduler.scheduleAtFixedRate(updateSystemTask, INITIAL_DELAY, REFRESH_TIMEOUT, TimeUnit.SECONDS);
                this.autoUpdate.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (!this.autoUpdate.get()) return;
                if (scheduledFuture != null && !scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
                this.autoUpdate.set(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logMethodEntranceExit(false);
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

    private System() {
        logMethodEntranceExit(true);
        scheduler.setRemoveOnCancelPolicy(true);
        this.systemParamsOld = new SystemParams();
        this.systemParams = new SystemParams();
        this.systemParams.setAndroid(Build.VERSION.RELEASE);
        this.systemParams.setManufacturer(Build.MANUFACTURER);
        this.systemParams.setModel(Build.MODEL);
        String serial = MqttHelper.getInstance().getClientId();
        this.systemParams.setSerial(serial);
        this.systemParams.setName(Build.MODEL + " " + serial.substring(serial.length() - 4));
        this.systemParams.setBuild(Build.ID);
        this.systemParams.setBootloader(Build.BOOTLOADER);
        this.systemParams.setBoard(Build.BOARD);
        this.systemParams.setSdk(Build.VERSION.SDK_INT);
        this.systemParams.setSecurity(Build.VERSION.SECURITY_PATCH);
        logMethodEntranceExit(false);
    }

    public static System getInstance() {
        return instance;
    }

    private void doUpdateSystemParams(SystemParams systemParams) {
        logMethodEntranceExit(true);
        try {
            String homeAppVersion = SotiAgentApplication.getAppContext().getPackageManager().getPackageInfo("com.google.android.wearable.app", 0).versionName;
            systemParams.setHomeApp(homeAppVersion);
            systemParams.setWear(this.homeAppToOsVersion(homeAppVersion));

            systemParams.setPlayServices(SotiAgentApplication.getAppContext().getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0).versionName);

        } catch (Exception e) {
            e.printStackTrace();
            logMethodEntranceExit(false, "Exception occured!");
        }
        logMethodEntranceExit(false);
    }

    private void updateSystemAsync() {
        if (this.observationMaskMap.isEmpty()) return;
        this.doUpdateSystemParams(this.systemParams);
        if (this.systemParams.equals(this.systemParamsOld)) return;
        new UpdateSystemRunnable(this.systemParams, this.systemParamsOld);
        this.systemParamsOld = this.systemParams;
        this.systemParams = new SystemParams(this.systemParamsOld);
    }

    private class UpdateSystemRunnable implements Runnable {

        private final SystemParams systemParamsOld;
        private final SystemParams systemParams;

        public UpdateSystemRunnable(SystemParams systemParams, SystemParams systemParamsOld) {
            this.systemParamsOld = new SystemParams(systemParamsOld);
            this.systemParams = new SystemParams(systemParams);
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            if (getInstance() == null) {
                logMethodEntranceExit(false, "instance == null");
                return;
            }
            try {
                if (System.this.autoUpdate.get()) {
                    System.this.observationMaskMap.forEach((key, value) -> {
                        int observationMask = System.this.observationMaskMap.get(key).intValue();
                        if (!this.systemParams.equalsMasked(this.systemParamsOld, observationMask))
                            this.sendSystemMessage(key, observationMask);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
            logMethodEntranceExit(false);
        }

        private void sendSystemMessage(String target, int observationMask) {
            if (MqttHelper.getInstance() == null) {
                logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
                return;
            }
            if (!MqttHelper.getInstance().isConnected()) {
                logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
                MqttHelper.getInstance().connect();
                return;
            }
            JSONObject systemJsonObject = this.systemParams.getJsonObject(this.systemParamsOld, observationMask);
            if (systemJsonObject.length() < 1) return;
            if (systemJsonObject.length() == 1) {
                Iterator<String> keys = systemJsonObject.keys();
                String key=keys.next();
                Object value = systemJsonObject.opt(key);
                String messageId = key.substring(0, 1).toUpperCase() + key.substring(1);
                JSONObject systemInnerJsonObject = (JSONObject)value;
                if (systemInnerJsonObject.length() < 1) return;
                if (systemInnerJsonObject.length() == 1) {
                    Iterator<String> innerKeys = systemInnerJsonObject.keys();
                    String innerKey=innerKeys.next();
                    Object innerValue = systemInnerJsonObject.opt(innerKey);
                    String innerMessageId = innerKey.substring(0, 1).toUpperCase() + innerKey.substring(1);
                    MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId + innerMessageId, target, innerValue);
                } else {
                    MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId, target, value);
                }
            } else {
                MqttHelper.getInstance().doNotify(MQTT_SOURCE, target, systemJsonObject);
            }
        }
    }

    private String homeAppToOsVersion(String wearAppVersion) {
        if (!wearAppVersion.startsWith("2.")) return "unknown";
        try {
            int homeAppVersion = Integer.parseInt(wearAppVersion.substring(2, 4));
            if (homeAppVersion < 10/* || homeAppVersion > 29*/) return "unknown";
            if (homeAppVersion < 18) return "1." + (homeAppVersion-10);
            if (homeAppVersion < 20) return "2." + (homeAppVersion-18) + " (unverified)";
            if (homeAppVersion < 22) return "2." + (homeAppVersion-18);
            if (homeAppVersion < 25) return "2.6";
            if (homeAppVersion < 28) return "2.7";
//            if (homeAppVersion < 35) return "2." + (homeAppVersion-19);
            return "2." + (homeAppVersion-19);
//            return "2." + (homeAppVersion-19) + " (unverified)";

//            switch(homeAppVersion) {
//                case 18:
//                    return "2.0 (unverified)";
//                case 19:
//                    return "2.1 (unverified)";
//                case 20:
//                    return "2.2";
//                case 21:
//                    return "2.3";
//                case 22:
//                case 23:
//                case 24:
//                    return "2.6";
//                case 25:
//                    return "2.7";
//                case 26:
//                case 27:
//                    return "2.8";
//                case 28:
//                    return "2.9";
//                case 29:
//                    return "2.10";
//                case 30:
//                    return "2.11";
//                case 31:
//                    return "2.12";
//                case 32:
//                    return "2.13";
//                case 33:
//                    return "2.14 (unverified)";
//                case 34:
//                    return "2.15 (unverified)";
//                case 35:
//                    return "2.16 (unverified)";
//                case 36:
//                    return "2.17 (unverified)";
//                case 37:
//                    return "2.18 (unverified)";
//                case 38:
//                    return "2.19 (unverified)";
//                case 39:
//                    return "2.20 (unverified)";
//                case 40:
//                    return "2.21 (unverified)";
//                case 41:
//                    return "2.22 (unverified)";
//                case 42:
//                    return "2.23 (unverified)";
//                case 43:
//                    return "2.24 (unverified)";
//                case 44:
//                    return "2.25 (unverified)";
//                case 45:
//                    return "2.26 (unverified)";
//                case 46:
//                    return "2.27 (unverified)";
//                case 47:
//                    return "2.28 (unverified)";
//                case 48:
//                    return "2.29 (unverified)";
//                case 49:
//                    return "2.30 (unverified)";
//                default:
//                    return "unknown";
//            }
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    static {
        instance = new System();
    }
}
