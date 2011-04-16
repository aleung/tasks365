package leoliang.tasks365.task;

import java.util.Calendar;

import leoliang.tasks365.MyApplication;
import leoliang.tasks365.task.AndroidCalendar.OperationFailure;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class TaskManager {

    private static final String LOG_TAG = "tasks365";

    private MyApplication application;
    private AndroidCalendar calendar;

    public TaskManager(Context context, MyApplication application) {
        calendar = new AndroidCalendar(context);
        this.application = application;
    }

    public void markTaskDone(Task task) {
        task.isDone = true;
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }

    public void markTaskUndone(Task task) {
        task.isDone = false;
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }

    public void dealWithTasksInThePast() {
        long calendarId = application.getCalendarId();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(application.getLastRunTime());
        Cursor cursor;

        while (date.before(AndroidCalendar.beginOfToday())) {
            cursor = calendar.queryAllDayEventsByDate(calendarId, date);
            dealWithTasksInThePast(cursor);
            cursor.close();

            cursor = calendar.queryNonAllDayEventsByDate(calendarId, date);
            dealWithTasksInThePast(cursor);
            cursor.close();

            date.add(java.util.Calendar.DATE, 1);
            application.setLastRunTime(date.getTimeInMillis());
        }
        application.setLastRunTime(System.currentTimeMillis());
    }

    private void dealWithTasksInThePast(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }
        do {
            Task task = AndroidCalendar.readTask(cursor);
            if (!task.isRecurrentEvent) {
                if (task.isDone) {
                    archiveTask(task);
                } else {
                    moveTaskToToday(task);
                }
            }
        } while (cursor.moveToNext());
    }

    private void moveTaskToToday(Task task) {
        task.scheduleToday();
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }

    /**
     * No archive, delete directly.
     * 
     * @param task
     */
    private void archiveTask(Task task) {
        Log.v(LOG_TAG, "Archive task: " + task);
        calendar.deleteTask(task.id);
    }

    public void createTask(String title, String description) {
        Task task = new Task();
        task.isNew = true;
        task.calendarId = application.getCalendarId();
        task.title = title;
        task.description = description;
        task.scheduleToday();
        calendar.createTask(task);
    }

    public void saveTask(Task task) {
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }

    public void starTask(Task task) {
        task.isStarred = true;
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }

    public void unstarTask(Task task) {
        task.isStarred = false;
        try {
            calendar.updateTask(task);
        } catch (OperationFailure e) {
            // TODO Auto-generated catch block
            Log.w(LOG_TAG, e);
        }
    }
}
