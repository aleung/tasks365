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
import android.app.Activity;
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
    private Activity activity;
    private MyApplication application;
    private SingleDayTaskQuery query;
    private TaskManager taskManager;

    public DayTaskListView(Activity activity) {
        super(activity, null);
        this.activity = activity;
        application = (MyApplication) activity.getApplication();
        taskManager = new TaskManager(activity, application);
    }

    public void initialize(Calendar date) {
        adapter = new TaskListAdapter(activity);
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
                    Toast.makeText(activity, R.string.done_task_no_move, Toast.LENGTH_LONG);
                }
            }
        });

    }

    private void initializeQuery(Calendar date) {
        long calendarId = application.getCalendarId();
        query = new SingleDayTaskQuery(activity, calendarId, adapter);
        query.query(date);
    }

    private void showQuickActionBar(View view, final int position) {
        final Task task = adapter.getItem(position);
        QuickActionBar bar = new QuickActionBar(activity);
        if (task.isDone) {
            bar.addQuickAction(new QuickAction(MENU_MARK_TASK_UNDONE, activity, R.drawable.checkbox_unchecked,
                    R.string.mark_task_undone));
        } else {
            if (!task.isPinned()) {
                bar.addQuickAction(new QuickAction(MENU_MARK_TASK_DONE, activity, R.drawable.checkbox_checked,
                        R.string.mark_task_done));
                bar.addQuickAction(new QuickAction(MENU_SCHEDULE_TASK, activity, R.drawable.calendar,
                        R.string.schedule_task));
                bar.addQuickAction(new QuickAction(MENU_EDIT_TASK, activity, R.drawable.pencil,
                        R.string.edit_task));
            }
            if (task.isStarred) {
                bar.addQuickAction(new QuickAction(MENU_UNSTAR_TASK, activity, R.drawable.unstar,
                        R.string.unstar_task));
            } else {
                bar.addQuickAction(new QuickAction(MENU_STAR_TASK, activity, R.drawable.star_semi_empty,
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
        new DefaultDateSlider(activity, new OnDateSetListener() {
            @Override
            public void onDateSet(@SuppressWarnings("unused") DateSlider view, Calendar selectedDate) {
                task.startTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
                task.startTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
                task.startTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
                Calendar now = Calendar.getInstance();
                if (task.startTime.before(now)) {
                    task.startTime = now;
                }
                task.isNew = false;
                task.endTime = (Calendar) task.startTime.clone();
                taskManager.saveTask(task);
            }
        }, task.startTime).show();
    }


}
