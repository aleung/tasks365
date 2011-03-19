package leoliang.tasks365.task;

import java.util.List;


public interface QueryResultObserver {
    void onResultChanged(List<Task> result);
    void onInvalidated();
}