package leoliang.tasks365;

import leoliang.tasks365.calendar.Calendar;
import leoliang.tasks365.task.AndroidCalendar;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class CalendarChooseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_choose);

        final MyApplication app = (MyApplication) getApplication();
        long calendarId = app.getCalendarId();

        AndroidCalendar calendar = new AndroidCalendar(this);
        Cursor cursor = calendar.queryCalendars();
        startManagingCursor(cursor);

        ListView listView = (ListView) findViewById(R.id.calendarList);
        listView.setAdapter(new CalendarListAdapter(this, cursor));
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        for (int position = 0; position < listView.getCount(); position++) {
            if (listView.getItemIdAtPosition(position) == calendarId) {
                listView.setItemChecked(position, true);
                break;
            }
        }

        listView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unused")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                app.setCalendarId(id);
                CalendarChooseActivity.this.finish();
            }
        });
    }

    private class CalendarListAdapter extends SimpleCursorAdapter {

        public CalendarListAdapter(Context context, Cursor curosr) {
            super(context, android.R.layout.simple_list_item_single_choice, curosr,
                    new String[] { Calendar.Calendars.DISPLAY_NAME }, new int[] { android.R.id.text1 });
        }
    }
}
