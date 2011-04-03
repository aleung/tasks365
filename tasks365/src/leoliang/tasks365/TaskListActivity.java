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
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.DatePicker;

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
                if (from == to) {
                    return;
                }
                moveTaskToPosition(from, to);
            }
        });

        // TODO: move it to background service
        taskManager = new TaskManager(this, application.getCalendarId());
        taskManager.dealWithTasksInThePast();
        // end of TODO
    }

    /**
     * The list is sort by start time of the tasks. To move a task, the start time has to be updated.
     * 
     * @param originalPosition
     * @param newPosition
     */
    private void moveTaskToPosition(int originalPosition, int newPosition) {
        Log.d(LOG_TAG, "moveTaskToPosition: " + originalPosition + " -> " + newPosition);

        if (newPosition >= adapter.getCount()) {
            Log.w(LOG_TAG, "moveTaskToPosition: Invalid new position " + newPosition);
            return;
        }

        int targetPosition = newPosition;
        if (newPosition > originalPosition) {
            targetPosition = newPosition + 1;
        }

        Task taskToBeMoved = adapter.getItem(originalPosition);
        if (!taskToBeMoved.isAllDay || taskToBeMoved.isDone) {
            // TODO: notify user: non all day task and done task isn't draggable
            Log.i(LOG_TAG, "Non all day task and done task is not allow to change start time.");
            return;
        }

        Task prevItem = null;
        Calendar prevItemTime;
        if (targetPosition == 0) {
            prevItemTime = Task.beginOfToday();
        } else {
            prevItem = adapter.getItem(targetPosition - 1);
            prevItemTime = getTaskStartTime(prevItem);
        }

        Calendar nextItemTime;
        if (targetPosition < adapter.getCount()) {
            nextItemTime = getTaskStartTime(adapter.getItem(targetPosition));
        } else {
            nextItemTime = Task.endOfToday();
        }

        if (prevItemTime.equals(nextItemTime)) {
            if ((targetPosition > 0) && prevItem.isAllDay && !prevItem.isDone) {
                moveTaskToPosition(targetPosition - 1, targetPosition - 1);
            }
            prevItemTime = getTaskStartTime(prevItem);
        }

        taskToBeMoved.isNew = false;
        taskToBeMoved.startTime.setTimeInMillis((prevItemTime.getTimeInMillis() + nextItemTime.getTimeInMillis()) / 2);
        taskToBeMoved.endTime = (Calendar) taskToBeMoved.startTime.clone();
        taskManager.saveTask(taskToBeMoved);
    }

    private Calendar getTaskStartTime(Task task) {
        if (task.isDone) {
            return Task.endOfToday();
        }
        return task.startTime;
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
        query.query(Calendar.getInstance());
    }

    private void showQuickActionBar(View view, final int position) {
        final Task task = adapter.getItem(position);
        QuickActionBar bar = new QuickActionBar(this);
        if (task.isDone) {
            bar.addQuickAction(new QuickAction(MENU_MARK_TASK_UNDONE, this, R.drawable.gd_action_bar_export,
                    R.string.mark_task_undone));
        } else {
            bar.addQuickAction(new QuickAction(MENU_MARK_TASK_DONE, this, R.drawable.gd_action_bar_export,
                    R.string.mark_task_done));
            if (task.isStarred) {
                bar.addQuickAction(new QuickAction(MENU_UNSTAR_TASK, this, R.drawable.action_star_empty,
                        R.string.unstar_task));
            } else {
                bar.addQuickAction(new QuickAction(MENU_STAR_TASK, this, R.drawable.action_star,
                        R.string.star_task));
            }
            bar.addQuickAction(new QuickAction(MENU_SCHEDULE_TASK, this, R.drawable.gd_action_bar_edit,
                    R.string.schedule_task));
            bar.addQuickAction(new QuickAction(MENU_EDIT_TASK, this, R.drawable.gd_action_bar_edit, R.string.edit_task));
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
            }
        });
        bar.show(view);
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