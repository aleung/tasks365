package leoliang.tasks365;

import java.util.Calendar;

import leoliang.tasks365.calendar.AndroidCalendar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

public class TaskListActivity extends Activity {

    private static final String LOG_TAG = "tasks365";

    private AndroidCalendar calendar;
    private Cursor cursor;

    // Read from preference
    private int calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        calendar = new AndroidCalendar(this);
        cursor = calendar.queryTasksByDate(calendarId, Calendar.getInstance());
        startManagingCursor(cursor);

        ListAdapter adapter = new DayViewCursorAdapter(this, cursor);
        ListView listView = (ListView) findViewById(R.id.taskList);
        listView.setAdapter(adapter);

        Button addButton = (Button) findViewById(R.id.addTaskButton);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@SuppressWarnings("unused") View view) {
                Intent intent = new Intent(getApplicationContext(), NewTaskActivity.class);
                startActivity(intent);
            }

        });
    }

}