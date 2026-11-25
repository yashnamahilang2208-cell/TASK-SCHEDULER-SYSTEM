package scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Task;

/**
 * Common interface for every scheduling strategy (polymorphism).
 * schedule() returns the tasks in the order they should be done
 * (this is the "Execution details" table on the website).
 */
public interface Scheduler {

    String getName();

    List<Task> schedule(List<Task> tasks);

    // ------------------------------------------------------------------
    //                    helper methods shared by all
    // ------------------------------------------------------------------

    /** Deep copy so the original task list is never modified. */
    static List<Task> copyOf(List<Task> tasks) {
        List<Task> copy = new ArrayList<>();
        for (Task t : tasks) {
            copy.add(new Task(t));
        }
        return copy;
    }

    /** Only the tasks that still have to be done (Done is not ticked). */
    static List<Task> pending(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.isDone()) result.add(t);
        }
        return result;
    }

    /** Prints the execution order as a table (console version). */
    static void printPlan(String title, List<Task> order) {
        System.out.println();
        System.out.println("==================== EXECUTION DETAILS: " + title + " ====================");
        if (order.isEmpty()) {
            System.out.println("Nothing pending - every task is done!");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        System.out.printf("%-5s %-4s %-20s %-9s %-22s %-11s %-22s %s%n",
                "Step", "ID", "Task Name", "Priority", "Time", "Duration", "Deadline", "Status");
        int step = 1;
        for (Task t : order) {
            System.out.printf("%-5d %-4d %-20s %-9d %-22s %-11s %-22s %s%n",
                    step++, t.getId(), t.getName(), t.getPriority(), t.getTimeText(),
                    t.getDurationText(), t.getDeadlineText(), t.getStatus(now).label());
        }
    }
}
