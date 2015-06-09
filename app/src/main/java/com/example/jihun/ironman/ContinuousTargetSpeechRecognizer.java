package com.example.jihun.ironman;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
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
    private SoundController sound_controllor_ = new SoundController();

    public ContinuousTargetSpeechRecognizer(Activity parent_activity, Listener listener) {
        super(parent_activity, listener);
    }

    public void start() {
        sound_controllor_.soundOff();
        super.start();
        listener_.onRmsChanged(0);
    }

    public void stop() {
        super.stop();
        sound_controllor_.soundOn();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        super.onReadyForSpeech(params);
    }

    @Override
    public void onBeginningOfSpeech() {
        super.onBeginningOfSpeech();
    }

    @Override
    public void onError(int error) {
        // DO NOT call super method since onEndListening is being calling.
        // super.onError(error);

        Log.d(TAG, "onError for speech: " + error);
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
            listener_.onEndListening(match);
        }
        // keep listening...
        Message message = Message.obtain(null, kMsgRecognizerStartListening);
        try {
            server_messenger_.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

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

    protected class SoundController {
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
