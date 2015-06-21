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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "Ironman";
    private TextView txt_speach_result_;
    private ProgressBar prograss_bar_;
    private ContinuousTargetSpeechRecognizer signal_speech_recognizer_;
    private ArduinoConnector arduinoConnector_;

    // max speech value from SpeechRecognizer
    private final float kSpeechMinValue = -2.12f;
    // min speech value from SpeechRecognizer
    private final int kSpeechMaxValue = 10;
    // the value for magnifying to display on prograss bar.
    private final int kSpeechMagnifyingValue = 100;
    // the signal speech that the recognition starts with.
    private final String kSignalSpeech = "Lucy";
    // the commands ordered in speech.
    private final String[] kCommandList = {
            "light on", "light off", "music on", "music off"};

    private int normalizeSpeechValue(float value) {
        return (int)((value + Math.abs(kSpeechMinValue)) * kSpeechMagnifyingValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start main");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set gradient background color.
        View layout = findViewById(R.id.mainLayout);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFFF0FAFF,0xFFA3E0FF});
        gd.setCornerRadius(0f);
        layout.setBackground(gd);

        prograss_bar_ = (ProgressBar)findViewById(R.id.progressBarSpeech);
        prograss_bar_.setMax(normalizeSpeechValue(kSpeechMaxValue));
        txt_speach_result_ = (TextView) findViewById(R.id.textViewSpeachResult);
        arduinoConnector_ = new ArduinoConnector(getApplicationContext());
    }

    public void onPair(View v){
        Intent intent = new Intent(getApplicationContext(), BluetoothPairActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        signal_speech_recognizer_ =
                new ContinuousTargetSpeechRecognizer(this, signal_listener_);
        signal_speech_recognizer_.setTargetSpeech(kSignalSpeech, kCommandList);
        signal_speech_recognizer_.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduinoConnector_.destroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        signal_speech_recognizer_.stop();
        signal_speech_recognizer_.destroy();
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

        //noinspection SimplifiableIfStatement
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

    private ContinuousTargetSpeechRecognizer.Listener signal_listener_ =
        new ContinuousTargetSpeechRecognizer.Listener() {
        @Override
        public void onEndListening(String speech) {
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

        @Override
        public void onRmsChanged(float rmsdB) {
            final int increment = normalizeSpeechValue(rmsdB) - prograss_bar_.getProgress();
            prograss_bar_.incrementProgressBy(increment);
        }
    };
}
