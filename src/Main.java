import ds.SortUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Predicate;
import model.Task;
import scheduler.EDFScheduler;
import scheduler.FCFSScheduler;
import scheduler.PriorityScheduler;
import scheduler.SJFScheduler;
import scheduler.Scheduler;
import util.FileHandler;

/**
 * TASK SCHEDULER SYSTEM - menu driven console application.
 * (The website version is in web/WebServer.java)
 *
 * Task table columns: ID | Task Name | Priority | Done | Not Completed | Deadline | Time
 *
 * Data structures used:
 *   Queue          -> plan "in the order added"
 *   PriorityQueue  -> plan "priority first"
 *   MinHeap (own)  -> plan "shortest task first" and "earliest deadline first"
 *   Sorting (own)  -> Merge Sort / Quick Sort of the task table
 */
public class Main {

    private static final List<Task> tasks = new ArrayList<>();
    private static final Scanner sc = new Scanner(System.in);
    private static int nextId = 1;

    private static final DateTimeFormatter DATE_IN = DateTimeFormatter.ofPattern("d-M-uuuu");
    private static final DateTimeFormatter TIME_IN = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);

    public static void main(String[] args) {
        setTasks(FileHandler.loadOrSample());
        System.out.println("Welcome to the TASK SCHEDULER SYSTEM");
        System.out.println(tasks.size() + " tasks loaded. Changes are saved automatically to " + FileHandler.DATA_FILE);

        while (true) {
            printMenu();
            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1:  addTask(); break;
                case 2:  viewTasks(); break;
                case 3:  deleteTask(); break;
                case 4:  markTask(true); break;
                case 5:  markTask(false); break;
                case 6:  sortTasks(); break;
                case 7:  plan(new FCFSScheduler()); break;
                case 8:  plan(new PriorityScheduler()); break;
                case 9:  plan(new SJFScheduler()); break;
                case 10: plan(new EDFScheduler()); break;
                case 11: summary(); break;
                case 12: setTasks(FileHandler.sampleTasks()); save(); System.out.println("Demo tasks restored."); break;
                case 0:
                    System.out.println("Exiting... Thank you!");
                    return;
                default:
                    System.out.println("Invalid choice, try again.");
            }
        }
    }

    // ------------------------------------------------------------------
    private static void printMenu() {
        System.out.println();
        System.out.println("============= TASK SCHEDULER SYSTEM =============");
        System.out.println(" 1. Add Task");
        System.out.println(" 2. View All Tasks");
        System.out.println(" 3. Delete Task (by ID)");
        System.out.println(" 4. Tick Done");
        System.out.println(" 5. Tick Not Completed");
        System.out.println(" 6. Sort Tasks (Merge Sort / Quick Sort)");
        System.out.println(" 7. Execution details: In the order added     [Queue]");
        System.out.println(" 8. Execution details: Priority first         [PriorityQueue]");
        System.out.println(" 9. Execution details: Shortest task first    [MinHeap]");
        System.out.println("10. Execution details: Earliest deadline first[MinHeap]");
        System.out.println("11. Summary (Today / Week / Month)");
        System.out.println("12. Restore demo tasks");
        System.out.println(" 0. Exit");
        System.out.println("=================================================");
    }

    private static void setTasks(List<Task> list) {
        tasks.clear();
        tasks.addAll(list);
        nextId = 1;
        for (Task t : tasks) nextId = Math.max(nextId, t.getId() + 1);
    }

    private static void save() {
        try {
            FileHandler.save(tasks, FileHandler.DATA_FILE);
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static Task find(int id) {
        for (Task t : tasks) if (t.getId() == id) return t;
        return null;
    }

    // ---------------------------- 1. Add ------------------------------
    private static void addTask() {
        System.out.print("Task name: ");
        String name = readLine().trim().replace(",", " ");
        if (name.isEmpty()) name = "Task" + nextId;
        int pr = readIntMin("Priority (1 = highest): ", 1);
        LocalDate date = readDate("Deadline date (dd-mm-yyyy): ");
        LocalTime dTime = readTime("Deadline time (e.g. 6:00 PM): ");
        LocalTime from = readTime("Time slot - from (e.g. 9:00 AM): ");
        LocalTime to = readTime("Time slot - to   (e.g. 11:00 AM): ");
        Task t = new Task(nextId++, name, pr, date.atTime(dTime), from, to);
        tasks.add(t);
        save();
        System.out.println("Task added with ID " + t.getId() + " (" + t.getSlot() + ")");
    }

    // ---------------------------- 2. View -----------------------------
    private static void viewTasks() {
        System.out.println();
        System.out.println("---------- ALL TASKS ----------");
        if (tasks.isEmpty()) {
            System.out.println("No tasks.");
            return;
        }
        System.out.println(Task.header());
        for (Task t : tasks) System.out.println(t);
        System.out.println("Total tasks: " + tasks.size());
    }

    // ---------------------------- 3. Delete ---------------------------
    private static void deleteTask() {
        int id = readInt("Enter Task ID to delete: ");
        Task t = find(id);
        if (t == null) {
            System.out.println("Task " + id + " not found.");
            return;
        }
        System.out.print("Delete task " + id + " \"" + t.getName() + "\"? (y/n): ");
        if (readLine().trim().equalsIgnoreCase("y")) {
            tasks.remove(t);
            save();
            System.out.println("Task " + id + " deleted.");
        } else {
            System.out.println("Not deleted.");
        }
    }

    // ------------------------ 4/5. Done / Not Completed ---------------
    private static void markTask(boolean done) {
        int id = readInt("Enter Task ID: ");
        Task t = find(id);
        if (t == null) {
            System.out.println("Task " + id + " not found.");
            return;
        }
        if (done) {
            t.markDone(LocalDateTime.now().withSecond(0).withNano(0));
            System.out.println("Task " + id + " ticked Done -> " + t.getStatus(LocalDateTime.now()).label());
        } else {
            t.markNotCompleted();
            System.out.println("Task " + id + " ticked Not Completed.");
        }
        save();
    }

    // ---------------------------- 6. Sort -----------------------------
    private static void sortTasks() {
        System.out.println("Sort by: 1.Priority  2.Deadline  3.Time  4.ID  5.Task Name");
        int f = readInt("Choose field: ");
        Comparator<Task> cmp;
        String field;
        switch (f) {
            case 1: cmp = SortUtils.BY_PRIORITY; field = "Priority";  break;
            case 2: cmp = SortUtils.BY_DEADLINE; field = "Deadline";  break;
            case 3: cmp = SortUtils.BY_TIME;     field = "Time";      break;
            case 4: cmp = SortUtils.BY_ID;       field = "ID";        break;
            case 5: cmp = SortUtils.BY_NAME;     field = "Task Name"; break;
            default: System.out.println("Invalid field."); return;
        }
        System.out.println("Algorithm: 1.Merge Sort  2.Quick Sort");
        int a = readInt("Choose algorithm: ");
        long start = System.nanoTime();
        String algo;
        if (a == 1) {
            SortUtils.mergeSort(tasks, cmp);
            algo = "Merge Sort";
        } else if (a == 2) {
            SortUtils.quickSort(tasks, cmp);
            algo = "Quick Sort";
        } else {
            System.out.println("Invalid algorithm.");
            return;
        }
        long micros = (System.nanoTime() - start) / 1000;
        save();
        viewTasks();
        System.out.println("Sorted by " + field + " using " + algo + " in " + micros + " microseconds");
    }

    // ------------------------- 7-10. Execution details ----------------
    private static void plan(Scheduler s) {
        Scheduler.printPlan(s.getName(), s.schedule(Scheduler.pending(tasks)));
    }

    // ---------------------------- 11. Summary -------------------------
    private static void summary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);   // Monday
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth month = YearMonth.from(today);

        System.out.println();
        System.out.println("==================== SUMMARY ====================");
        printCounts("Today      (" + today.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)) + ")",
                t -> t.getDeadline().toLocalDate().equals(today));
        printCounts("This week  (" + weekStart.getDayOfMonth() + " - " + weekEnd.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)) + ")",
                t -> !t.getDeadline().toLocalDate().isBefore(weekStart) && !t.getDeadline().toLocalDate().isAfter(weekEnd));
        printCounts("This month (" + month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)) + ")",
                t -> YearMonth.from(t.getDeadline()).equals(month));
    }

    private static void printCounts(String title, Predicate<Task> filter) {
        LocalDateTime now = LocalDateTime.now();
        int onTime = 0, late = 0, missed = 0, upcoming = 0;
        for (Task t : tasks) {
            if (!filter.test(t)) continue;
            switch (t.getStatus(now)) {
                case DONE_ON_TIME:  onTime++;   break;
                case DONE_LATE:     late++;     break;
                case NOT_COMPLETED: missed++;   break;
                default:            upcoming++;
            }
        }
        System.out.println(title);
        System.out.printf("   Done on time: %d | Done late: %d | Not completed: %d | Upcoming: %d%n",
                onTime, late, missed, upcoming);
    }

    // ------------------------- input helpers --------------------------
    private static String readLine() {
        if (!sc.hasNextLine()) {          // input finished (e.g. Ctrl+Z)
            System.out.println();
            System.out.println("Input closed. Exiting.");
            System.exit(0);
        }
        return sc.nextLine();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static int readIntMin(String prompt, int min) {
        while (true) {
            int v = readInt(prompt);
            if (v >= min) return v;
            System.out.println("Value must be >= " + min + ".");
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(readLine().trim(), DATE_IN);
            } catch (DateTimeParseException e) {
                System.out.println("Please type the date like 25-11-2026.");
            }
        }
    }

    private static LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalTime.parse(readLine().trim(), TIME_IN);
            } catch (DateTimeParseException e) {
                System.out.println("Please type the time like 9:30 AM or 6:00 PM.");
            }
        }
    }
}
