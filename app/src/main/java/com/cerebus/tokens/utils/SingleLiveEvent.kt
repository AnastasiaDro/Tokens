package com.cerebus.tokens.utils

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean
import com.cerebus.tokens.settings_screen.SettingsViewModel

/**
 * [SingleLiveEvent] - Mutable live data for single events
 * like opening dialogs or showing toasts
 *
 * @see MutableLiveData
 * @see SettingsViewModel for example
 *
 * @author Anastasia Drogunova
 * @since 17.07.2023
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) { Log.w(TAG, SINGLE_OBSERVER_WARNING) }


        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    @MainThread
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }

    /**
     * Used for cases where T is Void to make calls cleaner
     */

    @MainThread
    fun call() {
        value = null
    }

    private companion object {
        const val TAG = "SingleLiveEvent"
        const val SINGLE_OBSERVER_WARNING = "Multiple observers registered but only one will be notified of changes"
    }
}