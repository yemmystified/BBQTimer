// The MIT License (MIT)
//
// Copyright (c) 2015 Jerry Morrison
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.onefishtwo.bbqtimer.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.onefishtwo.bbqtimer.MinutesChoices;
import com.onefishtwo.bbqtimer.TimeCounter;

/**
 * Saves the application's state persistently in SharedPreferences and caches it in a static
 * variable while the process is in memory.<p/>
 *
 * This does not currently provide listener notifications.<p/>
 *
 * The setters only update the state in memory. Call {@link #save} to persist the changes.
 */
public class ApplicationState {
    private static final String TAG = "ApplicationState";

    /** PERSISTENT STATE filename. */
    private static final String APPLICATION_PREF_FILE = "BBQ_Timer_Prefs";

    /** PERSISTENT STATE identifiers. */
    private static final String PREF_MAIN_ACTIVITY_IS_VISIBLE = "App_mainActivityIsVisible";
    private static final String PREF_ENABLE_REMINDERS = "App_enableReminders";
    private static final String PREF_SECONDS_PER_REMINDER = "App_secondsPerReminder";

    private static ApplicationState sharedInstance;

    private final TimeCounter timeCounter = new TimeCounter();
    private boolean mainActivityIsVisible; // between onStart() .. onStop()
    private boolean enableReminders;
    private int secondsPerReminder;

    /**
     * Returns the shared instance, using context to load the persistent state if needed and to save
     * the normalized-loaded state if needed. (See {@link TimeCounter#load} and
     * {@link com.onefishtwo.bbqtimer.ResumeReceiver}.)
     *<p/>
     * NOTE: After updating the shared instance, call {@link #save} to save it persistently.
     */
    public static ApplicationState sharedInstance(@NonNull Context context) {
        if (sharedInstance == null) {
            ApplicationState state = new ApplicationState();
            boolean needToSave     = state.load(context);

            if (needToSave) {
                state.save(context);
                Log.i(TAG, "*** Reset and saved the timer ***");
            }

            sharedInstance = state;
        }

        return sharedInstance;
    }

    /**
     * Package-scoped constructor for tests to access or override while the rest of the app is
     * limited to {@link #sharedInstance}.
     */
    ApplicationState() {
    }

    /**
     * Loads and normalizes persistent state using context. Overridable for tests.
     * Normalization matters if the device rebooted while the timer was running or if the app was
     * replaced with a version that doesn't support the current choice of secondsPerReminder.
     *
     * @return true if the caller should {@link #save} the normalized state to ensure consistent
     * results after {@link TimeCounter#load(SharedPreferences)} had to reset the timer.
     */
    boolean load(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);

        boolean needToSave    = timeCounter.load(prefs);
        mainActivityIsVisible = prefs.getBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, false);
        enableReminders       = prefs.getBoolean(PREF_ENABLE_REMINDERS, true);
        int secs              = prefs.getInt(PREF_SECONDS_PER_REMINDER, 5 * 60);
        secondsPerReminder    = MinutesChoices.normalizeSeconds(secs);

        return needToSave;
    }

    /** Saves persistent state using context. */
    public void save(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        timeCounter.save(prefsEditor);
        prefsEditor.putBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, mainActivityIsVisible);
        prefsEditor.putBoolean(PREF_ENABLE_REMINDERS, enableReminders);
        prefsEditor.putInt(PREF_SECONDS_PER_REMINDER, secondsPerReminder);
        prefsEditor.apply();
    }

    /**
     * Returns the shared TimeCounter instance.<p/>
     *
     * NOTE: The TimeCounter is a shared, mutable object. After updating it, call {@link #save} to
     * save it persistently.
     */
    @NonNull
    public TimeCounter getTimeCounter() {
        return timeCounter;
    }

    /**
     * Returns a boolean indicating whether MainActivity is visible [it's between
     * onStart() .. onStop()].
     */
    public boolean isMainActivityVisible() {
        return mainActivityIsVisible;
    }

    /**
     * Sets a boolean to remember whether MainActivity is visible. Call {@link #save} to save it.
     */
    public void setMainActivityIsVisible(boolean mainActivityIsVisible) {
        this.mainActivityIsVisible = mainActivityIsVisible;
    }

    /** Returns a boolean indicating whether periodic reminders are enabled. */
    public boolean isEnableReminders() {
        return enableReminders;
    }

    /**
     * Sets a boolean indicating whether periodic reminder alarms are enabled. Call {@link #save} to
     * save it.
     */
    public void setEnableReminders(boolean enableReminders) {
        this.enableReminders = enableReminders;
    }

    /** Returns the number of seconds between periodic reminder alarms. */
    public int getSecondsPerReminder() {
        return secondsPerReminder;
    }

    /** Returns the number of milliseconds between periodic reminder alarms. */
    public long getMillisecondsPerReminder() {
        return getSecondsPerReminder() * 1000L;
    }

    /**
     * Sets the number of seconds between periodic reminder alarms. Call {@link #save} to save it.
     */
    public void setSecondsPerReminder(int secondsPerReminder) {
        this.secondsPerReminder = secondsPerReminder;
    }
}
