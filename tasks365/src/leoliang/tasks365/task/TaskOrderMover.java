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
        if (originalPosition == newPosition) {
            return;
        }
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
            prevItemTime = AndroidCalendar.beginOfToday();
        } else {
            prevItem = (Task) list.getItem(prevItemPosition);
            prevItemTime = getTaskStartTime(prevItem);
        }

        int nextItemPosition = targetPosition;
        Task nextItem = null;
        Calendar nextItemTime;
        if (nextItemPosition < list.getCount()) {
            nextItem = (Task) list.getItem(nextItemPosition);
            nextItemTime = getTaskStartTime(nextItem);
        } else {
            nextItemTime = AndroidCalendar.endOfToday();
        }

        Log.v(LOG_TAG,
                "> Prev task: " + Task.formatDate(prevItemTime) + ", next task: " + Task.formatDate(nextItemTime));

        if (isStartTimeChangable(taskToBeMoved)) {
            if (prevItemTime.equals(nextItemTime)) {
                if (prevItem != null) {
                    bringForwardTask(prevItemPosition, nextItemTime, originalPosition);
                    prevItemTime = getTaskStartTime(prevItem);
                } else if (nextItem != null) {
                    putOffTask(nextItemPosition, prevItemTime, originalPosition);
                    nextItemTime = getTaskStartTime(nextItem);
                }
            }
            Log.v(LOG_TAG,
                    "< Prev task: " + Task.formatDate(prevItemTime) + ", next task: " + Task.formatDate(nextItemTime));

            taskToBeMoved.isNew = false;
            taskToBeMoved.startTime
                    .setTimeInMillis((prevItemTime.getTimeInMillis() + nextItemTime.getTimeInMillis()) / 2);
            taskToBeMoved.endTime = (Calendar) taskToBeMoved.startTime.clone();
            taskManager.saveTask(taskToBeMoved);
        } else {
            if (!prevItemTime.before(taskToBeMoved.startTime)) {
                bringForwardTask(prevItemPosition, taskToBeMoved.startTime, originalPosition);
            }
            if (!nextItemTime.after(taskToBeMoved.startTime)) {
                putOffTask(nextItemPosition, taskToBeMoved.startTime, originalPosition);
            }
        }
    }

    /**
     * Bring forward the start time of a task. May also bring forward previous tasks in the list, to keep the order
     * unchanged.
     * 
     * @param itemPosition - the position of task to be brought forward
     * @param time - before when bring forward to
     * @param ignoreItemPosition - skip this item
     */
    private void bringForwardTask(int itemPosition, Calendar time, int ignoreItemPosition) {
        if (itemPosition < 0) {
            return;
        }
        Task task = (Task) list.getItem(itemPosition);
        if (!isStartTimeChangable(task)) {
            return;
        }
        int prevItemPosition = itemPosition - 1;
        if (prevItemPosition == ignoreItemPosition) {
            prevItemPosition -= 1;
        }
        Calendar prevItemTime;
        Task prevItem = null;
        if (prevItemPosition < 0) {
            prevItemTime = AndroidCalendar.beginOfToday();
        } else {
            prevItem = (Task) list.getItem(prevItemPosition);
            prevItemTime = getTaskStartTime(prevItem);
        }

        if (!prevItemTime.before(time)) {
            bringForwardTask(prevItemPosition, time, ignoreItemPosition);
        }

        task.startTime.setTimeInMillis((prevItemTime.getTimeInMillis() + time.getTimeInMillis()) / 2);
        task.endTime = (Calendar) task.startTime.clone();
        Log.v(LOG_TAG, "Bring forward task " + task.id);
        taskManager.saveTask(task);
    }

    /**
     * Put off the start time of a task. May also put off next tasks in the list, to keep the order unchanged.
     * 
     * @param itemPosition - the position of task to be put off
     * @param time - after when put off to
     * @param ignoreItemPosition - skip this item
     */
    private void putOffTask(int itemPosition, Calendar time, int ignoreItemPosition) {
        int lastPositionInList = list.getCount() - 1;
        if (itemPosition > lastPositionInList) {
            return;
        }
        Task task = (Task) list.getItem(itemPosition);
        if (!isStartTimeChangable(task)) {
            return;
        }
        int nextItemPosition = itemPosition + 1;
        if (nextItemPosition == ignoreItemPosition) {
            nextItemPosition += 1;
        }
        Calendar nextItemTime;
        Task nextItem = null;
        if (nextItemPosition > lastPositionInList) {
            nextItemTime = AndroidCalendar.endOfToday();
        } else {
            nextItem = (Task) list.getItem(nextItemPosition);
            nextItemTime = getTaskStartTime(nextItem);
        }

        if (!nextItemTime.after(time)) {
            putOffTask(nextItemPosition, time, ignoreItemPosition);
        }

        task.startTime.setTimeInMillis((nextItemTime.getTimeInMillis() + time.getTimeInMillis()) / 2);
        task.endTime = (Calendar) task.startTime.clone();
        Log.v(LOG_TAG, "Put off task " + task.id);
        taskManager.saveTask(task);
    }


    private Calendar getTaskStartTime(Task task) {
        if (task.isDone) {
            return AndroidCalendar.endOfToday();
        }
        return task.startTime;
    }

    private boolean isStartTimeChangable(Task task) {
        return !task.isDone && task.isAllDay;
    }
}
