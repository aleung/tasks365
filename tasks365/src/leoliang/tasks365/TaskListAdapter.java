package leoliang.tasks365;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import leoliang.tasks365.task.QueryResultObserver;
import leoliang.tasks365.task.Task;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TaskListAdapter extends BaseAdapter implements QueryResultObserver {

    private static final String LOG_TAG = "tasks365";
    private static final int VIEW_TYPE_OPEN_TASK = 0;
    private static final int VIEW_TYPE_FINISHED_TASK = 1;

    private List<Task> tasks;
    private LayoutInflater inflater;

    /**
     * Creates a new instance of <code>OneDayListAdapter</code>.
     * 
     * @param context
     */
    public TaskListAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        if (tasks == null) {
            return 0;
        }
        return tasks.size();
    }

    @Override
    public Task getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        Task task = getItem(position);
        return task.isDone ? VIEW_TYPE_FINISHED_TASK : VIEW_TYPE_OPEN_TASK;
    }

    @Override
    public int getViewTypeCount() {
        // we have two view type: VIEW_TYPE_OPEN_TASK, VIEW_TYPE_FINISHED_TASK
        return 2;
    }

    @Override
    public View getView(int position, View convertView, @SuppressWarnings("unused") ViewGroup parent) {
        Task task = getItem(position);
        View v;
        if (convertView == null) {
            v = newView(task);
        } else {
            v = convertView;
        }
        bindView(v, task);
        return v;
    }

    private void bindView(View view, Task task) {
        if (task.isDone) {
            bindViewForCompletedTask(view, task);
        } else {
            bindViewForOpenTask(view, task);
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
        String tags = "";
        if (!task.isAllDay) {
            tags += formatStartAndEndTime(task.startTime, task.endTime);
        }
        if (task.isNew) {
            tags += " [NEW]";
        }
        viewHolder.tags.setText(tags);
        // TODO: tags, daysToDue
    }

    private String formatStartAndEndTime(Calendar startTime, Calendar endTime) {
        DateFormat formatter = DateFormat.getTimeInstance();
        return formatter.format(startTime.getTime()) + " - " + formatter.format(endTime.getTime());
    }

    private View newView(Task task) {
        if (task.isDone) {
            return newViewForCompletedTask();
        } else {
            return newViewForOpenTask();
        }
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

    @Override
    public void onResultChanged(List<Task> result) {
        tasks = result;
        notifyDataSetChanged();
    }

    @Override
    public void onInvalidated() {
        notifyDataSetInvalidated();
    }


    /**
     * Cache the sub views inside a list item view.
     */
    private class ViewHolder {
        public TextView title;
        public TextView tags;
        public TextView daysToDue;
    }

}
