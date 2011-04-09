package leoliang.tasks365;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import android.widget.ImageView;
import android.widget.TextView;

public class TaskListAdapter extends BaseAdapter implements QueryResultObserver {

    private static final String LOG_TAG = "tasks365";

    // view type
    private static final int VIEW_TYPE_OPEN_TASK = 0;
    private static final int VIEW_TYPE_FINISHED_TASK = 1;

    private static final DateFormat timeFormatter = new SimpleDateFormat("HH:mm ");

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

    private void bindViewForOpenTask(View view, final Task task) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.title.setText(task.title);
        String scheduleTime = task.isAllDay ? "" : formatTime(task.startTime);
        viewHolder.scheduleTime.setText(scheduleTime);
        viewHolder.tags.setText(task.isNew ? "[new]" : "");
        if (task.isStarred) {
            viewHolder.star.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.star.setVisibility(View.INVISIBLE);
        }
        // TODO: tags, daysToDue
    }

    private String formatTime(Calendar time) {
        return timeFormatter.format(time.getTime());
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
        viewHolder.scheduleTime = (TextView) view.findViewById(R.id.scheduleTime);
        viewHolder.star = (ImageView) view.findViewById(R.id.star);
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
    public class ViewHolder {
        public TextView title;
        public TextView tags;
        public TextView daysToDue;
        public TextView scheduleTime;
        public ImageView star;
    }

}
