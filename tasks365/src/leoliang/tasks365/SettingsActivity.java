package leoliang.tasks365;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference calendarPreference = findPreference("calendar");
        calendarPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@SuppressWarnings("unused") Preference preference) {
                startActivity(new Intent(getApplicationContext(), CalendarChooseActivity.class));
                return true;
            }
        });
    }

}
