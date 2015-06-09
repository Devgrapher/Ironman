package com.example.jihun.ironman;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Main extends Activity {
    private static final String TAG = "Ironman";
    private Button btn_speech_;
    private TextView txt_speach_result_;
    private ProgressBar prograss_bar_;
    private ContinuousTargetSpeechRecognizer signal_speech_recognizer_;
    private TargetSpeechRecognizer command_speech_recognizer_;

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

        btn_speech_ = (Button) findViewById(R.id.buttonSpeach);
        btn_speech_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        prograss_bar_ = (ProgressBar)findViewById(R.id.progressBarSpeech);
        prograss_bar_.setMax(NormalizeSpeechValue(kSpeechMaxValue));
        txt_speach_result_ = (TextView) findViewById(R.id.textViewSpeachResult);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        signal_speech_recognizer_ =
                new ContinuousTargetSpeechRecognizer(this, signal_listener_);
        String[] signal = {kSignalSpeech};
        signal_speech_recognizer_.setTargetSpeech(signal);
        signal_speech_recognizer_.start();

        command_speech_recognizer_ = new TargetSpeechRecognizer(this, command_listener_);
        command_speech_recognizer_.setTargetSpeech(kCommandList);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        signal_speech_recognizer_.stop();
        signal_speech_recognizer_.destroy();
        command_speech_recognizer_.stop();
        command_speech_recognizer_.destroy();
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
            if (speech.equals(kSignalSpeech)) {
                command_speech_recognizer_.start();
            }
            else {
                Log.w(TAG, "signal speech is incorrect!");
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            final int increament = NormalizeSpeechValue(rmsdB) - prograss_bar_.getProgress();
            prograss_bar_.incrementProgressBy(increament);
        }
    };

    protected TargetSpeechRecognizer.Listener command_listener_ =
            new TargetSpeechRecognizer.Listener() {
        @Override
        public void onEndListening(String speech) {
            if (!speech.isEmpty()) {
                Log.i(TAG, "command recognized : " + speech);
                txt_speach_result_.setText(speech);
            }
            signal_speech_recognizer_.start();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            final int increament = NormalizeSpeechValue(rmsdB) - prograss_bar_.getProgress();
            prograss_bar_.incrementProgressBy(increament);
        }
    };
}
