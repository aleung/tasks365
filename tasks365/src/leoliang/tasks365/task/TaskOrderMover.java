package leoliang.tasks365.task;

import java.util.Calendar;

import android.util.Log;
import android.widget.ListAdapter;

public class TaskOrderMover {
    
    public class MoveNotAllowException extends Exception {
        public MoveNotAllowException(String message) {
            super(message);
        }
    }

    private final static String LOG_TAG = "tasks365";

    private ListAdapter list;
    private TaskManager taskManager;

    public TaskOrderMover(ListAdapter list, TaskManager taskManager) {
        this.list = list;
        this.taskManager = taskManager;
    }

    /**
     * Move task in the list.
     * <p>
     * Tasks in list are order by start time, except that done tasks are always at the end of the list regardless of
     * their start time.
     * <p>
     * Moving a task leads to modification of the start time, start time is set to middle between its previous and its
     * next of the new position. Non all day task has specific start time, which won't be changed during reordering.
     * 
     * @param originalPosition
     * @param newPosition
     * @throws MoveNotAllowException
     */
    public void moveTaskToPosition(int originalPosition, int newPosition) throws MoveNotAllowException {
        Log.d(LOG_TAG, "moveTaskToPosition: " + originalPosition + " -> " + newPosition);

        if (newPosition >= list.getCount()) {
            Log.w(LOG_TAG, "moveTaskToPosition: Invalid new position " + newPosition);
            return;
        }

        int targetPosition = newPosition;
        if (newPosition > originalPosition) {
            targetPosition = newPosition + 1;
        }

        Task taskToBeMoved = (Task) list.getItem(originalPosition);
        if (taskToBeMoved.isDone) {
            throw new MoveNotAllowException("Done task is not allowed to move: " + taskToBeMoved.title);
        }

        int prevItemPosition = targetPosition - 1;
        Task prevItem = null;
        Calendar prevItemTime;
        if (prevItemPosition < 0) {
            prevItemTime = Task.beginOfToday();
        } else {
            prevItem = (Task) list.getItem(prevItemPosition);
            prevItemTime = getTaskStartTime(prevItem);
        }

        int nextItemPosition = (targetPosition == originalPosition) ? targetPosition : targetPosition + 1;
        Task nextItem = null;
        Calendar nextItemTime;
        if (nextItemPosition < list.getCount()) {
            nextItem = (Task) list.getItem(nextItemPosition);
            nextItemTime = getTaskStartTime(nextItem);
        } else {
            nextItemTime = Task.endOfToday();
        }

        Log.v(LOG_TAG,
                "> Prev task: " + Task.formatDate(prevItemTime) + ", next task: " + Task.formatDate(nextItemTime));

        if (prevItemTime.equals(nextItemTime) || !isStartTimeChangable(taskToBeMoved)) {
            if ((prevItem != null) && isStartTimeChangable(prevItem)) {
                moveTaskToPosition(prevItemPosition, prevItemPosition);
                prevItemTime = getTaskStartTime(prevItem);
            } else if ((nextItem != null) && isStartTimeChangable(nextItem)) {
                moveTaskToPosition(nextItemPosition, nextItemPosition);
                nextItemTime = getTaskStartTime(nextItem);
            }
        }

        Log.v(LOG_TAG,
                "< Prev task: " + Task.formatDate(prevItemTime) + ", next task: " + Task.formatDate(nextItemTime));

        if (isStartTimeChangable(taskToBeMoved)) {
            taskToBeMoved.isNew = false;
            taskToBeMoved.startTime
                    .setTimeInMillis((prevItemTime.getTimeInMillis() + nextItemTime.getTimeInMillis()) / 2);
            taskToBeMoved.endTime = (Calendar) taskToBeMoved.startTime.clone();
            taskManager.saveTask(taskToBeMoved);
        }
    }

    private Calendar getTaskStartTime(Task task) {
        if (task.isDone) {
            return Task.endOfToday();
        }
        return task.startTime;
    }

    private boolean isStartTimeChangable(Task task) {
        return !task.isDone && task.isAllDay;
    }
}
