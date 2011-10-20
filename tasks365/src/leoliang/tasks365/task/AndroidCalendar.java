/*
 * Copyright (c) 2010, Lauren Darcey and Shane Conder
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this list of
 *   conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 *   of conditions and the following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 * 
 * * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific prior
 *   written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * <ORGANIZATION> = Mamlambo
 */
package leoliang.tasks365.task;

import leoliang.tasks365.calendar.Calendar;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class AndroidCalendar {

    public class OperationFailure extends Exception {
        public OperationFailure(String message) {
            super(message);
        }
    }

    private static final String LOG_TAG = "tasks365";

    /**
     * Value to add to the day number find the Julian Day number.
     * 
     * This is the Julian Day number for 1/1/1970.
     * 
     * @see "http://www.hermetic.ch/cal_stud/jdn.htm"
     */
    private static final int EPOCH_UNIX_ERA_DAY = 2440588;

    /**
     * What columns are in the result of event query methods.
     */
    private static final String[] TASK_COLUMNS = { Calendar.Events._ID, Calendar.Events.CALENDAR_ID,
            Calendar.Events.TITLE, Calendar.Events.DESCRIPTION, Calendar.Events.DTSTART, Calendar.Events.DTEND,
            Calendar.Events.ALL_DAY, Calendar.Events.RRULE };

    /**
     * What columns are in the result of event query methods.
     */
    private static final String[] INSTANCE_COLUMNS = { Calendar.Instances.EVENT_ID, Calendar.Events.CALENDAR_ID,
            Calendar.Events.TITLE, Calendar.Events.DESCRIPTION, Calendar.Instances.START_DAY,
            Calendar.Instances.END_DAY, Calendar.Instances.START_MINUTE, Calendar.Instances.END_MINUTE,
            Calendar.Events.ALL_DAY, Calendar.Events.RRULE };

    /**
     * What columns are in the result of {@link #queryCalendars}.
     */
    private static final String[] CALENDAR_COLUMNS = { Calendar.Calendars._ID, Calendar.Calendars.DISPLAY_NAME,
            Calendar.Calendars.COLOR, Calendar.Calendars.SYNC_EVENTS, Calendar.Calendars.SELECTED };

    private static final String TASK_SORT_ORDER = Calendar.Events.DTSTART;

    private static final String NON_ALL_DAY_EVENT_SELECTION_CRITERIA = Calendar.Events.CALENDAR_ID + "=? AND "
            + Calendar.Events.DTEND + ">=? AND " + Calendar.Events.DTSTART + "<? AND " + Calendar.Events.ALL_DAY + "=0";

    private static final String ALL_DAY_EVENT_SELECTION_CRITERIA = Calendar.Events.CALENDAR_ID + "=? AND "
            + Calendar.Events.DTSTART + "=? AND " + Calendar.Events.ALL_DAY + "=1";

    private static final String PASSED_EVENT_SELECTION_CRITERIA = Calendar.Events.CALENDAR_ID + "=? AND "
            + Calendar.Events.DTEND + "<=? AND " + Calendar.Events.DTSTART + "<? AND " + Calendar.Events.ALL_DAY + "=?";

    private Context context;

    public AndroidCalendar(Context context) {
        this.context = context;
        // TODO: compatible to different calendar provider URI
        // calendarUriBase = getCalendarUriBase();
        // if (calendarUriBase == null) { throw new Exception(); }
    }

    public static Time beginOfToday() {
        Time time = new Time();
        time.setToNow();
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        return time;
    }

    public static Time endOfToday() {
        Time time = new Time();
        time.setToNow();
        time.hour = 23;
        time.minute = 59;
        time.second = 0;
        return time;
    }

    /**
     * Read the task at cursor current location.
     * 
     * @param c
     * @return the task read
     */
    public static Task readTask(Cursor c) {
        Task task = new Task();

        int columnEventId = c.getColumnIndex(Calendar.Instances.EVENT_ID);
        if (columnEventId == -1) {
            columnEventId = c.getColumnIndexOrThrow(Calendar.Events._ID);
        }
        task.id = c.getLong(columnEventId);

        task.calendarId = c.getLong(c.getColumnIndexOrThrow(Calendar.Events.CALENDAR_ID));
        task.setTitleWithTags(c.getString(c.getColumnIndexOrThrow(Calendar.Events.TITLE)));
        task.isAllDay = c.getInt(c.getColumnIndexOrThrow(Calendar.Events.ALL_DAY)) == 1 ? true : false;
        task.isRecurrentEvent = !TextUtils.isEmpty(c.getString(c.getColumnIndexOrThrow(Calendar.Events.RRULE)));

        int columnDtStart = c.getColumnIndex(Calendar.Events.DTSTART);
        if (columnDtStart != -1) {
            task.startTime.set(c.getLong(columnDtStart));
        }

        int columnDtEnd = c.getColumnIndex(Calendar.Events.DTEND);
        if (columnDtEnd != -1) {
            task.endTime.set(c.getLong(columnDtEnd));
        }

        int columnStartDay = c.getColumnIndex(Calendar.Instances.START_DAY);
        if (columnStartDay != -1) {
            int minutes = c.getInt(c.getColumnIndexOrThrow(Calendar.Instances.START_MINUTE));
            int day = c.getInt(c.getColumnIndexOrThrow(Calendar.Instances.START_DAY));
            task.startTime = new Time();
            task.startTime.setJulianDay(day);
            task.startTime.minute += minutes;
            task.startTime.normalize(false);
            // TODO: set end time
            task.endTime = new Time(task.startTime);
        }

        // FIXME: Tricky, setDescriptionWithExtraData() must be called after setting isAllDay, startTime and endTime
        task.setDescriptionWithExtraData(c.getString(c.getColumnIndexOrThrow(Calendar.Events.DESCRIPTION)));

        Log.v(LOG_TAG, "Read task. " + task);
        return task;
    }

    public void createTask(Task task) {
        ContentValues values = createContentValues(task);
        values.put(Calendar.Events.CALENDAR_ID, task.calendarId);
        Log.d(LOG_TAG, "Create task: " + task);
        Uri uri = context.getContentResolver().insert(Calendar.Events.CONTENT_URI, values);
        if (uri == null) {
            // TODO: throw new CalendarException();
        }
        Log.d(LOG_TAG, "New task created: " + uri);
    }

    /**
     * @param task
     * @throws OperationFailure
     */
    public void updateTask(Task task) throws OperationFailure {
        Uri uri = ContentUris.withAppendedId(Calendar.Events.CONTENT_URI, task.id);
        ContentValues values = createContentValues(task);
        Log.d(LOG_TAG, "Update task " + task.id + ": " + values);
        int numUpdatedRows = context.getContentResolver().update(uri, values, null, null);
        if (numUpdatedRows != 1) {
            throw new OperationFailure("Update task failed.");
        }
    }

    private ContentValues createContentValues(Task task) {
        ContentValues values = new ContentValues();
        values.put(Calendar.Events.TITLE, task.getTitleWithTags());
        if (!task.isPinned()) {
            values.put(Calendar.Events.DESCRIPTION, task.getDescriptionWithExtraData());
            if (task.isAllDay) {
                values.put(Calendar.Events.ALL_DAY, 1);
                Time time = new Time(task.startTime);
                time.switchTimezone("UMT");
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                values.put(Calendar.Events.DTSTART, time.toMillis(true));
                time.monthDay += 1;
                values.put(Calendar.Events.DTEND, time.toMillis(true));
            } else {
                values.put(Calendar.Events.ALL_DAY, 0);
                values.put(Calendar.Events.DTSTART, task.startTime.toMillis(true));
                values.put(Calendar.Events.DTEND, task.endTime.toMillis(true));
            }
        }
        return values;
    }

    public void deleteTask(long taskId) {
        Uri uri = ContentUris.withAppendedId(Calendar.Events.CONTENT_URI, taskId);
        Log.d(LOG_TAG, "Delete task: " + taskId);
        context.getContentResolver().delete(uri, null, null);
    }

    /**
     * @return all calendars
     */
    public Cursor queryCalendars() {
        return Calendar.Calendars.query(context.getContentResolver(), CALENDAR_COLUMNS, null, null);
    }

    /**
     * Query non all day events which are scheduled in one day (0:00 ~ 24:00 in time zone that specific in parameter
     * date).
     * 
     * @param calendarId
     * @param date - hour/minute/seconds are ignored, time zone is used
     * @return events scheduled in specified date
     */
    public Cursor queryNonAllDayEventsByDate(long calendarId, Time date) {
        Time from = new Time();
        from.set(date.monthDay, date.month, date.year);
        Time to = new Time(from);
        to.monthDay += 1;

        Log.v(LOG_TAG, "Query non all day events in [" + from.format3339(false) + ", " + to.format3339(false) + ")");
        Cursor cursor = Calendar.Instances.query(context.getContentResolver(), INSTANCE_COLUMNS, from.toMillis(true),
                to.toMillis(true), calendarId);
        return cursor;
    }

    /**
     * Query all day events
     * 
     * @param calendarId
     * @param date - in local time zone, hour/minute/seconds are ignored
     * @return events scheduled in specified date
     */
    public Cursor queryAllDayEventsByDate(long calendarId, Time date) {
        Time umtDate = new Time("UMT");
        umtDate.set(date.monthDay, date.month, date.year);

        String[] whereArgs = new String[2];
        whereArgs[0] = String.valueOf(calendarId);
        whereArgs[1] = String.valueOf(umtDate.toMillis(false));

        Log.v(LOG_TAG, "Query all day events at " + umtDate.format3339(true));
        Cursor cursor = Calendar.Events.query(context.getContentResolver(), TASK_COLUMNS,
                ALL_DAY_EVENT_SELECTION_CRITERIA, whereArgs, null);
        return cursor;
    }

    /**
     * Determines if it's a pre 2.1 or a 2.2 calendar Uri, and returns the Uri
     */
    private String getCalendarUriBase() {
        String calendarUriBase = null;
        Uri calendars = Uri.parse("content://calendar/calendars");
        Cursor cursor = null;
        try {
            // TODO: set URI base
            cursor = Calendar.Calendars.query(context.getContentResolver(), CALENDAR_COLUMNS, null, null);
        } catch (Exception e) {
            // eat
        }

        if (cursor != null) {
            calendarUriBase = "content://calendar/";
        } else {
            calendars = Uri.parse("content://com.android.calendar/calendars");
            try {
                // TODO: set URI base
                cursor = Calendar.Calendars.query(context.getContentResolver(), CALENDAR_COLUMNS, null, null);
            } catch (Exception e) {
                // eat
            }

            if (cursor != null) {
                calendarUriBase = "content://com.android.calendar/";
            }

        }

        return calendarUriBase;
    }

}
