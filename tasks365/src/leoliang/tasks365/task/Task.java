package leoliang.tasks365.task;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leoliang.tasks365.task.TagParser.TagParseResult;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * This class stores information of a task in calendar.
 */
public class Task {

    private static final String LOG_TAG = "tasks365";

    private static final String TAG_NEW = "new";
    private static final String TAG_STAR = "star";
    private static final String TAG_DONE = "done";
    private static final String EXTRA_DATA_MAGIC_STRING = "-#%#-";
    private static final String EXTRA_DATA_SEPERATOR = "--- DO NOT MODIFY BELOW ---\n";

    private static final DateFormat dueFormatter = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    // fields directly maps to content provider data model
    public long id;
    public long calendarId;
    public Calendar startTime = Calendar.getInstance();
    public Calendar endTime = Calendar.getInstance();
    public boolean isAllDay = true;

    /** pure title, without tags */
    public String title;

    /** pure description, without tags */
    public String description;

    // extra fields stored in tags
    public boolean isDone = false;
    public boolean isNew = false;
    public boolean isStarred = false;

    // extra fields stored in JSON
    public Calendar due = null;

    public static String formatDate(Date date) {
        return dateFormatter.format(date);
    }

    public static String formatDate(java.util.Calendar date) {
        return formatDate(date.getTime());
    }

    public String getDescriptionWithExtraData() {
        StringBuilder s = new StringBuilder();
        if (description != null) {
            s.append(description.trim());
        }
        constructExtraDataJson(s);
        return s.toString();
    }

    public String getTitleWithTags() {
        StringBuilder s = new StringBuilder();
        if (isDone) {
            addTag(s, TAG_DONE);
        }
        if (isStarred) {
            addTag(s, TAG_STAR);
        }
        if (isNew) {
            addTag(s, TAG_NEW);
        }
        if (title != null) {
            s.append(title.trim());
        }
        return s.toString();
    }

    /**
     * Extra data can be put as JSON inside description:
     * <ul>
     * <li>due : string in pattern YYYY-MM-DD</li>
     * <li>startTime : long, exists when isAllDay is true</li>
     * <li>endTime : long, exists when isAllDay is true</li>
     * </ul>
     * 
     * FIXME: Tricky, setDescriptionWithExtraData() must be called after setting isAllDay, startTime and endTime
     * 
     * @param s
     */
    public void setDescriptionWithExtraData(String s) {
        if (s == null) {
            return;
        }
        ExtraDataParser.getInstance().parse(s, this);
    }

    /**
     * Tags can be put at the beginning of title:
     * <ul>
     * <li>#new</li>
     * <li>#star</li>
     * <li>#done</li>
     * </ul>
     * 
     * @param s
     */
    public void setTitleWithTags(String s) {
        if (s == null) {
            return;
        }
        TagParseResult parseResult = TagParser.getInstance().parse(s);
        title = parseResult.text.trim();
        for (String tag : parseResult.tags) {
            if (tag.equals(TAG_NEW)) {
                isNew = true;
                continue;
            }
            if (tag.equals(TAG_STAR)) {
                isStarred = true;
                continue;
            }
            if (tag.equals(TAG_DONE)) {
                isDone = true;
                continue;
            }
        }
    }

    /**
     * Schedule as an all day even in today.
     */
    public void scheduleToday() {
        startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, 23);
        startTime.set(Calendar.MINUTE, 59);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        endTime = (Calendar) startTime.clone();
        isAllDay = true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Formatter formatter = new Formatter(s);
        formatter.format("{id:%d, calendarId:%d, title:%s, isAllDay:%b, startTime:%s, endTime:%s}", id, calendarId,
                title, isAllDay, formatDate(startTime), formatDate(endTime));
        return s.toString();
    }

    private void addTag(StringBuilder s, String tag) {
        s.append('#');
        s.append(tag);
        s.append(' ');
    }

    private void constructExtraDataJson(StringBuilder s) {
        try {
            JSONObject json = new JSONObject();
            if (due != null) {
                json.put("due", dueFormatter.format(due.getTime()));
            }
            if (isAllDay) {
                json.put(
                        "scheduledTime",
                        new JSONObject().put("minute", startTime.get(Calendar.MINUTE)).put("hour",
                                startTime.get(Calendar.HOUR_OF_DAY)));
            }
            if (json.length() > 0) {
                s.append("\n\n");
                s.append(EXTRA_DATA_SEPERATOR);
                s.append(EXTRA_DATA_MAGIC_STRING);
                s.append(json.toString());
                s.append(EXTRA_DATA_MAGIC_STRING);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ExtraDataParser {
        private final static Pattern CONTEXT_PATTERN = Pattern.compile("(.*)" + "\n*" + EXTRA_DATA_SEPERATOR
                + EXTRA_DATA_MAGIC_STRING + "(.*)" + EXTRA_DATA_MAGIC_STRING);
        private static final ExtraDataParser INSTANCE = new ExtraDataParser();

        public static ExtraDataParser getInstance() {
            return INSTANCE;
        }

        public void parse(String inputText, Task task) {
            Matcher m = CONTEXT_PATTERN.matcher(inputText);
            if (!m.find()) {
                task.description = inputText;
                return;
            }
            task.description = m.group(1);
            try {
                JSONObject jsonObject = new JSONObject(m.group(2));
                if (jsonObject.has("due")) {
                    String due = jsonObject.getString("due");
                    try {
                        Date date = dueFormatter.parse(due);
                        if (date != null) {
                            task.due = GregorianCalendar.getInstance();
                            task.due.setTime(date);
                        }
                    } catch (ParseException e) {
                        Log.w(LOG_TAG, "Invalid due date: " + due, e);
                    }
                }
                if (task.isAllDay) {
                    JSONObject scheduledTimeJson = jsonObject.optJSONObject("scheduledTime");
                    if (scheduledTimeJson != null) {
                        task.startTime.set(Calendar.HOUR_OF_DAY, scheduledTimeJson.optInt("hour", 23));
                        task.startTime.set(Calendar.MINUTE, scheduledTimeJson.optInt("minute", 59));
                        task.startTime.set(Calendar.SECOND, 0);
                        task.startTime.set(Calendar.MILLISECOND, 0);
                        task.endTime = (Calendar) task.startTime.clone();
                    }
                }
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Invalid JSON: " + m.group(2), e);
            }
        }

    }

    public boolean isScheduledToday() {
        if (isAllDay) {

        } else {

        }
        // TODO Auto-generated method stub
        return false;
    }

}

class TagParser {

    public static class TagParseResult {
        public final List<String> tags;
        public final String text;

        public TagParseResult(List<String> tags, String text) {
            this.tags = tags;
            this.text = text;
        }
    }

    private final static Pattern CONTEXT_PATTERN = Pattern.compile("(?:^|\\s)#(\\w+:?\\w*)");
    private static final TagParser INSTANCE = new TagParser();

    public static TagParser getInstance() {
        return INSTANCE;
    }

    public TagParseResult parse(String inputText) {
        Matcher m = CONTEXT_PATTERN.matcher(inputText);
        List<String> tags = new ArrayList<String>();
        int end = 0;
        while (m.find()) {
            String context = m.group(1);
            tags.add(context);
            end = m.end();
        }
        String text = inputText.substring(end);
        return new TagParseResult(tags, text);
    }

}