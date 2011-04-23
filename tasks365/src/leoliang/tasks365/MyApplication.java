package leoliang.tasks365;

import greendroid.app.GDApplication;
import android.content.SharedPreferences;

public class MyApplication extends GDApplication {

    public static final String LOG_TAG = "tasks365";

    private static final String PREFERENCES_FILE_NAME = "default";
    private static final String PREFERENCE_CALENDAR_ID = "calendarId";
    private static final String PREFERENCE_LAST_RUN_TIME = "lastRunTime";

    /**
     * Creates a new instance of <code>MyApplication</code>.
     */
    public MyApplication() {
        super();
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
    }

    /**
     * Get ID of calendar that this application integrates to.
     * 
     * @return -1 if no calendar is chose
     */
    public long getCalendarId() {
        return getPreferences().getLong(PREFERENCE_CALENDAR_ID, -1);
    }

    /**
     * Set ID of calendar that this application integrates to.
     * 
     * @param id
     */
    public void setCalendarId(long id) {
        getPreferences().edit().putLong(PREFERENCE_CALENDAR_ID, id).commit();
    }

    public long getLastRunTime() {
        return getPreferences().getLong(PREFERENCE_LAST_RUN_TIME, System.currentTimeMillis());
    }

    public void setLastRunTime(long time) {
        getPreferences().edit().putLong(PREFERENCE_LAST_RUN_TIME, time).commit();
    }

}
