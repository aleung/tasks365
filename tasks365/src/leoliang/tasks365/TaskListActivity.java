/*
 * Copyright (C) 2011 Leo Liang <leo.liang@gmail.com>
 * Portions Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leoliang.tasks365;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;

import java.util.Calendar;

import leoliang.tasks365.task.TaskManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Config;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ViewSwitcher;

/**
 * Main activity of the application, contains the task list of one day.
 */
public class TaskListActivity extends GDActivity implements ViewSwitcher.ViewFactory {

    private static final String LOG_TAG = "tasks365";

    /**
     * The view id used for all the views we create. It's OK to have all child views have the same ID. This ID is used
     * to pick which view receives focus when a view hierarchy is saved / restore
     */
    private static final int VIEW_ID = 1;

    // option menu
    private static final int MENU_SETTING = 1;

    private TaskManager taskManager;
    private MyApplication application;
    private ViewSwitcher viewSwitcher;
    private long calendarId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.v(LOG_TAG, "onCreate()");
        }

        application = (MyApplication) getApplication();
        validateCalendar();

        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.main);

        addActionBarItem(Type.Add, R.id.action_bar_add);

        viewSwitcher = (ViewSwitcher) findViewById(R.id.switcher);
        viewSwitcher.setFactory(this);
        DayTaskListView taskListView = (DayTaskListView) viewSwitcher.getCurrentView();
        taskListView.requestFocus();

        // TODO: set day by savedInstanceState
        taskListView.initialize(Calendar.getInstance());

        // TODO: move it to background service
        taskManager = new TaskManager(this, application);
        taskManager.dealWithTasksInThePast();
        // end of TODO
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (application.getCalendarId() != calendarId) {
            DayTaskListView taskListView = (DayTaskListView) viewSwitcher.getCurrentView();
            taskListView.initialize(Calendar.getInstance());
        }
    }

    private void validateCalendar() {
        if (application.getCalendarId() == -1) {
            startActivity(new Intent(application, CalendarChooseActivity.class));
        }
        // TODO: check calendar existence, is synced
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        switch (item.getItemId()) {
        case R.id.action_bar_add:
            startActivity(new Intent(getApplicationContext(), NewTaskActivity.class));
            return true;
        default:
            return super.onHandleActionBarItemClick(item, position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SETTING, Menu.NONE, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SETTING:
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public View makeView() {
        DayTaskListView view = new DayTaskListView(this);
        view.setId(VIEW_ID);
        view.setLayoutParams(new ViewSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return view;
    }
}
