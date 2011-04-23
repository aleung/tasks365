package leoliang.tasks365.task;

import java.util.ArrayList;
import java.util.List;

import leoliang.android.util.CursorHelper;
import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.text.format.Time;
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

    public void close() {
        activity.stopManagingCursor(eventifyTaskCursor);
        eventifyTaskCursor.close();
        activity.stopManagingCursor(normalTaskCursor);
        normalTaskCursor.close();
    }

    /**
     * Start the query.
     * <p>
     * Usually you would not reuse a query object. Create another query object for a new query.
     * 
     * @param date
     */
    public void query(Time date) {
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
        Log.v(LOG_TAG, "Requery on cursors");
        if (!eventifyTaskCursor.isClosed()) {
            eventifyTaskCursor.requery();
        }
        if (!normalTaskCursor.isClosed()) {
            normalTaskCursor.requery();
        }
    }

    private void loadTasks(Cursor cursor) {
        if (cursor.isClosed()) {
            return;
        }
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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                observer.onResultChanged(tasks);
            }
        });
    }

    private class MyDataSetObserver extends DataSetObserver {
        private DelayAction action;

        public MyDataSetObserver() {
            action = new DelayAction(new Runnable() {
                @Override
                public void run() {
                    loadTasks();
                }
            }, 500, 2);
        }

        @Override
        public void onChanged() {
            Log.v(LOG_TAG, "Data set is changed");
            action.trigger();
        }

        @Override
        public void onInvalidated() {
            Log.v(LOG_TAG, "Data set is invalidated");
            action.cancel();
            observer.onInvalidated();
        }
    }

    private class ChangeObserver extends ContentObserver {
        private DelayAction action;

        public ChangeObserver() {
            super(new Handler());
            action = new DelayAction(new Runnable() {
                @Override
                public void run() {
                    requery();
                }
            }, 1000, 65536);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(LOG_TAG, "Content changed. Is self change:" + selfChange);
            action.trigger();
        }
    }

}

/**
 * Schedule one-shot action for execution. The action will be executed when it is triggered up to threshold times, or
 * after the specified delay.
 */
class DelayAction {
    private long maxDelayMillis;
    private int triggerThreshold;
    private Runnable action;
    private int triggerCount;
    private Thread timer;

    public DelayAction(Runnable action, long maxDelayMillis, int triggerThreshold) {
        this.action = action;
        this.maxDelayMillis = maxDelayMillis;
        this.triggerThreshold = triggerThreshold;
    }

    public synchronized void trigger() {
        triggerCount++;
        if (triggerCount >= triggerThreshold) {
            new Thread(action).start();
            cancel();
        } else {
            if (timer == null) {
                timer = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(maxDelayMillis);
                        } catch (InterruptedException e) {
                            return;
                        }
                        triggerCount = 0;
                        timer = null;
                        action.run();
                    }
                });
                timer.start();
            }
        }
    }

    public synchronized void cancel() {
        triggerCount = 0;
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
    }
}
