package leoliang.tasks365;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;
import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionBar;
import greendroid.widget.QuickActionWidget;
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener;

import java.util.Calendar;

import leoliang.tasks365.DraggableListView.DropListener;
import leoliang.tasks365.TaskListAdapter.ViewHolder;
import leoliang.tasks365.task.SingleDayTaskQuery;
import leoliang.tasks365.task.Task;
import leoliang.tasks365.task.TaskManager;
import leoliang.tasks365.task.TaskOrderMover;
import leoliang.tasks365.task.TaskOrderMover.MoveNotAllowException;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Config;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.googlecode.android.widgets.DateSlider.DateSlider;
import com.googlecode.android.widgets.DateSlider.DateSlider.OnDateSetListener;
import com.googlecode.android.widgets.DateSlider.DefaultDateSlider;

public class TaskListActivity extends GDActivity {

    private static final String LOG_TAG = "tasks365";

    // context menu / quick action
    private static final int MENU_MARK_TASK_DONE = 1;
    private static final int MENU_MARK_TASK_UNDONE = 2;
    private static final int MENU_SCHEDULE_TASK = 3;
    private static final int MENU_EDIT_TASK = 4;
    private static final int MENU_STAR_TASK = 5;
    private static final int MENU_UNSTAR_TASK = 6;

    // option menu
    private static final int MENU_SETTING = 1;

    private SingleDayTaskQuery query;
    private TaskManager taskManager;
    private TaskListAdapter adapter;
    private MyApplication application;
    private long calendarId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.v(LOG_TAG, "onCreate()");
        }

        application = (MyApplication) getApplication();
        validateCalendar();

        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.main);

        addActionBarItem(Type.Add, R.id.action_bar_add);

        adapter = new TaskListAdapter(this);
        initializeQuery();

        DraggableListView listView = (DraggableListView) findViewById(R.id.taskList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                showQuickActionBar(viewHolder.title, position);
            }
        });
        listView.setDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                try {
                    new TaskOrderMover(adapter, taskManager).moveTaskToPosition(from, to);
                    adapter.notifyDataSetChanged();
                } catch (MoveNotAllowException e) {
                    Toast.makeText(TaskListActivity.this, R.string.done_task_no_move, Toast.LENGTH_LONG);
                }
            }
        });

        // TODO: move it to background service
        taskManager = new TaskManager(this, application);
        taskManager.dealWithTasksInThePast();
        // end of TODO
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (application.getCalendarId() != calendarId) {
            // setting has been changed, re-initialize query to show new calendar
            initializeQuery();
        }
    }

    private void validateCalendar() {
        if (application.getCalendarId() == -1) {
            startActivity(new Intent(application, CalendarChooseActivity.class));
        }
        // TODO: check calendar existence, is synced
    }

    private void initializeQuery() {
        calendarId = application.getCalendarId();
        query = new SingleDayTaskQuery(this, calendarId, adapter);
        Time now = new Time();
        now.setToNow();
        query.query(now);
    }

    private void showQuickActionBar(View view, final int position) {
        final Task task = adapter.getItem(position);
        QuickActionBar bar = new QuickActionBar(this);
        if (task.isDone) {
            bar.addQuickAction(new QuickAction(MENU_MARK_TASK_UNDONE, this, R.drawable.checkbox_unchecked,
                    R.string.mark_task_undone));
        } else {
            if (!task.isPinned()) {
                bar.addQuickAction(new QuickAction(MENU_MARK_TASK_DONE, this, R.drawable.checkbox_checked,
                        R.string.mark_task_done));
                bar.addQuickAction(new QuickAction(MENU_SCHEDULE_TASK, this, R.drawable.calendar,
                        R.string.schedule_task));
                bar.addQuickAction(new QuickAction(MENU_EDIT_TASK, this, R.drawable.pencil,
                        R.string.edit_task));
            }
            if (task.isStarred) {
                bar.addQuickAction(new QuickAction(MENU_UNSTAR_TASK, this, R.drawable.unstar,
                        R.string.unstar_task));
            } else {
                bar.addQuickAction(new QuickAction(MENU_STAR_TASK, this, R.drawable.star_semi_empty,
                        R.string.star_task));
            }
        }
        bar.setOnQuickActionClickListener(new OnQuickActionClickListener() {
            @Override
            public void onQuickActionClicked(QuickActionWidget widget, int actionId) {
                switch (actionId) {
                case MENU_MARK_TASK_DONE:
                    taskManager.markTaskDone(task);
                    break;
                case MENU_MARK_TASK_UNDONE:
                    taskManager.markTaskUndone(task);
                    break;
                case MENU_SCHEDULE_TASK:
                    scheduleTask(task);
                    break;
                case MENU_STAR_TASK:
                    taskManager.starTask(task);
                    break;
                case MENU_UNSTAR_TASK:
                    taskManager.unstarTask(task);
                    break;
                }
                adapter.notifyDataSetChanged();
                // TODO: remove tasks which isn't scheduled in today
            }
        });
        bar.show(view);
    }

    private void scheduleTask(final Task task) {
        Calendar defaultTime = Calendar.getInstance();
        defaultTime.setTimeInMillis(task.startTime.toMillis(false));

        new DefaultDateSlider(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DateSlider view, Calendar selectedDate) {
                task.startTime.year = selectedDate.get(Calendar.YEAR);
                task.startTime.month = selectedDate.get(Calendar.MONTH);
                task.startTime.monthDay = selectedDate.get(Calendar.DAY_OF_MONTH);
                Time now = new Time();
                now.setToNow();
                if (task.startTime.before(now)) {
                    task.startTime = now;
                }
                task.isNew = false;
                task.endTime = new Time(task.startTime);
                taskManager.saveTask(task);
            }
        }, defaultTime).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SETTING, Menu.NONE, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SETTING:
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            return true;
        }
        return false;
    }
}