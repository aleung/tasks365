package leoliang.tasks365;

import leoliang.tasks365.task.TaskManager;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class NewTaskActivity extends Activity {

    private TaskManager taskManager;
    private EditText editText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_task);

        editText = (EditText) findViewById(R.id.text);

        final MyApplication app = (MyApplication) getApplication();
        taskManager = new TaskManager(this, app.getCalendarId());

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
        String title;
        String description = null;
        String text = editText.getText().toString();
        if (text.length() == 0) {
            // TODO: notify user
            return;
        }
        int firstLineBreakPosition = text.indexOf('\n');
        if (firstLineBreakPosition == -1) {
            title = text;
        } else {
            title = text.substring(0, firstLineBreakPosition);
            description = text.substring(firstLineBreakPosition);
        }

        taskManager.createTask(title, description);
    }

}
