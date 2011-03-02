package leoliang.tasks365;

import java.util.Calendar;

import leoliang.tasks365.calendar.AndroidCalendar;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;

public class TaskListActivity extends ListActivity {

    private static final String LOG_TAG = "tasks365";

    private AndroidCalendar calendar;
    private Cursor cursor;

    // Read from preference
    private int calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calendar = new AndroidCalendar(this);
        cursor = calendar.queryTasksByDate(calendarId, Calendar.getInstance());
        startManagingCursor(cursor);

        ListAdapter adapter = new DayViewCursorAdapter(this, cursor);
        setListAdapter(adapter);
    }

}