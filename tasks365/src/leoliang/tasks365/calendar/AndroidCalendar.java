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
package leoliang.tasks365.calendar;

import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AndroidCalendar {

    private static final String LOG_TAG = "tasks365.AndroidCalendar";

    private static final String[] TASK_PROJECTION = { Calendar.Events._ID, Calendar.Events.CALENDAR_ID,
            Calendar.Events.TITLE, Calendar.Events.DESCRIPTION, Calendar.Events.DTSTART, Calendar.Events.DTEND,
            Calendar.Events.ALL_DAY };
    private static final String TASK_SORT_ORDER = Calendar.Events.DTSTART;
    private static final String TASK_SELECTION_CRITERIA = Calendar.Events.CALENDAR_ID + "=? AND "
            + Calendar.Events.DTEND + ">? AND " + Calendar.Events.DTSTART + "<?";
    private static final String[] CALENDAR_PROJECTION = { Calendar.Calendars._ID, Calendar.Calendars.DISPLAY_NAME,
            Calendar.Calendars.COLOR, Calendar.Calendars.SYNC_EVENTS, Calendar.Calendars.SELECTED };

    private Context context;

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
        task.startTime = c.getLong(c.getColumnIndexOrThrow(Calendar.Events.DTSTART));
        task.endTime = c.getLong(c.getColumnIndexOrThrow(Calendar.Events.DTEND));
        task.isAllDay = c.getInt(c.getColumnIndexOrThrow(Calendar.Events.ALL_DAY)) == 1 ? true : false;
        task.setTitleWithTags(c.getString(c.getColumnIndexOrThrow(Calendar.Events.TITLE)));
        // FIXME: Tricky, setDescriptionWithExtraData() must be called after setting isAllDay, startTime and endTime
        task.setDescriptionWithExtraData(c.getString(c.getColumnIndexOrThrow(Calendar.Events.DESCRIPTION)));
        Log.v(LOG_TAG, "Read task. " + task);
        return task;
    }

    public AndroidCalendar(Context context) {
        this.context = context;
        // TODO: compatible to different calendar provider URI
        // calendarUriBase = getCalendarUriBase();
        // if (calendarUriBase == null) { throw new Exception(); }
    }

    public void createTask(Task task) {
        ContentValues values = new ContentValues();
        values.put(Calendar.Events.CALENDAR_ID, task.calendarId);
        values.put(Calendar.Events.TITLE, task.getTitleWithTags());
        values.put(Calendar.Events.DESCRIPTION, task.getDescriptionWithExtraData());
        values.put(Calendar.Events.DTSTART, task.startTime);
        values.put(Calendar.Events.DTEND, task.endTime);
        values.put(Calendar.Events.ALL_DAY, task.isAllDay ? 1 : 0);
        Uri uri = context.getContentResolver().insert(Calendar.Events.CONTENT_URI, values);
        if (uri == null) {
            // TODO: throw new CalendarException();
        }
        Log.d(LOG_TAG, "New task created: " + uri);
    }

    public void deleteTask(long taskId) {
        Uri uri = ContentUris.withAppendedId(Calendar.Events.CONTENT_URI, taskId);
        context.getContentResolver().delete(uri, null, null);
    }

    /**
     * @return all calendars
     */
    public Cursor queryCalendars() {
        return Calendar.Calendars.query(context.getContentResolver(), CALENDAR_PROJECTION, null, null);
    }

    /**
     * Query tasks which are scheduled in one day (0:00 ~ 24:00 in system's time zone).
     * 
     * @param calendarId
     * @param date
     * @return
     */
    public Cursor queryTasksByDate(long calendarId, java.util.Calendar date) {
        java.util.Calendar from = java.util.Calendar.getInstance();
        from.set(date.get(java.util.Calendar.YEAR), date.get(java.util.Calendar.MONTH),
                date.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0);
        java.util.Calendar to = (java.util.Calendar) from.clone();
        to.add(java.util.Calendar.DAY_OF_MONTH, 1);
        return queryTasksByTimeRange(calendarId, from.getTime(), to.getTime());
    }

    /**
     * Query tasks which are scheduled in a range of time.
     * 
     * @param calendarId
     * @param from
     * @param to
     * @return
     */
    public Cursor queryTasksByTimeRange(long calendarId, Date from, Date to) {
        String[] whereArgs = new String[3];
        whereArgs[0] = String.valueOf(calendarId);
        whereArgs[1] = String.valueOf(from.getTime());
        whereArgs[2] = String.valueOf(to.getTime());
        Cursor cursor = Calendar.Events.query(context.getContentResolver(), TASK_PROJECTION, TASK_SELECTION_CRITERIA,
                whereArgs, TASK_SORT_ORDER);
        return cursor;
    }

    /**
     * Query tasks which were scheduled in passed days (before 0:00 of today in system's time zone).
     * 
     * @param calendarId
     * @return
     */
    public Cursor queryTasksInPassedDays(long calendarId) {
        java.util.Calendar from = java.util.Calendar.getInstance();
        from.clear();
        java.util.Calendar to = java.util.Calendar.getInstance();
        to.clear(java.util.Calendar.HOUR_OF_DAY);
        to.clear(java.util.Calendar.MINUTE);
        to.clear(java.util.Calendar.SECOND);
        to.clear(java.util.Calendar.MILLISECOND);
        return queryTasksByTimeRange(calendarId, from.getTime(), to.getTime());
    }

    /**
     * @param task
     */
    public void updateTask(Task task) {
        Uri uri = ContentUris.withAppendedId(Calendar.Events.CONTENT_URI, task.id);
        ContentValues values = new ContentValues();
        values.put(Calendar.Events.TITLE, task.getTitleWithTags());
        values.put(Calendar.Events.DESCRIPTION, task.getDescriptionWithExtraData());
        values.put(Calendar.Events.DTSTART, task.startTime);
        values.put(Calendar.Events.DTEND, task.endTime);
        values.put(Calendar.Events.ALL_DAY, task.isAllDay ? 1 : 0);
        context.getContentResolver().update(uri, values, null, null);
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
            cursor = Calendar.Calendars.query(context.getContentResolver(), CALENDAR_PROJECTION, null, null);
        } catch (Exception e) {
            // eat
        }

        if (cursor != null) {
            calendarUriBase = "content://calendar/";
        } else {
            calendars = Uri.parse("content://com.android.calendar/calendars");
            try {
                // TODO: set URI base
                cursor = Calendar.Calendars.query(context.getContentResolver(), CALENDAR_PROJECTION, null, null);
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
