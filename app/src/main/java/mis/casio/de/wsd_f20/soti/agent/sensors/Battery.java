package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

public class Battery {

    public static class BatteryParams {

        private final Object batteryPercentLock = new Object();
        private int batteryPercent;
        private final Object batteryStatusLock = new Object();
        private String batteryStatus;
        private final Object chargingLock = new Object();
        private boolean charging;
        private final Object chargeTypeLock = new Object();
        private String chargeType;
        private final Object batteryHealthLock = new Object();
        private String batteryHealth;
        private final Object voltageLock = new Object();
//        private String voltage;
        private float voltage;
        private final Object temperatureLock = new Object();
//        private String temperature;
        private float temperature;
        private final Object currentLock = new Object();
        private int current;
        private final Object averageCurrentLock = new Object();
        private int averageCurrent;
        private final Object globalLock = new Object();

        public String getBatteryStatus() {
            return batteryStatus;
        }

        public void setBatteryStatus(String batteryStatus) {
            synchronized (batteryStatusLock) {
                this.batteryStatus = batteryStatus;
            }
        }

        public boolean isCharging() {
            return charging;
        }

        public void setCharging(boolean charging) {
            synchronized (chargingLock) {
                this.charging = charging;
            }
        }

        public String getChargeType() {
            return chargeType;
        }

        public void setChargeType(String chargeType) {
            synchronized (chargeTypeLock) {
                this.chargeType = chargeType;
            }
        }

        public String getBatteryHealth() {
            return batteryHealth;
        }

        public void setBatteryHealth(String batteryHealth) {
            synchronized (batteryHealthLock) {
                this.batteryHealth = batteryHealth;
            }
        }

//        public String getVoltage() {
//            return voltage;
//        }

        public float getVoltage() {
            return voltage;
        }

//        public void setVoltage(String voltage) {
//            synchronized (voltageLock) {
//                this.voltage = voltage;
//            }
//        }

//        public void setVoltage(float voltage) {
//            logMethodEntranceExit(true);
//            synchronized (voltageLock) {
//                DecimalFormat df = new DecimalFormat("0.###");
//                df.setRoundingMode(RoundingMode.HALF_EVEN);
//                this.voltage = df.format(voltage);
//            }
//            logMethodEntranceExit(false);
//        }

        public void setVoltage(float voltage) {
            synchronized (voltageLock) {
                this.voltage = voltage;
            }
        }

//        public String getTemperature() {
//            return temperature;
//        }
//
//        public void setTemperature(String temperature) {
//            synchronized (temperatureLock) {
//                this.temperature = temperature;
//            }
//        }
//
//        public void setTemperature(float temperature) {
//            logMethodEntranceExit(true);
//            synchronized (temperatureLock) {
//                DecimalFormat df = new DecimalFormat("0.#");
//                df.setRoundingMode(RoundingMode.HALF_EVEN);
//                this.temperature = df.format(temperature);
//            }
//            logMethodEntranceExit(false);
//        }

        public float getTemperature() {
            return temperature;
        }

        public void setTemperature(float temperature) {
            synchronized (temperatureLock) {
                this.temperature = temperature;
            }
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            synchronized (currentLock) {
                this.current = current;
            }
        }

        public int getAverageCurrent() {
            return averageCurrent;
        }

        public void setAverageCurrent(int averageCurrent) {
            synchronized (averageCurrentLock) {
                this.averageCurrent = averageCurrent;
            }
        }


        public int getBatteryPercent() {
            return batteryPercent;
        }

        public void setBatteryPercent(int batteryPercent) {
            synchronized (batteryPercentLock) {
                this.batteryPercent = batteryPercent;
            }
        }


        public BatteryParams() {
            this.setDefaults();
        }

        public BatteryParams(BatteryParams batteryParams) {
            this.set(batteryParams);
        }

        public void setDefaults() {
            synchronized (globalLock) {
                this.batteryPercent = -1;
                this.batteryStatus = "unknown";
                this.charging = false;
                this.chargeType = "unknown";
                this.batteryHealth = "unknown";
                this.voltage = 0;
                this.temperature = 0;
                this.current = -1;
                this.averageCurrent = -1;
            }
        }

        public void set(BatteryParams batteryParams) {
            logMethodEntranceExit(true);
            synchronized (globalLock) {
                this.batteryPercent = batteryParams.batteryPercent;
                this.batteryStatus = batteryParams.batteryStatus;
                this.charging = batteryParams.charging;
                this.chargeType = batteryParams.chargeType;
                this.batteryHealth = batteryParams.batteryHealth;
                this.voltage = batteryParams.voltage;
                this.temperature = batteryParams.temperature;
                this.current = batteryParams.current;
                this.averageCurrent = batteryParams.averageCurrent;
            }
            logMethodEntranceExit(false);
        }

        public JSONObject getJsonObject() {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();

            try {
                synchronized (globalLock) {
                    jsonObject.put("batteryPercent", this.batteryPercent);
                    jsonObject.put("batteryStatus", this.batteryStatus);
                    jsonObject.put("batteryHealth", this.batteryHealth);
                    jsonObject.put("charging", this.charging);
                    jsonObject.put("chargeType", this.chargeType);
                    jsonObject.put("voltage", this.voltage);
                    jsonObject.put("temperature", this.temperature);
                    jsonObject.put("current", this.current);
                    jsonObject.put("averageCurrent", this.averageCurrent);
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

            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_BATTERY_PERCENT) != 0)
                        jsonObject.put("batteryPercent", this.batteryPercent);
                    if ((mask & OBSERVE_BATTERY_STATUS) != 0)
                        jsonObject.put("batteryStatus", this.batteryStatus);
                    if ((mask & OBSERVE_BATTERY_HEALTH) != 0)
                        jsonObject.put("batteryHealth", this.batteryHealth);
                    if ((mask & OBSERVE_BATTERY_CHARGING) != 0)
                        jsonObject.put("charging", this.charging);
                    if ((mask & OBSERVE_BATTERY_CHARGETYPE) != 0)
                        jsonObject.put("chargeType", this.chargeType);
                    if ((mask & OBSERVE_BATTERY_VOLTAGE) != 0)
                        jsonObject.put("voltage", this.voltage);
                    if ((mask & OBSERVE_BATTERY_TEMPERATURE) != 0)
                        jsonObject.put("temperature", this.temperature);
                    if ((mask & OBSERVE_BATTERY_CURRENT) != 0)
                        jsonObject.put("current", this.current);
                    if ((mask & OBSERVE_BATTERY_AVERAGECURRENT) != 0)
                        jsonObject.put("averageCurrent", this.averageCurrent);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logMethodEntranceExit(false);
            return jsonObject;
        }

        public JSONObject getJsonObject(BatteryParams batteryParamsOld, int mask) {
            logMethodEntranceExit(true);
            JSONObject jsonObject = new JSONObject();

            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_BATTERY_PERCENT) != 0 && this.batteryPercent != batteryParamsOld.batteryPercent)
                        jsonObject.put("batteryPercent", this.batteryPercent);
                    if ((mask & OBSERVE_BATTERY_STATUS) != 0 && !this.batteryStatus.equalsIgnoreCase(batteryParamsOld.batteryStatus))
                        jsonObject.put("batteryStatus", this.batteryStatus);
                    if ((mask & OBSERVE_BATTERY_HEALTH) != 0 && !this.batteryHealth.equalsIgnoreCase(batteryParamsOld.batteryHealth))
                        jsonObject.put("batteryHealth", this.batteryHealth);
                    if ((mask & OBSERVE_BATTERY_CHARGING) != 0 && this.charging != batteryParamsOld.charging)
                        jsonObject.put("charging", this.charging);
                    if ((mask & OBSERVE_BATTERY_CHARGETYPE) != 0 && !this.chargeType.equalsIgnoreCase(batteryParamsOld.chargeType))
                        jsonObject.put("chargeType", this.chargeType);
                    if ((mask & OBSERVE_BATTERY_VOLTAGE) != 0 && Math.abs(this.voltage - batteryParamsOld.voltage) > 0.001F)
                        jsonObject.put("voltage", this.voltage);
                    if ((mask & OBSERVE_BATTERY_TEMPERATURE) != 0 && Math.abs(this.temperature - batteryParamsOld.temperature) > 0.001F)
                        jsonObject.put("temperature", this.temperature);
                    if ((mask & OBSERVE_BATTERY_CURRENT) != 0 && this.current != batteryParamsOld.current)
                        jsonObject.put("current", this.current);
                    if ((mask & OBSERVE_BATTERY_AVERAGECURRENT) != 0 && this.averageCurrent != batteryParamsOld.averageCurrent)
                        jsonObject.put("averageCurrent", this.averageCurrent);
                }
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
            BatteryParams batteryParams = (BatteryParams) o;

            logMethodEntranceExit(false);
            synchronized (globalLock) {
                if (this.batteryPercent != batteryParams.batteryPercent) return false;
                if (this.charging != batteryParams.charging) return false;
                if (Math.abs(this.temperature - batteryParams.temperature) > 0.001F) return false;
                if (Math.abs(this.voltage - batteryParams.voltage) > 0.001F) return false;
                if (this.current != batteryParams.current) return false;
                if (this.averageCurrent != batteryParams.averageCurrent) return false;
                if (!this.batteryHealth.equalsIgnoreCase(batteryParams.batteryHealth)) return false;
                if (!this.batteryStatus.equalsIgnoreCase(batteryParams.batteryStatus)) return false;
                if (!this.chargeType.equalsIgnoreCase(batteryParams.chargeType)) return false;
            }
            return true;
        }

        public boolean equalsMasked(BatteryParams batteryParams, int mask) {
            logMethodEntranceExit(true);
            logMethodEntranceExit(false);
            synchronized (globalLock) {
                if ((mask & OBSERVE_BATTERY_PERCENT) != 0 && this.batteryPercent != batteryParams.batteryPercent)
                    return false;
                if ((mask & OBSERVE_BATTERY_CHARGING) != 0 && this.charging != batteryParams.charging)
                    return false;
                if ((mask & OBSERVE_BATTERY_TEMPERATURE) != 0 && Math.abs(this.temperature - batteryParams.temperature) > 0.001F)
                    return false;
                if ((mask & OBSERVE_BATTERY_VOLTAGE) != 0 && Math.abs(this.voltage - batteryParams.voltage) > 0.001F)
                    return false;
                if ((mask & OBSERVE_BATTERY_CURRENT) != 0 && this.current != batteryParams.current)
                    return false;
                if ((mask & OBSERVE_BATTERY_AVERAGECURRENT) != 0 && this.averageCurrent != batteryParams.averageCurrent)
                    return false;
                if ((mask & OBSERVE_BATTERY_HEALTH) != 0 && !this.batteryHealth.equalsIgnoreCase(batteryParams.batteryHealth))
                    return false;
                if ((mask & OBSERVE_BATTERY_STATUS) != 0 && !this.batteryStatus.equalsIgnoreCase(batteryParams.batteryStatus))
                    return false;
                if ((mask & OBSERVE_BATTERY_CHARGETYPE) != 0 && !this.chargeType.equalsIgnoreCase(batteryParams.chargeType))
                    return false;
            }
            return true;
        }

    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x07FF;
    public static final int OBSERVE_BATTERY_PERCENT = 0x0004;
    public static final int OBSERVE_BATTERY_STATUS = 0x0008;
    public static final int OBSERVE_BATTERY_HEALTH = 0x0010;
    public static final int OBSERVE_BATTERY_CHARGING = 0x0020;
    public static final int OBSERVE_BATTERY_CHARGETYPE = 0x0040;
    public static final int OBSERVE_BATTERY_VOLTAGE = 0x0080;
    public static final int OBSERVE_BATTERY_TEMPERATURE = 0x0100;
    public static final int OBSERVE_BATTERY_CURRENT = 0x0200;
    public static final int OBSERVE_BATTERY_AVERAGECURRENT = 0x0400;

    private static final String TAG = "MQTT (Battery Sensor)";
    private static final String MQTT_SOURCE = "Battery";

    private static final int INITIAL_DELAY = 10;
    private static final int REFRESH_TIMEOUT = 60;
    private static Battery instance;

    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture;

    private BatteryParams batteryParamsOld;
    private BatteryParams batteryParams;

    private final Lock batteryLock = new ReentrantLock();
    private final Condition notificationArrived  = batteryLock.newCondition();
    private AtomicBoolean waitingForMoreUpdates = new AtomicBoolean(false);

    private void signalNotificationArrived() {
        this.batteryLock.lock();
        this.notificationArrived.signalAll();
        this.batteryLock.unlock();
    }

    public BatteryParams getBatteryParams() {
        this.doUpdateBatteryParams(this.batteryParams);
        return new BatteryParams(this.batteryParams);
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
        if (get | observe) this.doUpdateBatteryParams(this.batteryParams);
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:get?observationMask:OBSERVE_NONE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getJsonObject(OBSERVE_ALL));
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getJsonObject());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "Observing Battery cancelled.");
                }
                break;
            case "Percent":
                if (observe) observationMask |= OBSERVE_BATTERY_PERCENT; else if (!get) observationMask &= ~OBSERVE_BATTERY_PERCENT;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getBatteryPercent());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getBatteryPercent());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "Observing Battery Charging Level (Percentage) cancelled.");
                }
                break;
            case "Status":
                if (observe) observationMask |= OBSERVE_BATTERY_STATUS; else if (!get) observationMask &= ~OBSERVE_BATTERY_STATUS;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getBatteryStatus());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getBatteryStatus());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "Observing Battery Status cancelled.");
                }
                break;
            case "Health":
                if (observe) observationMask |= OBSERVE_BATTERY_HEALTH; else if (!get) observationMask &= ~OBSERVE_BATTERY_HEALTH;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getBatteryHealth());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getBatteryHealth());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Health cancelled.");
                }
                break;
            case "Charging":
                if (observe) observationMask |= OBSERVE_BATTERY_CHARGING; else if (!get) observationMask &= ~OBSERVE_BATTERY_CHARGING;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.isCharging());
                } else if (get) {
                   retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.isCharging());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Charging State cancelled.");
                }
                break;
            case "ChargeType":
                if (observe) observationMask |= OBSERVE_BATTERY_CHARGETYPE; else if (!get) observationMask &= ~OBSERVE_BATTERY_CHARGETYPE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getChargeType());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getChargeType());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Charge Type cancelled.");
                }
                break;
            case "Voltage":
                if (observe) observationMask |= OBSERVE_BATTERY_VOLTAGE; else if (!get) observationMask &= ~OBSERVE_BATTERY_VOLTAGE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getVoltage());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getVoltage());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Voltage cancelled.");
                }
                break;
            case "Temperature":
                if (observe) observationMask |= OBSERVE_BATTERY_TEMPERATURE; else if (!get) observationMask &= ~OBSERVE_BATTERY_TEMPERATURE;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getTemperature());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getTemperature());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Temperature cancelled.");
                }
                break;
            case "Current":
                if (observe) observationMask |= OBSERVE_BATTERY_CURRENT; else if (!get) observationMask &= ~OBSERVE_BATTERY_CURRENT;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getCurrent());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getCurrent());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Current cancelled.");
                }
                break;
            case "AverageCurrent":
                if (observe) observationMask |= OBSERVE_BATTERY_AVERAGECURRENT; else if (!get) observationMask &= ~OBSERVE_BATTERY_AVERAGECURRENT;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.batteryParams.getAverageCurrent());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.batteryParams.getAverageCurrent());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBattery Average Current cancelled.");
                }
                break;
            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown Battery Property requested: " + property);
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
                Runnable updateBatteryTask = () -> updateBatteryAsync();
                scheduledFuture = scheduler.scheduleAtFixedRate(updateBatteryTask, INITIAL_DELAY, REFRESH_TIMEOUT, TimeUnit.SECONDS);
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

    private Battery() {
        logMethodEntranceExit(true);
        scheduler.setRemoveOnCancelPolicy(true);
        this.batteryParamsOld = new BatteryParams();
        this.batteryParams = new BatteryParams();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BatteryManager.ACTION_CHARGING);
        intentFilter.addAction(BatteryManager.ACTION_DISCHARGING);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        SotiAgentApplication.getAppContext().registerReceiver(this.batteryInfoReceiver, intentFilter);
        logMethodEntranceExit(false);
    }

    public static Battery getInstance() {
        return Battery.instance;
    }

    private void sendBatteryAlert(int batteryLevel) {
        logMethodEntranceExit(true);
        JSONObject jsonObject = new JSONObject();
        try {
                jsonObject.put("id", "LowBattery");
                jsonObject.put("description", "Battery charge is below 10%");
                jsonObject.put("type", "Warning");
                jsonObject.put("batteryLevel", batteryLevel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttHelper.getInstance().doAlert(jsonObject);
        logMethodEntranceExit(false);
    }

    private void doUpdateBatteryParams(BatteryParams batteryParams) {
        Intent batteryStatus = SotiAgentApplication.getAppContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        logMethodEntranceExit(true);
        try {
            int status = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : BatteryManager.BATTERY_STATUS_UNKNOWN;
            batteryParams.setBatteryStatus(this.batteryStatusToString(status));

            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            batteryParams.setCharging(isCharging);

            int chargePlug = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : -1;
            batteryParams.setChargeType(this.chargePlugToString(chargePlug));

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
            double batteryPct = 100D * (double) level / (double) scale;
            batteryParams.setBatteryPercent((int)batteryPct);
            if (batteryPct < 10D) this.sendBatteryAlert((int)batteryPct);

            int health = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) : BatteryManager.BATTERY_HEALTH_UNKNOWN;
            batteryParams.setBatteryHealth(this.batteryHealthToString(health));

            float voltage = (float)(batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) : -1);
            if (voltage > 1000)
                voltage /= 1000F;
            batteryParams.setVoltage(voltage);

            float temp = (float)(batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1) : -1) / 10F;
            batteryParams.setTemperature(temp);

            BatteryManager batteryManager = (BatteryManager) SotiAgentApplication.getAppContext().getSystemService(SotiAgentApplication.getAppContext().BATTERY_SERVICE);
            int cu =  batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            batteryParams.setCurrent(cu);
            int cu2 =  batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            batteryParams.setAverageCurrent(cu2);
        } catch (Exception e) {
            e.printStackTrace();
            logMethodEntranceExit(false, "Exception occured!");
        }
        logMethodEntranceExit(false);
    }

    private void updateBatteryAsync() {
        if (this.observationMaskMap.isEmpty()) return;
        if (this.waitingForMoreUpdates.get()) {
            this.signalNotificationArrived();
            return;
        }

        new Thread(){
            public void run(){
                Battery.this.batteryLock.lock();
                Battery.this.waitingForMoreUpdates.set(true);
                try {
                    int waitSomeMore = 10;
                    while(waitSomeMore-- > 0) {
                        if (!Battery.this.notificationArrived.await(100, TimeUnit.MILLISECONDS)) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Battery.this.waitingForMoreUpdates.set(false);
                    Battery.this.batteryLock.unlock();
                }
                if (Battery.this.batteryParams.equals(Battery.this.batteryParamsOld)) return;
                new Battery.UpdateBatteryRunnable(Battery.this.batteryParams, Battery.this.batteryParamsOld);
                Battery.this.batteryParamsOld = Battery.this.batteryParams;
                Battery.this.batteryParams = new BatteryParams(Battery.this.batteryParamsOld);
            }
        }.start();
    }

    private class UpdateBatteryRunnable implements Runnable {

        private final BatteryParams batteryParamsOld;
        private final BatteryParams batteryParams;

        public UpdateBatteryRunnable(BatteryParams batteryParams, BatteryParams batteryParamsOld) {
            this.batteryParamsOld = new BatteryParams(batteryParamsOld);
            this.batteryParams = new BatteryParams(batteryParams);
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            if (getInstance() == null) {
                logMethodEntranceExit(false, "instance == null");
                return;
            }
            try {
                if (Battery.this.autoUpdate.get()) {
                    Battery.this.observationMaskMap.forEach((key, value) -> {
                        int observationMask = Battery.this.observationMaskMap.get(key).intValue();
                        if (!this.batteryParams.equalsMasked(this.batteryParamsOld, observationMask))
                            this.sendBatteryMessage(key, observationMask);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
            logMethodEntranceExit(false);
        }

        private void sendBatteryMessage(String target, int observationMask) {
            if (MqttHelper.getInstance() == null) {
                logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
                return;
            }
            if (!MqttHelper.getInstance().isConnected()) {
                logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
                MqttHelper.getInstance().connect();
                return;
            }
            JSONObject batteryJsonObject = this.batteryParams.getJsonObject(this.batteryParamsOld, observationMask);
            if (batteryJsonObject.length() < 1) return;
            if (batteryJsonObject.length() == 1) { // Just a single value to notify!
                Iterator<String> keys = batteryJsonObject.keys();
                String key=keys.next();
                Object value = batteryJsonObject.opt(key);
                String messageId = key.substring(0, 1).toUpperCase() + key.substring(1);
                MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId, target, value);
            } else {
                MqttHelper.getInstance().doNotify(MQTT_SOURCE, target, batteryJsonObject);
            }
        }
    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logMethodEntranceExit(true);
            updateBatteryAsync();
            logMethodEntranceExit(false);
        }
    };

    private String batteryStatusToString(int batteryStatus) {
        switch(batteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "discharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "not charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "unknown";
        }
    }

    private String batteryHealthToString(int batteryHealth) {
        switch(batteryHealth) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "cold";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "dead";
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "good";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "overvoltage";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "overheat";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "unspecified failure";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            default:
                return "unknown";
        }
    }

    private String chargePlugToString(int chargePlug) {
        switch(chargePlug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "wireless";
            default:
                return "unknown";
        }
    }

    static {
        instance = new Battery();
    }
}
