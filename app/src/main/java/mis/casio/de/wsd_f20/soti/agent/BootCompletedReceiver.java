// Disclaimer:
// This software is provided AS IS for educational purposes only,
// with no warranties, and confers no rights. Error checking is
// elided for clarity. Carry out sufficient tests prior to using
// it in a productive environment.
// CASIO Europe GmbH accepts no responsibility for loss or damage
// arising from its use, including damage from virus.
//
// Please, note: This configuration information may be changed
// without any further notice.
// Casio Europe GmbH does not guarantee future compatibility. By
// using this software you agree to these terms and conditions.
// 10.12.18 Casio Europe GmbH
//////////////////////////////////////////////////////////////////////

/**
 * BootCompletedReceiver.java
 * Handle the autostart of service on boot
 * If the application is installed, the intent .BOOT_COMPLETED does start the service
 **/

package mis.casio.de.wsd_f20.soti.agent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;

import mis.casio.de.wsd_f20.soti.agent.service.MqttService;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "MQTT (BootCompletedReceiver)";
    private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

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

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            logMethodEntranceExit(true);
            MqttService.start(context);
            logMethodEntranceExit(false);
        }
    }
}

