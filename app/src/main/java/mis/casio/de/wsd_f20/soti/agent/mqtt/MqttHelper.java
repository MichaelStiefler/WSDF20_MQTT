package mis.casio.de.wsd_f20.soti.agent.mqtt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import mis.casio.de.wsd_f20.soti.agent.MainActivity;
import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.net.DownloadController;
import mis.casio.de.wsd_f20.soti.agent.sensors.Apps;
import mis.casio.de.wsd_f20.soti.agent.sensors.Battery;
import mis.casio.de.wsd_f20.soti.agent.sensors.Bluetooth;
import mis.casio.de.wsd_f20.soti.agent.sensors.Gps;
import mis.casio.de.wsd_f20.soti.agent.sensors.System;
import mis.casio.de.wsd_f20.soti.agent.sensors.Wifi;
import mis.casio.de.wsd_f20.soti.agent.service.MqttService;

public class MqttHelper {

    private class OutputMessage {
        private String topic;
        private String feedback;
        int qos;

        public OutputMessage(String topic, String feedback) {
            this(topic, feedback, 2);
        }

        public OutputMessage(String topic, String feedback, int qos) {
            this.topic = topic;
            this.feedback = feedback;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public int getQos() {
            return qos;
        }

        public void setQos(int qos) {
            this.qos = qos;
        }
    }

    private class OutputRunnable implements Runnable {
        private Queue<OutputMessage> queue;
        private MqttAndroidClient mqttAndroidClient;
        private long lastPendingConnectionAttempt = -RECONNECT_GRACE_PERIOD_MS;
        private static final long RECONNECT_GRACE_PERIOD_MS = 60000;

        public OutputRunnable(Queue<OutputMessage> queue, MqttAndroidClient mqttAndroidClient) {
            this.queue = queue;
            this.mqttAndroidClient = mqttAndroidClient;
            new Thread(this).start();
        }

        public void run() {
            logMethodEntranceExit(true);
            while (true) {
                try {
                    if (queue.isEmpty()) {
                        TimeUnit.MILLISECONDS.sleep(100);
                        continue;
                    }
                    if (!mqttAndroidClient.isConnected() && MqttService.LAUNCHER.isUpAndRunning()) {
                        if (MqttHelper.this.inConnection.get() && java.lang.System.currentTimeMillis() > this.lastPendingConnectionAttempt + RECONNECT_GRACE_PERIOD_MS) {
                            TimeUnit.MILLISECONDS.sleep(1000);
                            continue;
                        }
                        this.lastPendingConnectionAttempt = java.lang.System.currentTimeMillis();
                        MqttHelper.this.connect();
                        continue;
//                        try {
//                            mqttAndroidClient.connect();
//                        } catch (MqttException me) {
//                            me.printStackTrace();
//                        }
                    }
                    OutputMessage outputMessage = queue.poll();
                    MqttMessage feedbackMessage = new MqttMessage(outputMessage.getFeedback().getBytes());
                    feedbackMessage.setQos(outputMessage.getQos());
                    mqttAndroidClient.publish(outputMessage.getTopic(), feedbackMessage);
                } catch(InterruptedException e) {
                    e.printStackTrace();
//                    break;
                } catch (MqttPersistenceException e) {
                    e.printStackTrace();
                } catch (MqttException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            logMethodEntranceExit(false);
        }
    }

    public class PendingReply {
        public final static int REPLY_TYPE_UNKNOWN = 0;
        public final static int REPLY_TYPE_RESPONSE = 1;
        public final static int REPLY_TYPE_NOTIFICATION = 2;
        public final static int REPLY_TYPE_ALERT = 3;
        private String messageId;
        private String originator;
        private long transactionId;
        private Object value;
        private int replyType;

        public String getMessageId() {
            return messageId;
        }

        public void setReplyType(int replyType) {
            this.replyType = replyType;
        }

        public String getOriginator() {
            return originator;
        }

        public long getTransactionId() {
            return transactionId;
        }

        public Object getValue() {
            return value;
        }

        public int getReplyType() {
            return replyType;
        }

        public PendingReply(int replyType, String messageId, String originator, long transactionId, Object value) {
            this.replyType = replyType;
            this.messageId = messageId;
            this.originator = originator;
            this.transactionId = transactionId;
            this.value = value;
        }

        public PendingReply(String messageId, String originator, long transactionId, Object value) {
            this(REPLY_TYPE_UNKNOWN, messageId, originator, transactionId, value);
        }

        public PendingReply(int replyType, String messageId, String originator, Object value) {
            this(replyType, messageId, originator, TRANSACTION_ID_NONE, value);
        }

        public PendingReply(String messageId, String originator, Object value) {
            this(REPLY_TYPE_UNKNOWN, messageId, originator, TRANSACTION_ID_NONE, value);
        }

        public PendingReply(int replyType, Object value) {
            this(replyType, null, null, TRANSACTION_ID_NONE, value);
        }

        public PendingReply(Object value) {
            this(REPLY_TYPE_UNKNOWN, null, null, TRANSACTION_ID_NONE, value);
        }
    }

    private static MqttHelper instance;

    public static MqttHelper getInstance() {
        return MqttHelper.instance;
    }

    public MqttAndroidClient mqttAndroidClient;

//    final String serverUri = "tcp://m24.cloudmqtt.com:11734";
//    final String serverUri = "ssl://m24.cloudmqtt.com:21734";

    @SuppressLint("MissingPermission")
    public String getClientId() {
        if (this.clientId == null) this.clientId = android.os.Build.getSerial();
        return this.clientId;
    }

    private String clientId = null;

    public static final String MQTT_SUCCESS = "Success";
    public static final String MQTT_ERROR = "Error";
    public static final String MQTT_ALERT = "Alert";
    public static final String MQTT_NOTIFY = "Notify";
    public static final String MQTT_CONNECTED = "Connected";
    public static final String MQTT_DISCONNECTED = "Disconnected";
//    private static AtomicInteger specialTransactionId = new AtomicInteger(0);
    public final static String BASE_REQUEST_TOPIC = "SOTICONNECT/clients/request/json/";
    public final static String BASE_TOPIC_MESSAGE = "SOTICONNECT/clients/status/json/";
    public final static String BASE_RESPONSE_TOPIC = "SOTICONNECT/clients/response/json/";
    public final static String BASE_NOTIFY_TOPIC = "SOTICONNECT/clients/notify/json/";
    public final static String BASE_ALERT_TOPIC = "SOTICONNECT/clients/alerts/json/";
    public final static String BASE_ID_PREFIX = "Casio";
    public final static String[] NO_ID_PREFIX = {MQTT_SUCCESS, MQTT_ERROR, MQTT_CONNECTED, MQTT_DISCONNECTED};
    public final static int TRANSACTION_ID_NONE = -1;

    public String getSubscriptionTopic() {
        return subscriptionTopic;
    }

    public String getMessageTopic() {
        return messageTopic;
    }

    private String subscriptionTopic;
    private String messageTopic;
    private boolean hasPrefix = false;

//    final String username = "njhmkwxw";
//    final String password = "K5BItB4eaUpZ";
    private static final String TAG = "MQTT (MqttHelper)";

    private LocalBroadcastManager broadcastManager;

    private OutputRunnable outputRunnable = null;
    private Queue<OutputMessage> outputQueue = new ConcurrentLinkedQueue<>();
    private AtomicBoolean inConnection = new AtomicBoolean(false);
    private Queue<PendingReply> pendingRepliesQueue = new ConcurrentLinkedQueue<>();

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

//    public static int getSpecialTransactionId() {
//        int curId;
//        int newId;
//        do {
//            curId = specialTransactionId.get();
//            newId = curId < 0xFFFFFD ? curId + 1 : 1;
//        } while (!specialTransactionId.compareAndSet(curId, newId));
//        return (newId | 0x10000000) & 0xFFFFFFFF;
//    }

    public static String getResponseTopic(String target) {
        return BASE_RESPONSE_TOPIC + target + "/" + getInstance().getClientId();
    }

    public static String getNotifyTopic(String target) {
        return BASE_NOTIFY_TOPIC + target + "/" + getInstance().getClientId();
    }

    public static String getAlertTopic() {
        return BASE_ALERT_TOPIC + getInstance().getClientId();
    }

    MqttCallbackExtended mqttCallbackExtended = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean b, String s) {
            logMethodEntranceExit(true);
            onConnectStateMessage();
            Intent intent = new Intent(MainActivity.RECEIVE_CONNECTION_UPDATE);
            intent.putExtra("connected", true);
            broadcastManager.sendBroadcast(intent);
            logMethodEntranceExit(false);
        }

        @Override
        public void connectionLost(Throwable throwable) {
            logMethodEntranceExit(true);
            Intent intent = new Intent(MainActivity.RECEIVE_CONNECTION_UPDATE);
            intent.putExtra("connected", false);
            broadcastManager.sendBroadcast(intent);
            logMethodEntranceExit(false);
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            logMethodEntranceExit(true);
            Intent intent = new Intent(MainActivity.RECEIVE_RECEIVE_UPDATE);
            String extra = "Message received";
            try {
                if (mqttMessage != null) {
                    JSONObject jsonObject = new JSONObject(mqttMessage.toString());
                    extra = "R: " + jsonObject.getString("Id");
                }
            } catch (Exception e) {

            }
            intent.putExtra("transfer", extra);
            broadcastManager.sendBroadcast(intent);
            handleMqttMessage(mqttMessage);
            logMethodEntranceExit(false);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            logMethodEntranceExit(true);
            Intent intent = new Intent(MainActivity.RECEIVE_TRANSMIT_UPDATE);
            String extra = "Message sent";
            try {
                if (iMqttDeliveryToken.getMessage() != null) {
                    JSONObject jsonObject = new JSONObject(iMqttDeliveryToken.getMessage().toString());
                    extra = "T: " + jsonObject.getString("Id");
                }
            } catch (Exception e) {

            }
            intent.putExtra("transfer", extra);
            broadcastManager.sendBroadcast(intent);
            logMethodEntranceExit(false);
        }
    };

    private boolean handleMqttMessage(MqttMessage mqttMessage) {
        String from = null;
        String to = null;
        String messageId = "";
        long transactionId = 0;
        try {
            JSONObject jsonObject = new JSONObject(mqttMessage.toString());
            messageId = jsonObject.getString("Id");
            from = jsonObject.getString("From");
            to = jsonObject.getString("To");
            transactionId = jsonObject.getLong("TransactionId");
            if (!to.equalsIgnoreCase(this.clientId)) {
                this.doRespond(MQTT_ERROR, from, transactionId, "Id " + messageId + " received with wrong recipient!");
                return false;
            }

            // Updated 2019-11-06 on request by SOTI / Vadim: Prefix messages with "Casio" except for Error, Success, Connected, Disconnected.
            // +++
//            if (!messageId.startsWith(BASE_ID_PREFIX)) {
//                this.doRespond(MQTT_ERROR, from, transactionId, "Missing Casio Message Id Prefix!");
//                return false;
//            }
//            messageId = messageId.substring(BASE_ID_PREFIX.length());

            // Updated 2020-01-23 / make Prefix optional!
            if (messageId.startsWith(BASE_ID_PREFIX)) {
                messageId = messageId.substring(BASE_ID_PREFIX.length());
                hasPrefix = true;
            }
            // ---

            if (messageId.startsWith("Get")) return handleMqttGetMessage(messageId, from, transactionId);
            else if (messageId.startsWith("Observe")) {
                boolean retVal = handleMqttObserveMessage(messageId, from, transactionId);
                SotiAgentApplication.saveSettings();
                return retVal;
            }
            else if (messageId.startsWith("CancelObserve")) {
                boolean retVal = handleMqttCancelObserveMessage(messageId, from, transactionId);
                SotiAgentApplication.saveSettings();
                return retVal;
            }
            else if (messageId.equals("ChangeSettings")) return handleMqttChangeSettingsMessage(jsonObject.getJSONObject("Resource"), from, transactionId);
            else if (messageId.startsWith("SysCommand")) return handleMqttSysCommandMessage(messageId, from, transactionId);
            else if (messageId.startsWith("Install")) return handleMqttInstallMessage(messageId, from, transactionId, mqttMessage);
            else {
                this.doRespond(MQTT_ERROR, from, transactionId, "Id " + messageId + " unknown!");
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (from == null) {
                if (messageId.length() < 1) {
                    this.doAlert("Unparsable message received without originator!");
                } else {
                    this.doAlert("Message: " + messageId + " received without originator!");
                }
            } else {
//                if (transactionId == 0) {
//                    this.doRespond(MQTT_ERROR, from, MqttHelper.getSpecialTransactionId(), "Message: " + messageId + " received without transactionId!");
//                } else {
                    if (to == null) {
                        this.doRespond(MQTT_ERROR, from, transactionId, "Message: " + messageId + " received without recipient!");
                    } else {
                        this.doRespond(MQTT_ERROR, from, transactionId, "Error parsing message: " + messageId);
                    }
//                }
            }
            return false;
        }
    }

    private boolean handleMqttGetMessage(String messageId, String from, long transactionId) {
        messageId = messageId.substring(3);
        if (messageId.startsWith("Device")) return handleMqttGetDeviceMessage(from, transactionId);
        else if (messageId.startsWith("Wifi")) return handleMqttWifiMessage(messageId.substring(4), from, transactionId, true, false);
        else if (messageId.startsWith("Bluetooth")) return handleMqttBluetoothMessage(messageId.substring(9), from, transactionId, true, false);
        else if (messageId.startsWith("System")) return handleMqttSystemMessage(messageId.substring(6), from, transactionId, true, false);
        else if (messageId.startsWith("Battery")) return handleMqttBatteryMessage(messageId.substring(7), from, transactionId, true, false);
        else if (messageId.startsWith("Gps")) return handleMqttGpsMessage(messageId.substring(3), from, transactionId, true, false);
        else if (messageId.startsWith("Apps")) return handleMqttAppsMessage(messageId.substring(4), from, transactionId, true, false);
        else {
            this.doRespond(MQTT_ERROR, from, transactionId, "Message: " + messageId + " contains unknown sensor!");
            return false;
        }
    }

    private boolean handleMqttObserveMessage(String messageId, String from, long transactionId) {
        messageId = messageId.substring(7);
        boolean retVal;
        if (messageId.startsWith("Device")) {
            retVal = handleMqttSystemMessage("", from, TRANSACTION_ID_NONE, false, true);
            retVal &= handleMqttBatteryMessage("", from, TRANSACTION_ID_NONE, false, true);
            retVal &= handleMqttWifiMessage("", from, TRANSACTION_ID_NONE, false, true);
            retVal &= handleMqttBluetoothMessage("", from, TRANSACTION_ID_NONE, false, true);
            retVal &= handleMqttGpsMessage("", from, TRANSACTION_ID_NONE, false, true);
            retVal &= handleMqttAppsMessage("", from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveDevice " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("Wifi")) {
            retVal=handleMqttWifiMessage(messageId.substring(4), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveWifi " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("Bluetooth")) {
            retVal=handleMqttBluetoothMessage(messageId.substring(9), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveBluetooth " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("System")) {
            retVal=handleMqttSystemMessage(messageId.substring(6), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveSystem " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("Battery")) {
            retVal=handleMqttBatteryMessage(messageId.substring(7), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveBattery " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("Gps")) {
            retVal=handleMqttGpsMessage(messageId.substring(3), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveGps " + (retVal?"succeeded.":"failed."));
        } else if (messageId.startsWith("Apps")) {
            retVal=handleMqttAppsMessage(messageId.substring(4), from, TRANSACTION_ID_NONE, false, true);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "ObserveApps " + (retVal?"succeeded.":"failed."));
        } else {
            this.doRespond(MQTT_ERROR, from, transactionId, "Message: " + messageId + " contains unknown sensor!");
            return false;
        }

        while (!this.outputQueue.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.sendPendingReplies();
        return retVal;

    }

    private boolean handleMqttCancelObserveMessage(String messageId, String from, long transactionId) {
        messageId = messageId.substring(13);
        if (messageId.startsWith("Device")) {
            boolean retVal = handleMqttSystemMessage("", from, TRANSACTION_ID_NONE, false, false);
            retVal &= handleMqttBatteryMessage("", from, TRANSACTION_ID_NONE, false, false);
            retVal &= handleMqttWifiMessage("", from, TRANSACTION_ID_NONE, false, false);
            retVal &= handleMqttBluetoothMessage("", from, TRANSACTION_ID_NONE, false, false);
            retVal &= handleMqttGpsMessage("", from, TRANSACTION_ID_NONE, false, false);
            retVal &= handleMqttAppsMessage("", from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveDevice " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("Wifi")) {
            boolean retVal= handleMqttWifiMessage(messageId.substring(4), from, transactionId, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveWifi " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("Bluetooth")) {
            boolean retVal= handleMqttBluetoothMessage(messageId.substring(9), from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveBluetooth " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("System")) {
            boolean retVal= handleMqttSystemMessage(messageId.substring(6), from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveSystem " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("Battery")) {
            boolean retVal= handleMqttBatteryMessage(messageId.substring(7), from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveBattery " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("Gps")) {
            boolean retVal= handleMqttGpsMessage(messageId.substring(3), from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveGps " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else if (messageId.startsWith("Apps")) {
            boolean retVal= handleMqttAppsMessage(messageId.substring(4), from, TRANSACTION_ID_NONE, false, false);
            this.doRespond(retVal?MQTT_SUCCESS:MQTT_ERROR, from, transactionId, "CancelObserveApps " + (retVal?"succeeded.":"failed."));
            return retVal;
        }
        else {
            this.doRespond(MQTT_ERROR, from, transactionId, "Message: " + messageId + " contains unknown sensor!");
            return false;
        }
    }

    private boolean handleMqttGetDeviceMessage(String from, long transactionId) {
        System.SystemParams systemParams = System.getInstance().getSystemParams();
        Battery.BatteryParams batteryParams = Battery.getInstance().getBatteryParams();
        Wifi.WifiParams wifiParams = Wifi.getInstance().getWifiParams();
        Bluetooth.BluetoothParams bluetoothParams = Bluetooth.getInstance().getBluetoothParams();
        Gps.GpsParams gpsParams = Gps.getInstance().getGpsParams();
        Apps.AppParams appParams = Apps.getInstance().getAppParams();

        try {
            JSONObject deviceResourceJsonObject = new JSONObject();
            JSONObject deviceJsonObject = new JSONObject();
            deviceJsonObject.put("system", systemParams.getJsonObject());
            deviceJsonObject.put("battery", batteryParams.getJsonObject());
            deviceJsonObject.put("wifi", wifiParams.getJsonObject());
            deviceJsonObject.put("bluetooth", bluetoothParams.getJsonObject());
            deviceJsonObject.put("gps", gpsParams.getJsonObject());
            deviceJsonObject.put("apps", appParams.getJsonArray());
            deviceResourceJsonObject.put("device", deviceJsonObject);
            return this.doRespond("Device", from, transactionId, deviceResourceJsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean handleMqttChangeSettingsMessage(JSONObject resource, String from, long transactionId) {
        Iterable<String> iterableKeys = () -> resource.keys();
        StreamSupport.stream(iterableKeys.spliterator(), true).forEach(key -> {
            boolean isImplemented = false;
            switch (key) {
                case "device":
                    isImplemented = true;
                    try {
                        handleMqttChangeSettingsMessage(resource.getJSONObject("device"), from, transactionId);
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings (" + key + ") failed.");
                    break;
                case "wifi":
                    isImplemented = true;
                    try {
                        handleMqttChangeSettingsWifiMessage(resource.getJSONObject("wifi"), from, transactionId);
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings (" + key + ") failed.");
                    break;
                case "bluetooth":
                    isImplemented = true;
                    try {
                        handleMqttChangeSettingsBluetoothMessage(resource.getJSONObject("bluetooth"), from, transactionId);
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings (" + key + ") failed.");
                    break;
                case "gps":
                    break;
                case "apps":
                    break;
                case "system":
                    break;
                case "battery":
                    break;
            }
            if (!isImplemented) this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings (" + key + ") is not implemented yet!");
        });
        return true;
    }

    private boolean handleMqttChangeSettingsWifiMessage(JSONObject resource, String from, long transactionId) {
        Iterable<String> iterableKeys = () -> resource.keys();
        StreamSupport.stream(iterableKeys.spliterator(), true).forEach(key -> {
            boolean isImplemented = false;
            switch (key) {
                case "enabled":
                    isImplemented = true;
                    try {
                        Wifi.enableWifi(resource.getBoolean(key));
                        this.doRespond(MQTT_SUCCESS, from, transactionId, "ChangeSettings Wifi success.");
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings Wifi (" + key + ") failed.");
                    break;
            }
            if (!isImplemented) this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings Wifi (" + key + ") is not implemented yet!");
        });
        return true;
    }

    private boolean handleMqttChangeSettingsBluetoothMessage(JSONObject resource, String from, long transactionId) {
        Iterable<String> iterableKeys = () -> resource.keys();
        StreamSupport.stream(iterableKeys.spliterator(), true).forEach(key -> {
            boolean isImplemented = false;
            switch (key) {
                case "enabled":
                    isImplemented = true;
                    try {
                        Bluetooth.enableBluetooth(resource.getBoolean(key));
                        this.doRespond(MQTT_SUCCESS, from, transactionId, "ChangeSettings Bluetooth success.");
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings Bluetooth (" + key + ") failed.");
                    break;
            }
            if (!isImplemented) this.doRespond(MQTT_ERROR, from, transactionId, "ChangeSettings Bluetooth (" + key + ") is not implemented yet!");
        });
        return true;
    }

    private boolean handleMqttInstallMessage(String messageId, String from, long transactionId, MqttMessage mqttMessage) {
        if (messageId.equals("InstallUrl")) {
            return this.handleMqttInstallUrlMessage(from, transactionId, mqttMessage);
        } else if (messageId.equals("InstallInline")) {
            return this.handleMqttInstallInlineMessage(from, transactionId, mqttMessage);
        } else {
            this.doRespond(MQTT_ERROR, from, transactionId, "Id " + messageId + " unknown!");
            return false;
        }
    }

    private boolean handleMqttInstallUrlMessage(String from, long transactionId, MqttMessage mqttMessage) {
        try {
            JSONObject jsonMessageResource = new JSONObject(mqttMessage.toString()).getJSONObject("Resource");
            String url = jsonMessageResource.getString("Url");
            String contentType = jsonMessageResource.optString("ContentType", "application/vnd.android.package-archive");
            String base64SHA256 = jsonMessageResource.optString("Hash", null);
            new DownloadController(url, from, transactionId, contentType, base64SHA256);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.doRespond(MQTT_ERROR, from, transactionId, "InstallUrl is not implemented yet!");
        return false;
    }

    private boolean handleMqttInstallInlineMessage(String from, long transactionId, MqttMessage mqttMessage) {
        this.doRespond(MQTT_ERROR, from, transactionId, "InstallInline is not implemented yet!");
        return false;
    }

    private boolean handleMqttSysCommandMessage(String messageId, String from, long transactionId) {
        this.doRespond(MQTT_ERROR, from, transactionId, "SysCommand is not implemented yet!");
        return false;
    }

    private boolean handleMqttWifiMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return Wifi.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private boolean handleMqttBluetoothMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return Bluetooth.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private boolean handleMqttSystemMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return System.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private boolean handleMqttBatteryMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return Battery.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private boolean handleMqttGpsMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return Gps.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private boolean handleMqttAppsMessage(String messageId, String from, long transactionId, boolean get, boolean observe) {
        return Apps.getInstance().requestData(messageId, from, get, observe, transactionId);
    }

    private void onConnectStateMessage() {
        logMethodEntranceExit(true);
        if (this.outputRunnable == null) this.outputRunnable = new OutputRunnable(this.outputQueue, this.mqttAndroidClient);
        try {
            JSONObject jsonMessageObject = new JSONObject();
            JSONObject jsonMessageObjectResource = new JSONObject();
            jsonMessageObjectResource.put("Type", "CASIO WSD-F20");
            jsonMessageObjectResource.put("Id", this.getClientId());
            jsonMessageObject.put("Id", MQTT_CONNECTED);
            jsonMessageObject.put("From", this.getClientId());
            jsonMessageObject.put("Resource", jsonMessageObjectResource);
            sendFeedback(messageTopic, jsonMessageObject.toString(), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
    }

    private void disconnectStateMessage() {
        logMethodEntranceExit(true);
        try {
            JSONObject jsonMessageObject = new JSONObject();
            jsonMessageObject.put("Id", MQTT_DISCONNECTED);
            jsonMessageObject.put("From", this.getClientId());

            MqttMessage feedbackMessage = new MqttMessage(jsonMessageObject.toString().getBytes());
            feedbackMessage.setQos(1);
            mqttAndroidClient.publish(messageTopic, feedbackMessage);

//            sendFeedback(messageTopic, jsonMessageObject.toString(), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
    }

    private void initMqttHelper() {
        broadcastManager = LocalBroadcastManager.getInstance(SotiAgentApplication.getAppContext());
//        String serverUri = (SotiAgentApplication.isUseTLS()?"ssl://":"tcp://") + SotiAgentApplication.getServerAddress() + ":" + SotiAgentApplication.getServerPort();
//        StringBuilder sbServerUri = new StringBuilder();
//        if (SotiAgentApplication.isUseTLS()) {
//            sbServerUri.append("ssl://");
//        } else {
//            sbServerUri.append("tcp://");
//        }
//        sbServerUri.append(SotiAgentApplication.getServerAddress());
//        sbServerUri.append(":");
//        sbServerUri.append(SotiAgentApplication.getServerPort());
//
//        String serverUri = sbServerUri.toString();

        mqttAndroidClient = new MqttAndroidClient(
                SotiAgentApplication.getAppContext(),
                this.getServerUri(),
                this.getClientId());
//        mqttAndroidClient = new MqttAndroidClient(SotiAgentApplication.getAppContext(), serverUri, this.getClientId());
        mqttAndroidClient.setCallback(mqttCallbackExtended);
        doConnect();
    }

    @SuppressLint("MissingPermission")
    public MqttHelper() {
        logMethodEntranceExit(true);
        instance = this;
        subscriptionTopic = BASE_REQUEST_TOPIC + this.getClientId();
        messageTopic = BASE_TOPIC_MESSAGE + this.getClientId();
        this.initMqttHelper();
        logMethodEntranceExit(false);
    }

    public boolean isConnected() {
        return this.mqttAndroidClient != null && this.mqttAndroidClient.isConnected();
    }

    public void connect(){
        this.doConnect();
    }

    private String getServerUri() {
        StringBuilder sbServerUri = new StringBuilder();
        if (SotiAgentApplication.isUseTLS()) {
            sbServerUri.append("ssl://");
        } else {
            sbServerUri.append("tcp://");
        }
        sbServerUri.append(SotiAgentApplication.getServerAddress());
        sbServerUri.append(":");
        sbServerUri.append(SotiAgentApplication.getServerPort());

        return sbServerUri.toString();
    }

    private void doConnect(){
        logMethodEntranceExit(true);
        if (this.inConnection.get()) {
            logMethodEntranceExit(false);
            return;
        }
        this.inConnection.set(true);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
//        mqttConnectOptions.setUserName(username);
//        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setUserName(SotiAgentApplication.getUsername());
        mqttConnectOptions.setPassword(SotiAgentApplication.getPassword().toCharArray());

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws
                            CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws
                            CertificateException {
                    }
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        SSLContext sslContext = null;
        SSLSocketFactory sslSocketFactory;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        sslSocketFactory = sslContext.getSocketFactory();

        if (SotiAgentApplication.isUseTLS())
//            mqttConnectOptions.setSocketFactory(SSLSocketFactory.getDefault());
            mqttConnectOptions.setSocketFactory(sslSocketFactory);
        else
            mqttConnectOptions.setSocketFactory(SocketFactory.getDefault());

        Log.v(TAG, "URL: " + this.getServerUri());
//        Log.w(TAG, "User: " + mqttConnectOptions.getUserName());
//        Log.w(TAG, "Password: " + mqttConnectOptions.getPassword());

        if (!this.getServerUri().equalsIgnoreCase(mqttAndroidClient.getServerURI())) {
            this.disconnect();
            mqttAndroidClient = new MqttAndroidClient(
                    SotiAgentApplication.getAppContext(),
                    this.getServerUri(),
                    this.getClientId());
            mqttAndroidClient.setCallback(mqttCallbackExtended);
        }

        JSONObject jsonMessageObject = new JSONObject();
        try {
            jsonMessageObject.put("Id", MQTT_DISCONNECTED);
            jsonMessageObject.put("From", this.getClientId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mqttConnectOptions.setWill(this.messageTopic, jsonMessageObject.toString().getBytes(), 1, false);

        try {

//            logMethodEntranceExit(true, "calling mqttAndroidClient.connect() now...");
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    MqttHelper.this.inConnection.set(false);
                    logMethodEntranceExit(true);
                    try {
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        Log.i(TAG, "Connected to: " + mqttAndroidClient.getServerURI());
                        subscribeToTopic();
                    } catch (Exception e) {
                        logMethodEntranceExit(false, "Exception occured in onSuccess");
                        e.printStackTrace();
                        return;
                    }
                    logMethodEntranceExit(false);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    MqttHelper.this.inConnection.set(false);
                    logMethodEntranceExit(true);
                    Log.w(TAG, "Failed to connect to: " + mqttAndroidClient.getServerURI() + ", Exception: " + (exception == null?"":exception.toString()));
                    if (exception != null) exception.printStackTrace();

                    // new 2020-17-03: If connection fails, retry after one second.
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MqttHelper.this.connect();
                        }
                    }, 1000);

                    logMethodEntranceExit(false);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
            this.inConnection.set(false);
            // this.initMqttHelper(); // removed 2020-17-03, see below.
            // new 2020-17-03: If connection fails, retry after one second.
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    MqttHelper.this.initMqttHelper();
                }
            }, 1000);
        } catch (Exception e) {
            e.printStackTrace();
            this.inConnection.set(false);
            // this.initMqttHelper(); // removed 2020-17-03, see below.
            // new 2020-17-03: If connection fails, retry after one second.
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    MqttHelper.this.initMqttHelper();
                }
            }, 1000);
        }
        logMethodEntranceExit(false);
    }


    private void subscribeToTopic() {
        logMethodEntranceExit(true);
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logMethodEntranceExit(true);
                    logMethodEntranceExit(false);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logMethodEntranceExit(true);
                    logMethodEntranceExit(false);
                }
            });

        } catch (MqttException ex) {
            java.lang.System.err.println("subscribeToTopic Exception");
            ex.printStackTrace();
        }
        logMethodEntranceExit(false);
    }

    public void disconnect() {
        logMethodEntranceExit(true);
        try {
            if (mqttAndroidClient.isConnected()) {
                try {
                    disconnectStateMessage();
                    mqttAndroidClient.disconnect();
                } catch (MqttException me) {
                    me.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
    }

    public boolean sendFeedback(String topic, String feedback) {
        return sendFeedback(topic, feedback, 2);
    }

    public boolean sendFeedback(String topic, String feedback, int qos) {
        logMethodEntranceExit(true);
        if (mqttAndroidClient == null) {
            logMethodEntranceExit(false, "MQTT client not initialized");
            return false;
        }
        this.outputQueue.offer(new OutputMessage(topic, feedback, qos));
        logMethodEntranceExit(false);
        return true;
    }

    private boolean sendJson (JSONObject jsonObject, String topic) {
        return sendJson(jsonObject, topic, 2);
    }

    private boolean sendJson (JSONObject jsonObject, String topic, int qos) {
        logMethodEntranceExit(true);
        if (MqttHelper.getInstance() == null) {
            logMethodEntranceExit(false, "MqttHelper.getInstance() == null");
            return false;
        }
        if (!MqttHelper.getInstance().mqttAndroidClient.isConnected()) {
            logMethodEntranceExit(false, "MqttHelper.getInstance().mqttAndroidClient.isConnected() is false!");
            return false;
        }
        logMethodEntranceExit(false);
        return MqttHelper.getInstance().sendFeedback(topic, jsonObject.toString(), qos);
    }

    private boolean respondJson (JSONObject jsonObject, String originator) {
        return sendJson(jsonObject, MqttHelper.getResponseTopic(originator), 2);
    }

    private boolean notifyJson (JSONObject jsonObject, String originator) {
        return sendJson(jsonObject, MqttHelper.getNotifyTopic(originator), 0);
    }

    private boolean alertJson (JSONObject jsonObject) {
        return sendJson(jsonObject, MqttHelper.getAlertTopic(), 1);
    }

    // Updated 2019-11-06 on request by SOTI / Vadim: Prefix messages with "Casio" except for Error, Success, Connected, Disconnected.
    // +++
    private String prefixId(String id) {
        if (!this.hasPrefix) return id; // FIXME
        if (Arrays.stream(NO_ID_PREFIX).anyMatch(id::equals)) return id;
        return BASE_ID_PREFIX+id;
    }
    // ---


    private JSONObject baseJsonObject(String originator, long transactionId, String messageId) {
        logMethodEntranceExit(true);
        JSONObject jsonObject = new JSONObject();
        try {
            // Updated 2019-11-06 on request by SOTI / Vadim: Prefix messages with "Casio" except for Error, Success, Connected, Disconnected.
            // +++
//            jsonObject.put("Id", messageId);
            jsonObject.put("Id", this.prefixId(messageId));
            // ---
            jsonObject.put("From", MqttHelper.getInstance().getClientId());
            if (originator!=null) jsonObject.put("To", originator);
            if (transactionId != -1) jsonObject.put("TransactionId", transactionId);
//            jsonObject.put("timestamp", Instant.now().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
        return jsonObject;
    }

    private JSONObject baseResponseJsonObject(String originator, long transactionId, String messageId) {
        return baseJsonObject(originator, transactionId, messageId);
    }

    private JSONObject baseNotifyJsonObject(String originator, long transactionId, String messageId) {
        return baseJsonObject(originator, transactionId, MQTT_NOTIFY+messageId);
    }

    private JSONObject baseAlertJsonObject() {
//        return baseJsonObject(null, MqttHelper.getSpecialTransactionId(), MQTT_ALERT);
        return baseJsonObject(null, -1L, MQTT_ALERT);
    }

    public boolean doRespond(String messageId, String originator, long transactionId, Object value) {
        logMethodEntranceExit(true);
        JSONObject rootJsonObject = baseResponseJsonObject(originator, transactionId, messageId);
        try {
            if (value instanceof JSONObject) {
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value instanceof  String) {
                String tempString = new String((String)value);
                value = new JSONObject();
                ((JSONObject) value).put("description", tempString);
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value == null) {
                value = new JSONObject();
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            }
            rootJsonObject.put("Resource", value);
            logMethodEntranceExit(false);
            return respondJson(rootJsonObject, originator);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
        return false;
    }

    public boolean doNotify(String messageId, String originator, Object value) {
        logMethodEntranceExit(true);
//        int transactionId = MqttHelper.getSpecialTransactionId();
//        JSONObject rootJsonObject = baseNotifyJsonObject(originator, transactionId, messageId);
        JSONObject rootJsonObject = baseNotifyJsonObject(originator, -1L, messageId);
        try {
            if (value instanceof JSONObject) {
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value instanceof  String) {
                String tempString = new String((String)value);
                value = new JSONObject();
                ((JSONObject) value).put("description", tempString);
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value == null) {
                value = new JSONObject();
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            }
            rootJsonObject.put("Resource", value);
            logMethodEntranceExit(false);
            return notifyJson(rootJsonObject, originator);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
        return false;
    }

    public boolean doAlert(Object value) {
        logMethodEntranceExit(true);
//        int transactionId = MqttHelper.getSpecialTransactionId();
        JSONObject rootJsonObject = baseAlertJsonObject();
        try {
            if (value instanceof JSONObject) {
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value instanceof  String) {
                String tempString = new String((String)value);
                value = new JSONObject();
                ((JSONObject) value).put("description", tempString);
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            } else if (value == null) {
                value = new JSONObject();
                ((JSONObject) value).put("timestamp", Instant.now().toString());
            }
            rootJsonObject.put("Resource", value);
            logMethodEntranceExit(false);
            return alertJson(rootJsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        logMethodEntranceExit(false);
        return false;
    }

    public void doRespondDelayed(String messageId, String originator, long transactionId, Object value) {
        logMethodEntranceExit(true);
        this.pendingRepliesQueue.offer(new PendingReply(PendingReply.REPLY_TYPE_RESPONSE, messageId, originator, transactionId, value));
        logMethodEntranceExit(false);
    }

    public void doNotifyDelayed(String messageId, String originator, Object value) {
        logMethodEntranceExit(true);
        this.pendingRepliesQueue.offer(new PendingReply(PendingReply.REPLY_TYPE_NOTIFICATION, messageId, originator, value));
        logMethodEntranceExit(false);
    }

    public void doAlertDelayed(Object value) {
        logMethodEntranceExit(true);
        this.pendingRepliesQueue.offer(new PendingReply(PendingReply.REPLY_TYPE_ALERT, value));
        logMethodEntranceExit(false);
    }

    public void sendPendingReplies() {
        while (!this.pendingRepliesQueue.isEmpty()) {
            PendingReply pendingReply = this.pendingRepliesQueue.poll();
            if (pendingReply.replyType == PendingReply.REPLY_TYPE_UNKNOWN) {
                if (pendingReply.getTransactionId() != TRANSACTION_ID_NONE)
                    pendingReply.setReplyType(PendingReply.REPLY_TYPE_RESPONSE);
                else if (pendingReply.getMessageId() != null && pendingReply.getOriginator() != null)
                    pendingReply.setReplyType(PendingReply.REPLY_TYPE_NOTIFICATION);
                else
                    pendingReply.setReplyType(PendingReply.REPLY_TYPE_ALERT);
            }
            switch (pendingReply.getReplyType()) {
                case PendingReply.REPLY_TYPE_RESPONSE:
                    this.doRespond(pendingReply.getMessageId(), pendingReply.getOriginator(), pendingReply.getTransactionId(), pendingReply.getValue());
                    break;
                case PendingReply.REPLY_TYPE_NOTIFICATION:
                    this.doNotify(pendingReply.getMessageId(), pendingReply.getOriginator(), pendingReply.getValue());
                    break;
                case PendingReply.REPLY_TYPE_ALERT:
                    this.doAlert(pendingReply.getValue());
                default:
                    break;
            }
        }
    }

    static {
        instance = new MqttHelper();
    }
}
