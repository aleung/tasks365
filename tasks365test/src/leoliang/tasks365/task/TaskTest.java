package leoliang.tasks365.task;

import java.util.Calendar;

import junit.framework.TestCase;

public class TaskTest extends TestCase {

    public void testDescriptionTags1() {
        Task task = new Task();
        task.isAllDay = false;
        assertNull(task.due);
        task.setDescriptionWithExtraData("#new  #star {due:\"20100328\"}");
        assertNull(task.due);
        assertFalse(task.isNew);
        assertFalse(task.isStarred);
        assertFalse(task.isDone);
        assertEquals("#new  #star {due:\"20100328\"}", task.description);
        assertEquals("#new  #star {due:\"20100328\"}", task.getDescriptionWithExtraData());
    }

    public void testDescriptionTags1a() {
        Task task = new Task();
        task.isAllDay = false;
        assertNull(task.due);
        task.setDescriptionWithExtraData("description --- DO NOT MODIFY BELOW ---\n-#%#-{due:\"20100328\"}");
        assertNull(task.due);
        assertEquals("description --- DO NOT MODIFY BELOW ---\n-#%#-{due:\"20100328\"}", task.description);
        assertEquals("description --- DO NOT MODIFY BELOW ---\n-#%#-{due:\"20100328\"}",
                task.getDescriptionWithExtraData());
    }

    public void testDescriptionTags2() {
        Task task = new Task();
        task.isAllDay = false;
        task.setDescriptionWithExtraData("description\n--- DO NOT MODIFY BELOW ---\n-#%#-{due:\"20100328\"}-#%#- ignored");
        assertEquals(2010, task.due.get(Calendar.YEAR));
        assertEquals(2, task.due.get(Calendar.MONTH));
        assertEquals(28, task.due.get(Calendar.DAY_OF_MONTH));
        assertEquals("description", task.description);
        assertEquals("description\n\n--- DO NOT MODIFY BELOW ---\n-#%#-{\"due\":\"20100328\"}-#%#-",
                task.getDescriptionWithExtraData());
    }

    public void testDescriptionTags3() {
        Task task = new Task();
        task.isAllDay = false;
        task.setDescriptionWithExtraData("description\n--- DO NOT MODIFY BELOW ---\n-#%#-{due:\"2010-3-28\"}-#%#-");
        assertNull(task.due);
        assertEquals("description", task.description);
        assertEquals("description", task.getDescriptionWithExtraData());
    }

    public void testDescriptionTags4() {
        Task task = new Task();
        task.setDescriptionWithExtraData("--- DO NOT MODIFY BELOW ---\n-#%#-{startTime:1,endTime:2}-#%#-");
        assertNull(task.due);
        assertEquals(1, task.startTime);
        assertEquals(2, task.endTime);
        assertEquals("", task.description);
        assertEquals("\n\n--- DO NOT MODIFY BELOW ---\n-#%#-{\"startTime\":1,\"endTime\":2}-#%#-",
                task.getDescriptionWithExtraData());
    }

    public void testDescriptionTags5() {
        Task task = new Task();
        task.isAllDay = false;
        task.setDescriptionWithExtraData("");
        assertNull(task.due);
        assertEquals("", task.description);
        assertEquals("", task.getDescriptionWithExtraData());
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
