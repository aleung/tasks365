package leoliang.tasks365;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import leoliang.tasks365.calendar.AndroidCalendar;
import leoliang.tasks365.calendar.Task;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OneDayListAdapter extends BaseAdapter {

    private static final String LOG_TAG = "tasks365";
    private static final int VIEW_TYPE_OPEN_TASK = 0;
    private static final int VIEW_TYPE_FINISHED_TASK = 1;

    private List<Task> tasks = new ArrayList<Task>();
    private LayoutInflater inflater;
    private Cursor cursor;
    private ChangeObserver changeObserver;
    private DataSetObserver dataSetObserver = new MyDataSetObserver();

    /**
     * Creates a new instance of <code>OneDayListAdapter</code>.
     * 
     * @param context
     * @param cursor
     */
    public OneDayListAdapter(Context context, Cursor cursor) {
        this.cursor = cursor;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadTasks();
        changeObserver = new ChangeObserver();
        cursor.registerContentObserver(changeObserver);
        cursor.registerDataSetObserver(dataSetObserver);
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Task getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        Task task = getItem(position);
        return task.isDone ? VIEW_TYPE_FINISHED_TASK : VIEW_TYPE_OPEN_TASK;
    }

    @Override
    public int getViewTypeCount() {
        // we have two view type: VIEW_TYPE_OPEN_TASK, VIEW_TYPE_FINISHED_TASK
        return 2;
    }

    @Override
    public View getView(int position, View convertView, @SuppressWarnings("unused") ViewGroup parent) {
        Task task = getItem(position);
        View v;
        if (convertView == null) {
            v = newView(task);
        } else {
            v = convertView;
        }
        bindView(v, task);
        return v;
    }

    private void bindView(View view, Task task) {
        if (task.isDone) {
            bindViewForCompletedTask(view, task);
        } else {
            bindViewForOpenTask(view, task);
        }
    }

    private void bindViewForCompletedTask(View view, Task task) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.title.setText(task.title);
        viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }

    private void bindViewForOpenTask(View view, Task task) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.title.setText(task.title);
        String tags = formatStartAndEndTime(task.startTime, task.endTime);
        if (task.isNew) {
            tags += " [NEW]";
        }
        viewHolder.tags.setText(tags);
        // TODO: tags, daysToDue
    }

    private String formatStartAndEndTime(long startTime, long endTime) {
        DateFormat formatter = DateFormat.getTimeInstance();
        return formatter.format(new Date(startTime)) + " - " + formatter.format(new Date(endTime));
    }

    private View newView(Task task) {
        if (task.isDone) {
            return newViewForCompletedTask();
        } else {
            return newViewForOpenTask();
        }
    }

    private View newViewForCompletedTask() {
        ViewHolder viewHolder = new ViewHolder();
        View view = inflater.inflate(R.layout.list_task_done, null);
        viewHolder.title = (TextView) view.findViewById(R.id.taskTitle);
        view.setTag(viewHolder);
        return view;
    }

    private View newViewForOpenTask() {
        ViewHolder viewHolder = new ViewHolder();
        View view = inflater.inflate(R.layout.list_task_open, null);
        viewHolder.title = (TextView) view.findViewById(R.id.taskTitle);
        viewHolder.daysToDue = (TextView) view.findViewById(R.id.daysToDue);
        viewHolder.tags = (TextView) view.findViewById(R.id.tags);
        view.setTag(viewHolder);
        return view;
    }

    private void loadTasks() {
        tasks.clear();
        if (!cursor.moveToFirst()) {
            return;
        }
        do {
            Task task = AndroidCalendar.readTask(cursor);
            // TODO
            //if (task.isScheduledToday()) {
                tasks.add(task);
            //}
        } while (cursor.moveToNext());
        Collections.sort(tasks, new TaskComparator());
        Log.v(LOG_TAG, "Loaded " + tasks.size() + " tasks.");
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification. The default implementation
     * provides the auto-requery logic, but may be overridden by sub classes.
     * 
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        if (!cursor.isClosed()) {
            Log.v(LOG_TAG, "Auto requerying due to content is changed");
            // TODO: set a flag, delay 10 seconds before requery
            cursor.requery();
        }
    }

    /**
     * Cache the sub views inside a list item view.
     */
    private class ViewHolder {
        public TextView title;
        public TextView tags;
        public TextView daysToDue;
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(LOG_TAG, "Content changed. Is self change:" + selfChange);
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            Log.v(LOG_TAG, "Data set is changed");
            loadTasks();
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            Log.v(LOG_TAG, "Data set is invalidated");
            notifyDataSetInvalidated();
        }
    }

    /**
     * Sort tasks by status and startTime. Tasks of different status order in: normal, new, done.
     */
    private class TaskComparator implements Comparator<Task> {

        @Override
        public int compare(Task task1, Task task2) {
            if (task1.isDone != task2.isDone) {
                if (task1.isDone) {
                    return 1;
                }
                return -1;
            }

            if (task1.isNew != task2.isNew) {
                if (task1.isNew) {
                    return 1;
                }
                return -1;
            }

            if (task1.startTime < task2.startTime) {
                return -1;
            }
            if (task1.startTime > task2.startTime) {
                return 1;
            }
            return 0;
        }

    }
}
