package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class Gps {

    public static class GpsParams {

        private boolean enabled;
        private Location location;
        private final Object globalLock = new Object();

        public GpsParams() {
            this.setDefaults();
        }

        public GpsParams(GpsParams gpsParams) {
            this.set(gpsParams);
        }

        public GpsParams(boolean enabled) {
            synchronized (globalLock) {
                this.enabled = enabled;
            }
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.enabled = false;
                this.location = new Location("");
            }
        }

        public void set(GpsParams GpsParams) {
            synchronized (globalLock) {
                this.enabled = GpsParams.enabled;
                this.location = new Location(GpsParams.location);
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            synchronized (globalLock) {
                this.enabled = enabled;
            }
        }

        public Location getLocation() {
            return new Location(location);
        }

        public void setLocation(Location location) {
            synchronized (globalLock) {
                this.location.set(location);
            }
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    jsonObject.put("enabled", this.enabled);
                    jsonObject.put("latitude", this.location.getLatitude());
                    jsonObject.put("longitude", this.location.getLongitude());
                    if (this.location.hasAccuracy())
                        jsonObject.put("accuracy", this.location.getAccuracy());
                    if (this.location.hasAltitude())
                        jsonObject.put("altitude", this.location.getAltitude());
                    if (this.location.hasVerticalAccuracy())
                        jsonObject.put("verticalAccuracy", this.location.getVerticalAccuracyMeters());
                    if (this.location.hasBearing())
                        jsonObject.put("bearing", this.location.getBearing());
                    if (this.location.hasBearingAccuracy())
                        jsonObject.put("bearingAccuracy", this.location.getBearingAccuracyDegrees());
                    if (this.location.hasSpeed()) jsonObject.put("speed", this.location.getSpeed());
                    if (this.location.hasSpeedAccuracy())
                        jsonObject.put("speedAccuracy", this.location.getSpeedAccuracyMetersPerSecond());
                    jsonObject.put("time", this.location.getTime());
                    jsonObject.put("realtimenanos", this.location.getElapsedRealtimeNanos());
                    jsonObject.put("provider", this.location.getProvider());
                    jsonObject.put("mockProvider", this.location.isFromMockProvider());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        public JSONObject getJsonObject(int mask) {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_ENABLED) != 0) jsonObject.put("enabled", this.enabled);
                    if ((mask & OBSERVE_LATITUDE) != 0)
                        jsonObject.put("latitude", this.location.getLatitude());
                    if ((mask & OBSERVE_LONGITUDE) != 0)
                        jsonObject.put("longitude", this.location.getLongitude());
                    if ((mask & OBSERVE_ACCURACY) != 0 && this.location.hasAccuracy())
                        jsonObject.put("accuracy", this.location.getAccuracy());
                    if ((mask & OBSERVE_ALTITUDE) != 0 && this.location.hasAltitude())
                        jsonObject.put("altitude", this.location.getAltitude());
                    if ((mask & OBSERVE_VERTICAL_ACCURACY) != 0 && this.location.hasVerticalAccuracy())
                        jsonObject.put("verticalAccuracy", this.location.getVerticalAccuracyMeters());
                    if ((mask & OBSERVE_BEARING) != 0 && this.location.hasBearing())
                        jsonObject.put("bearing", this.location.getBearing());
                    if ((mask & OBSERVE_BEARING_ACCURACY) != 0 && this.location.hasBearingAccuracy())
                        jsonObject.put("bearingAccuracy", this.location.getBearingAccuracyDegrees());
                    if ((mask & OBSERVE_SPEED) != 0 && this.location.hasSpeed())
                        jsonObject.put("speed", this.location.getSpeed());
                    if ((mask & OBSERVE_SPEED_ACCURACY) != 0 && this.location.hasSpeedAccuracy())
                        jsonObject.put("speedAccuracy", this.location.getSpeedAccuracyMetersPerSecond());
                    if ((mask & OBSERVE_TIME) != 0) jsonObject.put("time", this.location.getTime());
                    if ((mask & OBSERVE_REAL_TIME_NANOS) != 0)
                        jsonObject.put("realtimenanos", this.location.getElapsedRealtimeNanos());
                    if ((mask & OBSERVE_PROVIDER) != 0)
                        jsonObject.put("provider", this.location.getProvider());
                    if ((mask & OBSERVE_MOCK_PROVIDER) != 0)
                        jsonObject.put("mockProvider", this.location.isFromMockProvider());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        public JSONObject getJsonObject(GpsParams GpsParamsOld, int mask) {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != GpsParamsOld.enabled)
                        jsonObject.put("enabled", this.enabled);
                    if ((mask & OBSERVE_LATITUDE) != 0 && this.location.getLatitude() != GpsParamsOld.location.getLatitude())
                        jsonObject.put("latitude", this.location.getLatitude());
                    if ((mask & OBSERVE_LONGITUDE) != 0 && this.location.getLongitude() != GpsParamsOld.location.getLongitude())
                        jsonObject.put("longitude", this.location.getLongitude());
                    if ((mask & OBSERVE_ACCURACY) != 0 && this.location.hasAccuracy() && (!GpsParamsOld.location.hasAccuracy() || this.location.getAccuracy() != GpsParamsOld.location.getAccuracy()))
                        jsonObject.put("accuracy", this.location.getAccuracy());
                    if ((mask & OBSERVE_ALTITUDE) != 0 && this.location.hasAltitude() && (!GpsParamsOld.location.hasAltitude() || this.location.getAltitude() != GpsParamsOld.location.getAltitude()))
                        jsonObject.put("altitude", this.location.getAltitude());
                    if ((mask & OBSERVE_VERTICAL_ACCURACY) != 0 && this.location.hasVerticalAccuracy() && (!GpsParamsOld.location.hasVerticalAccuracy() || this.location.getVerticalAccuracyMeters() != GpsParamsOld.location.getVerticalAccuracyMeters()))
                        jsonObject.put("verticalAccuracy", this.location.getVerticalAccuracyMeters());
                    if ((mask & OBSERVE_BEARING) != 0 && this.location.hasBearing() && (!GpsParamsOld.location.hasBearing() || this.location.getBearing() != GpsParamsOld.location.getBearing()))
                        jsonObject.put("bearing", this.location.getBearing());
                    if ((mask & OBSERVE_BEARING_ACCURACY) != 0 && this.location.hasBearingAccuracy() && (!GpsParamsOld.location.hasBearingAccuracy() || this.location.getBearingAccuracyDegrees() != GpsParamsOld.location.getBearingAccuracyDegrees()))
                        jsonObject.put("bearingAccuracy", this.location.getBearingAccuracyDegrees());
                    if ((mask & OBSERVE_SPEED) != 0 && this.location.hasSpeed() && (!GpsParamsOld.location.hasSpeed() || this.location.getSpeed() != GpsParamsOld.location.getSpeed()))
                        jsonObject.put("speed", this.location.getSpeed());
                    if ((mask & OBSERVE_SPEED_ACCURACY) != 0 && this.location.hasSpeedAccuracy() && (!GpsParamsOld.location.hasSpeedAccuracy() || this.location.getSpeedAccuracyMetersPerSecond() != GpsParamsOld.location.getSpeedAccuracyMetersPerSecond()))
                        jsonObject.put("speedAccuracy", this.location.getSpeedAccuracyMetersPerSecond());
                    if ((mask & OBSERVE_TIME) != 0 && this.location.getTime() != GpsParamsOld.location.getTime())
                        jsonObject.put("time", this.location.getTime());
                    if ((mask & OBSERVE_REAL_TIME_NANOS) != 0 && this.location.getElapsedRealtimeNanos() != GpsParamsOld.location.getElapsedRealtimeNanos())
                        jsonObject.put("realtimenanos", this.location.getElapsedRealtimeNanos());
                    if ((mask & OBSERVE_PROVIDER) != 0 && !this.location.getProvider().equals(GpsParamsOld.location.getProvider()))
                        jsonObject.put("provider", this.location.getProvider());
                    if ((mask & OBSERVE_MOCK_PROVIDER) != 0 && this.location.isFromMockProvider() != GpsParamsOld.location.isFromMockProvider())
                        jsonObject.put("mockProvider", this.location.isFromMockProvider());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GpsParams GpsParams = (GpsParams) o;

            synchronized (globalLock) {
                if (this.enabled != GpsParams.enabled) return false;
                if (this.location.getLatitude() != GpsParams.location.getLatitude()) return false;
                if (this.location.getLongitude() != GpsParams.location.getLongitude()) return false;
                if (this.location.hasAccuracy() != GpsParams.location.hasAccuracy()) return false;
                if (this.location.hasAccuracy() && this.location.getAccuracy() != GpsParams.location.getAccuracy())
                    return false;
                if (this.location.hasAltitude() != GpsParams.location.hasAltitude()) return false;
                if (this.location.getAltitude() != GpsParams.location.getAltitude()) return false;
                if (this.location.hasVerticalAccuracy() != GpsParams.location.hasVerticalAccuracy())
                    return false;
                if (this.location.hasVerticalAccuracy() && this.location.getVerticalAccuracyMeters() != GpsParams.location.getVerticalAccuracyMeters())
                    return false;
                if (this.location.hasBearing() != GpsParams.location.hasBearing()) return false;
                if (this.location.hasBearing() && this.location.getBearing() != GpsParams.location.getBearing())
                    return false;
                if (this.location.hasBearingAccuracy() != GpsParams.location.hasBearingAccuracy())
                    return false;
                if (this.location.hasBearingAccuracy() && this.location.getBearingAccuracyDegrees() != GpsParams.location.getBearingAccuracyDegrees())
                    return false;
                if (this.location.hasSpeed() != GpsParams.location.hasSpeed()) return false;
                if (this.location.hasSpeed() && this.location.getSpeed() != GpsParams.location.getSpeed())
                    return false;
                if (this.location.hasSpeedAccuracy() != GpsParams.location.hasSpeedAccuracy())
                    return false;
                if (this.location.hasSpeedAccuracy() && this.location.getSpeedAccuracyMetersPerSecond() != GpsParams.location.getSpeedAccuracyMetersPerSecond())
                    return false;
                if (this.location.getTime() != GpsParams.location.getTime()) return false;
                if (this.location.getElapsedRealtimeNanos() != GpsParams.location.getElapsedRealtimeNanos())
                    return false;
                if (!this.location.getProvider().equals(GpsParams.location.getProvider()))
                    return false;
                if (this.location.isFromMockProvider() != GpsParams.location.isFromMockProvider())
                    return false;
            }
            return true;
        }


        public boolean equalsMasked(GpsParams GpsParams, int mask) {
            synchronized (globalLock) {
                if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != GpsParams.enabled)
                    return false;
                if ((mask & OBSERVE_LATITUDE) != 0 && this.location.getLatitude() != GpsParams.location.getLatitude())
                    return false;
                if ((mask & OBSERVE_LONGITUDE) != 0 && this.location.getLongitude() != GpsParams.location.getLongitude())
                    return false;
                if ((mask & OBSERVE_ACCURACY) != 0 && this.location.hasAccuracy() != GpsParams.location.hasAccuracy())
                    return false;
                if ((mask & OBSERVE_ACCURACY) != 0 && this.location.hasAccuracy() && this.location.getAccuracy() != GpsParams.location.getAccuracy())
                    return false;
                if ((mask & OBSERVE_ALTITUDE) != 0 && this.location.hasAltitude() != GpsParams.location.hasAltitude())
                    return false;
                if ((mask & OBSERVE_ALTITUDE) != 0 && this.location.getAltitude() != GpsParams.location.getAltitude())
                    return false;
                if ((mask & OBSERVE_VERTICAL_ACCURACY) != 0 && this.location.hasVerticalAccuracy() != GpsParams.location.hasVerticalAccuracy())
                    return false;
                if ((mask & OBSERVE_VERTICAL_ACCURACY) != 0 && this.location.hasVerticalAccuracy() && this.location.getVerticalAccuracyMeters() != GpsParams.location.getVerticalAccuracyMeters())
                    return false;
                if ((mask & OBSERVE_BEARING) != 0 && this.location.hasBearing() != GpsParams.location.hasBearing())
                    return false;
                if ((mask & OBSERVE_BEARING) != 0 && this.location.hasBearing() && this.location.getBearing() != GpsParams.location.getBearing())
                    return false;
                if ((mask & OBSERVE_BEARING_ACCURACY) != 0 && this.location.hasBearingAccuracy() != GpsParams.location.hasBearingAccuracy())
                    return false;
                if ((mask & OBSERVE_BEARING_ACCURACY) != 0 && this.location.hasBearingAccuracy() && this.location.getBearingAccuracyDegrees() != GpsParams.location.getBearingAccuracyDegrees())
                    return false;
                if ((mask & OBSERVE_SPEED) != 0 && this.location.hasSpeed() != GpsParams.location.hasSpeed())
                    return false;
                if ((mask & OBSERVE_SPEED) != 0 && this.location.hasSpeed() && this.location.getSpeed() != GpsParams.location.getSpeed())
                    return false;
                if ((mask & OBSERVE_SPEED_ACCURACY) != 0 && this.location.hasSpeedAccuracy() != GpsParams.location.hasSpeedAccuracy())
                    return false;
                if ((mask & OBSERVE_SPEED_ACCURACY) != 0 && this.location.hasSpeedAccuracy() && this.location.getSpeedAccuracyMetersPerSecond() != GpsParams.location.getSpeedAccuracyMetersPerSecond())
                    return false;
                if ((mask & OBSERVE_TIME) != 0 && this.location.getTime() != GpsParams.location.getTime())
                    return false;
                if ((mask & OBSERVE_REAL_TIME_NANOS) != 0 && this.location.getElapsedRealtimeNanos() != GpsParams.location.getElapsedRealtimeNanos())
                    return false;
                if ((mask & OBSERVE_PROVIDER) != 0 && !this.location.getProvider().equals(GpsParams.location.getProvider()))
                    return false;
                if ((mask & OBSERVE_MOCK_PROVIDER) != 0 && this.location.isFromMockProvider() != GpsParams.location.isFromMockProvider())
                    return false;
            }
            return true;
        }

    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x03FF;
    public static final int OBSERVE_LOCATION = 0x03FE;
    public static final int OBSERVE_ENABLED = 0x0001;
    public static final int OBSERVE_LATITUDE = 0x0002;
    public static final int OBSERVE_LONGITUDE = 0x0004;
    public static final int OBSERVE_ACCURACY = 0x0008;
    public static final int OBSERVE_ALTITUDE = 0x0010;
    public static final int OBSERVE_VERTICAL_ACCURACY  = 0x0020;
    public static final int OBSERVE_BEARING = 0x0040;
    public static final int OBSERVE_BEARING_ACCURACY = 0x0080;
    public static final int OBSERVE_SPEED = 0x0100;
    public static final int OBSERVE_SPEED_ACCURACY = 0x0200;
    public static final int OBSERVE_TIME = 0x0400;
    public static final int OBSERVE_REAL_TIME_NANOS = 0x0800;
    public static final int OBSERVE_PROVIDER = 0x1000;
    public static final int OBSERVE_MOCK_PROVIDER = 0x2000;

    private static final String TAG = "MQTT (Gps Sensor)";
    private static final String MQTT_SOURCE = "Gps";

    private static Gps instance;

    private long UPDATE_INTERVAL = 1000;
    private long FASTEST_INTERVAL = 250;

    private LocationManager locationManager;
    private LocationRequest locationRequest;

    private GpsParams gpsParamsOld;
    private GpsParams gpsParams;

    private final Lock gpsLock = new ReentrantLock();
    private final Condition notificationArrived  = gpsLock.newCondition();
    private AtomicBoolean waitingForMoreUpdates = new AtomicBoolean(false);

    private void signalNotificationArrived() {
        this.gpsLock.lock();
        this.notificationArrived.signalAll();
        this.gpsLock.unlock();
    }

    public GpsParams getGpsParams() {
        return new GpsParams(this.gpsParams);
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
        Integer observationMaskInteger = observationMaskMap.get(originator);
        int observationMask = (observationMaskInteger == null)?OBSERVE_NONE:observationMaskInteger.intValue();
        boolean retVal = true;
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:get?observationMask:OBSERVE_NONE;
                if (observe) {
                    this.notifyLastLocation(property, originator, OBSERVE_ALL, true);
                } else if (get) {
                    this.respondLastLocation(property, originator, transactionId, OBSERVE_ALL);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS cancelled.");
                }
                break;
            case "Location":
                if (observe) observationMask |= OBSERVE_LOCATION; else if (!get) observationMask &= ~OBSERVE_LOCATION;
                if (observe) {
                    this.notifyLastLocation(property, originator, OBSERVE_LOCATION, true);
                } else if (get) {
                    this.respondLastLocation(property, originator, transactionId, OBSERVE_LOCATION);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Location cancelled.");
                }
                break;
            case "Enabled":
                if (observe) observationMask |= OBSERVE_ENABLED; else if (!get) observationMask &= ~OBSERVE_ENABLED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.isEnabled());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.isEnabled());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Enabled State cancelled.");
                }
                break;
            case "Latitude":
                if (observe) observationMask |= OBSERVE_LATITUDE; else if (!get) observationMask &= ~OBSERVE_LATITUDE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getLatitude());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getLatitude());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Latitude cancelled.");
                }
                break;
            case "Longitude":
                if (observe) observationMask |= OBSERVE_LONGITUDE; else if (!get) observationMask &= ~OBSERVE_LONGITUDE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getLongitude());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getLongitude());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Longitude cancelled.");
                }
                break;
            case "Accuracy":
                if (observe) observationMask |= OBSERVE_ACCURACY; else if (!get) observationMask &= ~OBSERVE_ACCURACY;
                if (observe) {
                    if (gpsParams.getLocation().hasAccuracy())
                        MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getAccuracy());
                } else if (get) {
                    if (gpsParams.getLocation().hasAccuracy())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getAccuracy());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Accuracy cancelled.");
                }
                break;
            case "Altitude":
                if (observe) observationMask |= OBSERVE_ALTITUDE; else if (!get) observationMask &= ~OBSERVE_ALTITUDE;
                if (observe) {
                    if (gpsParams.getLocation().hasAltitude())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getAltitude());
                } else if (get) {
                    if (gpsParams.getLocation().hasAltitude())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getAltitude());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Altitude cancelled.");
                }
                break;
            case "VerticalAccuracy":
                if (observe) observationMask |= OBSERVE_VERTICAL_ACCURACY; else if (!get) observationMask &= ~OBSERVE_VERTICAL_ACCURACY;
                if (observe) {
                    if (gpsParams.getLocation().hasVerticalAccuracy())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getVerticalAccuracyMeters());
                } else if (get) {
                    if (gpsParams.getLocation().hasVerticalAccuracy())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getVerticalAccuracyMeters());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Vertical Accuracy cancelled.");
                }
                break;
            case "Bearing":
                if (observe) observationMask |= OBSERVE_BEARING; else if (!get) observationMask &= ~OBSERVE_BEARING;
                if (observe) {
                    if (gpsParams.getLocation().hasBearing())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getBearing());
                } else if (get) {
                    if (gpsParams.getLocation().hasBearing())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getBearing());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Bearing cancelled.");
                }
                break;
            case "BearingAccuracy":
                if (observe) observationMask |= OBSERVE_BEARING_ACCURACY; else if (!get) observationMask &= ~OBSERVE_BEARING_ACCURACY;
                if (observe) {
                    if (gpsParams.getLocation().hasBearingAccuracy())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getBearingAccuracyDegrees());
                } else if (get) {
                    if (gpsParams.getLocation().hasBearingAccuracy())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getBearingAccuracyDegrees());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Bearing Accuracy cancelled.");
                }
                break;
            case "Speed":
                if (observe) observationMask |= OBSERVE_SPEED; else if (!get) observationMask &= ~OBSERVE_SPEED;
                if (observe) {
                    if (gpsParams.getLocation().hasSpeed())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getSpeed());
                } else if (get) {
                    if (gpsParams.getLocation().hasSpeed())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getSpeed());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Speed cancelled.");
                }
                break;
            case "SpeedAccuracy":
                if (observe) observationMask |= OBSERVE_SPEED_ACCURACY; else if (!get) observationMask &= ~OBSERVE_SPEED_ACCURACY;
                if (observe) {
                    if (gpsParams.getLocation().hasSpeedAccuracy())
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getSpeedAccuracyMetersPerSecond());
                } else if (get) {
                    if (gpsParams.getLocation().hasSpeedAccuracy())
                        retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getSpeedAccuracyMetersPerSecond());
                    else
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Property currently unavailable: " + property);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Speed Accuracy cancelled.");
                }
                break;
            case "Time":
                if (observe) observationMask |= OBSERVE_TIME; else if (!get) observationMask &= ~OBSERVE_TIME;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getTime());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getTime());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Time cancelled.");
                }
                break;
            case "RealTimeNanos":
                if (observe) observationMask |= OBSERVE_REAL_TIME_NANOS; else if (!get) observationMask &= ~OBSERVE_REAL_TIME_NANOS;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getElapsedRealtimeNanos());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getElapsedRealtimeNanos());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS RealTimeNanos cancelled.");
                }
                break;
            case "Provider":
                if (observe) observationMask |= OBSERVE_PROVIDER; else if (!get) observationMask &= ~OBSERVE_PROVIDER;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().getProvider());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().getProvider());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Provider cancelled.");
                }
                break;
            case "MockProvider":
                if (observe) observationMask |= OBSERVE_MOCK_PROVIDER; else if (!get) observationMask &= ~OBSERVE_MOCK_PROVIDER;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, gpsParams.getLocation().isFromMockProvider());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, gpsParams.getLocation().isFromMockProvider());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingGPS Mock Provider cancelled.");
                }
                break;
            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown Gps Property requested: " + property);
                retVal = false;
        }
        if (observationMask == OBSERVE_NONE) {
            observationMaskMap.remove(originator);
        } else {
            observationMaskMap.put(originator, new Integer(observationMask));
        }
        this.autoUpdate(!observationMaskMap.isEmpty());

        return retVal;
    }

    @SuppressLint("MissingPermission")
    private void autoUpdate(boolean enableAutoUpdate) {
        Log.d(TAG, "autoUpdate(" + enableAutoUpdate + ")");
        if (enableAutoUpdate) {
            try {
                if (this.autoUpdate.get()) return;
                getFusedLocationProviderClient(SotiAgentApplication.getAppContext()).requestLocationUpdates(locationRequest, locationCallback, null);
                this.autoUpdate.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (!this.autoUpdate.get()) return;
                getFusedLocationProviderClient(SotiAgentApplication.getAppContext()).removeLocationUpdates(locationCallback);
                this.autoUpdate.set(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    private Gps() {
        logMethodEntranceExit(true);
        this.gpsParamsOld = new GpsParams();
        this.gpsParams = new GpsParams();
        this.locationManager = (LocationManager ) SotiAgentApplication.getAppContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = Gps.this.locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
        this.gpsParams.setEnabled(isEnabled);
        if (isEnabled) this.notifyLastLocation(null, null,0, false);
        this.locationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(UPDATE_INTERVAL).setFastestInterval(FASTEST_INTERVAL);
        LocationServices.getSettingsClient(SotiAgentApplication.getAppContext()).checkLocationSettings( new LocationSettingsRequest.Builder().addLocationRequest(this.locationRequest).build() );
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        SotiAgentApplication.getAppContext().registerReceiver(this.broadcastReceiver, intentFilter);
        logMethodEntranceExit(false);
    }

    public static Gps getInstance() {
        return Gps.instance;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
         logMethodEntranceExit(true);
         if (intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
             Gps.this.gpsParams.setEnabled(Gps.this.locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ));
             Gps.this.updateGpsAsync();
         }
         logMethodEntranceExit(false);
        }
    };

    private void updateGpsAsync() {
        if (this.observationMaskMap.isEmpty()) return;
        if (this.waitingForMoreUpdates.get()) {
            this.signalNotificationArrived();
            return;
        }

        new Thread(){
            public void run(){
                Gps.this.gpsLock.lock();
                Gps.this.waitingForMoreUpdates.set(true);
                try {
                    int waitSomeMore = 10;
                    while(waitSomeMore-- > 0) {
                        if (!Gps.this.notificationArrived.await(100, TimeUnit.MILLISECONDS)) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Gps.this.waitingForMoreUpdates.set(false);
                    Gps.this.gpsLock.unlock();
                }
                if (Gps.this.gpsParams.equals(Gps.this.gpsParamsOld)) return;
                new UpdateGpsRunnable(Gps.this.gpsParams, Gps.this.gpsParamsOld);
                Gps.this.gpsParamsOld = Gps.this.gpsParams;
                Gps.this.gpsParams = new GpsParams(Gps.this.gpsParamsOld);
            }
        }.start();
    }

    private class UpdateGpsRunnable implements Runnable {

        private final GpsParams gpsParamsOld;
        private final GpsParams gpsParams;

        public UpdateGpsRunnable(GpsParams gpsParams, GpsParams gpsParamsOld) {
            this.gpsParamsOld = new GpsParams(gpsParamsOld);
            this.gpsParams = new GpsParams(gpsParams);
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            if (getInstance() == null) {
                logMethodEntranceExit(false, "instance == null");
                return;
            }
            try {
                if (Gps.this.autoUpdate.get()) {
                    Gps.this.observationMaskMap.forEach((key, value) -> {
                        int observationMask = Gps.this.observationMaskMap.get(key).intValue();
                        if (!this.gpsParams.equalsMasked(this.gpsParamsOld, observationMask))
                            this.sendGpsMessage(key, observationMask);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
            logMethodEntranceExit(false);
        }

        private void sendGpsMessage(String target, int observationMask) {
            if (MqttHelper.getInstance() == null) {
                logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
                return;
            }
            if (!MqttHelper.getInstance().isConnected()) {
                logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
                MqttHelper.getInstance().connect();
                return;
            }
            JSONObject GpsJsonObject = this.gpsParams.getJsonObject(this.gpsParamsOld, observationMask);
            if (GpsJsonObject.length() < 1) return;
            if (GpsJsonObject.length() == 1) { // Just a single value to notify!
                Iterator<String> keys = GpsJsonObject.keys();
                String key=keys.next();
                Object value = GpsJsonObject.opt(key);
                String messageId = key.substring(0, 1).toUpperCase() + key.substring(1);
                MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId, target, value);
            } else {
                MqttHelper.getInstance().doNotify(MQTT_SOURCE, target, GpsJsonObject);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void respondLastLocation(String property, String originator, long transactionId, int mask) {
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(SotiAgentApplication.getAppContext());
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        this.signalNotificationArrived();
                        Gps.this.gpsParams.setLocation(location);
                        if (originator != null) MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, Gps.this.gpsParams.getJsonObject(mask));
                    } else {
                        if (originator != null) MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "respondLastLocation returned null!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error trying to get last GPS location");
                    e.printStackTrace();
                    if (originator != null) MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "respondLastLocation Failure!");
                });
    }

    @SuppressLint("MissingPermission")
    private void notifyLastLocation(String property, String originator, int mask, boolean delayedNotification) {
        if (originator == null) return;
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(SotiAgentApplication.getAppContext());
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        this.signalNotificationArrived();
                        Gps.this.gpsParams.setLocation(location);
                        if (delayedNotification) {
                            MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, Gps.this.gpsParams.getJsonObject(mask));
                        } else {
                            MqttHelper.getInstance().doNotify(MQTT_SOURCE + property, originator, Gps.this.gpsParams.getJsonObject(mask));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error trying to get last GPS location");
                    e.printStackTrace();
                    if (delayedNotification) {
                        MqttHelper.getInstance().doRespondDelayed(MqttHelper.MQTT_ERROR, originator, MqttHelper.TRANSACTION_ID_NONE, "respondLastLocation Failure!");
                    } else {
                        MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, MqttHelper.TRANSACTION_ID_NONE, "respondLastLocation Failure!");
                    }
                });
    }

    private void onLocationChanged(Location location) {
        this.gpsParams.setLocation(location);
        this.updateGpsAsync();
    }

    private LocationCallback locationCallback  = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
        onLocationChanged(new Location(locationResult.getLastLocation()));
        }
    };

    static {
        instance = new Gps();
    }
}
