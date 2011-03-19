package leoliang.tasks365;

import java.util.Calendar;

import leoliang.tasks365.task.SingleDayTaskQuery;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class TaskListActivity extends Activity {

    private static final String LOG_TAG = "tasks365";

    private SingleDayTaskQuery query;

    // Read from preference
    private long calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.v(LOG_TAG, "onCreate()");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // TODO: move it to somewhere more appropriate
        TaskManager taskManager = new TaskManager(this, calendarId);
        taskManager.dealWithTasksInThePast();


        Button addButton = (Button) findViewById(R.id.addTaskButton);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@SuppressWarnings("unused") View view) {
                Intent intent = new Intent(getApplicationContext(), NewTaskActivity.class);
                startActivity(intent);
            }

        });

        TaskListAdapter adapter = new TaskListAdapter(this);

        query = new SingleDayTaskQuery(this, calendarId, adapter);
        query.query(Calendar.getInstance());

        ListView listView = (ListView) findViewById(R.id.taskList);
        listView.setAdapter(adapter);
    }

}