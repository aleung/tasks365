package leoliang.tasks365.calendar;

import java.util.Calendar;

import junit.framework.TestCase;

public class TaskTest extends TestCase {

    public void testDescriptionTags1() {
        Task task = new Task();
        assertNull(task.due);
        task.setDescriptionWithTags("#new  #star #due");
        assertNull(task.due);
        assertFalse(task.isNew);
        assertFalse(task.isStarred);
        assertFalse(task.isDone);
        assertEquals("", task.description);
        assertEquals("", task.getDescriptionWithTags());
    }

    public void testDescriptionTags2() {
        Task task = new Task();
        task.setDescriptionWithTags("#new  #due:20100328  description");
        assertEquals(2010, task.due.get(Calendar.YEAR));
        assertEquals(2, task.due.get(Calendar.MONTH));
        assertEquals(28, task.due.get(Calendar.DAY_OF_MONTH));
        assertEquals("description", task.description);
        assertEquals("#due:20100328 description", task.getDescriptionWithTags());
    }

    public void testDescriptionTags3() {
        Task task = new Task();
        task.setDescriptionWithTags("#due:2010-3-28  description");
        assertNull(task.due);
        assertEquals("-3-28  description", task.description);
        assertEquals("-3-28  description", task.getDescriptionWithTags());
    }

    public void testDescriptionTags4() {
        Task task = new Task();
        task.setDescriptionWithTags("#due:2010feb23  description");
        assertNull(task.due);
        assertEquals("description", task.description);
        assertEquals("description", task.getDescriptionWithTags());
    }

    public void testDescriptionTags5() {
        Task task = new Task();
        task.setDescriptionWithTags("");
        assertNull(task.due);
        assertEquals("", task.description);
        assertEquals("", task.getDescriptionWithTags());
    }

    public void testTitleTags1() {
        Task task = new Task();
        assertFalse(task.isNew);
        assertTrue(task.isAllDay);
        assertFalse(task.isStarred);
        task.setTitleWithTags("#new  #star");
        assertTrue(task.isNew);
        assertTrue(task.isAllDay);
        assertTrue(task.isStarred);
        assertEquals("", task.title);
        String titleWithTags = task.getTitleWithTags();
        assertEquals("#star #new ", titleWithTags);
    }

    public void testTitleTags2() {
        Task task = new Task();
        assertFalse(task.isNew);
        assertFalse(task.isDone);
        task.setTitleWithTags(" #done Title here ");
        assertFalse(task.isNew);
        assertTrue(task.isDone);
        assertFalse(task.isStarred);
        assertEquals("Title here", task.title);
        task.title = " <New title> ";
        String titleWithTags = task.getTitleWithTags();
        assertEquals("#done <New title>", titleWithTags);
    }

    public void testTitleTags3() {
        Task task = new Task();
        task.setTitleWithTags("#unknow 中文 English.");
        assertFalse(task.isNew);
        assertFalse(task.isStarred);
        assertFalse(task.isDone);
        assertEquals("中文 English", task.title); // I don't know why it fails
    }

    public void testTitleTags4() {
        Task task = new Task();
        task.setTitleWithTags("new: no tags");
        assertFalse(task.isNew);
        assertFalse(task.isStarred);
        assertFalse(task.isDone);
        assertEquals("new: no tags", task.title);
    }

}
