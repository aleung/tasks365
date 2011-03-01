package leoliang.tasks365;

import leoliang.tasks365.calendar.AndroidCalendar;
import leoliang.tasks365.calendar.BaseColumns;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;

public class TaskListActivity extends ListActivity {

    private static final String CALENDAR_NAME = "leoliang.tasks365";
    private static final String DEBUG_TAG = "tasks365";
    private static final String CALENDAR_DISPLAY_NAME = "tasks365";

    private AndroidCalendar calendar;

    // Read from preference
    private int calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calendar = new AndroidCalendar(this);

        String[] projection = new String[] { BaseColumns.ID, AndroidCalendar.EVENT_FIELD_TITLE,
                AndroidCalendar.EVENT_FIELD_START_TIME };
        Cursor cursor = calendar.getCalendarManagedCursor(projection, "calendar_id=" + calendarId,
                AndroidCalendar.PATH_EVENTS);

        ListAdapter adapter = new DayViewCursorAdapter(this, cursor);
        setListAdapter(adapter);
    }

}