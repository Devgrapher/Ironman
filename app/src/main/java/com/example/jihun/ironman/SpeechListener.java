package com.example.jihun.ironman;

/**
 * Interface for listening speeches.
 *
 * Implements this interface to be notified speeches that recognized from SpeechRecognizer.
 * This is especially needed for implementing Decorator Pattern, which adds new feature in
 * speech callback.
 */
public interface SpeechListener {
    // called on finish of recognizing with the list that were recognized.
    void onSpeechRecognized(String speech);
}
