package scheduler;

import ds.MinHeap;
import ds.SortUtils;
import java.util.ArrayList;
import java.util.List;
import model.Task;

/**
 * SJF - Shortest Job First ("Shortest task first").
 * Data structure: OUR OWN MinHeap, ordered by the length of the time slot.
 *
 * Doing short tasks first finishes the most tasks quickly
 * (lowest average waiting time).
 *   insert / extractMin : O(log n)  ->  total O(n log n)
 */
public class SJFScheduler implements Scheduler {

    @Override
    public String getName() {
        return "Shortest task first (MinHeap)";
    }

    @Override
    public List<Task> schedule(List<Task> tasks) {
        MinHeap heap = new MinHeap(SortUtils.BY_DURATION);
        for (Task t : Scheduler.copyOf(tasks)) {
            heap.insert(t);                 // O(log n)
        }

        List<Task> order = new ArrayList<>();
        while (!heap.isEmpty()) {
            order.add(heap.extractMin());   // shortest task - O(log n)
        }
        return order;
    }
}
