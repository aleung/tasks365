package leoliang.tasks365;

import leoliang.tasks365.task.AndroidCalendar;
import leoliang.tasks365.task.Task;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class TaskManager {

    private static final String LOG_TAG = "tasks365";

    private AndroidCalendar calendar;
    private long calendarId;

    public TaskManager(Context context, long calendarId) {
        calendar = new AndroidCalendar(context);
        this.calendarId = calendarId;
    }

    public void markTaskDone(Task task) {
        task.isDone = true;
        calendar.updateTask(task);
    }

    public void markTaskUndone(Task task) {
        task.isDone = false;
        calendar.updateTask(task);
    }

    public void dealWithTasksInThePast() {
        Cursor cursor = calendar.queryAllDayEventsInPastDays(calendarId);
        dealWithTasksInThePast(cursor);
        cursor.close();

        cursor = calendar.queryNonAllDayEventsInPastDays(calendarId);
        dealWithTasksInThePast(cursor);
        cursor.close();
    }

    private void dealWithTasksInThePast(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            return;
        }
        do {
            Task task = AndroidCalendar.readTask(cursor);
            if (task.isDone) {
                archiveTask(task);
            } else {
                moveTaskToToday(task);
            }
        } while (cursor.moveToNext());
    }

    private void moveTaskToToday(Task task) {
        task.scheduleToday();
        calendar.updateTask(task);
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
        task.calendarId = calendarId;
        task.title = title;
        task.description = description;
        task.scheduleToday();
        calendar.createTask(task);
    }

    public void saveTask(Task task) {
        calendar.updateTask(task);
    }

}
