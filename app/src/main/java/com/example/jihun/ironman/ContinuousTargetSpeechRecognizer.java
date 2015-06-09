package com.example.jihun.ironman;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;

public class ContinuousTargetSpeechRecognizer extends TargetSpeechRecognizer {
    static final int kMsgRecognizerStartListening = 1;
    static final int kMsgRecognizerStop = 2;

    private final Messenger server_messenger_ = new Messenger(new IncomingHandler(this));
    private AudioManager audio_manager_;

    private boolean is_stream_solo_;

    public ContinuousTargetSpeechRecognizer(Activity parent_activity, Listener listener) {
        super(parent_activity, listener);
    }

    public void start() {
        super.start();
        audio_manager_ = (AudioManager) parent_activity_.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);

        listener_.onRmsChanged(0);
    }

    public void stop() {
        super.stop();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        super.onReadyForSpeech(params);
        no_speech_count_down_.start();
    }

    @Override
    public void onBeginningOfSpeech() {
        super.onBeginningOfSpeech();
        // speech input will be processed, so there is no need for count down anymore
        no_speech_count_down_.cancel();
    }

    @Override
    public void onError(int error) {
        // DO NOT call super method since onEndListening is being calling.
        // super.onError(error);

        Log.d(TAG, "onError for speech: " + error);
        no_speech_count_down_.cancel();

        // start listening again.
        Message message = Message.obtain(null, kMsgRecognizerStartListening);
        try {
            server_messenger_.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResults(Bundle results) {
        String match = processMatchResult(results);
        if (!match.isEmpty()) {
            audio_manager_.setStreamSolo(
                    AudioManager.STREAM_VOICE_CALL, false);
            is_stream_solo_ = false;

            listener_.onEndListening(match);
        } else {
            // since we didn't grab the signal, start listening again.
            Message message = Message.obtain(null, kMsgRecognizerStartListening);
            try {
                server_messenger_.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Prevent to run speech recognizing more than 5 seconds. Otherwise, the speech recognize
    // service dies silently without calling onError.
    // http://stackoverflow.com/questions/14940657/android-speech-recognition-as-a-service-on-android-4-1-4-2
    protected CountDownTimer no_speech_count_down_ = new CountDownTimer(5000, 5000) {
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {/*
            Log.i(TAG, "CountDownTimer onFinish");
            Message message = Message.obtain(null, kMsgRecognizerStop);
            try {
                server_messenger_.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }*/
        }
    };

    protected static class IncomingHandler extends Handler {
        private WeakReference<ContinuousTargetSpeechRecognizer> target_;

        IncomingHandler(ContinuousTargetSpeechRecognizer target) {
            target_ = new WeakReference<ContinuousTargetSpeechRecognizer>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final ContinuousTargetSpeechRecognizer target = target_.get();

            switch (msg.what) {
                case kMsgRecognizerStartListening:
                    Log.d(TAG, "message start listening");
                    // turn off beep sound
                    if (!target.is_stream_solo_) {
                        target.audio_manager_.setStreamSolo(
                                AudioManager.STREAM_VOICE_CALL, true);
                        target.is_stream_solo_ = true;
                    }
                    target.start();
                    break;

                case kMsgRecognizerStop:
                    Log.d(TAG, "message stop recognizer");
                    target.stop();
                    if (target.is_stream_solo_) {
                        target.audio_manager_.setStreamSolo(
                                AudioManager.STREAM_VOICE_CALL, false);
                        target.is_stream_solo_ = false;
                    }
                    break;
            }
        }
    }
}
