package com.cerebus.tokens.navigator

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cerebus.tokens.presentation.settings_screen.SettingsFragment
import com.cerebus.tokens.presentation.MainActivity
import com.cerebus.tokens.R
import com.cerebus.tokens.presentation.tokens_screen.TokensFragment

/**
 * [NavigatorImpl] - a realisation of the [Navigator] interface
 * Intended for screen navigation
 *
 * @see Navigator
 * @see MainActivity
 *
 * @author Anastasia Drogunova
 * @since 28.05.2023
 */
class NavigatorImpl : Navigator {
    override var destination: Destinations = Destinations.TOKENS_FRAGMENT

    override fun navigate(destination: Destinations, activity: AppCompatActivity?, modifier: Modifiers) {
        if (activity == null) return
        val f: (newFragment: Fragment, activity: AppCompatActivity, tag: String) -> Unit = when (modifier) {
            Modifiers.REPLACE -> ::replaceFragment
            Modifiers.REPLACE_WITH_BACK -> ::replaceFragmentWithBackStack
        }
        when(destination) {
            Destinations.ABOUT_APP_FRAGMENT -> f(SettingsFragment.newInstance(), activity, SettingsFragment.TAG)
            Destinations.TOKENS_FRAGMENT -> f(TokensFragment.newInstance() , activity, TokensFragment.TAG)
        }
    }

    private fun replaceFragment(newFragment: Fragment, activity: AppCompatActivity, tag: String) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_container, newFragment)
            .commit()
        Log.d(TAG, "navigated to the $tag NO backstack")
    }

    private fun replaceFragmentWithBackStack(newFragment: Fragment, activity: AppCompatActivity, tag: String) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_container, newFragment)
            .addToBackStack(tag)
            .commit()
        Log.d(TAG, "navigated to the $tag WITH backstack")
    }

    companion object {
        const val TAG = "Navigator"
    }
}