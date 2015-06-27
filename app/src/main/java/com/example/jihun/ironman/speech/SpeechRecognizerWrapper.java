package com.example.jihun.ironman.speech;

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
 * Runs SpeechRecognizer continuously if there are constant request.
 *
 * By default, SpeechRecognizer ends in 5 seconds after starting.
 * So this class runs the recognizer over and over while start() method is called on.
 *
 * Mute the sounds at the begin and end of speech recognition, which is automatically generated
 * by the library and is unable to mute.
 */
public class SpeechRecognizerWrapper implements RecognitionListener {
    private static final int kMsgRecognizerStart = 1;
    private static final int kMsgRecognizerStop = 2;
    private static final int kMsgSoundOn = 3;
    private static final String TAG = "Ironman.SR";

    private SpeechRecognizer speech_recog_;
    // Intent fo speech recognizer
    private Intent intent_;
    private Activity parent_activity_;
    private SpeechListener speech_listener_;
    private Listener listener_;
    // Whether start() is called during the speech listening..
    private boolean duplicated_listening_ = false;
    private final SoundController sound_controllor_;
    private final Messenger server_messenger_ = new Messenger(new IncomingHandler(this));

    public interface Listener {
        // Start of speech recognition.
        void onStart();
        // End of speech recognition.
        void onStop();
        // Notifying sound level.
        void onSoundChanged(float rmsdB);
    }

    public SpeechRecognizerWrapper(Activity parent_activity, Listener listener,
                                   SpeechListener speech_listener) {
        parent_activity_ = parent_activity;
        listener_ = listener;
        speech_listener_ = speech_listener;

        sound_controllor_ = new SoundController(parent_activity.getApplicationContext());

        // set intent for speech recognizer
        intent_ = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent_.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                parent_activity_.getPackageName());
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        sound_controllor_.soundOff();
    }

    // Start speech recognizing
    public void start() {
        Log.i(TAG, "start listening");
        if (speech_recog_ != null) {
            Log.i(TAG, "duplicated listening");
            duplicated_listening_ = true;
            return;
        }

        speech_recog_ = SpeechRecognizer.createSpeechRecognizer(parent_activity_);
        speech_recog_.setRecognitionListener(this);
        speech_recog_.startListening(intent_);

        listener_.onStart();
        listener_.onSoundChanged(0);
    }

    // Stop speech recognizing
    public void stop() {
        Log.i(TAG, "stop listening");
        if (speech_recog_ != null) {
            speech_recog_.cancel();
            speech_recog_.destroy();
            speech_recog_ = null;
        }

        listener_.onStop();
        // reset sound level.
        listener_.onSoundChanged(0);
    }

    // Start speech recognizing asynchronously.
    public void asyncStart() {
        Message message = Message.obtain(null, kMsgRecognizerStart);
        try {
            server_messenger_.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Should be called before app terminates
    public void destroy() {
        if (speech_recog_ != null) {
            speech_recog_.cancel();
            speech_recog_.destroy();
            speech_recog_ = null;
        }
        sound_controllor_.soundOff();
    }

    /*
        Implements RecognitionListener interface below
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
        stop();

        // If there were duplicated start requests, start listening again.
        if (duplicated_listening_) {
            duplicated_listening_ = false;
            asyncStart();
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
        stop();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    private static class IncomingHandler extends Handler {
        private WeakReference<SpeechRecognizerWrapper> target_;

        IncomingHandler(SpeechRecognizerWrapper target) {
            target_ = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final SpeechRecognizerWrapper target = target_.get();

            switch (msg.what) {
                case kMsgRecognizerStart:
                    Log.d(TAG, "message start listening");
                    // turn off beep sound
                    target.start();
                    break;

                case kMsgRecognizerStop:
                    Log.d(TAG, "message stop recognizer");
                    target.stop();
                    break;
            }
        }
    }

    // System sound on / off controller.
    private class SoundController {
        private boolean is_on = true;
        private AudioManager audio_manager_;

        public SoundController(Context app_context) {
            audio_manager_ = (AudioManager)app_context.getSystemService(Context.AUDIO_SERVICE);
        }

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
