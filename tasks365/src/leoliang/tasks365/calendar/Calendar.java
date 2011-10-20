// Code originates from android/2.2.1_r1/android/provider/Calendar.java

/*
 * Copyright (C) 2006 The Android Open Source Project
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

package leoliang.tasks365.calendar;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * The Calendar provider contains all calendar events.
 */
public final class Calendar {

    /**
     * Contains a list of available calendars.
     */
    public static class Calendars implements BaseColumns, CalendarsColumns
    {
        private static final String WHERE_DELETE_FOR_ACCOUNT = Calendars._SYNC_ACCOUNT + "=?"
                + " AND " + Calendars._SYNC_ACCOUNT_TYPE + "=?";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/calendars");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "displayName";

        /**
         * The URL to the calendar
         * <P>Type: TEXT (URL)</P>
         */
        public static final String URL = "url";

        /**
         * The name of the calendar
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The display name of the calendar
         * <P>Type: TEXT</P>
         */
        public static final String DISPLAY_NAME = "displayName";

        /**
         * The location the of the events in the calendar
         * <P>Type: TEXT</P>
         */
        public static final String LOCATION = "location";

        /**
         * Should the calendar be hidden in the calendar selection panel?
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String HIDDEN = "hidden";

        /**
         * The owner account for this calendar, based on the calendar feed.
         * This will be different from the _SYNC_ACCOUNT for delegated calendars.
         * <P>Type: String</P>
         */
        public static final String OWNER_ACCOUNT = "ownerAccount";

        /**
         * Can the organizer respond to the event?  If no, the status of the
         * organizer should not be shown by the UI.  Defaults to 1
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String ORGANIZER_CAN_RESPOND = "organizerCanRespond";

        /**
         * Convenience method perform a delete on the Calendar provider
         *
         * @param cr the ContentResolver
         * @param selection the rows to delete
         * @return the count of rows that were deleted
         */
        public static int delete(ContentResolver cr, String selection, String[] selectionArgs)
        {
            return cr.delete(CONTENT_URI, selection, selectionArgs);
        }

        /**
         * Convenience method to delete all calendars that match the account.
         *
         * @param cr the ContentResolver
         * @param account the account whose rows should be deleted
         * @return the count of rows that were deleted
         */
        public static int deleteCalendarsForAccount(ContentResolver cr, Account account) {
            // delete all calendars that match this account
            return Calendar.Calendars.delete(cr,
                    WHERE_DELETE_FOR_ACCOUNT,
                    new String[] { account.name, account.type });
        }

        public static final Cursor query(ContentResolver cr, String[] projection,
                                       String where, String orderBy)
        {
            return cr.query(CONTENT_URI, projection, where,
                                         null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }
    }

    /**
     * Columns from the Calendars table that other tables join into themselves.
     */
    public interface CalendarsColumns
    {
        /**
         * The color of the calendar
         * <P>Type: INTEGER (color value)</P>
         */
        public static final String COLOR = "color";

        /**
         * The level of access that the user has for the calendar
         * <P>Type: INTEGER (one of the values below)</P>
         */
        public static final String ACCESS_LEVEL = "access_level";

        /** Cannot access the calendar */
        public static final int NO_ACCESS = 0;
        /** Can only see free/busy information about the calendar */
        public static final int FREEBUSY_ACCESS = 100;
        /** Can read all event details */
        public static final int READ_ACCESS = 200;
        public static final int RESPOND_ACCESS = 300;
        public static final int OVERRIDE_ACCESS = 400;
        /** Full access to modify the calendar, but not the access control settings */
        public static final int CONTRIBUTOR_ACCESS = 500;
        public static final int EDITOR_ACCESS = 600;
        /** Full access to the calendar */
        public static final int OWNER_ACCESS = 700;
        /** Domain admin */
        public static final int ROOT_ACCESS = 800;

        /**
         * Is the calendar selected to be displayed?
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String SELECTED = "selected";

        /**
         * The timezone the calendar's events occurs in
         * <P>Type: TEXT</P>
         */
        public static final String TIMEZONE = "timezone";

        /**
         * If this calendar is in the list of calendars that are selected for
         * syncing then "sync_events" is 1, otherwise 0.
         * <p>Type: INTEGER (boolean)</p>
         */
        public static final String SYNC_EVENTS = "sync_events";

        /**
         * Sync state data.
         * <p>Type: String (blob)</p>
         */
        public static final String SYNC_STATE = "sync_state";

        /**
         * The account that was used to sync the entry to the device.
         * <P>Type: TEXT</P>
         */
        public static final String _SYNC_ACCOUNT = "_sync_account";

        /**
         * The type of the account that was used to sync the entry to the device.
         * <P>Type: TEXT</P>
         */
        public static final String _SYNC_ACCOUNT_TYPE = "_sync_account_type";

        /**
         * The unique ID for a row assigned by the sync source. NULL if the row has never been synced.
         * <P>Type: TEXT</P>
         */
        public static final String _SYNC_ID = "_sync_id";

        /**
         * The last time, from the sync source's point of view, that this row has been synchronized.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _SYNC_TIME = "_sync_time";

        /**
         * The version of the row, as assigned by the server.
         * <P>Type: TEXT</P>
         */
        public static final String _SYNC_VERSION = "_sync_version";

        /**
         * For use by sync adapter at its discretion; not modified by CalendarProvider
         * Note that this column was formerly named _SYNC_LOCAL_ID.  We are using it to avoid a
         * schema change.
         * TODO Replace this with something more general in the future.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _SYNC_DATA = "_sync_local_id";

        /**
         * Used only in persistent providers, and only during merging.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _SYNC_MARK = "_sync_mark";

        /**
         * Used to indicate that local, unsynced, changes are present.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String _SYNC_DIRTY = "_sync_dirty";

        /**
         * The name of the account instance to which this row belongs, which when paired with
         * {@link #ACCOUNT_TYPE} identifies a specific account.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_NAME = "account_name";

        /**
         * The type of account to which this row belongs, which when paired with
         * {@link #ACCOUNT_NAME} identifies a specific account.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_TYPE = "account_type";
    }

    /**
     * Contains one entry per calendar event. Recurring events show up as a single entry.
     */
    public static final class Events implements BaseColumns, EventsColumns, CalendarsColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/events");

        public static final Uri DELETED_CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/deleted_events");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "";

        public static final Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
        }

        public static final Cursor query(ContentResolver cr, String[] projection, String where, String[] whereArgs,
                String orderBy) {
            return cr.query(CONTENT_URI, projection, where, whereArgs, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }
    }

    /**
     * Columns from the Events table that other tables join into themselves.
     */
    public interface EventsColumns
    {
        /**
         * The calendar the event belongs to
         * <P>Type: INTEGER (foreign key to the Calendars table)</P>
         */
        public static final String CALENDAR_ID = "calendar_id";

        /**
         * The URI for an HTML version of this event.
         * <P>Type: TEXT</P>
         */
        public static final String HTML_URI = "htmlUri";

        /**
         * The title of the event
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The description of the event
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * Where the event takes place.
         * <P>Type: TEXT</P>
         */
        public static final String EVENT_LOCATION = "eventLocation";

        /**
         * The event status
         * <P>Type: INTEGER (int)</P>
         */
        public static final String STATUS = "eventStatus";

        public static final int STATUS_TENTATIVE = 0;
        public static final int STATUS_CONFIRMED = 1;
        public static final int STATUS_CANCELED = 2;

        /**
         * This is a copy of the attendee status for the owner of this event.
         * This field is copied here so that we can efficiently filter out
         * events that are declined without having to look in the Attendees
         * table.
         *
         * <P>Type: INTEGER (int)</P>
         */
        public static final String SELF_ATTENDEE_STATUS = "selfAttendeeStatus";

        /**
         * This column is available for use by sync adapters
         * <P>Type: TEXT</P>
         */
        public static final String SYNC_ADAPTER_DATA = "syncAdapterData";

        /**
         * The comments feed uri.
         * <P>Type: TEXT</P>
         */
        public static final String COMMENTS_URI = "commentsUri";

        /**
         * The time the event starts
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String DTSTART = "dtstart";

        /**
         * The time the event ends
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String DTEND = "dtend";

        /**
         * The duration of the event
         * <P>Type: TEXT (duration in RFC2445 format)</P>
         */
        public static final String DURATION = "duration";

        /**
         * The timezone for the event.
         * <P>Type: TEXT
         */
        public static final String EVENT_TIMEZONE = "eventTimezone";

        /**
         * Whether the event lasts all day or not
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String ALL_DAY = "allDay";

        /**
         * Visibility for the event.
         * <P>Type: INTEGER</P>
         */
        public static final String VISIBILITY = "visibility";

        public static final int VISIBILITY_DEFAULT = 0;
        public static final int VISIBILITY_CONFIDENTIAL = 1;
        public static final int VISIBILITY_PRIVATE = 2;
        public static final int VISIBILITY_PUBLIC = 3;

        /**
         * Transparency for the event -- does the event consume time on the calendar?
         * <P>Type: INTEGER</P>
         */
        public static final String TRANSPARENCY = "transparency";

        public static final int TRANSPARENCY_OPAQUE = 0;

        public static final int TRANSPARENCY_TRANSPARENT = 1;

        /**
         * Whether the event has an alarm or not
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String HAS_ALARM = "hasAlarm";

        /**
         * Whether the event has extended properties or not
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String HAS_EXTENDED_PROPERTIES = "hasExtendedProperties";

        /**
         * The recurrence rule for the event.
         * than one.
         * <P>Type: TEXT</P>
         */
        public static final String RRULE = "rrule";

        /**
         * The recurrence dates for the event.
         * <P>Type: TEXT</P>
         */
        public static final String RDATE = "rdate";

        /**
         * The recurrence exception rule for the event.
         * <P>Type: TEXT</P>
         */
        public static final String EXRULE = "exrule";

        /**
         * The recurrence exception dates for the event.
         * <P>Type: TEXT</P>
         */
        public static final String EXDATE = "exdate";

        /**
         * The _sync_id of the original recurring event for which this event is
         * an exception.
         * <P>Type: TEXT</P>
         */
        public static final String ORIGINAL_EVENT = "originalEvent";

        /**
         * The original instance time of the recurring event for which this
         * event is an exception.
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String ORIGINAL_INSTANCE_TIME = "originalInstanceTime";

        /**
         * The allDay status (true or false) of the original recurring event
         * for which this event is an exception.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String ORIGINAL_ALL_DAY = "originalAllDay";

        /**
         * The last date this event repeats on, or NULL if it never ends
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String LAST_DATE = "lastDate";

        /**
         * Whether the event has attendee information.  True if the event
         * has full attendee data, false if the event has information about
         * self only.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String HAS_ATTENDEE_DATA = "hasAttendeeData";

        /**
         * Whether guests can modify the event.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String GUESTS_CAN_MODIFY = "guestsCanModify";

        /**
         * Whether guests can invite other guests.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String GUESTS_CAN_INVITE_OTHERS = "guestsCanInviteOthers";

        /**
         * Whether guests can see the list of attendees.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String GUESTS_CAN_SEE_GUESTS = "guestsCanSeeGuests";

        /**
         * Email of the organizer (owner) of the event.
         * <P>Type: STRING</P>
         */
        public static final String ORGANIZER = "organizer";

        /**
         * Whether the user can invite others to the event.
         * The GUESTS_CAN_INVITE_OTHERS is a setting that applies to an arbitrary guest,
         * while CAN_INVITE_OTHERS indicates if the user can invite others (either through
         * GUESTS_CAN_INVITE_OTHERS or because the user has modify access to the event).
         * <P>Type: INTEGER (boolean, readonly)</P>
         */
        public static final String CAN_INVITE_OTHERS = "canInviteOthers";

        /**
         * The owner account for this calendar, based on the calendar (foreign
         * key into the calendars table).
         * <P>Type: String</P>
         */
        public static final String OWNER_ACCOUNT = "ownerAccount";

        /**
         * Whether the row has been deleted.  A deleted row should be ignored.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String DELETED = "deleted";
    }

    /**
     * Contains one entry per calendar event instance. Recurring events show up every time
     * they occur.
     */
    public static final class Instances implements BaseColumns, EventsColumns, CalendarsColumns {

        private static final String WHERE_CALENDARS_SELECTED = Calendars.SELECTED + "=1";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY +
                "/instances/when");

        public static final Uri CONTENT_BY_DAY_URI =
            Uri.parse("content://" + AUTHORITY + "/instances/whenbyday");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "begin ASC";
        /**
         * The sort order is: events with an earlier start time occur
         * first and if the start times are the same, then events with
         * a later end time occur first. The later end time is ordered
         * first so that long-running events in the calendar views appear
         * first.  If the start and end times of two events are
         * the same then we sort alphabetically on the title.  This isn't
         * required for correctness, it just adds a nice touch.
         */
        public static final String SORT_CALENDAR_VIEW = "begin ASC, end DESC, title ASC";

        /**
         * The beginning time of the instance, in UTC milliseconds
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String BEGIN = "begin";

        /**
         * The ending time of the instance, in UTC milliseconds
         * <P>Type: INTEGER (long; millis since epoch)</P>
         */
        public static final String END = "end";

        /**
         * The event for this instance
         * <P>Type: INTEGER (long, foreign key to the Events table)</P>
         */
        public static final String EVENT_ID = "event_id";

        /**
         * The Julian start day of the instance, relative to the local timezone
         * <P>Type: INTEGER (int)</P>
         */
        public static final String START_DAY = "startDay";

        /**
         * The Julian end day of the instance, relative to the local timezone
         * <P>Type: INTEGER (int)</P>
         */
        public static final String END_DAY = "endDay";

        /**
         * The start minute of the instance measured from midnight in the
         * local timezone.
         * <P>Type: INTEGER (int)</P>
         */
        public static final String START_MINUTE = "startMinute";

        /**
         * The end minute of the instance measured from midnight in the
         * local timezone.
         * <P>Type: INTEGER (int)</P>
         */
        public static final String END_MINUTE = "endMinute";

        public static final Cursor query(ContentResolver cr, String[] projection, long begin, long end, long calendarId) {
            Uri.Builder builder = CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, end);
            return cr.query(builder.build(), projection, 
                    Calendar.Events.CALENDAR_ID + "=" + calendarId + " AND " + Calendar.Events.ALL_DAY + "=0", 
                    null, DEFAULT_SORT_ORDER);
        }

        public static final Cursor query(ContentResolver cr, String[] projection,
                                         long begin, long end) {
            Uri.Builder builder = CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, end);
            return cr.query(builder.build(), projection, WHERE_CALENDARS_SELECTED,
                         null, DEFAULT_SORT_ORDER);
        }

        public static final Cursor query(ContentResolver cr, String[] projection,
                                         long begin, long end, String where, String orderBy) {
            Uri.Builder builder = CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, end);
            if (TextUtils.isEmpty(where)) {
                where = WHERE_CALENDARS_SELECTED;
            } else {
                where = "(" + where + ") AND " + WHERE_CALENDARS_SELECTED;
            }
            return cr.query(builder.build(), projection, where,
                         null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }
    }

    /** Authority of calendar provider in Android 2.2 */
    public static final String AUTHORITY = "com.android.calendar";

    /**
     * The content:// style URL for the top-level calendar authority
     */
    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY);


}