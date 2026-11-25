package scheduler;

import ds.MinHeap;
import ds.SortUtils;
import java.util.ArrayList;
import java.util.List;
import model.Task;

/**
 * EDF - Earliest Deadline First.
 * Data structure: OUR OWN MinHeap, ordered by deadline (date + time).
 *
 * The most urgent task always comes out of the heap first.
 *   insert / extractMin : O(log n)  ->  total O(n log n)
 */
public class EDFScheduler implements Scheduler {

    @Override
    public String getName() {
        return "Earliest deadline first (MinHeap)";
    }

    @Override
    public List<Task> schedule(List<Task> tasks) {
        MinHeap heap = new MinHeap(SortUtils.BY_DEADLINE);
        for (Task t : Scheduler.copyOf(tasks)) {
            heap.insert(t);                 // O(log n)
        }

        List<Task> order = new ArrayList<>();
        while (!heap.isEmpty()) {
            order.add(heap.extractMin());   // earliest deadline - O(log n)
        }
        return order;
    }
}
