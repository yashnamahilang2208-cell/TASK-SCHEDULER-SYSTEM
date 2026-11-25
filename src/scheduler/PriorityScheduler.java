package scheduler;

import ds.SortUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import model.Task;

/**
 * Priority Scheduling ("Priority first").
 * Data structure: java.util.PriorityQueue (a binary heap given by Java).
 * Priority 1 = most important, so it comes out first.
 * Same priority -> the one with the earlier deadline first.
 *
 *   offer / poll : O(log n)   ->   total O(n log n)
 */
public class PriorityScheduler implements Scheduler {

    @Override
    public String getName() {
        return "Priority first (PriorityQueue)";
    }

    @Override
    public List<Task> schedule(List<Task> tasks) {
        PriorityQueue<Task> pq = new PriorityQueue<>(SortUtils.BY_PRIORITY);
        for (Task t : Scheduler.copyOf(tasks)) {
            pq.offer(t);                    // O(log n)
        }

        List<Task> order = new ArrayList<>();
        while (!pq.isEmpty()) {
            order.add(pq.poll());           // highest priority task - O(log n)
        }
        return order;
    }
}
