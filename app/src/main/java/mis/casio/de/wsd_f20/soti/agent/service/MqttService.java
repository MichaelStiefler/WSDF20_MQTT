package mis.casio.de.wsd_f20.soti.agent.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;

import mis.casio.de.wsd_f20.soti.agent.MainActivity;
import mis.casio.de.wsd_f20.soti.agent.R;
import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;
import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;
import mis.casio.de.wsd_f20.soti.agent.sensors.Apps;
import mis.casio.de.wsd_f20.soti.agent.sensors.Battery;
import mis.casio.de.wsd_f20.soti.agent.sensors.Bluetooth;
import mis.casio.de.wsd_f20.soti.agent.sensors.Gps;
import mis.casio.de.wsd_f20.soti.agent.sensors.System;
import mis.casio.de.wsd_f20.soti.agent.sensors.Wifi;

public class MqttService extends Service {

    private static final String TAG = "MQTT (MqttService)";

//    private Toast mToast = null;
//    Handler mainHandler = new Handler(getMainLooper());
    Handler mainHandler = new Handler();
    private LocalBroadcastManager broadcastManager;

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

    public static MqttServiceLauncher LAUNCHER = new MqttServiceLauncher(MqttService.class);

    public static boolean start(Context context) {
        logMethodEntranceExit(true);
        logMethodEntranceExit(false);
        return LAUNCHER.startService(context);
    }

    public static boolean stop(Context context) {
        logMethodEntranceExit(true);
        logMethodEntranceExit(false);
        return LAUNCHER.stopService(context);
    }

//    private static File storagePath;

    public MqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        logMethodEntranceExit(true);
        super.onCreate();

//        storagePath = getExternalFilesDir(null);
//        storagePath.mkdir();

        broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        logMethodEntranceExit(false);
    }

    @Override
    public void onDestroy() {
        logMethodEntranceExit(true);
        this.stopMqtt();
        Wifi.stopAutoUpdate();
        Bluetooth.stopAutoUpdate();
        System.stopAutoUpdate();
        Battery.stopAutoUpdate();
        Gps.stopAutoUpdate();
        Apps.stopAutoUpdate();

//        if (mToast == null)
//            mToast = Toast.makeText(this, "MQTT Service Stopped", Toast.LENGTH_SHORT);
//        else
//            mToast.setText("MQTT Service Stopped");
//        mToast.show();

//        mainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                // Do your stuff here related to UI, e.g. show toast
//                Toast.makeText(getApplicationContext(), "MQTT Service Stopped", Toast.LENGTH_SHORT).show();
//            }
//        });


        Intent broadcastIntent = new Intent(MainActivity.RECEIVE_SERVICE_UPDATE);
        broadcastIntent.putExtra("payload", false);
        broadcastManager.sendBroadcast(broadcastIntent);
        logMethodEntranceExit(false);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logMethodEntranceExit(true);

//        SotiAgentApplication.readAppSetting(); // new 2020-03-17, restore settings before starting Mqtt...
//        SotiAgentApplication.restoreSettings(); // new 2020-03-17, restore settings before starting Mqtt...


        startMqtt();




//        if (mToast == null)
//            mToast = Toast.makeText(this, "MQTT Service Started", Toast.LENGTH_SHORT);
//        else
//            mToast.setText("MQTT Service Started");
//        mToast.show();


//        mainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                // Do your stuff here related to UI, e.g. show toast
//                Toast.makeText(getApplicationContext(), "MQTT Service Started", Toast.LENGTH_SHORT).show();
//            }
//        });


        showNotification();

        LAUNCHER.onServiceCreated(this);

//        SotiAgentApplication.restoreSettings(); // new 2020-03-17, restore settings before starting Mqtt...
//        startMqtt();
         SotiAgentApplication.restoreSettings(); // disabled 2020-03-17, restore settings before starting Mqtt...

//        Wifi.startAutoUpdate();
//        Bluetooth.startAutoUpdate();
//        System.startAutoUpdate();
//        Battery.startAutoUpdate();
//        Gps.startAutoUpdate();
//        Apps.startAutoUpdate();

        Intent broadcastIntent = new Intent(MainActivity.RECEIVE_SERVICE_UPDATE);
        broadcastIntent.putExtra("payload", true);
        broadcastManager.sendBroadcast(broadcastIntent);
        logMethodEntranceExit(false);
        return START_STICKY;
    }

    private void showNotification() {
        logMethodEntranceExit(true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        String NOTIFICATION_CHANNEL_ID = "mis.casio.de.wsd_f20.soti.agent";
        String channelName = "MQTT Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Service Alive")
                .setTicker("Service Alive")
                .setContentText("Service Text")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .setOngoing(true)

                .build();
        startForeground(Notification.FLAG_FOREGROUND_SERVICE, notification);
        logMethodEntranceExit(false);
    }

    private void stopMqtt() {
        logMethodEntranceExit(true);
        if (MqttHelper.getInstance() == null) return;
        MqttHelper.getInstance().disconnect();
        logMethodEntranceExit(false);
    }

    private void startMqtt(){
        logMethodEntranceExit(true);
//        this.stopMqtt();
//        new MqttHelper(getApplicationContext(), storagePath);
//        new MqttHelper(getApplicationContext());
        if (MqttHelper.getInstance() == null)
            new MqttHelper();
        if (!MqttHelper.getInstance().isConnected())
            MqttHelper.getInstance().connect();
        logMethodEntranceExit(false);
    }
}