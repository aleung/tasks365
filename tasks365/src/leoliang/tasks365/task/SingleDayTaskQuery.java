package leoliang.tasks365.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.Log;

/**
 * Query all tasks that scheduled in a specific day, the task can be normal task or eventify task.
 */
public class SingleDayTaskQuery {

    private static final String LOG_TAG = "tasks365";

    private List<Task> tasks = new ArrayList<Task>();
    private Activity activity;
    private long calendarId;
    private Cursor eventifyTaskCursor;
    private Cursor normalTaskCursor;
    private QueryResultObserver observer;

    /**
     * Creates a new instance of <code>SingleDayTaskQuery</code>.
     * 
     * @param activity - the activity which owns the query, the cursors back of this query are managed by the activity
     * @param calendarId
     * @param observer
     */
    public SingleDayTaskQuery(Activity activity, long calendarId, QueryResultObserver observer) {
        this.activity = activity;
        this.observer = observer;
        this.calendarId = calendarId;
    }

    /**
     * Start the query.
     * <p>
     * Usually you would not reuse a query object. Create another query object for a new query.
     * 
     * @param date
     */
    public void query(java.util.Calendar date) {
        MyDataSetObserver myDataSetObserver = new MyDataSetObserver();
        AndroidCalendar calenar = new AndroidCalendar(activity);

        eventifyTaskCursor = calenar.queryNonAllDayEventsByDate(calendarId, date);
        eventifyTaskCursor.registerContentObserver(new ChangeObserver(eventifyTaskCursor));
        eventifyTaskCursor.registerDataSetObserver(myDataSetObserver);
        activity.startManagingCursor(eventifyTaskCursor);

        normalTaskCursor = calenar.queryAllDayEventsByDate(calendarId, date);
        normalTaskCursor.registerContentObserver(new ChangeObserver(normalTaskCursor));
        normalTaskCursor.registerDataSetObserver(myDataSetObserver);
        activity.startManagingCursor(normalTaskCursor);

        loadTasks();
    }

    private void loadTasks(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            Log.v(LOG_TAG, "Cursor is empty");
            return;
        }
        do {
            Task task = AndroidCalendar.readTask(cursor);
            tasks.add(task);
        } while (cursor.moveToNext());
    }

    private void loadTasks() {
        tasks.clear();
        loadTasks(normalTaskCursor);
        loadTasks(eventifyTaskCursor);
        Collections.sort(tasks, new TaskComparator());
        Log.v(LOG_TAG, "Loaded " + tasks.size() + " tasks.");
        observer.onResultChanged(Collections.unmodifiableList(tasks));
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            Log.v(LOG_TAG, "Cursor data set is changed");
            loadTasks();
        }

        @Override
        public void onInvalidated() {
            Log.v(LOG_TAG, "Data set is invalidated");
            observer.onInvalidated();
        }
    }

    private class ChangeObserver extends ContentObserver {
        Cursor cursor;

        public ChangeObserver(Cursor cursor) {
            super(new Handler());
            this.cursor = cursor;
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(LOG_TAG, "Content changed. Is self change:" + selfChange);
            if (!cursor.isClosed()) {
                Log.v(LOG_TAG, "Auto requerying due to content is changed");
                // TODO: set a flag, delay 10 seconds before requery
                cursor.requery();
            }
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

            return task1.startTime.compareTo(task2.startTime);
        }

    }

}
