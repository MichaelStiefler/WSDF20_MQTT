package mis.casio.de.wsd_f20.soti.agent;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import mis.casio.de.wsd_f20.soti.agent.mqtt.MqttHelper;
import mis.casio.de.wsd_f20.soti.agent.service.MqttService;

public class MainActivity extends WearableActivity {

    public static final boolean LOG_METHOD_ENTRANCE_EXIT = false;
    public static final int REQUEST_RESULT_LOGIN = 123467890;

    private static String TAG = "MQTT (MainActivity)";
    TextView connectionState;
    TextView transmitState;
    TextView receiveState;
    private boolean mqttConnected = false;
    private boolean timerActive = false;

    public static final int TIMER_PERIOD = 100;

    private Handler handler;
    private Runnable fadeTextRunnable = this::doFadeText;

    public static final String RECEIVE_SERVICE_UPDATE = "mis.casio.de.wsd_f20.soti.agent.RECEIVE_SERVICE_UPDATE";
    public static final String RECEIVE_CONNECTION_UPDATE = "mis.casio.de.wsd_f20.soti.agent.RECEIVE_CONNECTION_UPDATE";
    public static final String RECEIVE_TRANSMIT_UPDATE = "mis.casio.de.wsd_f20.soti.agent.RECEIVE_TRANSMIT_UPDATE";
    public static final String RECEIVE_RECEIVE_UPDATE = "mis.casio.de.wsd_f20.soti.agent.RECEIVE_RECEIVE_UPDATE";

    private Switch serviceSwitch; // Switch to toggle MQTT Service on/off

    private final static int MY_PERMISSIONS_REQUEST_MQTT = 99;

    private boolean permissionRequestPending = false;
    private boolean loginActivityPending = false;
    private ArrayList<String> missingPermissions = new ArrayList<>();

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
        Arrays.stream(addonTags).forEach(sb::append);

        Log.v(TAG, nameofCurrMethod + " " + sb.toString() + (entrance?" +":" -"));
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logMethodEntranceExit(true);
            switch (Objects.requireNonNull(intent.getAction())) {
                case RECEIVE_SERVICE_UPDATE:
                    serviceSwitch.setChecked(intent.getBooleanExtra("payload", false));
                    serviceSwitch.setClickable(true);
                    updateActivity();
                    break;
                case RECEIVE_CONNECTION_UPDATE:
                    MainActivity.this.mqttConnected = intent.getBooleanExtra("connected", false);
                    if (MainActivity.this.mqttConnected) {
                        if (!isMyServiceRunning()) {
                            if (!MqttService.LAUNCHER.isStarting()) {
                                serviceSwitch.setClickable(false);
                                MqttService.start(getApplicationContext());
                            }
                        } else {
                            serviceSwitch.setChecked(true);
                        }
                    }
                    updateActivity();
                    break;
                case RECEIVE_TRANSMIT_UPDATE:
                    MainActivity.this.transmitState.setText(intent.getStringExtra("transfer"));
                    MainActivity.this.transmitState.setTextColor(Color.CYAN);
                    MainActivity.this.transmitState.setTypeface(null, Typeface.BOLD);
                    break;
                case RECEIVE_RECEIVE_UPDATE:
                    MainActivity.this.receiveState.setText(intent.getStringExtra("transfer"));
                    MainActivity.this.receiveState.setTextColor(Color.CYAN);
                    MainActivity.this.receiveState.setTypeface(null, Typeface.BOLD);
                    break;

            }
            logMethodEntranceExit(false);
        }
    };

    private void updateActivity() {
        logMethodEntranceExit(true);
        serviceSwitch.setChecked(isMyServiceRunning());
        connectionState.setText(this.mqttConnected?"connected":"disconnected");
        connectionState.setTextColor(this.mqttConnected? Color.GREEN:Color.RED);
        logMethodEntranceExit(false);
    }

    private void doFadeText() {
        fadeText(transmitState);
        fadeText(receiveState);
        this.handler.postDelayed(this.fadeTextRunnable, TIMER_PERIOD);
    }

    private void fadeText(View view) {
        if (!(view instanceof TextView)) return;
        TextView textView = (TextView)view;
        int currentColor = textView.getCurrentTextColor();
        int oldAlpha = Color.alpha(currentColor);
        int oldRed = Color.red(currentColor);
        int oldGreen = Color.green(currentColor);
        int oldBlue = Color.blue(currentColor);
        if (oldAlpha <= 40 && oldRed >= 255 && oldGreen >= 255 && oldBlue >= 255) return;
        int newRed = oldRed + 10;
        int newGreen = oldGreen + 10;
        int newBlue = oldBlue + 10;
        int newAlpha = oldAlpha - 10;
        if (newAlpha < 40) newAlpha = 40;


        if (newRed > 255) newRed = 255;
        if (newGreen > 255) newGreen = 255;
        if (newBlue > 255) newBlue = 255;

        int newColor = Color.argb(newAlpha, newRed, newGreen, newBlue);
        textView.setTextColor(newColor);
        if (newAlpha < 100) textView.setTypeface(null, Typeface.NORMAL);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logMethodEntranceExit(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.connectionState = findViewById(R.id.connectionState);
        this.transmitState = findViewById(R.id.transmitState);
        this.receiveState = findViewById(R.id.receiveState);

        serviceSwitch = findViewById(R.id.switchServiceOnOff);

        // Enables Always-on
        setAmbientEnabled();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_SERVICE_UPDATE);
        intentFilter.addAction(RECEIVE_CONNECTION_UPDATE);
        intentFilter.addAction(RECEIVE_TRANSMIT_UPDATE);
        intentFilter.addAction(RECEIVE_RECEIVE_UPDATE);
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);

        this.handler = new Handler(getMainLooper());

        if (!timerActive) {
            handler.postDelayed(this.fadeTextRunnable, TIMER_PERIOD);
            timerActive = true;
        }

        updateActivity();

        serviceSwitch.setOnClickListener(view -> {
            if (serviceSwitch.isChecked()) {
                if (!isMyServiceRunning()) {
                    if (!MqttService.LAUNCHER.isStarting()) {
                        serviceSwitch.setClickable(false);
                        MqttService.start(getApplicationContext());
                    }
                }
            }
            else {
                if (isMyServiceRunning()) {
                    serviceSwitch.setClickable(false);
                    MqttService.stop(getApplicationContext());
                }
            }
        });


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES) != PackageManager.PERMISSION_GRANTED)
//            missingPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);

        if (!missingPermissions.isEmpty()) {
            permissionRequestPending = true;
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), MY_PERMISSIONS_REQUEST_MQTT);
            logMethodEntranceExit(false, "Missing Permissions");
            return;
        }

        //this.checkRequiredActivity();

        logMethodEntranceExit(false);
    }

    private void checkRequiredActivity() {
        if (permissionRequestPending) return;
        if (!loginActivityPending && !SotiAgentApplication.readAppSetting()) {
            loginActivityPending = true;
            Intent activityIntent;
            activityIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(activityIntent, REQUEST_RESULT_LOGIN);
        } else {
            if (!isMyServiceRunning()) {
                MqttService.start(getApplicationContext());
            } else {
                serviceSwitch.setChecked(true);
            }
            if (SotiAgentApplication.readAppSetting()) {
                if (!MqttHelper.getInstance().isConnected())
                    MqttHelper.getInstance().connect();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESULT_LOGIN) {
            loginActivityPending = false;
            this.checkRequiredActivity();
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        logMethodEntranceExit(true);
        permissionRequestPending = false;
        loginActivityPending = false;
        if (requestCode == MY_PERMISSIONS_REQUEST_MQTT) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.checkRequiredActivity();
//                if (!isMyServiceRunning()) {
//                    MqttService.start(getApplicationContext());
//                    serviceSwitch.setChecked(true);
//                }
            } else {
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    showPermissionRequiredAlert();
                } else {
                    showPermissionRejected();
                }
            }
            logMethodEntranceExit(false);
            return;
        }
        logMethodEntranceExit(false, "unknown permission request code");
    }

    private void showPermissionRejected() {
        logMethodEntranceExit(true);
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.SOTI_MQTT_Permission_Rejected)
                .setPositiveButton(android.R.string.ok, (dialogYes, which) -> {
                    dialogYes.dismiss();
                    this.finishAndRemoveTask();
                    Log.e(TAG, "Exiting due to missing permissions (1)");
                    System.exit(0);
                    // Do stuff if user accepts
                }).setNegativeButton(android.R.string.cancel, (dialogNo, which) -> {
                    dialogNo.dismiss();
                    this.finishAndRemoveTask();
                    Log.e(TAG, "Exiting due to missing permissions (2)");
                    System.exit(0);
                    // Do stuff when user neglects.
                }).setOnCancelListener(dialogCancel -> {
                    dialogCancel.dismiss();
                    this.finishAndRemoveTask();
                    Log.e(TAG, "Exiting due to missing permissions (3)");
                    System.exit(0);
                    // Do stuff when cancelled
                }).create();
        dialog.setCancelable(false);
        dialog.show();
        logMethodEntranceExit(false);
    }


    private void showPermissionRequiredAlert() {
        logMethodEntranceExit(true);
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.SOTI_MQTT_Permission_Required)
                .setPositiveButton(android.R.string.ok, (dialogYes, which) -> {
                    dialogYes.dismiss();
                    permissionRequestPending = true;
                    ActivityCompat.requestPermissions(this,
                            missingPermissions.toArray(new String[0]),
                            MY_PERMISSIONS_REQUEST_MQTT);
                    // Do stuff if user accepts
                }).setNegativeButton(android.R.string.cancel, (dialogNo, which) -> {
                    dialogNo.dismiss();
                    this.finishAndRemoveTask();
                    System.exit(0);
                    // Do stuff when user neglects.
                }).setOnCancelListener(dialogCancel -> {
                    dialogCancel.dismiss();
                    this.finishAndRemoveTask();
                    System.exit(0);
                    // Do stuff when cancelled
                }).create();
        dialog.setCancelable(false);
        dialog.show();
        logMethodEntranceExit(false);
    }

    /**
     * Update the user interface in onResume
     */
    @Override
    protected void onResume() {
        logMethodEntranceExit(true);
        super.onResume();
        if (permissionRequestPending) return;
        if (!loginActivityPending && !SotiAgentApplication.readAppSetting()) {
            loginActivityPending = true;
            Intent activityIntent;
            activityIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(activityIntent, REQUEST_RESULT_LOGIN);
            return;
        }
        this.mqttConnected = MqttHelper.getInstance() != null && MqttHelper.getInstance().isConnected();
        updateActivity();
        if (!timerActive) {
            handler.postDelayed(this.fadeTextRunnable, TIMER_PERIOD);
            timerActive = true;
        }
        logMethodEntranceExit(false);
    }

    /**
     * Unregister the receiver in onDestroy
     */
    @Override
    protected void onDestroy() {
        logMethodEntranceExit(true);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        if (!timerActive) {
            handler.removeCallbacks(this.fadeTextRunnable);
            timerActive = false;
        }
        super.onDestroy();
        logMethodEntranceExit(false);
    }

    /**
     * Check if the Service is alive and return the result
     *
     * @return boolean result
     */
    @SuppressWarnings("deprecation")
    private boolean isMyServiceRunning() {
        logMethodEntranceExit(true);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

         for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MqttService.class.getName().equals(service.service.getClassName())) {
                logMethodEntranceExit(false, "Service is running");
                return true; // Service is alive
            }
        }
        logMethodEntranceExit(false, "Service is not running");
        return false; // Service is stopped
    }

}
