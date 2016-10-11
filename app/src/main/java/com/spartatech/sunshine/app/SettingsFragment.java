package com.spartatech.sunshine.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.spartatech.sunshine.app.data.WeatherContract;
import com.spartatech.sunshine.app.sync.SunshineSyncAdapter;
import com.spartatech.sunshine.app.util.Utility;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_art_pack_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_temperature_unit_key)));
    }

    @Override
    public void onResume() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    // This gets called after the preference is changed, which is important because we
    // start our synchronization here
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals(getString(R.string.pref_location_key)) ) {
            // we've changed the location
            // first clear locationStatus
            Utility.resetLocationStatus(getActivity());
            SunshineSyncAdapter.syncImmediately(getActivity());
        } else if ( key.equals(getString(R.string.pref_temperature_unit_key)) ) {
            // units have changed. update lists of weather entries accordingly
            getActivity().getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        } else if ( key.equals(getString(R.string.pref_location_status_key)) ) {
            // our location status has changed.  Update the summary accordingly
            Preference locationPreference = findPreference(getString(R.string.pref_location_key));
            bindPreferenceSummaryToValue(locationPreference);
        }
    }

    // This gets called before the preference is changed
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        setPreferenceSummary(preference, value);
        return true;
    }

    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (key.equals(getString(R.string.pref_location_key))) {
            @SunshineSyncAdapter.LocationStatus int status = Utility.getLocationStatus(getActivity());
            switch (status) {
                case SunshineSyncAdapter.LOCATION_STATUS_OK:
                    preference.setSummary(stringValue);
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                    preference.setSummary(getString(R.string.pref_location_unknown_description, value.toString()));
                    break;
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    preference.setSummary(getString(R.string.pref_location_error_description, value.toString()));
                    break;
                default:
                    // Note --- if the server is down we still assume the value
                    // is valid
                    preference.setSummary(stringValue);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }

    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
}
