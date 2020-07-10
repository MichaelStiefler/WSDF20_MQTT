package mis.casio.de.wsd_f20.soti.agent.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;

import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;

public class MqttServiceLauncher {
    public boolean isStarting() {
        return isStarting;
    }

    public boolean isShouldStop() {
        return shouldStop;
    }

    private boolean isStarting = false;
    private boolean shouldStop = false;

    private boolean upAndRunning = false;
    private Class serviceClass;
    private static final String TAG = "MQTT (MQTTServiceLauncher)";

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

    public MqttServiceLauncher(Class serviceClass) {
        logMethodEntranceExit(true, serviceClass.getSimpleName());
        this.serviceClass = serviceClass;
        logMethodEntranceExit(false, serviceClass.getSimpleName());
    }

    public boolean startService(Context context) {
        logMethodEntranceExit(true);
        if (this.isStarting) return false;
        synchronized(this) {
            this.isStarting = true;
            this.shouldStop = false;
            SotiAgentApplication.readAppSetting(); // new 2020-03-18, restore settings before starting Mqtt...
            context.startForegroundService(new Intent(context, serviceClass));
        }
        logMethodEntranceExit(false);
        return true;
    }

    public boolean stopService(Context context) {
        logMethodEntranceExit(true);
        synchronized(this) {
            if (this.isStarting) {
                this.shouldStop = true;
                logMethodEntranceExit(false, "Service is busy starting...");
                return false;
            } else {
                context.stopService(new Intent(context, serviceClass));
            }
        }
//        MqttHelper.getInstance().disconnect();
        this.setUpAndRunning(false);
        logMethodEntranceExit(false);
        return true;
    }

    public void onServiceCreated(Service service) {
        logMethodEntranceExit(true);
        synchronized(this) {
            this.isStarting = false;
            if (this.shouldStop) {
                this.shouldStop = false;
                service.stopSelf();
            }
            this.setUpAndRunning(true);
//            MqttHelper.getInstance().connect();
        }
        logMethodEntranceExit(false);
    }

    public boolean isUpAndRunning() {
        return upAndRunning;
    }

    public void setUpAndRunning(boolean upAndRunning) {
        this.upAndRunning = upAndRunning;
    }

}
