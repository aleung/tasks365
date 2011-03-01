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

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AndroidCalendar {

    public class Event {
    }

    public static final String CALENDAR_FIELD_NAME = "name";


    public static final String CALENDAR_FIELD_DISPLAY_NAME = "displayName";
    public static final String PATH_EVENTS = "events";
    public static final String PATH_DELETED_EVENTS = "deleted_events";

    public static final String PATH_CALENDARS = "calendars";

    private static final String QUERY_CRITERIA_SELECTED_CALENDAR = "selected=1";

    private static final String DEBUG_TAG = "tasks365.AndroidCalendar";
    private Activity activity;

    private String calendarUriBase;

    /**
     * Read the task at cursor current location.
     * 
     * @param c
     * @return the task read
     */
    public static Task readTask(Cursor c) {
        Task task = new Task();
        task.id = c.getLong(c.getColumnIndexOrThrow(BaseColumns.ID));
        task.setTitleWithTags(c.getString(c.getColumnIndexOrThrow(EVENT_FIELD_TITLE)));
        task.setDescriptionWithTags(c.getString(c.getColumnIndexOrThrow(EVENT_FIELD_DESCRIPTION)));
        task.startTime = c.getLong(c.getColumnIndexOrThrow(EVENT_FIELD_START_TIME));
        task.endTime = c.getLong(c.getColumnIndexOrThrow(EVENT_FIELD_END_TIME));
        task.isAllDay = c.getInt(c.getColumnIndexOrThrow(EVENT_FIELD_IS_ALL_DAY)) == 1 ? true : false;
        Log.v(DEBUG_TAG, "Read task. ID=" + task.id);
        return task;
    }

    public AndroidCalendar(Activity activity) {
        this.activity = activity;
        calendarUriBase = getCalendarUriBase();
        // TODO: if (calendarUriBase == null) { throw new Exception(); }
    }

    public Uri addEvent(ContentValues event) {
        Uri eventsUri = Uri.parse(calendarUriBase + PATH_EVENTS);
        Uri insertedUri = activity.getContentResolver().insert(eventsUri, event);
        Log.d(DEBUG_TAG, "Added event " + insertedUri);
        return insertedUri;
    }

    public void addEvent(int calendarId, String title, long startTime, boolean isAllDay) {
        ContentValues event = new ContentValues();
        event.put(EVENT_FIELD_CALENDAR_ID, calendarId);
        event.put(EVENT_FIELD_TITLE, title);
        event.put(EVENT_FIELD_START_TIME, startTime);
        event.put(EVENT_FIELD_END_TIME, startTime);
        event.put(EVENT_FIELD_IS_ALL_DAY, isAllDay ? 1 : 0);
        addEvent(event);
    }

    public Cursor getAllEvents(int calendarId) {
        String[] projection = new String[] { BaseColumns.ID, EVENT_FIELD_TITLE };
        return getCalendarManagedCursor(projection, "calendar_id=" + calendarId, PATH_EVENTS);
    }

    public Cursor getAllSelectedCalendars() {
        String[] projection = new String[] { "_id", "name" };
        return getAllSelectedCalendars(projection);
    }

    public Cursor getAllSelectedCalendars(String[] projection) {
        String selection = QUERY_CRITERIA_SELECTED_CALENDAR;
        return getCalendarManagedCursor(projection, selection, PATH_CALENDARS);
    }

    /**
     * Find calendar by name and return the ID.
     * 
     * @param calendarName
     * @return null if not found
     */
    public String getCalendarId(String calendarName) {
        String[] projection = new String[] { BaseColumns.ID, CALENDAR_FIELD_NAME };
        Cursor cursor = getCalendarManagedCursor(projection, QUERY_CRITERIA_SELECTED_CALENDAR + " AND "
                + CALENDAR_FIELD_NAME + "='" + calendarName + "'", PATH_CALENDARS);
        if (cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        return null;
    }

    /**
     * <ul>
     * <li>All events in a calendar: selection="calendar_id="+calendarId, path=null</li>
     * <li>Specific event: selection=null, path="events/"+eventId</li>
     * <li>Deleted events: selection=null, path="deleted_events/"+eventId</li>
     * <li>All calendars: selection=null, path="calendars"</li>
     * <li>Active calendars: selection="selected=1", path="calendars"</li>
     * <li>Specific calendar by name: selection="name="+name, path="calendars"</li>
     * </ul>
     * 
     * @param activity
     * @param projection
     * @param selection
     * @param path
     * @return
     */
    public Cursor getCalendarManagedCursor(String[] projection, String selection, String path) {
        Uri calendars = Uri.parse(calendarUriBase + path);

        Cursor managedCursor = null;
        try {
            managedCursor = activity.managedQuery(calendars, projection, selection, null, null);
        } catch (IllegalArgumentException e) {
            Log.w(DEBUG_TAG, "Failed to get provider at [" + calendars.toString() + "]");
        }
        return managedCursor;
    }


    public int updateEvent(int entryID, ContentValues event) {
        Uri eventsUri = Uri.parse(calendarUriBase + "events");
        Uri eventUri = ContentUris.withAppendedId(eventsUri, entryID);

        int iNumRowsUpdated = activity.getContentResolver().update(eventUri, event, null, null);

        Log.i(DEBUG_TAG, "Updated " + iNumRowsUpdated + " calendar entry.");

        return iNumRowsUpdated;
    }

    private int DeleteEvent(int eventId) {
        int iNumRowsDeleted = 0;

        Uri eventsUri = Uri.parse(calendarUriBase + PATH_EVENTS);
        Uri eventUri = ContentUris.withAppendedId(eventsUri, eventId);
        iNumRowsDeleted = activity.getContentResolver().delete(eventUri, null, null);

        Log.i(DEBUG_TAG, "Deleted " + iNumRowsDeleted + " calendar entry.");

        return iNumRowsDeleted;
    }

    /**
     * Determines if it's a pre 2.1 or a 2.2 calendar Uri, and returns the Uri
     */
    // TODO: not use managedQuery(), close the query after used
    private String getCalendarUriBase() {
        String calendarUriBase = null;
        Uri calendars = Uri.parse("content://calendar/calendars");
        Cursor managedCursor = null;
        try {
            managedCursor = activity.managedQuery(calendars, null, null, null, null);
        } catch (Exception e) {
            // eat
        }

        if (managedCursor != null) {
            calendarUriBase = "content://calendar/";
        } else {
            calendars = Uri.parse("content://com.android.calendar/calendars");
            try {
                managedCursor = activity.managedQuery(calendars, null, null, null, null);
            } catch (Exception e) {
                // eat
            }

            if (managedCursor != null) {
                calendarUriBase = "content://com.android.calendar/";
            }

        }

        return calendarUriBase;
    }
}
