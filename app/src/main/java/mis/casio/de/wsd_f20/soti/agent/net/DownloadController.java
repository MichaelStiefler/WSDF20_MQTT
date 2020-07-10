package mis.casio.de.wsd_f20.soti.agent.net;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import mis.casio.de.wsd_f20.soti.agent.BuildConfig;
import mis.casio.de.wsd_f20.soti.agent.SotiAgentApplication;

// see https://androidwave.com/download-and-install-apk-programmatically/

public class DownloadController extends BroadcastReceiver  {

//    private static class DownloadBroadcastReceiver extends BroadcastReceiver {
//        private DownloadManager dManager;
//        private long reference;
//
//        public DownloadBroadcastReceiver(DownloadManager dManager, long reference) {
//            this.dManager = dManager;
//            this.reference = reference;
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            long myDownloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//            if (reference == myDownloadID) {
//                Intent install = new Intent(Intent.ACTION_VIEW);
//                Uri downloadFileUri=null;
//                if (android.os.Build.VERSION.SDK_INT >= 24) {
//                    downloadFileUri = FileProvider.getUriForFile(context, "com.zmtmt.zhibohao.fileProvider", new File(Constants.UPGRADE_URL));
//                    // 给目标应用一个临时授权
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                }else {
//                    downloadFileUri= dManager.getUriForDownloadedFile(reference);
//                }
//                install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
//                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(install);
//            }
//        }
//    }


    private static final String TAG = "MQTT (DownloadController)";

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

    public static String calculateBase64SHA256(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] sha256sum = digest.digest();
            return Base64.getEncoder().encodeToString(sha256sum);

//            BigInteger bigInt = new BigInteger(1, sha256sum);
//            String output = bigInt.toString(16);
//            // Fill to 32 chars
//            output = String.format("%32s", output).replace(' ', '0');
//            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for SHA-256", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing SHA-256 input stream", e);
            }
        }
    }


    public DownloadController(String url, String from, long transactionId, String contentType, String base64SHA256) {
        this(SotiAgentApplication.getAppContext(), url, from, transactionId, contentType, base64SHA256);
    }

    public DownloadController(String url, String filename, String from, long transactionId, String contentType, String base64SHA256) {
        this(SotiAgentApplication.getAppContext(), url, filename, from, transactionId, contentType, base64SHA256);
    }

    public DownloadController(Context context, String url, String from, long transactionId, String contentType, String base64SHA256) {
        this(context, url, URLUtil.guessFileName(url, null, "application/vnd.android.package-archive"), from, transactionId, contentType, base64SHA256);
    }

    public DownloadController(Context context, String url, String filename, String from, long transactionId, String contentType, String base64SHA256) {
        this.context = context;
        this.url = url;
        this.filename = filename;
        this.from = from;
        this.transactionId = transactionId;
        this.contentType = contentType;
        this.base64SHA256 = base64SHA256;
        this.enqueueDownload();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long myDownloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (reference == myDownloadID) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(this.reference);
                query.setFilterByStatus(DownloadManager.STATUS_FAILED|DownloadManager.STATUS_PAUSED|DownloadManager.STATUS_SUCCESSFUL|DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PENDING);
                Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
                if (cursor.moveToFirst()) {
                    if (cursor.getCount() > 0) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                   switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                Log.v(TAG, "Status is SUCCESSFUL");
                                Uri contentUri = FileProvider.getUriForFile(
                                        context,
                                        BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                                        new File("/sdcard/Download/" + this.filename)
                                );
                                Intent install = new Intent(Intent.ACTION_VIEW);
                                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                                install.setData(contentUri);
                                context.startActivity(install);
                                context.unregisterReceiver(this);
                                break;
                            case DownloadManager.STATUS_FAILED:
                                Log.v(TAG, "Status is FAILED");
                                context.unregisterReceiver(this);
                                break;
                            case DownloadManager.STATUS_PAUSED:
                                Log.v(TAG, "Status is PAUSED");
                                break;
                            case DownloadManager.STATUS_PENDING:
                                Log.v(TAG, "Status is PENDING");
                                break;
                            default:
                                Log.v(TAG, "Status is UNKNOWN (" + status + ")");
                                break;
                        }
                    }
                }
                cursor.close();
            }
        }
    }


    public void enqueueDownload() {
        try {
//        String destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + filename;
            String destination = "/sdcard/Download/" + filename;
            File file = new File(destination);
            if (file.exists()) file.delete();
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri)
                    .setMimeType(contentType)
                    .setTitle(filename)// Title of the Download Notification
                    .setDescription(url)// Description of the Download Notification
                    .setDestinationUri(Uri.parse(FILE_BASE_PATH + destination))// Uri of the destination file
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)// Visibility of the download Notification
                    .setVisibleInDownloadsUi(false)
                    .setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
            this.reference = downloadManager.enqueue(request);
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        DownloadBroadcastReceiver downloadReceiver = new DownloadBroadcastReceiver(downloadManager, reference);
//        this.context.registerReceiver(downloadReceiver, filter);
            this.context.registerReceiver(this, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Context context;
    private String url;
    private String filename;
    private long reference;
    private String from;
    private long transactionId;
    private String contentType;
    private String base64SHA256;
    private static final String FILE_BASE_PATH = "file://";
    private static final String PROVIDER_PATH = ".provider";
}