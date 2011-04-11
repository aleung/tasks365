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

import java.util.TimeZone;

import leoliang.tasks365.calendar.Calendar;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AndroidCalendar {

    private static final String LOG_TAG = "tasks365";

    /**
     * What columns are in the result of event query methods.
     */
    private static final String[] TASK_COLUMNS = { Calendar.Events._ID, Calendar.Events.CALENDAR_ID,
            Calendar.Events.TITLE, Calendar.Events.DESCRIPTION, Calendar.Events.DTSTART, Calendar.Events.DTEND,
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

    /**
     * Read the task at cursor current location.
     * 
     * @param c
     * @return the task read
     */
    public static Task readTask(Cursor c) {
        Task task = new Task();
        task.id = c.getLong(c.getColumnIndexOrThrow(Calendar.Events._ID));
        task.calendarId = c.getLong(c.getColumnIndexOrThrow(Calendar.Events.CALENDAR_ID));
        task.startTime.setTimeInMillis(c.getLong(c.getColumnIndexOrThrow(Calendar.Events.DTSTART)));
        task.endTime.setTimeInMillis(c.getLong(c.getColumnIndexOrThrow(Calendar.Events.DTEND)));
        Log.v(LOG_TAG,
                "Read task. dtStart=" + Task.formatDate(task.startTime) + ", dtEnd=" + Task.formatDate(task.endTime));
        task.isAllDay = c.getInt(c.getColumnIndexOrThrow(Calendar.Events.ALL_DAY)) == 1 ? true : false;
        String recurrenceRule = c.getString(c.getColumnIndexOrThrow(Calendar.Events.RRULE));
        task.isRecurrentEvent = ((recurrenceRule != null) && (recurrenceRule.length() == 0));
        task.setTitleWithTags(c.getString(c.getColumnIndexOrThrow(Calendar.Events.TITLE)));
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
     */
    public void updateTask(Task task) {
        Uri uri = ContentUris.withAppendedId(Calendar.Events.CONTENT_URI, task.id);
        ContentValues values = createContentValues(task);
        Log.d(LOG_TAG, "Update task: " + task);
        context.getContentResolver().update(uri, values, null, null);
    }

    private ContentValues createContentValues(Task task) {
        ContentValues values = new ContentValues();
        values.put(Calendar.Events.TITLE, task.getTitleWithTags());
        values.put(Calendar.Events.DESCRIPTION, task.getDescriptionWithExtraData());
        if (task.isAllDay) {
            values.put(Calendar.Events.ALL_DAY, 1);
            java.util.Calendar time = (java.util.Calendar) task.startTime.clone();
            time.setTimeZone(TimeZone.getTimeZone("UMT"));
            time.set(java.util.Calendar.HOUR_OF_DAY, 0);
            time.set(java.util.Calendar.MINUTE, 0);
            time.set(java.util.Calendar.SECOND, 0);
            time.set(java.util.Calendar.MILLISECOND, 0);
            values.put(Calendar.Events.DTSTART, time.getTimeInMillis());
            time.add(java.util.Calendar.DAY_OF_MONTH, 1);
            values.put(Calendar.Events.DTEND, time.getTimeInMillis());
        } else {
            values.put(Calendar.Events.ALL_DAY, 0);
            values.put(Calendar.Events.DTSTART, task.startTime.getTimeInMillis());
            values.put(Calendar.Events.DTEND, task.endTime.getTimeInMillis());
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
    public Cursor queryNonAllDayEventsByDate(long calendarId, java.util.Calendar date) {
        java.util.Calendar from = java.util.Calendar.getInstance();
        from.clear();
        from.set(date.get(java.util.Calendar.YEAR), date.get(java.util.Calendar.MONTH),
                date.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0);
        java.util.Calendar to = (java.util.Calendar) from.clone();
        to.add(java.util.Calendar.DAY_OF_MONTH, 1);

        Log.v(LOG_TAG, "Query non all day events in [" + Task.formatDate(from) + ", " + Task.formatDate(to) + ")");
        Cursor cursor = Calendar.Instances.query(context.getContentResolver(), TASK_COLUMNS, from.getTimeInMillis(),
                to.getTimeInMillis(), calendarId);
        return cursor;
    }

    /**
     * Query all day events
     * 
     * @param calendarId
     * @param date - in local time zone, hour/minute/seconds are ignored
     * @return events scheduled in specified date
     */
    public Cursor queryAllDayEventsByDate(long calendarId, java.util.Calendar date) {
        java.util.Calendar umtDate = (java.util.Calendar) date.clone();
        umtDate.setTimeZone(TimeZone.getTimeZone("UMT"));
        umtDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        umtDate.set(java.util.Calendar.MINUTE, 0);
        umtDate.set(java.util.Calendar.SECOND, 0);
        umtDate.set(java.util.Calendar.MILLISECOND, 0);

        String[] whereArgs = new String[2];
        whereArgs[0] = String.valueOf(calendarId);
        whereArgs[1] = String.valueOf(umtDate.getTimeInMillis());

        Log.v(LOG_TAG, "Query all day events at " + Task.formatDate(umtDate));
        Cursor cursor = Calendar.Events.query(context.getContentResolver(), TASK_COLUMNS,
                ALL_DAY_EVENT_SELECTION_CRITERIA, whereArgs, null);
        return cursor;
    }

    /**
     * Query non all day events which were scheduled in passed days (before 0:00 of today in system's time zone).
     * 
     * @param calendarId
     * @return
     */
    public Cursor queryNonAllDayEventsInPastDays(long calendarId) {
        return queryEventsInPastDays(calendarId, false);
    }

    /**
     * Query all day events which were scheduled in passed days.
     * 
     * @param calendarId
     * @return
     */
    public Cursor queryAllDayEventsInPastDays(long calendarId) {
        return queryEventsInPastDays(calendarId, true);
    }

    private Cursor queryEventsInPastDays(long calendarId, boolean isAllDay) {
        java.util.Calendar beforeDate = java.util.Calendar.getInstance();
        if (isAllDay) {
            beforeDate.setTimeZone(TimeZone.getTimeZone("UMT"));
        }
        beforeDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        beforeDate.set(java.util.Calendar.MINUTE, 0);
        beforeDate.set(java.util.Calendar.SECOND, 0);
        beforeDate.set(java.util.Calendar.MILLISECOND, 0);

        String[] whereArgs = new String[4];
        whereArgs[0] = String.valueOf(calendarId);
        whereArgs[1] = String.valueOf(beforeDate.getTimeInMillis());
        whereArgs[2] = whereArgs[1];
        whereArgs[3] = isAllDay ? "1" : "0";

        Log.v(LOG_TAG,
                "Query " + (isAllDay ? "all day" : "non all day") + " events before "
                        + Task.formatDate(beforeDate.getTime()));
        Cursor cursor = Calendar.Events.query(context.getContentResolver(), TASK_COLUMNS,
                PASSED_EVENT_SELECTION_CRITERIA, whereArgs, TASK_SORT_ORDER);
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
