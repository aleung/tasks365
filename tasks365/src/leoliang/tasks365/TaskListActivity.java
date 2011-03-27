package leoliang.tasks365;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;

import java.util.Calendar;

import leoliang.tasks365.task.SingleDayTaskQuery;
import leoliang.tasks365.task.Task;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.DatePicker;
import android.widget.ListView;

public class TaskListActivity extends GDActivity {

    private static final String LOG_TAG = "tasks365";

    // context menu
    private static final int MENU_MARK_TASK_DONE = 1;
    private static final int MENU_MARK_TASK_UNDONE = 2;
    private static final int MENU_SCHEDULE_TASK = 3;

    private SingleDayTaskQuery query;
    private TaskManager taskManager;
    private TaskListAdapter adapter;

    // Read from preference
    private long calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.v(LOG_TAG, "onCreate()");
        }
        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.main);

        addActionBarItem(Type.Add, R.id.action_bar_add);

        // TODO: move it to somewhere more appropriate
        taskManager = new TaskManager(this, calendarId);
        taskManager.dealWithTasksInThePast();

        adapter = new TaskListAdapter(this);

        query = new SingleDayTaskQuery(this, calendarId, adapter);
        query.query(Calendar.getInstance());

        ListView listView = (ListView) findViewById(R.id.taskList);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (view.getId() == R.id.taskList) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            Task task = adapter.getItem(info.position);
            menu.setHeaderTitle(task.title);
            if (task.isDone) {
                menu.add(Menu.NONE, MENU_MARK_TASK_UNDONE, Menu.NONE, R.string.mark_task_undone);
            } else {
                menu.add(Menu.NONE, MENU_MARK_TASK_DONE, Menu.NONE, R.string.mark_task_done);
                menu.add(Menu.NONE, MENU_SCHEDULE_TASK, Menu.NONE, R.string.schedule_task);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Task task = adapter.getItem(info.position);
        switch (item.getItemId()) {
        case MENU_MARK_TASK_DONE:
            taskManager.markTaskDone(task);
            return true;
        case MENU_MARK_TASK_UNDONE:
            taskManager.markTaskUndone(task);
            return true;
        case MENU_SCHEDULE_TASK:
            scheduleTask(task);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void scheduleTask(final Task task) {
        new DatePickerDialog(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                task.startTime.set(Calendar.YEAR, year);
                task.startTime.set(Calendar.MONTH, monthOfYear);
                task.startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                Calendar now = Calendar.getInstance();
                if (task.startTime.before(now)) {
                    task.startTime = now;
                }
                task.isNew = false;
                taskManager.saveTask(task);
            }
        }, task.startTime.get(Calendar.YEAR), task.startTime.get(Calendar.MONTH),
                task.startTime.get(Calendar.DAY_OF_MONTH) + 1).show();
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        switch (item.getItemId()) {
        case R.id.action_bar_add:
            startActivity(new Intent(getApplicationContext(), NewTaskActivity.class));
            return true;
        default:
            return super.onHandleActionBarItemClick(item, position);
        }
    }

}