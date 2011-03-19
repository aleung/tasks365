package leoliang.tasks365;

import leoliang.tasks365.task.AndroidCalendar;
import leoliang.tasks365.task.Task;
import android.database.Cursor;
import android.util.Log;

public class TaskManager {

    private static final String LOG_TAG = "tasks365";

    private AndroidCalendar calendar;

    public TaskManager(AndroidCalendar calendar) {
        this.calendar = calendar;
    }

    public void dealWithTasksInThePast(int calendarId) {
        Cursor cursor = calendar.queryTasksInPastDays(calendarId);
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

    public void createTask(int calendarId, String title, String description) {
        Task task = new Task();
        task.isNew = true;
        task.calendarId = calendarId;
        task.title = title;
        task.description = description;
        task.scheduleToday();
        calendar.createTask(task);
    }


}
