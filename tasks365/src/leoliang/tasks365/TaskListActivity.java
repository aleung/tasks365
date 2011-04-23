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
import leoliang.tasks365.task.TaskManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Config;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
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

    private static int HORIZONTAL_SCROLL_THRESHOLD = 50;
    private static final long ANIMATION_DURATION = 400;

    // option menu
    private static final int MENU_SETTING = 1;

    private TaskManager taskManager;
    private MyApplication application;
    private ViewSwitcher viewSwitcher;
    private long calendarId = -1;
    private Time displayDate = new Time();
    GestureDetector flingDetector;


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

        // TODO: set day by savedInstanceState
        gotoToday();

        flingDetector = new GestureDetector(this, new SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.v(LOG_TAG, "TaskListActivity onFling");
                int deltaX = (int) e2.getX() - (int) e1.getX();
                int distanceX = Math.abs(deltaX);
                int deltaY = (int) e2.getY() - (int) e1.getY();
                int distanceY = Math.abs(deltaY);

                if ((distanceX >= HORIZONTAL_SCROLL_THRESHOLD) && (distanceX > distanceY)) {
                    boolean switchForward = initNextView(deltaX);
                    DayTaskListView view = (DayTaskListView) viewSwitcher.getCurrentView();
                    switchViews(switchForward, 0, view.getWidth());
                    return true;
                }
                return false;
            }
        });

        // TODO: move it to background service
        taskManager = new TaskManager(this, application);
        taskManager.dealWithTasksInThePast();
        // end of TODO
    }

    private boolean initNextView(int deltaX) {
        boolean switchForward;
        if (deltaX > 0) {
            switchForward = false;
        } else {
            switchForward = true;
        }

        DayTaskListView view = (DayTaskListView) viewSwitcher.getNextView();
        //view.layout(getLeft(), getTop(), getRight(), getBottom());

        // TODO: next day or previous day

        return switchForward;
    }

    private View switchViews(boolean forward, float xOffSet, float width) {
        float progress = Math.abs(xOffSet) / width;
        if (progress > 1.0f) {
            progress = 1.0f;
        }

        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        if (forward) {
            inFromXValue = 1.0f - progress;
            inToXValue = 0.0f;
            outFromXValue = -progress;
            outToXValue = -1.0f;
        } else {
            inFromXValue = progress - 1.0f;
            inToXValue = 0.0f;
            outFromXValue = progress;
            outToXValue = 1.0f;
        }

        // We have to allocate these animation objects each time we switch views
        // because that is the only way to set the animation parameters.
        TranslateAnimation inAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue, Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue, Animation.ABSOLUTE, 0.0f, Animation.ABSOLUTE, 0.0f);

        // Reduce the animation duration based on how far we have already swiped.
        long duration = (long) (ANIMATION_DURATION * (1.0f - progress));
        inAnimation.setDuration(duration);
        outAnimation.setDuration(duration);
        viewSwitcher.setInAnimation(inAnimation);
        viewSwitcher.setOutAnimation(outAnimation);

        // TODO: release queries in current view
        viewSwitcher.showNext();
        DayTaskListView view = (DayTaskListView) viewSwitcher.getCurrentView();
        view.requestFocus();
        return view;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (application.getCalendarId() != calendarId) {
            gotoToday();
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
    public boolean onTouchEvent(MotionEvent ev) {
        if (flingDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public View makeView() {
        DayTaskListView view = new DayTaskListView(this);
        view.setId(VIEW_ID);
        view.setLayoutParams(new ViewSwitcher.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return view;
    }

    private void gotoToday() {
        DayTaskListView taskListView = (DayTaskListView) viewSwitcher.getCurrentView();
        displayDate.setToNow();
        taskListView.requestFocus();
        taskListView.initialize(displayDate);
    }
}
