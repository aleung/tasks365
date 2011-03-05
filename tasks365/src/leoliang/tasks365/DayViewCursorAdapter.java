package leoliang.tasks365;

import java.text.DateFormat;
import java.util.Date;

import leoliang.tasks365.calendar.AndroidCalendar;
import leoliang.tasks365.calendar.Task;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;


public class DayViewCursorAdapter extends CursorAdapter {

    class ViewHolder {
        public TextView title;
        public TextView tags;
        public TextView daysToDue;
    }

    private static final String DEBUG_TAG = "tasks365.DayViewCursorAdapter";
    private static final int VIEW_TYPE_OPEN_TASK = 0;

    private static final int VIEW_TYPE_FINISHED_TASK = 1;

    private LayoutInflater inflater;

    public DayViewCursorAdapter(Context context, Cursor c) {
        super(context, c, true);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log.v(DEBUG_TAG, "bindView, position=" + cursor.getPosition());
        Task task = AndroidCalendar.readTask(cursor);
        if (task.isDone) {
            bindViewForCompletedTask(view, task);
        } else {
            bindViewForOpenTask(view, task);
        }
    }

    @Override
    public int getItemViewType (int position){
        Log.v(DEBUG_TAG, "getItemViewType, position=" + position);
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        Task task = AndroidCalendar.readTask(cursor);
        return task.isDone ? VIEW_TYPE_FINISHED_TASK : VIEW_TYPE_OPEN_TASK;
    }

    @Override
    public int getViewTypeCount(){
        // we have two view type: VIEW_TYPE_OPEN_TASK, VIEW_TYPE_FINISHED_TASK
        return 2;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.v(DEBUG_TAG, "newView, position=" + cursor.getPosition());
        Task task = AndroidCalendar.readTask(cursor);
        if (task.isDone) {
            return newViewForCompletedTask();
        } else {
            return newViewForOpenTask();
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
        viewHolder.tags.setText(formatStartAndEndTime(task.startTime, task.endTime));
        // TODO: tags, daysToDue
    }

    private String formatStartAndEndTime(long startTime, long endTime) {
        DateFormat formatter = DateFormat.getTimeInstance();
        return formatter.format(new Date(startTime)) + " - " + formatter.format(new Date(endTime));
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
}
