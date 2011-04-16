package leoliang.tasks365.task;

import java.util.ArrayList;
import java.util.List;

import leoliang.android.util.CursorHelper;
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
        ChangeObserver changeObserver = new ChangeObserver();
        AndroidCalendar calenar = new AndroidCalendar(activity);

        eventifyTaskCursor = calenar.queryNonAllDayEventsByDate(calendarId, date);
        eventifyTaskCursor.registerContentObserver(changeObserver);
        eventifyTaskCursor.registerDataSetObserver(myDataSetObserver);
        activity.startManagingCursor(eventifyTaskCursor);

        normalTaskCursor = calenar.queryAllDayEventsByDate(calendarId, date);
        normalTaskCursor.registerContentObserver(changeObserver);
        normalTaskCursor.registerDataSetObserver(myDataSetObserver);
        activity.startManagingCursor(normalTaskCursor);

        loadTasks();
    }

    private void requery() {
        if (!eventifyTaskCursor.isClosed()) {
            eventifyTaskCursor.requery();
        }
        if (!normalTaskCursor.isClosed()) {
            normalTaskCursor.requery();
        }
    }

    private void loadTasks(Cursor cursor) {
        Log.v(LOG_TAG, "Load tasks: " + CursorHelper.getResultSet(cursor));
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
        Log.v(LOG_TAG, "SingleDayTaskQuery: Loaded " + tasks.size() + " tasks.");
        observer.onResultChanged(tasks);
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            loadTasks();
        }

        @Override
        public void onInvalidated() {
            Log.v(LOG_TAG, "Data set is invalidated");
            observer.onInvalidated();
        }
    }

    private class ChangeObserver extends ContentObserver {

        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(LOG_TAG, "Content changed. Is self change:" + selfChange);
            requery();
        }
    }

}
