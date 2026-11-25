package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Task = one thing the user has to do.
 *
 * Columns (same as the website table):
 *   ID | Task Name | Priority | Done | Not Completed | Deadline | Time
 *
 *   priority      : 1 = highest (do this first)
 *   done          : ticked when the task is finished
 *   notCompleted  : ticked when the user gives up on the task
 *   deadline      : date + time it must be finished by (e.g. 18 Nov 2026, 6:00 PM)
 *   timeFrom/To   : the time slot to work on it (e.g. 9:00 AM - 11:00 AM -> Morning)
 */
public class Task {

    /** Result of a task. The website uses: green, orange, red (and grey for upcoming). */
    public enum Status {
        DONE_ON_TIME("Done on time"),     // green
        DONE_LATE("Done late"),           // orange
        NOT_COMPLETED("Not completed"),   // red
        UPCOMING("Upcoming");             // deadline not reached yet

        private final String label;

        Status(String label) { this.label = label; }

        public String label() { return label; }
    }

    public static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    // ---------- details entered by the user ----------
    private final int id;
    private final String name;
    private final int priority;
    private final LocalDateTime deadline;
    private final LocalTime timeFrom;
    private final LocalTime timeTo;

    // ---------- progress ----------
    private boolean done;
    private boolean notCompleted;
    private LocalDateTime completedAt;   // when "Done" was ticked

    public Task(int id, String name, int priority, LocalDateTime deadline, LocalTime timeFrom, LocalTime timeTo) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.deadline = deadline;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
    }

    /** Copy constructor - schedulers work on copies so the original list is never changed. */
    public Task(Task other) {
        this(other.id, other.name, other.priority, other.deadline, other.timeFrom, other.timeTo);
        this.done = other.done;
        this.notCompleted = other.notCompleted;
        this.completedAt = other.completedAt;
    }

    // ---------- tick boxes ----------
    public void markDone(LocalDateTime when) {
        done = true;
        notCompleted = false;
        completedAt = when;
    }

    public void markNotCompleted() {
        notCompleted = true;
        done = false;
        completedAt = null;
    }

    public void clearMark() {
        done = false;
        notCompleted = false;
        completedAt = null;
    }

    /** Used when loading from the CSV file. */
    public void restore(boolean done, boolean notCompleted, LocalDateTime completedAt) {
        this.done = done;
        this.notCompleted = notCompleted && !done;
        this.completedAt = done ? completedAt : null;
    }

    /**
     *   Done before deadline          -> DONE_ON_TIME  (green)
     *   Done after deadline           -> DONE_LATE     (orange)
     *   Not Completed ticked, or
     *   deadline passed and not done  -> NOT_COMPLETED (red)
     *   otherwise                     -> UPCOMING
     */
    public Status getStatus(LocalDateTime now) {
        if (done) {
            return (completedAt == null || !completedAt.isAfter(deadline)) ? Status.DONE_ON_TIME : Status.DONE_LATE;
        }
        if (notCompleted || now.isAfter(deadline)) {
            return Status.NOT_COMPLETED;
        }
        return Status.UPCOMING;
    }

    /** Length of the time slot in minutes (used by "Shortest task first"). */
    public long getDurationMinutes() {
        long m = Duration.between(timeFrom, timeTo).toMinutes();
        return m <= 0 ? m + 24 * 60 : m;     // slot like 11:00 PM - 12:30 AM crosses midnight
    }

    /** Morning / Afternoon / Evening / After Dinner, decided by the start time. */
    public String getSlot() {
        int h = timeFrom.getHour();
        if (h >= 5 && h < 12)  return "Morning";
        if (h >= 12 && h < 17) return "Afternoon";
        if (h >= 17 && h < 21) return "Evening";
        return "After Dinner";
    }

    public String getDeadlineText() { return deadline.format(DEADLINE_FORMAT); }

    public String getTimeText() { return timeFrom.format(TIME_FORMAT) + " - " + timeTo.format(TIME_FORMAT); }

    public String getDurationText() {
        long m = getDurationMinutes();
        long h = m / 60, r = m % 60;
        if (h == 0) return r + " min";
        if (r == 0) return h + " h";
        return h + " h " + r + " min";
    }

    // ---------- getters ----------
    public int getId()                   { return id; }
    public String getName()              { return name; }
    public int getPriority()             { return priority; }
    public LocalDateTime getDeadline()   { return deadline; }
    public LocalTime getTimeFrom()       { return timeFrom; }
    public LocalTime getTimeTo()         { return timeTo; }
    public boolean isDone()              { return done; }
    public boolean isNotCompleted()      { return notCompleted; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    // ---------- console table ----------
    public static String header() {
        return String.format("%-4s %-20s %-9s %-6s %-14s %-22s %s",
                "ID", "Task Name", "Priority", "Done", "Not Completed", "Deadline", "Time");
    }

    @Override
    public String toString() {
        Status s = getStatus(LocalDateTime.now());
        return String.format("%-4d %-20s %-9d %-6s %-14s %-22s %s (%s)",
                id, name, priority,
                done ? "[x]" : "[ ]",
                s == Status.NOT_COMPLETED ? "[x]" : "[ ]",
                getDeadlineText(), getTimeText(), getSlot());
    }
}
