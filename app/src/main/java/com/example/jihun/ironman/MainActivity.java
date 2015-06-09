package com.example.jihun.ironman;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
    private Button btn_connect_;
    private TextView txt_speach_result_;
    private ProgressBar prograss_bar_;
    private ContinuousTargetSpeechRecognizer signal_speech_recognizer_;
    private BluetoothAdapter bluetooth_;

    private final float kSpeechMinValue = -2.12f;
    private final int kSpeechMaxValue = 10;
    // the value for magnifying to display on prograss bar.
    private final int kSpeechMagnifyingValue = 100;
    // the signal speech that the recognition starts with.
    private final String kSignalSpeech = "Lucy";
    // the commands ordered in speech.
    private final String[] kCommandList = {
            "light on", "light off", "music on", "music off"};

    protected int NormalizeSpeechValue(float value) {
        return (int)((value + Math.abs(kSpeechMinValue)) * kSpeechMagnifyingValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start main");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View layout = findViewById(R.id.mainLayout);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFFF0FAFF,0xFFA3E0FF});
        gd.setCornerRadius(0f);
        layout.setBackground(gd);

        btn_connect_ = (Button) findViewById(R.id.buttonConnect);
        prograss_bar_ = (ProgressBar)findViewById(R.id.progressBarSpeech);
        prograss_bar_.setMax(NormalizeSpeechValue(kSpeechMaxValue));
        txt_speach_result_ = (TextView) findViewById(R.id.textViewSpeachResult);

    }

    public void onPair(View v){
        Intent intent = new Intent(getApplicationContext(), BluetoothPairActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        signal_speech_recognizer_ =
                new ContinuousTargetSpeechRecognizer(this, signal_listener_);
        signal_speech_recognizer_.setTargetSpeech(kSignalSpeech, kCommandList);
        signal_speech_recognizer_.start();
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

    protected TargetSpeechRecognizer.Listener signal_listener_ =
            new TargetSpeechRecognizer.Listener() {
        @Override
        public void onEndListening(String speech) {
            txt_speach_result_.setText(speech);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            final int increament = NormalizeSpeechValue(rmsdB) - prograss_bar_.getProgress();
            prograss_bar_.incrementProgressBy(increament);
        }
    };
}
