package scheduler;

import ds.SortUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import model.Task;

/**
 * FCFS - First Come First Serve ("In the order added").
 * Data structure: QUEUE (FIFO). The task that was added first is done first.
 *
 * Steps:
 *   1. Sort tasks by ID (smaller ID = added earlier), our merge sort -> O(n log n)
 *   2. Enqueue all of them                                          -> O(1) each
 *   3. Dequeue one by one                                           -> O(1) each
 * Total: O(n log n)
 */
public class FCFSScheduler implements Scheduler {

    @Override
    public String getName() {
        return "In the order added (Queue)";
    }

    @Override
    public List<Task> schedule(List<Task> tasks) {
        List<Task> sorted = Scheduler.copyOf(tasks);
        SortUtils.mergeSort(sorted, SortUtils.BY_ID);

        Queue<Task> queue = new LinkedList<>();
        for (Task t : sorted) {
            queue.offer(t);                 // enqueue - O(1)
        }

        List<Task> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            order.add(queue.poll());        // dequeue - O(1)
        }
        return order;
    }
}
