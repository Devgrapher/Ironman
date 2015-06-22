package com.example.jihun.ironman;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final String TAG = "Ironman";
    private TextView txt_speach_result_;
    private ProgressBar prograss_bar_;
    private ContinuousSpeechRecognizer speech_recognizer_;
    private ArduinoConnector arduinoConnector_;

    // Max speech value from SpeechRecognizer
    private final float kSpeechMinValue = -2.12f;
    // Min speech value from SpeechRecognizer
    private final int kSpeechMaxValue = 10;
    // The value for magnifying to display on prograss bar.
    private final int kSpeechMagnifyingValue = 100;
    // The signal speech that the recognition starts with.
    private final String kSignalSpeech = "Lucy";
    // The commands ordered in speech.
    private final String kCommandLightOn = "light on";
    private final String[] kCommandLightOnVariant =
            { "lights on", "lite on", "like on"};
    private final String kCommandLightOff = "light off";
    private final String[] kCommandLightOffVariant =
            { "lights off", "light of", "like talked"};


    private int normalizeSpeechValue(float value) {
        return (int)((value + Math.abs(kSpeechMinValue)) * kSpeechMagnifyingValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start main");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set gradient background color.
        View layout = findViewById(R.id.mainLayout);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFFF0FAFF,0xFFA3E0FF});
        gd.setCornerRadius(0f);
        layout.setBackground(gd);

        prograss_bar_ = (ProgressBar)findViewById(R.id.progressBarSpeech);
        prograss_bar_.setMax(normalizeSpeechValue(kSpeechMaxValue));
        txt_speach_result_ = (TextView) findViewById(R.id.textViewSpeachResult);
        txt_speach_result_.setText(getResources().getString(R.string.txtview_not_connected));

        // Wraps 'speech_listener_' in 'Filter classes', so that it only gets filtered speeches.
        CommandSpeechFilter cmd_filter = new CommandSpeechFilter(speech_listener_);
        cmd_filter.addPattern(kCommandLightOn,
                new ArrayList<>(Arrays.asList(kCommandLightOnVariant)));
        cmd_filter.addPattern(kCommandLightOff,
                new ArrayList<>(Arrays.asList(kCommandLightOffVariant)));
        SignalSpeechFilter signal_filter = new SignalSpeechFilter(cmd_filter, kSignalSpeech);
        speech_recognizer_ = new ContinuousSpeechRecognizer(
                this, speech_recognizer_listener_, signal_filter);

        arduinoConnector_ = new ArduinoConnector(getApplicationContext(), arduino_listener_);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduinoConnector_.destroy();
        speech_recognizer_.destroy();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        speech_recognizer_.start();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        speech_recognizer_.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            BluetoothDevice device = intent.getParcelableExtra("device");
            arduinoConnector_.connect(device);
        }
    }

    public void onPair(View v){
        Intent intent = new Intent(getApplicationContext(), BluetoothPairActivity.class);
        startActivityForResult(intent, 0);
    }

    // Handles the speeches delivered by ContinuousSpeechRecognizer.
    private SpeechListener speech_listener_ = new SpeechListener() {
        @Override
        public void onSpeechRecognized(String speech) {
            if (speech.isEmpty())
                return;
            txt_speach_result_.setText(speech);
            try {
                arduinoConnector_.send(speech);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    };

    private ContinuousSpeechRecognizer.Listener speech_recognizer_listener_ =
        new ContinuousSpeechRecognizer.Listener() {
        @Override
        public void onSoundChanged(float rmsdB) {
            final int increment = normalizeSpeechValue(rmsdB) - prograss_bar_.getProgress();
            prograss_bar_.incrementProgressBy(increment);
        }
    };

    private ArduinoConnector.Listener arduino_listener_ = new ArduinoConnector.Listener() {
        @Override
        public void onConnect(BluetoothDevice device) {
            txt_speach_result_.setText(getResources().getString(R.string.txtview_listening));
        }

        @Override
        public void onRead(String data) {
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect(BluetoothDevice device) {
            txt_speach_result_.setText(
                    getResources().getString(R.string.txtview_not_connected));
        }
    };
}
