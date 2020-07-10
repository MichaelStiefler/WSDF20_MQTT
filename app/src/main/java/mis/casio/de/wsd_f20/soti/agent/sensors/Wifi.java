package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

public class Wifi {

    public static class WifiParams {

        private final Object bssidLock = new Object();
        private String bssid;
        private final Object frequencyLock = new Object();
        private int frequency;
        private final Object hiddenSsidLock = new Object();
        private boolean hiddenSsid;
        private final Object ipv4Lock = new Object();
        private String ipv4;
        private final Object ipv6Lock = new Object();
        private String ipv6;
        private final Object linkSpeedLock = new Object();
        private int linkSpeed;
        private final Object macAddressLock = new Object();
        private String macAddress;
        private final Object rssiLock = new Object();
        private int rssi;
        private final Object ssidLock = new Object();
        private String ssid;
        private final Object enabledLock = new Object();
        private boolean enabled;
        private final Object connectedLock = new Object();
        private boolean connected;

        private final Object globalLock = new Object();

        public WifiParams() {
            this.macAddress = MAC_NONE;
            this.setDefaults();
        }

        public WifiParams(WifiParams wifiParams) {
            this.set(wifiParams);
        }

        public WifiParams(boolean enabled, boolean connected, String macAddress, String bssid, String ssid, boolean hiddenSsid, int frequency, int rssi, int linkSpeed, String ipv4, String ipv6) {
            synchronized (globalLock) {
                this.enabled = enabled;
                this.connected = connected;
                this.macAddress = (macAddress == null ? MAC_NONE : macAddress);
                this.bssid = (bssid == null ? MAC_NONE : bssid);
                this.ssid = (ssid == null ? "<unknown ssid>" : ssid);
                this.hiddenSsid = hiddenSsid;
                this.frequency = frequency;
                this.rssi = rssi;
                this.linkSpeed = linkSpeed;
                this.ipv4 = (ipv4 == null ? IPV4_NONE : ipv4);
                this.ipv6 = (ipv6 == null ? IPV6_NONE : ipv6);
            }
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.enabled = false;
                this.connected = false;
                this.bssid = MAC_NONE;
                this.ssid = "<unknown ssid>";
                this.hiddenSsid = false;
                this.frequency = -1;
                this.rssi = -127;
                this.linkSpeed = -1;
                this.ipv4 = IPV4_NONE;
                this.ipv6 = IPV6_NONE;
            }
        }

        public void set(String bssid, String ssid, boolean hiddenSsid, int frequency, int rssi, int linkSpeed, String ipv4, String ipv6) {
            synchronized (globalLock) {
                this.bssid = (bssid == null ? MAC_NONE : bssid);
                this.ssid = (ssid == null ? "<unknown ssid>" : ssid);
                this.hiddenSsid = hiddenSsid;
                this.frequency = frequency;
                this.rssi = rssi;
                this.linkSpeed = linkSpeed;
                this.ipv4 = (ipv4 == null ? IPV4_NONE : ipv4);
                this.ipv6 = (ipv6 == null ? IPV6_NONE : ipv6);
            }
        }

        public void set(String bssid, String ssid, boolean hiddenSsid, int frequency, int rssi, int linkSpeed) {
            synchronized (globalLock) {
                this.bssid = (bssid == null ? MAC_NONE : bssid);
                this.ssid = (ssid == null ? "<unknown ssid>" : ssid);
                this.hiddenSsid = hiddenSsid;
                this.frequency = frequency;
                this.rssi = rssi;
                this.linkSpeed = linkSpeed;
            }
        }

        public void set(WifiParams wifiParams) {
            synchronized (globalLock) {
                this.macAddress = wifiParams.macAddress;
                this.enabled = wifiParams.enabled;
                this.connected = wifiParams.connected;
                this.bssid = wifiParams.bssid;
                this.ssid = wifiParams.ssid;
                this.hiddenSsid = wifiParams.hiddenSsid;
                this.frequency = wifiParams.frequency;
                this.rssi = wifiParams.rssi;
                this.linkSpeed = wifiParams.linkSpeed;
                this.ipv4 = wifiParams.ipv4;
                this.ipv6 = wifiParams.ipv6;
            }
        }


        public String getBssid() {
                return bssid;
        }

        public void setBssid(String bssid) {
            synchronized (bssidLock) {
                this.bssid = (bssid == null ? MAC_NONE : bssid);
            }
        }

        public int getFrequency() {
                return frequency;
        }

        public void setFrequency(int frequency) {
            synchronized (frequencyLock) {
                this.frequency = frequency;
            }
        }

        public boolean isHiddenSsid() {
                return hiddenSsid;
        }

        public void setHiddenSsid(boolean hiddenSsid) {
            synchronized (hiddenSsidLock) {
                this.hiddenSsid = hiddenSsid;
            }
        }

        public String getIpv4() {
                return ipv4;
        }

        public void setIpv4(String ipv4) {
            synchronized (ipv4Lock) {
                this.ipv4 = (ipv4 == null ? IPV4_NONE : ipv4);
            }
        }

        public String getIpv6() {
                return ipv6;
        }

        public void setIpv6(String ipv6) {
            synchronized (ipv6Lock) {
                this.ipv6 = (ipv6 == null ? IPV6_NONE : ipv6);
            }
        }

        public int getLinkSpeed() {
                return linkSpeed;
        }

        public void setLinkSpeed(int linkSpeed) {
            synchronized (linkSpeedLock) {
                this.linkSpeed = linkSpeed;
            }
        }

        public String getMacAddress() {
                return macAddress;
         }

        public void setMacAddress(String macAddress) {
            synchronized (macAddressLock) {
                this.macAddress = (macAddress == null ? MAC_NONE : macAddress);
            }
        }

        public int getRssi() {
                return rssi;
       }

        public void setRssi(int rssi) {
            synchronized (rssiLock) {
                this.rssi = rssi;
            }
        }

        public String getSsid() {
                return ssid.replaceAll("^\"|\"$", "");
        }

        public void setSsid(String ssid) {
            synchronized (ssidLock) {
                this.ssid = (ssid == null ? "<unknown ssid>" : ssid.replaceAll("^\"|\"$", ""));
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            synchronized (enabledLock) {
                this.enabled = enabled;
            }
        }

        public boolean isConnected() {
            return this.connected;
        }

        public void setConnected(boolean connected) {
            synchronized (connectedLock) {
                this.connected = connected;
            }
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    jsonObject.put("macAddress", this.macAddress);
                    jsonObject.put("enabled", this.enabled);
                    jsonObject.put("connected", this.connected);
                    jsonObject.put("bssid", this.bssid);
                    jsonObject.put("ssid", this.ssid.replaceAll("^\"|\"$", ""));
                    jsonObject.put("hiddenSsid", this.hiddenSsid);
                    jsonObject.put("frequency", this.frequency);
                    jsonObject.put("rssi", this.rssi);
                    jsonObject.put("linkSpeed", this.linkSpeed);
                    jsonObject.put("ipv4", this.ipv4);
                    jsonObject.put("ipv6", this.ipv6);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        public JSONObject getJsonObject(WifiParams wifiParamsOld, int mask) {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_MACADDRESS) != 0 && !this.macAddress.equalsIgnoreCase(wifiParamsOld.macAddress))
                        jsonObject.put("macAddress", this.macAddress);
                    if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != wifiParamsOld.enabled)
                        jsonObject.put("enabled", this.enabled);
                    if ((mask & OBSERVE_CONNECTED) != 0 && this.connected != wifiParamsOld.connected)
                        jsonObject.put("connected", this.connected);
                    if ((mask & OBSERVE_BSSID) != 0 && !this.bssid.equalsIgnoreCase(wifiParamsOld.bssid))
                        jsonObject.put("bssid", this.bssid);
                    if ((mask & OBSERVE_SSID) != 0 && !this.ssid.equals(wifiParamsOld.ssid))
                        jsonObject.put("ssid", this.ssid.replaceAll("^\"|\"$", ""));
                    if ((mask & OBSERVE_HIDDENSSID) != 0 && this.hiddenSsid != wifiParamsOld.hiddenSsid)
                        jsonObject.put("hiddenSsid", this.hiddenSsid);
                    if ((mask & OBSERVE_FREQUENCY) != 0 && this.frequency != wifiParamsOld.frequency)
                        jsonObject.put("frequency", this.frequency);
                    if ((mask & OBSERVE_RSSI) != 0 && this.rssi != wifiParamsOld.rssi)
                        jsonObject.put("rssi", this.rssi);
                    if ((mask & OBSERVE_SPEED) != 0 && this.linkSpeed != wifiParamsOld.linkSpeed)
                        jsonObject.put("linkSpeed", this.linkSpeed);
                    if ((mask & OBSERVE_IPV4) != 0 && !this.ipv4.equals(wifiParamsOld.ipv4))
                        jsonObject.put("ipv4", this.ipv4);
                    if ((mask & OBSERVE_IPV6) != 0 && !this.ipv6.equals(wifiParamsOld.ipv6))
                        jsonObject.put("ipv6", this.ipv6);
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
                    if ((mask & OBSERVE_MACADDRESS) != 0)
                        jsonObject.put("macAddress", this.macAddress);
                    if ((mask & OBSERVE_ENABLED) != 0) jsonObject.put("enabled", this.enabled);
                    if ((mask & OBSERVE_CONNECTED) != 0)
                        jsonObject.put("connected", this.connected);
                    if ((mask & OBSERVE_BSSID) != 0) jsonObject.put("bssid", this.bssid);
                    if ((mask & OBSERVE_SSID) != 0) jsonObject.put("ssid", this.ssid.replaceAll("^\"|\"$", ""));
                    if ((mask & OBSERVE_HIDDENSSID) != 0)
                        jsonObject.put("hiddenSsid", this.hiddenSsid);
                    if ((mask & OBSERVE_FREQUENCY) != 0)
                        jsonObject.put("frequency", this.frequency);
                    if ((mask & OBSERVE_RSSI) != 0) jsonObject.put("rssi", this.rssi);
                    if ((mask & OBSERVE_SPEED) != 0) jsonObject.put("linkSpeed", this.linkSpeed);
                    if ((mask & OBSERVE_IPV4) != 0) jsonObject.put("ipv4", this.ipv4);
                    if ((mask & OBSERVE_IPV6) != 0) jsonObject.put("ipv6", this.ipv6);
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
            WifiParams wifiParams = (WifiParams) o;
            synchronized (globalLock) {
                return this.enabled == wifiParams.enabled &&
                        this.connected == wifiParams.connected &&
                        this.macAddress.equalsIgnoreCase(wifiParams.macAddress) &&
                        this.bssid.equalsIgnoreCase(wifiParams.bssid) &&
                        this.ssid.equals(wifiParams.ssid) &&
                        this.hiddenSsid == wifiParams.hiddenSsid &&
                        this.frequency == wifiParams.frequency &&
                        this.rssi == wifiParams.rssi &&
                        this.linkSpeed == wifiParams.linkSpeed &&
                        this.ipv4.equals(wifiParams.ipv4) &&
                        this.ipv6.equals(wifiParams.ipv6);
            }
        }

        public boolean equalsMasked(WifiParams wifiParams, int mask) {
            synchronized (globalLock) {
                if ((mask & OBSERVE_MACADDRESS) != 0 && !this.macAddress.equalsIgnoreCase(wifiParams.macAddress))
                    return false;
                if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != wifiParams.enabled)
                    return false;
                if ((mask & OBSERVE_CONNECTED) != 0 && this.connected != wifiParams.connected)
                    return false;
                if ((mask & OBSERVE_BSSID) != 0 && !this.bssid.equalsIgnoreCase(wifiParams.bssid))
                    return false;
                if ((mask & OBSERVE_SSID) != 0 && !this.ssid.equals(wifiParams.ssid)) return false;
                if ((mask & OBSERVE_HIDDENSSID) != 0 && this.hiddenSsid != wifiParams.hiddenSsid)
                    return false;
                if ((mask & OBSERVE_FREQUENCY) != 0 && this.frequency != wifiParams.frequency)
                    return false;
                if ((mask & OBSERVE_RSSI) != 0 && this.rssi != wifiParams.rssi) return false;
                if ((mask & OBSERVE_SPEED) != 0 && this.linkSpeed != wifiParams.linkSpeed)
                    return false;
                if ((mask & OBSERVE_IPV4) != 0 && !this.ipv4.equals(wifiParams.ipv4)) return false;
                if ((mask & OBSERVE_IPV6) != 0 && !this.ipv6.equals(wifiParams.ipv6)) return false;
            }
            return true;
        }
    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x07FF;
    public static final int OBSERVE_MACADDRESS = 0x0001;
    public static final int OBSERVE_ENABLED = 0x0002;
    public static final int OBSERVE_CONNECTED = 0x0004;
    public static final int OBSERVE_BSSID = 0x0008;
    public static final int OBSERVE_SSID = 0x0010;
    public static final int OBSERVE_HIDDENSSID = 0x0020;
    public static final int OBSERVE_FREQUENCY = 0x0040;
    public static final int OBSERVE_RSSI = 0x0080;
    public static final int OBSERVE_SPEED = 0x0100;
    public static final int OBSERVE_IPV4 = 0x0200;
    public static final int OBSERVE_IPV6 = 0x0400;

    private static final String TAG = "MQTT (Wifi Sensor)";
    private static final String MQTT_SOURCE = "Wifi";
    private static final boolean LOG_METHOD_ENTRANCE_EXIT = false;

    private static final String MAC_NONE = "00:00:00:00:00:00";
    private static final String IPV4_NONE = "0.0.0.0";
    private static final String IPV6_NONE = "::/0";
    private static final int INITIAL_DELAY = 1;
    private static final int REFRESH_TIMEOUT = 1;

    private static Wifi instance;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture;
    private WifiParams wifiParamsOld;
    private WifiParams wifiParams;
    private final Lock wifiLock = new ReentrantLock();
    private final Condition notificationArrived  = wifiLock.newCondition();
    private AtomicBoolean waitingForMoreUpdates = new AtomicBoolean(false);

    private void signalNotificationArrived() {
        this.wifiLock.lock();
        this.notificationArrived.signalAll();
        this.wifiLock.unlock();
    }

    public WifiParams getWifiParams() {
        this.doUpdateWifi(this.wifiParams);
        return new WifiParams(this.wifiParams);
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
        if (observe | get) this.doUpdateWifi(this.wifiParams);
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:OBSERVE_NONE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.wifiParams.getJsonObject(OBSERVE_ALL));
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.wifiParams.getJsonObject(OBSERVE_ALL));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi cancelled.");
                }
                break;
            case "MacAddress":
                if (observe) observationMask |= OBSERVE_MACADDRESS; else if (!get) observationMask &= ~OBSERVE_MACADDRESS;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getMacAddress());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getMacAddress());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi MacAddress cancelled.");
                }
                break;
            case "Enabled":
                if (observe) observationMask |= OBSERVE_ENABLED; else if (!get) observationMask &= ~OBSERVE_ENABLED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.isEnabled());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.isEnabled());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Enabled State cancelled.");
                }
                break;
            case "Connected":
                if (observe) observationMask |= OBSERVE_CONNECTED; else if (!get) observationMask &= ~OBSERVE_CONNECTED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.isConnected());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.isConnected());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Connected State cancelled.");
                }
                break;
            case "Bssid":
                if (observe) observationMask |= OBSERVE_BSSID; else if (!get) observationMask &= ~OBSERVE_BSSID;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getBssid());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getBssid());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Bssid cancelled.");
                }
                break;
            case "Ssid":
                if (observe) observationMask |= OBSERVE_SSID; else if (!get) observationMask &= ~OBSERVE_SSID;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getSsid());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getSsid());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Ssid cancelled.");
                }
                break;
            case "HiddenSsid":
                if (observe) observationMask |= OBSERVE_HIDDENSSID; else if (!get) observationMask &= ~OBSERVE_HIDDENSSID;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.isHiddenSsid());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.isHiddenSsid());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi HiddenSsid cancelled.");
                }
                break;
            case "Frequency":
                if (observe) observationMask |= OBSERVE_FREQUENCY; else if (!get) observationMask &= ~OBSERVE_FREQUENCY;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getFrequency());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getFrequency());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Frequency cancelled.");
                }
                break;
            case "Rssi":
                if (observe) observationMask |= OBSERVE_RSSI; else if (!get) observationMask &= ~OBSERVE_RSSI;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getRssi());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getRssi());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Rssi cancelled.");
                }
                break;
            case "Speed":
                if (observe) observationMask |= OBSERVE_SPEED; else if (!get) observationMask &= ~OBSERVE_SPEED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getLinkSpeed());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getLinkSpeed());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Speed cancelled.");
                }
                break;
            case "Ipv4":
                if (observe) observationMask |= OBSERVE_IPV4; else if (!get) observationMask &= ~OBSERVE_IPV4;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getIpv4());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getIpv4());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Ipv4 cancelled.");
                }
                break;
            case "Ipv6":
                if (observe) observationMask |= OBSERVE_IPV6; else if (!get) observationMask &= ~OBSERVE_IPV6;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, wifiParams.getIpv6());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, wifiParams.getIpv6());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingWifi Ipv6 cancelled.");
                }
                break;
            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown Wifi Property requested: " + property);
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

    private Wifi () {
        logMethodEntranceExit(true);
        this.wifiManager = (WifiManager) SotiAgentApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);
        scheduler.setRemoveOnCancelPolicy(true);
        this.wifiParamsOld = new WifiParams();
        this.wifiParams = new WifiParams();
        this.connectivityManager = (ConnectivityManager)SotiAgentApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        SotiAgentApplication.getAppContext().registerReceiver(this.broadcastReceiver, intentFilter);
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), this.networkCallback);

        logMethodEntranceExit(false);
    }



    private void autoUpdate(boolean enableAutoUpdate) {
        Log.d(TAG, "autoUpdate(" + enableAutoUpdate + ")");
        if (enableAutoUpdate) {
            try {
                if (this.autoUpdate.get()) return;
                if (scheduledFuture != null && !scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
                Runnable updateWifiTask = () -> updateWifiAsync();
                scheduledFuture = scheduler.scheduleAtFixedRate(updateWifiTask, INITIAL_DELAY, REFRESH_TIMEOUT, TimeUnit.SECONDS);
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

    public static Wifi getInstance() {
        return Wifi.instance;
    }

    public static void enableWifi(boolean enable) {
        getInstance().wifiManager.setWifiEnabled(enable);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logMethodEntranceExit(true);
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    if (info.isConnected()) {
                        Wifi.this.wifiParams.setConnected(true);
                    } else {
                        Wifi.this.wifiParams.setConnected(false);
                    }
                    Wifi.this.updateWifiAsync();
                }
            }
            else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION))  {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Wifi.this.wifiParams.setEnabled(true);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                    default:
                        Wifi.this.wifiParams.setEnabled(false);
                        break;
                }
                Wifi.this.updateWifiAsync();
            }
            else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION))  {
                Wifi.this.updateWifiAsync();
            }
            logMethodEntranceExit(false);
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(Network network) {
            Wifi.this.wifiParams.setConnected(true);
            updateWifiAsync();
        }
        @Override
        public void onLost(Network network) {
            Wifi.this.wifiParams.setConnected(false);
            updateWifiAsync();
        }
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            Field mSignalStrengthField = null;
            int signalStrength = -127;
            try {
                mSignalStrengthField = networkCapabilities.getClass().getDeclaredField("mSignalStrength");
                mSignalStrengthField.setAccessible(true);
                signalStrength = mSignalStrengthField.getInt(networkCapabilities);
                Wifi.this.wifiParams.setRssi(signalStrength);
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateWifiAsync();
        }
        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            Inet4Address inet4Address = null;
            Inet6Address inet6Address = null;
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                InetAddress inetAddress = linkAddress.getAddress();
                if (inetAddress instanceof Inet4Address) inet4Address = (Inet4Address)inetAddress;
                else if (inetAddress instanceof Inet6Address) inet6Address = (Inet6Address)inetAddress;
            }
            if (inet4Address != null) getInstance().wifiParams.setIpv4(inet4Address.getHostAddress());
            if (inet6Address != null) getInstance().wifiParams.setIpv6(inet6Address.getHostAddress());
            updateWifiAsync();
        }
    };

    private void updateWifiAsync() {
        if (this.observationMaskMap.isEmpty()) return;
        if (this.waitingForMoreUpdates.get()) {
            this.signalNotificationArrived();
            return;
        }

        new Thread(){
            public void run(){
                Wifi.this.wifiLock.lock();
                Wifi.this.waitingForMoreUpdates.set(true);
                try {
                    int waitSomeMore = 10;
                    while(waitSomeMore-- > 0) {
                        if (!Wifi.this.notificationArrived.await(100, TimeUnit.MILLISECONDS)) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Wifi.this.waitingForMoreUpdates.set(false);
                    Wifi.this.wifiLock.unlock();
                }
                if (Wifi.this.wifiParams.equals(Wifi.this.wifiParamsOld)) return;
                new Wifi.UpdateWifiRunnable(Wifi.this.wifiParams, Wifi.this.wifiParamsOld);
                Wifi.this.wifiParamsOld = Wifi.this.wifiParams;
                Wifi.this.wifiParams = new WifiParams(Wifi.this.wifiParamsOld);
            }
        }.start();
    }

    private void doUpdateWifi(WifiParams wifiParams) {
        logMethodEntranceExit(true);
        try {
            if (wifiParams.getMacAddress().equals(MAC_NONE))
                wifiParams.setMacAddress(getMacAddr());
            if (!wifiParams.isConnected()) {
                wifiParams.setDefaults();
                logMethodEntranceExit(false, "isWifiConnected is false!");
            } else {
                WifiInfo wifiInfo = this.wifiManager.getConnectionInfo();
                if (wifiInfo == null) {
                    wifiParams.setDefaults();
                    logMethodEntranceExit(false, "wifiInfo == null");
                } else {

                    wifiParams.set(
                            wifiInfo.getBSSID(),
                            wifiInfo.getSSID(),
                            wifiInfo.getHiddenSSID(),
                            wifiInfo.getFrequency(),
                            wifiInfo.getRssi(),
                            wifiInfo.getLinkSpeed());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logMethodEntranceExit(false, "Exception occured!");
        }
        logMethodEntranceExit(false);
    }

    private class UpdateWifiRunnable implements Runnable {

        private final WifiParams wifiParamsOld;
        private final WifiParams wifiParams;

        public UpdateWifiRunnable(WifiParams wifiParams, WifiParams wifiParamsOld) {
            this.wifiParamsOld = new WifiParams(wifiParamsOld);
            this.wifiParams = new WifiParams(wifiParams);
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            if (getInstance() == null) {
                logMethodEntranceExit(false, "instance == null");
                return;
            }
            try {
                if (Wifi.this.autoUpdate.get()) {
                    Wifi.this.observationMaskMap.forEach((key, value) -> {
                        int observationMask = Wifi.this.observationMaskMap.get(key).intValue();
                        if (!this.wifiParams.equalsMasked(this.wifiParamsOld, observationMask))
                            this.sendWifiMessage(key, observationMask);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
            logMethodEntranceExit(false);
        }

        private void sendWifiMessage(String target, int observationMask) {
            logMethodEntranceExit(true);
            if (MqttHelper.getInstance() == null) {
                logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
                return;
            }
            if (!MqttHelper.getInstance().isConnected()) {
                logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
                MqttHelper.getInstance().connect();
                return;
            }

            JSONObject wifiJsonObject = wifiParams.getJsonObject(wifiParamsOld, observationMask);
            if (wifiJsonObject.length() < 1) return;
            if (wifiJsonObject.length() == 1) { // Just a single value to notify!
                Iterator<String> keys = wifiJsonObject.keys();
                String key=keys.next();
                Object value = wifiJsonObject.opt(key);
                String messageId = key.substring(0, 1).toUpperCase() + key.substring(1);
                MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId, target, value);
            } else {
                MqttHelper.getInstance().doNotify(MQTT_SOURCE, target, wifiJsonObject);
            }
            logMethodEntranceExit(false);
        }
    }

    public static String getMacAddr() {
        logMethodEntranceExit(true);
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    logMethodEntranceExit(false, "macBytes == null");
                    return MAC_NONE;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02x:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                logMethodEntranceExit(false);
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        logMethodEntranceExit(false, "no MAC Address found!");
        return MAC_NONE;
    }

    static {
        instance = new Wifi();
    }
}
