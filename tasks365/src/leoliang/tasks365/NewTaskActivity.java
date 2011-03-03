package leoliang.tasks365;

import java.util.Calendar;

import leoliang.tasks365.calendar.AndroidCalendar;
import leoliang.tasks365.calendar.Task;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class NewTaskActivity extends Activity {

    private AndroidCalendar calendar;
    private EditText editText;

    // Read from preference
    private int calendarId = 14;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_task);

        editText = (EditText) findViewById(R.id.text);

        calendar = new AndroidCalendar(this);

        Button addButton = (Button) findViewById(R.id.addTaskButton);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@SuppressWarnings("unused") View view) {
                addTask();
                finish();
            }
        });
    }

    private void addTask() {
        Task task = new Task();
        task.isNew = true;
        task.calendarId = calendarId;

        String text = editText.getText().toString();
        if (text.length() == 0) {
            // TODO: notify user
            return;
        }
        int firstLineBreakPosition = text.indexOf('\n');
        if (firstLineBreakPosition == -1) {
            task.title = text;
        } else {
            task.title = text.substring(0, firstLineBreakPosition);
            task.description = text.substring(firstLineBreakPosition);
        }

        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, 23);
        time.set(Calendar.MINUTE, 59);
        time.set(Calendar.SECOND, 1);
        time.set(Calendar.MILLISECOND, 0);
        task.startTime = time.getTimeInMillis();
        task.endTime = task.startTime;

        calendar.createTask(task);
    }

}
