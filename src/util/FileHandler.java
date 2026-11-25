package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.Task;

/**
 * Saves / loads tasks to a simple CSV text file (no database needed).
 * The file is "tasks.csv" in the main "DSA project" folder.
 *
 * Format: id,name,priority,done,notCompleted,deadline,timeFrom,timeTo,completedAt
 */
public class FileHandler {

    private static final String HEADER = "id,name,priority,done,notCompleted,deadline,timeFrom,timeTo,completedAt";

    /** Same file whether the program is started from "DSA project" or from "DSA project/src". */
    public static final String DATA_FILE = resolveDataFile();

    private FileHandler() { }

    private static String resolveDataFile() {
        if (Files.isDirectory(Paths.get("src"))) {
            return "tasks.csv";                                   // started from the project folder
        }
        if (Files.exists(Paths.get("Main.java"))) {
            return Paths.get("..", "tasks.csv").toString();       // started from the src folder
        }
        return "tasks.csv";
    }

    public static void save(List<Task> tasks, String path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(HEADER);
            bw.newLine();
            for (Task t : tasks) {
                bw.write(t.getId() + "," + t.getName().replace(",", " ") + "," + t.getPriority() + ","
                        + t.isDone() + "," + t.isNotCompleted() + ","
                        + t.getDeadline() + "," + t.getTimeFrom() + "," + t.getTimeTo() + ","
                        + (t.getCompletedAt() == null ? "" : t.getCompletedAt().toString()));
                bw.newLine();
            }
        }
    }

    public static List<Task> load(String path) throws IOException {
        List<Task> tasks = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();               // skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p.length != 9) continue;           // skip bad / old-format lines
                try {
                    Task t = new Task(
                            Integer.parseInt(p[0].trim()), p[1].trim(), Integer.parseInt(p[2].trim()),
                            LocalDateTime.parse(p[5].trim()), LocalTime.parse(p[6].trim()), LocalTime.parse(p[7].trim()));
                    String done = p[8].trim();
                    t.restore(Boolean.parseBoolean(p[3].trim()), Boolean.parseBoolean(p[4].trim()),
                            done.isEmpty() ? null : LocalDateTime.parse(done));
                    tasks.add(t);
                } catch (RuntimeException e) {
                    // skip bad line
                }
            }
        }
        return tasks;
    }

    /** Loads tasks.csv if it exists, otherwise gives the demo tasks. */
    public static List<Task> loadOrSample() {
        if (Files.exists(Paths.get(DATA_FILE))) {
            try {
                return load(DATA_FILE);
            } catch (IOException e) {
                System.out.println("Could not read " + DATA_FILE + ", using demo tasks.");
            }
        }
        return sampleTasks();
    }

    /** Demo tasks around today's date, so every colour shows up in the graphs. */
    public static List<Task> sampleTasks() {
        LocalDate d = LocalDate.now();
        List<Task> list = new ArrayList<>();

        list.add(make(1,  "Submit DSA project",   1, d.plusDays(3),    "18:00", "10:00", "12:00"));
        list.add(make(2,  "Morning walk",         4, d,                "09:00", "06:30", "07:15"));
        list.get(1).markDone(d.atTime(7, 20));
        list.add(make(3,  "Revise heap notes",    2, d,                "21:00", "14:00", "16:00"));
        list.add(make(4,  "Call home",            5, d,                "22:30", "21:30", "22:00"));
        list.add(make(5,  "Laundry",              4, d,                "13:00", "12:00", "12:45"));
        list.get(4).markDone(d.atTime(14, 0));
        list.add(make(6,  "Yoga",                 5, d,                "07:00", "06:00", "06:30"));
        list.get(5).markNotCompleted();
        list.add(make(7,  "Pay electricity bill", 1, d.minusDays(1),   "17:00", "11:00", "11:30"));
        list.get(6).markDone(d.minusDays(1).atTime(16, 0));
        list.add(make(8,  "Gym session",          3, d.minusDays(1),   "20:00", "18:00", "19:00"));
        list.get(7).markNotCompleted();
        list.add(make(9,  "Lab record",           2, d.minusDays(2),   "10:00", "21:00", "22:30"));
        list.get(8).markDone(d.minusDays(2).atTime(13, 0));
        list.add(make(10, "Read Java chapter",    3, d.minusDays(3),   "22:00", "21:00", "22:00"));
        list.get(9).markDone(d.minusDays(3).atTime(21, 55));
        list.add(make(11, "Clean room",           5, d.minusDays(4),   "12:00", "10:00", "11:00"));
        list.get(10).markDone(d.minusDays(4).atTime(11, 10));
        list.add(make(12, "Group meeting",        2, d.minusDays(6),   "16:00", "15:00", "16:00"));
        list.get(11).markDone(d.minusDays(6).atTime(15, 58));
        list.add(make(13, "Grocery shopping",     4, d.minusDays(8),   "19:00", "17:30", "18:30"));
        list.add(make(14, "Mock test",            1, d.minusDays(10),  "11:00", "09:00", "11:00"));
        list.get(13).markDone(d.minusDays(10).atTime(10, 50));
        list.add(make(15, "Assignment 3",         2, d.minusDays(13),  "23:00", "20:00", "22:00"));
        list.get(14).markDone(d.minusDays(12).atTime(9, 0));
        list.add(make(16, "Prepare for viva",     1, d.plusDays(2),    "20:00", "19:00", "20:30"));
        list.add(make(17, "Dentist visit",        3, d.plusDays(5),    "16:00", "15:00", "16:00"));
        return list;
    }

    private static Task make(int id, String name, int priority, LocalDate day,
                             String deadlineTime, String from, String to) {
        return new Task(id, name, priority, day.atTime(LocalTime.parse(deadlineTime)),
                LocalTime.parse(from), LocalTime.parse(to));
    }
}
