package mis.casio.de.wsd_f20.soti.agent.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;

public class Bluetooth {

    public static class BluetoothParams {

        private CopyOnWriteArrayList<BluetoothDevice> connectedDevices;
        private CopyOnWriteArrayList<BluetoothDevice> pairedDevices;
        private final Object macAddressLock = new Object();
        private String macAddress;
        private final Object enabledLock = new Object();
//        private int enabled;
        private boolean enabled;
        private final Object globalLock = new Object();

        public BluetoothParams() {
            this.connectedDevices = new CopyOnWriteArrayList<>();
            this.pairedDevices = new CopyOnWriteArrayList<>();
            this.setDefaults();
        }

        public BluetoothParams(BluetoothParams bluetoothParams) {
            this.set(bluetoothParams);
        }

//        public BluetoothParams(int enabled, String macAddress, List<BluetoothDevice> connectedDevices) {
        public BluetoothParams(boolean enabled, String macAddress, List<BluetoothDevice> connectedDevices) {
            this.connectedDevices = new CopyOnWriteArrayList<>();
            this.pairedDevices = new CopyOnWriteArrayList<>();
            synchronized (globalLock) {
                this.enabled = enabled;
                this.macAddress = macAddress;
            }
            this.connectedDevices.clear();
            this.connectedDevices.addAll(connectedDevices);
        }

        public void setDefaults() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            synchronized (globalLock) {
//                this.enabled = bluetoothAdapter==null?-1:bluetoothAdapter.isEnabled()?1:0;
                this.enabled = bluetoothAdapter==null?false:bluetoothAdapter.isEnabled();
                this.macAddress = getMacAddr();
            }
            this.connectedDevices.clear();
            this.pairedDevices.clear();
            this.updatePairedDevices();
        }

//        public void set(int enabled, String macAddress, List<BluetoothDevice> connectedDevices) {
        public void set(boolean enabled, String macAddress, List<BluetoothDevice> connectedDevices) {
            this.connectedDevices = new CopyOnWriteArrayList<>();
            this.pairedDevices = new CopyOnWriteArrayList<>();
            synchronized (globalLock) {
                this.enabled = enabled;
                this.macAddress = macAddress;
            }
            this.connectedDevices.clear();
            this.connectedDevices.addAll(connectedDevices);
        }

//        public void set(int enabled, String macAddress) {
        public void set(boolean enabled, String macAddress) {
            synchronized (globalLock) {
                this.enabled = enabled;
                this.macAddress = macAddress;
            }
        }

        public void set(BluetoothParams bluetoothParams) {
            this.connectedDevices = new CopyOnWriteArrayList<>();
            this.pairedDevices = new CopyOnWriteArrayList<>();
            synchronized (globalLock) {
                this.enabled = bluetoothParams.enabled;
                this.macAddress = bluetoothParams.macAddress;
            }
            this.connectedDevices.clear();
            this.connectedDevices.addAll(bluetoothParams.connectedDevices);
            this.pairedDevices.clear();
            this.pairedDevices.addAll(bluetoothParams.pairedDevices);
        }

        public synchronized List<BluetoothDevice> getConnectedDevices() {
            return Collections.unmodifiableList(this.connectedDevices);
        }

        public synchronized List<BluetoothDevice> getPairedDevices() {
            return Collections.unmodifiableList(this.pairedDevices);
        }

        public synchronized void setConnectedDevices(List<BluetoothDevice> connectedDevices) {
            this.connectedDevices.clear();
            this.connectedDevices.addAll(connectedDevices);
        }

        public synchronized void setPairedDevices(List<BluetoothDevice> pairedDevices) {
            this.pairedDevices.clear();
            this.pairedDevices.addAll(pairedDevices);
        }

        public synchronized void addConnectedDevice(BluetoothDevice device) {
            if (this.connectedDevices.contains(device)) return;
            this.connectedDevices.add(device);
        }

        public synchronized void removeConnectedDevice(BluetoothDevice device) {
            if (!this.connectedDevices.contains(device)) return;
            this.connectedDevices.remove(device);
        }

        public synchronized void addPairedDevice(BluetoothDevice device) {
            if (this.pairedDevices.contains(device)) return;
            this.pairedDevices.add(device);
        }

        public synchronized void removePairedDevice(BluetoothDevice device) {
            if (!this.pairedDevices.contains(device)) return;
            this.pairedDevices.remove(device);
        }

        public synchronized void updateConnectedDevices(BluetoothManager bluetoothManager) {
            this.connectedDevices.clear();
            this.connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT));
            this.connectedDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER));
        }

        public synchronized void updatePairedDevices() {
            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            this.pairedDevices.clear();
            this.pairedDevices.addAll(pairedDevices);
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            synchronized (macAddressLock) {
                this.macAddress = macAddress;
            }
        }

//        public int getEnabled() {
//            return enabled;
//        }
//
//        public void setEnabled(int enabled) {
//            synchronized (enabledLock) {
//                this.enabled = enabled;
//            }
//        }

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            synchronized (enabledLock) {
                this.enabled = enabled;
            }
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    jsonObject.put("macAddress", this.macAddress);
//                    jsonObject.put("enabled", this.enabled == 0 ? "false" : this.enabled == 1 ? "true" : "unknown");
                    jsonObject.put("enabled", this.enabled);
                }
                Collection<JSONObject> connectedDevices = new ArrayList<JSONObject>();
                this.getConnectedDevices().forEach(connectedDevice -> {
                    JSONObject jsonObjectConnectedDevice = new JSONObject();
                    try {
                        jsonObjectConnectedDevice.put("name", connectedDevice.getName());
                        jsonObjectConnectedDevice.put("address", connectedDevice.getAddress());
                        ((ArrayList<JSONObject>) connectedDevices).add(jsonObjectConnectedDevice);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                if (!connectedDevices.isEmpty()) {
                    JSONArray connectedDevicesJsonArray = new JSONArray(connectedDevices);
                    jsonObject.put("connected", connectedDevicesJsonArray);
                }
                Collection<JSONObject> pairedDevices = new ArrayList<JSONObject>();
                this.getPairedDevices().forEach(pairedDevice -> {
                    JSONObject jsonObjectPairedDevice = new JSONObject();
                    try {
                        jsonObjectPairedDevice.put("name", pairedDevice.getName());
                        jsonObjectPairedDevice.put("address", pairedDevice.getAddress());
                        ((ArrayList<JSONObject>) pairedDevices).add(jsonObjectPairedDevice);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                if (!pairedDevices.isEmpty()) {
                    JSONArray pairedDevicesJsonArray = new JSONArray(pairedDevices);
                    jsonObject.put("paired", pairedDevicesJsonArray);
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
                    if ((mask & OBSERVE_ENABLED) != 0)
//                        jsonObject.put("enabled", this.enabled == 0 ? "false" : this.enabled == 1 ? "true" : "unknown");
                        jsonObject.put("enabled", this.enabled);
                }
                if ((mask & OBSERVE_CONNECTED) != 0) {
                    Collection<JSONObject> connectedDevices = new ArrayList<JSONObject>();
                    this.getConnectedDevices().forEach(connectedDevice -> {
                        JSONObject jsonObjectConnectedDevice = new JSONObject();
                        try {
                            jsonObjectConnectedDevice.put("name", connectedDevice.getName());
                            jsonObjectConnectedDevice.put("address", connectedDevice.getAddress());
                            ((ArrayList<JSONObject>) connectedDevices).add(jsonObjectConnectedDevice);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    if (!connectedDevices.isEmpty()) {
                        JSONArray connectedDevicesJsonArray = new JSONArray(connectedDevices);
                        jsonObject.put("connected", connectedDevicesJsonArray);
                    }
                }
                if ((mask & OBSERVE_PAIRED) != 0) {
                    Collection<JSONObject> pairedDevices = new ArrayList<JSONObject>();
                    this.getPairedDevices().forEach(pairedDevice -> {
                        JSONObject jsonObjectPairedDevice = new JSONObject();
                        try {
                            jsonObjectPairedDevice.put("name", pairedDevice.getName());
                            jsonObjectPairedDevice.put("address", pairedDevice.getAddress());
                            ((ArrayList<JSONObject>) pairedDevices).add(jsonObjectPairedDevice);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    if (!pairedDevices.isEmpty()) {
                        JSONArray pairedDevicesJsonArray = new JSONArray(pairedDevices);
                        jsonObject.put("paired", pairedDevicesJsonArray);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        public JSONObject getJsonObject(BluetoothParams bluetoothParamsOld, int mask) {
            JSONObject jsonObject = new JSONObject();
            try {
                synchronized (globalLock) {
                    if ((mask & OBSERVE_MACADDRESS) != 0 && !this.macAddress.equalsIgnoreCase(bluetoothParamsOld.macAddress))
                        jsonObject.put("macAddress", this.macAddress);
                    if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != bluetoothParamsOld.enabled)
                        jsonObject.put("enabled", this.enabled);
//                        jsonObject.put("enabled", this.enabled == 0 ? "false" : this.enabled == 1 ? "true" : "unknown");
                }
                if ((mask & OBSERVE_CONNECTED) != 0 && !isDevicesListsEqual(this.connectedDevices, bluetoothParamsOld.connectedDevices)) {
                    Collection<JSONObject> connectedDevices = new ArrayList<JSONObject>();
                    this.getConnectedDevices().forEach(connectedDevice -> {
                        JSONObject jsonObjectConnectedDevice = new JSONObject();
                        try {
                            jsonObjectConnectedDevice.put("name", connectedDevice.getName());
                            jsonObjectConnectedDevice.put("address", connectedDevice.getAddress());
                            ((ArrayList<JSONObject>) connectedDevices).add(jsonObjectConnectedDevice);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    if (!connectedDevices.isEmpty()) {
                        JSONArray connectedDevicesJsonArray = new JSONArray(connectedDevices);
                        jsonObject.put("connected", connectedDevicesJsonArray);
                    }
                }
                if ((mask & OBSERVE_PAIRED) != 0 && !isDevicesListsEqual(this.pairedDevices, bluetoothParamsOld.pairedDevices)) {
                    Collection<JSONObject> pairedDevices = new ArrayList<JSONObject>();
                    this.getPairedDevices().forEach(pairedDevice -> {
                        JSONObject jsonObjectPairedDevice = new JSONObject();
                        try {
                            jsonObjectPairedDevice.put("name", pairedDevice.getName());
                            jsonObjectPairedDevice.put("address", pairedDevice.getAddress());
                            ((ArrayList<JSONObject>) pairedDevices).add(jsonObjectPairedDevice);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    if (!pairedDevices.isEmpty()) {
                        JSONArray pairedDevicesJsonArray = new JSONArray(pairedDevices);
                        jsonObject.put("paired", pairedDevicesJsonArray);
                    }
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
            BluetoothParams bluetoothParams = (BluetoothParams) o;

            synchronized (globalLock) {
                if (this.enabled != bluetoothParams.enabled) return false;
                if (!this.macAddress.equalsIgnoreCase(bluetoothParams.macAddress)) return false;
            }
            if (!isDevicesListsEqual(this.connectedDevices, bluetoothParams.connectedDevices)) return false;
            if (!isDevicesListsEqual(this.pairedDevices, bluetoothParams.pairedDevices)) return false;
            return true;
        }

        private boolean isDevicesListsEqual(List<BluetoothDevice> list1, List<BluetoothDevice> list2) {
            if (list1.size() !=list2.size()) return false;
            for (BluetoothDevice bluetoothDevice : list1) {
                int otherBluetoothDeviceIndex = list2.indexOf(bluetoothDevice);
                if (otherBluetoothDeviceIndex < 0) return false;
                if (!list2.get(otherBluetoothDeviceIndex).equals(bluetoothDevice)) return false;
            }
            return true;
        }

        public boolean equalsMasked(BluetoothParams bluetoothParams, int mask) {
            synchronized (globalLock) {
                if ((mask & OBSERVE_MACADDRESS) != 0 && !this.macAddress.equalsIgnoreCase(bluetoothParams.macAddress))
                    return false;
                if ((mask & OBSERVE_ENABLED) != 0 && this.enabled != bluetoothParams.enabled)
                    return false;
            }
            if ((mask & OBSERVE_CONNECTED) != 0 && !isDevicesListsEqual(this.connectedDevices, bluetoothParams.connectedDevices)) return false;
            if ((mask & OBSERVE_PAIRED) != 0 && !isDevicesListsEqual(this.pairedDevices, bluetoothParams.pairedDevices)) return false;
            return true;
        }
    }

    public static final int OBSERVE_NONE = 0x0000;
    public static final int OBSERVE_ALL = 0x000F;
    public static final int OBSERVE_MACADDRESS = 0x0001;
    public static final int OBSERVE_ENABLED = 0x0002;
    public static final int OBSERVE_CONNECTED = 0x0004;
    public static final int OBSERVE_PAIRED = 0x0008;

    private static final String TAG = "MQTT (Bluetooth Sensor)";
    private static final String MQTT_SOURCE = "Bluetooth";

    private static final String MAC_NONE = "00:00:00:00:00:00";
    private static final int INITIAL_DELAY = 10;
    private static final int REFRESH_TIMEOUT = 10;
    private static Bluetooth instance;
    private BluetoothManager bluetoothManager;

    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture;

    private BluetoothParams bluetoothParamsOld;
    private BluetoothParams bluetoothParams;

    private final Lock bluetoothLock = new ReentrantLock();
    private final Condition notificationArrived  = bluetoothLock.newCondition();
    private AtomicBoolean waitingForMoreUpdates = new AtomicBoolean(false);

    private void signalNotificationArrived() {
        this.bluetoothLock.lock();
        this.notificationArrived.signalAll();
        this.bluetoothLock.unlock();
    }

    public BluetoothParams getBluetoothParams() {
        this.bluetoothParams.updatePairedDevices();
        return new BluetoothParams(this.bluetoothParams);
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
        if (observe | get) this.bluetoothParams.updatePairedDevices();
        switch (property) {
            case "":
                observationMask = observe?OBSERVE_ALL:get?observationMask:OBSERVE_NONE;
                if (observe) {
                    this.bluetoothParams.updatePairedDevices();
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.bluetoothParams.getJsonObject(OBSERVE_ALL));
                } else if (get) {
                    this.bluetoothParams.updatePairedDevices();
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.bluetoothParams.getJsonObject(OBSERVE_ALL));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBluetooth cancelled.");
                }
                break;
            case "MacAddress":
                if (observe) observationMask |= OBSERVE_MACADDRESS; else if (!get) observationMask &= ~OBSERVE_MACADDRESS;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, bluetoothParams.getMacAddress());
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, bluetoothParams.getMacAddress());
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBluetooth MacAddress cancelled.");
                }
                break;
            case "Enabled":
                if (observe) observationMask |= OBSERVE_ENABLED; else if (!get) observationMask &= ~OBSERVE_ENABLED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, bluetoothParams.enabled);
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, bluetoothParams.enabled);
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBluetooth Enabled State cancelled.");
                }
                break;
            case "Connected":
                if (observe) observationMask |= OBSERVE_CONNECTED; else if (!get) observationMask &= ~OBSERVE_CONNECTED;
                if (observe) {
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.bluetoothParams.getJsonObject(OBSERVE_CONNECTED));
                } else if (get) {
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.bluetoothParams.getJsonObject(OBSERVE_CONNECTED));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBluetooth Connected Devices cancelled.");
                }
                break;
            case "Paired":
                if (observe) observationMask |= OBSERVE_PAIRED; else if (!get) observationMask &= ~OBSERVE_PAIRED;
                if (observe) {
                    this.bluetoothParams.updatePairedDevices();
                    MqttHelper.getInstance().doNotifyDelayed(MQTT_SOURCE + property, originator, this.bluetoothParams.getJsonObject(OBSERVE_PAIRED));
                } else if (get) {
                    this.bluetoothParams.updatePairedDevices();
                    retVal = MqttHelper.getInstance().doRespond(MQTT_SOURCE + property, originator, transactionId, this.bluetoothParams.getJsonObject(OBSERVE_PAIRED));
                } else {
                    MqttHelper.getInstance().doRespond(MqttHelper.MQTT_SUCCESS, originator, transactionId, "ObservingBluetooth Paired Devices cancelled.");
                }
                break;
            default:
                MqttHelper.getInstance().doRespond(MqttHelper.MQTT_ERROR, originator, transactionId, "Unknown Bluetooth Property requested: " + property);
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

    public void autoUpdate(boolean enableAutoUpdate) {
        Log.d(TAG, "autoUpdate(" + enableAutoUpdate + ")");
        if (enableAutoUpdate) {
            try {
                if (this.autoUpdate.get()) return;
                if (scheduledFuture != null && !scheduledFuture.isCancelled()) scheduledFuture.cancel(true);
                Runnable updateBluetoothTask = () -> updateBluetoothAsync();
                scheduledFuture = scheduler.scheduleAtFixedRate(updateBluetoothTask, INITIAL_DELAY, REFRESH_TIMEOUT, TimeUnit.SECONDS);
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

    private Bluetooth() {
        logMethodEntranceExit(true);
        scheduler.setRemoveOnCancelPolicy(true);
        this.bluetoothParamsOld = new BluetoothParams();
        this.bluetoothParams = new BluetoothParams();
        this.bluetoothManager = (BluetoothManager) SotiAgentApplication.getAppContext().getSystemService(Context.BLUETOOTH_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        SotiAgentApplication.getAppContext().registerReceiver(broadcastReceiver, intentFilter);
        logMethodEntranceExit(false);
    }

    public static Bluetooth getInstance() {
        return Bluetooth.instance;
    }

    public static void enableBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            bluetoothAdapter.disable();
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logMethodEntranceExit(true);
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothAdapter.STATE_ON:
//                        Bluetooth.this.bluetoothParams.setEnabled(1);
                        Bluetooth.this.bluetoothParams.setEnabled(true);
                        break;
                    default:
//                        Bluetooth.this.bluetoothParams.setEnabled(0);
                        Bluetooth.this.bluetoothParams.setEnabled(false);
                        break;
                }
                Bluetooth.this.updateBluetoothAsync();
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    Bluetooth.this.bluetoothParams.addConnectedDevice(device);
                    Bluetooth.this.updateBluetoothAsync();
                }
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    Bluetooth.this.bluetoothParams.removeConnectedDevice(device);
                    Bluetooth.this.updateBluetoothAsync();
                }
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    Bluetooth.this.bluetoothParams.removeConnectedDevice(device);
                    Bluetooth.this.updateBluetoothAsync();
                }
            }
            logMethodEntranceExit(false);
        }
    };

    private void updateBluetoothAsync() {
        if (this.observationMaskMap.isEmpty()) return;
        if (this.waitingForMoreUpdates.get()) {
            this.signalNotificationArrived();
            return;
        }

        new Thread(){
            public void run(){
                Bluetooth.this.bluetoothLock.lock();
                Bluetooth.this.waitingForMoreUpdates.set(true);
                try {
                    int waitSomeMore = 10;
                    while(waitSomeMore-- > 0) {
                        if (!Bluetooth.this.notificationArrived.await(100, TimeUnit.MILLISECONDS)) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Bluetooth.this.waitingForMoreUpdates.set(false);
                    Bluetooth.this.bluetoothLock.unlock();
                }
                if (Bluetooth.this.bluetoothParams.equals(Bluetooth.this.bluetoothParamsOld)) return;
                new Bluetooth.UpdateBluetoothRunnable(Bluetooth.this.bluetoothParams, Bluetooth.this.bluetoothParamsOld);
                Bluetooth.this.bluetoothParamsOld = Bluetooth.this.bluetoothParams;
                Bluetooth.this.bluetoothParams = new BluetoothParams(Bluetooth.this.bluetoothParamsOld);
            }
        }.start();
    }

    private class UpdateBluetoothRunnable implements Runnable {

        private final BluetoothParams bluetoothParamsOld;
        private final BluetoothParams bluetoothParams;

        public UpdateBluetoothRunnable(BluetoothParams bluetoothParams, BluetoothParams bluetoothParamsOld) {
            this.bluetoothParamsOld = new BluetoothParams(bluetoothParamsOld);
            this.bluetoothParams = new BluetoothParams(bluetoothParams);
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            if (getInstance() == null) {
                logMethodEntranceExit(false, "instance == null");
                return;
            }
            try {
                if (Bluetooth.this.autoUpdate.get()) {
                    Bluetooth.this.observationMaskMap.forEach((key, value) -> {
                        int observationMask = Bluetooth.this.observationMaskMap.get(key).intValue();
                        if (!this.bluetoothParams.equalsMasked(this.bluetoothParamsOld, observationMask))
                            this.sendBluetoothMessage(key, observationMask);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logMethodEntranceExit(false, "Exception occured!");
            }
            logMethodEntranceExit(false);
        }

        private void sendBluetoothMessage(String target, int observationMask) {
            if (MqttHelper.getInstance() == null) {
                logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
                return;
            }
            if (!MqttHelper.getInstance().isConnected()) {
                logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
                MqttHelper.getInstance().connect();
                return;
            }
            JSONObject bluetoothJsonObject = this.bluetoothParams.getJsonObject(this.bluetoothParamsOld, observationMask);
            if (bluetoothJsonObject.length() < 1) return;
            if (bluetoothJsonObject.length() == 1) { // Just a single value to notify!
                Iterator<String> keys = bluetoothJsonObject.keys();
                String key=keys.next();
                Object value = bluetoothJsonObject.opt(key);
                String messageId = key.substring(0, 1).toUpperCase() + key.substring(1);
                MqttHelper.getInstance().doNotify(MQTT_SOURCE + messageId, target, value);
            } else {
                MqttHelper.getInstance().doNotify(MQTT_SOURCE, target, bluetoothJsonObject);
            }
        }
    }

    private static String getMacAddr() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = MAC_NONE;
        try {
            Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
            mServiceField.setAccessible(true);
            Object btManagerService = mServiceField.get(bluetoothAdapter);
            if (btManagerService != null) {
                bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

        }
        return bluetoothMacAddress;
    }

    static {
        instance = new Bluetooth();
    }
}
