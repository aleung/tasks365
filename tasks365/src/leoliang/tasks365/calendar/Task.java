package leoliang.tasks365.calendar;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import leoliang.tasks365.calendar.TagParser.TagParseResult;

/**
 * This class stores information of a task in calendar.
 */
public class Task {

    private static final String LOG_TAG = "tasks365.Task";

    private static final String TAG_NEW = "new";
    private static final String TAG_STAR = "star";
    private static final String TAG_DONE = "done";
    private static final String TAG_DUE = "due:";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    // fields directly maps to content provider data model
    public long id;
    public long calendarId;
    public long startTime;
    public long endTime;
    public boolean isAllDay = true;

    /** pure title, without tags */
    public String title;

    /** pure description, without tags */
    public String description;

    // fields stored as tags
    public boolean isDone = false;
    public boolean isNew = false;
    public boolean isStarred = false;
    public Calendar due = null;

    public String getDescriptionWithTags() {
        StringBuilder s = new StringBuilder();
        if (due != null) {
            addTag(s, TAG_DUE + dateFormat.format(due.getTime()));
        }
        if (description != null) {
            s.append(description.trim());
        }
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
     * Tags can be put inside description:
     * <ul>
     * <li>#due:YYYY-MM-DD</li>
     * </ul>
     * 
     * @param s
     */
    public void setDescriptionWithTags(String s) {
        //        Log.v(LOG_TAG, "setDescriptionWithTags(" + s + ")");
        if (s == null) {
            return;
        }
        TagParseResult parseResult = TagParser.getInstance().parse(s);
        description = parseResult.text.trim();
        for (String tag : parseResult.tags) {
            if (tag.startsWith(TAG_DUE)) {
                Date date = dateFormat.parse(tag, new ParsePosition(TAG_DUE.length()));
                if (date != null) {
                    due = GregorianCalendar.getInstance();
                    due.setTime(date);
                }
                continue;
            }
        }
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

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Formatter formatter = new Formatter(s);
        formatter.format("{id:%d, calendarId:%d, title:%s, isAllDay:%b, startTime:%d, endTime:%d}", id, calendarId,
                title, isAllDay, startTime, endTime);
        return s.toString();
    }

    private void addTag(StringBuilder s, String tag) {
        s.append('#');
        s.append(tag);
        s.append(' ');
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