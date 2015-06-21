package com.example.jihun.ironman;

/**
 * Created by Jihun on 2015-06-21.
 */
public interface SpeechListener {
    // called on finish of recognizing with the list that were recognized.
    void onSpeechRecognized(String speech);
}
