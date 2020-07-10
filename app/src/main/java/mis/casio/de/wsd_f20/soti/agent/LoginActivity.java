package mis.casio.de.wsd_f20.soti.agent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class LoginActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (BuildConfig.DEBUG) {
            ((CheckBox)findViewById(R.id.checkBoxTLS)).setChecked(true);
            ((EditText)findViewById(R.id.editServerUri)).setText("m24.cloudmqtt.com");
            ((EditText)findViewById(R.id.editServerPort)).setText("21734");
            ((EditText)findViewById(R.id.editUsername)).setText("njhmkwxw");
            ((EditText)findViewById(R.id.editPassword)).setText("K5BItB4eaUpZ");
//            ((CheckBox)findViewById(R.id.checkBoxTLS)).setChecked(false);
//            ((EditText)findViewById(R.id.editServerUri)).setText("172.16.30.180");
//            ((EditText)findViewById(R.id.editServerPort)).setText("1883");
//            ((EditText)findViewById(R.id.editUsername)).setText("");
//            ((EditText)findViewById(R.id.editPassword)).setText("");
        }

        Button mSaveButton = findViewById(R.id.buttonSave);

        // Enables Always-on
        setAmbientEnabled();

        this.checkSettingsFile();

        mSaveButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            try {
                CheckBox checkBoxTLS = findViewById(R.id.checkBoxTLS);
                EditText editServerUri = findViewById(R.id.editServerUri);
                EditText editServerPort = findViewById(R.id.editServerPort);
                EditText editUsername = findViewById(R.id.editUsername);
                EditText editPassword = findViewById(R.id.editPassword);
                boolean useTLS = checkBoxTLS.isChecked();
                String serverUri = editServerUri.getText().toString();
                int serverPort = Integer.valueOf(editServerPort.getText().toString());
                String username = editUsername.getText().toString();
                String password = editPassword.getText().toString();
                if (SotiAgentApplication.saveAppSetting(useTLS, serverUri, serverPort, username, password))
                    setResult(Activity.RESULT_OK, resultIntent);
                else
                    setResult(Activity.RESULT_CANCELED, resultIntent);
            } catch (Exception e) {
                e.printStackTrace();
                setResult(Activity.RESULT_CANCELED, resultIntent);
            } finally {
                finish();
            }
        });
    }

    private void checkSettingsFile() {
        try {
            File SDCardRoot = Environment.getExternalStorageDirectory().getAbsoluteFile();
            File file = new File(SDCardRoot.getAbsolutePath() + File.separator + "mqtt_server_settings.xml");
            if (!file.exists()) return;
            InputStream is = new FileInputStream(file.getPath());

            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            DefaultHandler handler = new DefaultHandler(){
                String currentValue = "";
                boolean currentElement = false;
                public void startElement(String uri, String localName,String qName, Attributes attributes) {
                    currentElement = true;
                    currentValue = "";
                }
                public void endElement(String uri, String localName, String qName) {
                    currentElement = false;
                    if (localName.equalsIgnoreCase("tls")) {
                        Log.d("tls", currentValue);
                        ((CheckBox)findViewById(R.id.checkBoxTLS)).setChecked(currentValue.trim().equalsIgnoreCase("1"));
                    } else if (localName.equalsIgnoreCase("url")) {
                        Log.d("url", currentValue);
                        ((EditText)findViewById(R.id.editServerUri)).setText(currentValue.trim());
                    } else if (localName.equalsIgnoreCase("port")) {
                        Log.d("port", currentValue);
                        ((EditText)findViewById(R.id.editServerPort)).setText(currentValue.trim());
                    } else if (localName.equalsIgnoreCase("user")) {
                        Log.d("user", currentValue);
                        ((EditText)findViewById(R.id.editUsername)).setText(currentValue.trim());
                    } else if (localName.equalsIgnoreCase("pass")) {
                        Log.d("pass", currentValue);
                        ((EditText)findViewById(R.id.editPassword)).setText(currentValue.trim());
                   }
                }
                @Override
                public void characters(char[] ch, int start, int length) {
                    if (currentElement) {
                        currentValue = currentValue +  new String(ch, start, length);
                    }
                }
            };
            parser.parse(is,handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
