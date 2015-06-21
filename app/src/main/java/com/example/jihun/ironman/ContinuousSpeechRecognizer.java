package com.example.jihun.ironman;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Runs SpeechRecognizer continuously.
 *
 * By default, SpeechRecognizer ends in 5 seconds after starting.
 * So this class runs the recognizer over and over.
 *
 * It waits for the target speeches that were set by 'setTargetSpeech'
 * Upon catching the targets, it notifies the client via 'Listener' interface.
 */
public class ContinuousSpeechRecognizer implements RecognitionListener {
    private static final int kMsgRecognizerStart = 1;
    private static final int kMsgRecognizerStop = 2;
    private static final String TAG = "Ironman.SR";

    private SpeechRecognizer speech_recog_;
    private Intent intent_;
    private Activity parent_activity_;
    private SpeechListener speech_listener_;
    private Listener listener_;
    private final SoundController sound_controllor_;
    private final Messenger server_messenger_ = new Messenger(new IncomingHandler(this));

    public interface Listener {
        // notifying sound level.
        void onSoundChanged(float rmsdB);
    }

    public ContinuousSpeechRecognizer(Activity parent_activity, Listener listener,
                                      SpeechListener speech_listener) {
        parent_activity_ = parent_activity;
        listener_ = listener;
        speech_listener_ = speech_listener;

        sound_controllor_ = new SoundController();

        // set intent for speech recognizer
        intent_ = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent_.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                parent_activity_.getPackageName());
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        speech_recog_ = SpeechRecognizer.createSpeechRecognizer(parent_activity_);
        speech_recog_.setRecognitionListener(this);
    }

    // start speech recognizing
    public void start() {
        sound_controllor_.soundOff();

        Log.i(TAG, "start listening");
        speech_recog_.startListening(intent_);

        listener_.onSoundChanged(0);
    }

    // stop speech recognizing
    public void stop() {
        if (speech_recog_ != null) {
            speech_recog_.cancel();
        }
        Log.i(TAG, "stop listening");

        sound_controllor_.soundOn();
    }

    // should be called before app terminates
    public void destroy() {
        speech_recog_.destroy();
    }

    /*
        implements RecognitionListener interface below
    */

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // it's too noisy
        //Log.d(TAG, "onRmsChanged: " + rmsdB);
        listener_.onSoundChanged(rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.d(TAG, "onError for speech: " + error);

        // start listening again.
        Message message = Message.obtain(null, kMsgRecognizerStart);
        try {
            server_messenger_.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "onResult");

        ArrayList<String> results_in_arraylist =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        // notifies the speeches each by each.
        for (String speech : results_in_arraylist) {
            Log.d(TAG, "match: " + speech);
            speech_listener_.onSpeechRecognized(speech);
        }

        // keep listening...
        Message message = Message.obtain(null, kMsgRecognizerStart);
        try {
            server_messenger_.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    private static class IncomingHandler extends Handler {
        private WeakReference<ContinuousSpeechRecognizer> target_;

        IncomingHandler(ContinuousSpeechRecognizer target) {
            target_ = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final ContinuousSpeechRecognizer target = target_.get();

            switch (msg.what) {
                case kMsgRecognizerStart:
                    Log.d(TAG, "message start listening");
                    // turn off beep sound
                    target.sound_controllor_.soundOff();
                    target.start();
                    break;

                case kMsgRecognizerStop:
                    Log.d(TAG, "message stop recognizer");
                    target.stop();
                    target.sound_controllor_.soundOn();
                    break;
            }
        }
    }

    // system sound on / off controller.
    private class SoundController {
        private boolean is_on = true;
        private AudioManager audio_manager_ =
                (AudioManager) parent_activity_.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);

        public void soundOn() {
            if (!is_on) {
                audio_manager_.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                is_on = true;
            }
        }

        public void soundOff() {
            if (is_on) {
                audio_manager_.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                is_on = false;
            }
        }
    }
}
