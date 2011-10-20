package leoliang.tasks365.task;

import java.util.Comparator;

import android.text.format.Time;

/**
 * Sort tasks by status and startTime. Tasks of different status order in: normal, new, done.
 */
public class TaskComparator implements Comparator<Task> {

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

        return Time.compare(task1.startTime, task2.startTime);
    }

}