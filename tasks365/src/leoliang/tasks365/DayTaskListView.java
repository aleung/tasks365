package leoliang.tasks365;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionBar;
import greendroid.widget.QuickActionWidget;
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener;

import java.util.Calendar;

import leoliang.tasks365.TaskListAdapter.ViewHolder;
import leoliang.tasks365.task.SingleDayTaskQuery;
import leoliang.tasks365.task.Task;
import leoliang.tasks365.task.TaskManager;
import leoliang.tasks365.task.TaskOrderMover;
import leoliang.tasks365.task.TaskOrderMover.MoveNotAllowException;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.googlecode.android.widgets.DateSlider.DateSlider;
import com.googlecode.android.widgets.DateSlider.DateSlider.OnDateSetListener;
import com.googlecode.android.widgets.DateSlider.DefaultDateSlider;

public class DayTaskListView extends DraggableListView {

    // quick action menu
    private static final int MENU_MARK_TASK_DONE = 1;
    private static final int MENU_MARK_TASK_UNDONE = 2;
    private static final int MENU_SCHEDULE_TASK = 3;
    private static final int MENU_EDIT_TASK = 4;
    private static final int MENU_STAR_TASK = 5;
    private static final int MENU_UNSTAR_TASK = 6;

    private TaskListAdapter adapter;
    private TaskListActivity parentActivity;
    private MyApplication application;
    private SingleDayTaskQuery query;
    private TaskManager taskManager;

    public DayTaskListView(TaskListActivity activity) {
        super(activity, null);
        parentActivity = activity;
        application = (MyApplication) activity.getApplication();
        taskManager = new TaskManager(parentActivity, application);
    }

    public void initialize(Time date) {
        adapter = new TaskListAdapter(parentActivity);
        initializeQuery(date);

        setAdapter(adapter);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                showQuickActionBar(viewHolder.title, position);
            }
        });
        setDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                try {
                    new TaskOrderMover(adapter, taskManager).moveTaskToPosition(from, to);
                    adapter.notifyDataSetChanged();
                } catch (MoveNotAllowException e) {
                    Toast.makeText(parentActivity, R.string.done_task_no_move, Toast.LENGTH_LONG);
                }
            }
        });

    }

    public void terminate() {
        if (query != null) {
            query.close();
        }
        query = null;
    }

    private void initializeQuery(Time date) {
        long calendarId = application.getCalendarId();
        query = new SingleDayTaskQuery(parentActivity, calendarId, adapter);
        query.query(date);
    }

    private void showQuickActionBar(View view, final int position) {
        final Task task = adapter.getItem(position);
        QuickActionBar bar = new QuickActionBar(parentActivity);
        if (task.isDone) {
            bar.addQuickAction(new QuickAction(MENU_MARK_TASK_UNDONE, parentActivity, R.drawable.checkbox_unchecked,
                    R.string.mark_task_undone));
        } else {
            if (!task.isPinned()) {
                bar.addQuickAction(new QuickAction(MENU_MARK_TASK_DONE, parentActivity, R.drawable.checkbox_checked,
                        R.string.mark_task_done));
                bar.addQuickAction(new QuickAction(MENU_SCHEDULE_TASK, parentActivity, R.drawable.calendar,
                        R.string.schedule_task));
                bar.addQuickAction(new QuickAction(MENU_EDIT_TASK, parentActivity, R.drawable.pencil,
                        R.string.edit_task));
            }
            if (task.isStarred) {
                bar.addQuickAction(new QuickAction(MENU_UNSTAR_TASK, parentActivity, R.drawable.unstar,
                        R.string.unstar_task));
            } else {
                bar.addQuickAction(new QuickAction(MENU_STAR_TASK, parentActivity, R.drawable.star_semi_empty,
                        R.string.star_task));
            }
        }
        bar.setOnQuickActionClickListener(new OnQuickActionClickListener() {
            @Override
            public void onQuickActionClicked(@SuppressWarnings("unused") QuickActionWidget widget, int actionId) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        builder.setTitle("Schedule to:");
        CharSequence[] items = {"Today", "Tomorrow", "Day after tomorrow", "Weekend", "Next week", "A month later", "Pick a day ..."};
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                Time date = new Time();
                date.setToNow();
                int advanceDays = 0;
                switch (item) {
                case 1: // tomorrow
                    advanceDays = 1;
                    break;
                case 2: // day after tomorrow
                    advanceDays = 2;
                    break;
                case 3: // weekend (Saturday or Sunday)
                    if ((date.weekDay != Time.SATURDAY) && (date.weekDay != Time.SUNDAY)) {
                        advanceDays = Time.SATURDAY - date.weekDay;
                    }
                    break;
                case 4: // next Monday
                    advanceDays = Time.MONDAY - date.weekDay;
                    if (advanceDays <= 0) {
                        advanceDays += 7;
                    }
                    break;
                case 5: // a month later
                    advanceDays = 30;
                    break;
                case 6: // pick a day
                    scheduleTaskWithDatePicker(task);
                    advanceDays = -1;
                    break;
                }

                if (advanceDays >= 0) {
                    date.monthDay += advanceDays;
                    task.startTime.year = date.year;
                    task.startTime.month = date.month;
                    task.startTime.monthDay = date.monthDay;
                    task.startTime.normalize(false);
                    task.endTime = new Time(task.startTime);
                    task.isNew = false;
                    taskManager.saveTask(task);
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void scheduleTaskWithDatePicker(final Task task) {
        Calendar defaultTime = Calendar.getInstance();
        defaultTime.setTimeInMillis(task.startTime.toMillis(false));

        new DefaultDateSlider(parentActivity, new OnDateSetListener() {
            @Override
            public void onDateSet(@SuppressWarnings("unused") DateSlider view, Calendar selectedDate) {
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
    public boolean onTouchEvent(MotionEvent ev) {
        if (parentActivity.flingDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

}
