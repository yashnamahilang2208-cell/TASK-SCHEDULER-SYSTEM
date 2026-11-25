/**
 *http://localhost:8080/ 
 ye run krne se web site browser me open ho jaye gi
 * 1st) javac Main.java model/*.java ds/*.java scheduler/*.java util/*.java web/*.java
 * 2nd) java web.WebServer
 * ye src ke terminal me run krna se website browse r me open ho jaye gi
 */

package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ds.SortUtils;
import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Task;
import scheduler.EDFScheduler;
import scheduler.FCFSScheduler;
import scheduler.PriorityScheduler;
import scheduler.SJFScheduler;
import scheduler.Scheduler;
import util.FileHandler;

/**
 * BACKEND of the website.
 *
 * Uses Java's built-in HttpServer (no Spring Boot, nothing to install).
 * The page (index.html) calls these URLs; this class answers with JSON
 * by calling the SAME DSA classes that the console app uses.
 *
 *   GET  /                 -> the web page (index.html)
 *   GET  /api/tasks        -> all tasks (already sorted)
 *   POST /api/add          -> add a task
 *   POST /api/delete       -> delete a task by ID
 *   POST /api/mark         -> tick Done / Not Completed
 *   POST /api/sort         -> Merge Sort / Quick Sort the task table
 *   GET  /api/plan?by=..   -> execution details (Queue / PriorityQueue / MinHeap)
 *   POST /api/reset        -> restore the demo tasks
 *
 * Every change is saved automatically to tasks.csv (no database).
 *
 * Run:  java web.WebServer          (then open http://localhost:8080)
 */
public class WebServer {

    private static final DateTimeFormatter ISO_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm");

    private static final List<Task> tasks = new ArrayList<>();
    private static int nextId = 1;
    private static String sortField = "priority";
    private static String sortAlgo = "merge";

    public static void main(String[] args) throws IOException {
        int port = 8080;
        boolean openBrowser = true;
        for (String a : args) {
            if (a.equals("--no-browser")) openBrowser = false;
            else port = Integer.parseInt(a);
        }

        setTasks(FileHandler.loadOrSample());
        applySort();

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (BindException e) {
            System.out.println("Port " + port + " is already in use.");
            System.out.println("Maybe the server is already running in another terminal.");
            System.out.println("Stop it, or use another port:  java web.WebServer 8081");
            return;
        }
        server.createContext("/", WebServer::handle);
        server.start();

        String url = "http://localhost:" + port;
        System.out.println("==================================================");
        System.out.println("  TASK SCHEDULER SYSTEM - web server is running");
        System.out.println("  Open in your browser:  " + url);
        System.out.println("  Tasks are saved in:    " + Paths.get(FileHandler.DATA_FILE).toAbsolutePath().normalize());
        System.out.println("  Press Ctrl + C to stop the server");
        System.out.println("==================================================");

        if (openBrowser) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception ignored) {
                // if the browser cannot open automatically, the user opens the URL manually
            }
        }
    }

    // ==================================================================
    //                            ROUTING
    // ==================================================================
    private static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            Map<String, String> p = parseParams(ex.getRequestURI().getRawQuery());
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                p.putAll(parseParams(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            }

            switch (path) {
                case "/":
                case "/index.html":
                    send(ex, 200, "text/html; charset=utf-8", readIndexHtml());
                    break;
                case "/api/tasks":
                    sendJson(ex, 200, stateJson(null));
                    break;
                case "/api/add": {
                    Task t = addTask(p);
                    applySort();
                    persist();
                    sendJson(ex, 200, stateJson("Task #" + t.getId() + " added"));
                    break;
                }
                case "/api/delete": {
                    Task t = findTask(p);
                    tasks.remove(t);
                    persist();
                    sendJson(ex, 200, stateJson("Task #" + t.getId() + " deleted"));
                    break;
                }
                case "/api/mark":
                    sendJson(ex, 200, stateJson(markTask(p)));
                    break;
                case "/api/sort": {
                    String field = p.getOrDefault("field", sortField);
                    String algo = p.getOrDefault("algo", sortAlgo);
                    comparatorFor(field);                       // validates the field
                    if (!algo.equals("merge") && !algo.equals("quick")) {
                        throw new IllegalArgumentException("Unknown sort algorithm");
                    }
                    sortField = field;
                    sortAlgo = algo;
                    long micros = applySort();
                    persist();
                    sendJson(ex, 200, stateJson("Sorted by " + fieldLabel(field) + " using "
                            + (algo.equals("merge") ? "Merge Sort" : "Quick Sort") + " in " + micros + " µs"));
                    break;
                }
                case "/api/plan":
                    sendJson(ex, 200, planJson(p.getOrDefault("by", "priority")));
                    break;
                case "/api/reset":
                    setTasks(FileHandler.sampleTasks());
                    applySort();
                    persist();
                    sendJson(ex, 200, stateJson("Demo tasks restored"));
                    break;
                default:
                    sendJson(ex, 404, error("Not found: " + path));
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, error(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, error(e.toString()));
        }
    }

    // ==================================================================
    //                        TASK OPERATIONS
    // ==================================================================
    private static void setTasks(List<Task> list) {
        tasks.clear();
        tasks.addAll(list);
        nextId = 1;
        for (Task t : tasks) nextId = Math.max(nextId, t.getId() + 1);
    }

    private static void persist() {
        try {
            FileHandler.save(tasks, FileHandler.DATA_FILE);
        } catch (IOException e) {
            System.out.println("Could not save tasks: " + e.getMessage());
        }
    }

    private static Task addTask(Map<String, String> p) {
        String name = p.getOrDefault("name", "").replace(",", " ").trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Please enter a task name");
        if (name.length() > 40) name = name.substring(0, 40);
        int priority = intParam(p, "priority", "Priority", 1);

        LocalDateTime deadline;
        LocalTime from, to;
        try {
            deadline = LocalDateTime.parse(p.getOrDefault("deadline", ""));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please choose the deadline date and time");
        }
        try {
            from = LocalTime.parse(p.getOrDefault("timeFrom", ""));
            to = LocalTime.parse(p.getOrDefault("timeTo", ""));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please choose the time (from and to)");
        }
        if (from.equals(to)) throw new IllegalArgumentException("'From' and 'To' time cannot be the same");

        Task t = new Task(nextId++, name, priority, deadline, from, to);
        tasks.add(t);
        return t;
    }

    private static Task findTask(Map<String, String> p) {
        int id = intParam(p, "id", "Task ID", 0);
        for (Task t : tasks) {
            if (t.getId() == id) return t;
        }
        throw new IllegalArgumentException("Task #" + id + " not found");
    }

    private static String markTask(Map<String, String> p) {
        Task t = findTask(p);
        String action = p.getOrDefault("action", "");
        String msg;
        switch (action) {
            case "done":
                t.markDone(LocalDateTime.now().withSecond(0).withNano(0));
                msg = t.getStatus(LocalDateTime.now()) == Task.Status.DONE_LATE
                        ? "Task #" + t.getId() + " done (after the deadline)"
                        : "Task #" + t.getId() + " done on time - well done!";
                break;
            case "notcompleted":
                t.markNotCompleted();
                msg = "Task #" + t.getId() + " marked not completed";
                break;
            case "clear":
                t.clearMark();
                msg = "Task #" + t.getId() + " unticked";
                break;
            default:
                throw new IllegalArgumentException("Unknown action");
        }
        persist();
        return msg;
    }

    // ==================================================================
    //                           SORTING
    // ==================================================================
    private static Comparator<Task> comparatorFor(String field) {
        switch (field) {
            case "priority": return SortUtils.BY_PRIORITY;
            case "deadline": return SortUtils.BY_DEADLINE;
            case "time":     return SortUtils.BY_TIME;
            case "id":       return SortUtils.BY_ID;
            case "name":     return SortUtils.BY_NAME;
            default: throw new IllegalArgumentException("Unknown sort field");
        }
    }

    private static String fieldLabel(String field) {
        switch (field) {
            case "priority": return "Priority";
            case "deadline": return "Deadline";
            case "time":     return "Time";
            case "id":       return "Task ID";
            default:         return "Task Name";
        }
    }

    /** Sorts the task table with OUR merge sort / quick sort. Returns the time taken in µs. */
    private static long applySort() {
        Comparator<Task> cmp = comparatorFor(sortField);
        long start = System.nanoTime();
        if (sortAlgo.equals("quick")) SortUtils.quickSort(tasks, cmp);
        else SortUtils.mergeSort(tasks, cmp);
        return (System.nanoTime() - start) / 1000;
    }

    // ==================================================================
    //                     EXECUTION DETAILS (PLAN)
    // ==================================================================
    private static Scheduler pickScheduler(String by) {
        switch (by) {
            case "added":    return new FCFSScheduler();       // Queue
            case "priority": return new PriorityScheduler();   // PriorityQueue
            case "shortest": return new SJFScheduler();        // MinHeap
            case "deadline": return new EDFScheduler();        // MinHeap
            default: throw new IllegalArgumentException("Unknown plan: " + by);
        }
    }

    private static String planJson(String by) {
        Scheduler s = pickScheduler(by);
        List<Task> order = s.schedule(Scheduler.pending(tasks));
        return "{\"by\":" + str(by) + ",\"name\":" + str(s.getName())
                + ",\"order\":" + list(order, LocalDateTime.now()) + "}";
    }

    // ==================================================================
    //                          JSON HELPERS
    // ==================================================================
    private static String stateJson(String message) {
        LocalDateTime now = LocalDateTime.now();
        return "{\"now\":" + str(now.format(ISO_MINUTES))
                + ",\"today\":" + str(now.toLocalDate().toString())
                + ",\"sortField\":" + str(sortField)
                + ",\"sortAlgo\":" + str(sortAlgo)
                + ",\"message\":" + (message == null ? "null" : str(message))
                + ",\"tasks\":" + list(tasks, now) + "}";
    }

    private static String list(List<Task> l, LocalDateTime now) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(taskJson(l.get(i), now));
        }
        return sb.append("]").toString();
    }

    private static String taskJson(Task t, LocalDateTime now) {
        Task.Status s = t.getStatus(now);
        return "{\"id\":" + t.getId()
                + ",\"name\":" + str(t.getName())
                + ",\"priority\":" + t.getPriority()
                + ",\"done\":" + t.isDone()
                + ",\"notCompleted\":" + t.isNotCompleted()
                + ",\"deadline\":" + str(t.getDeadline().format(ISO_MINUTES))
                + ",\"date\":" + str(t.getDeadline().toLocalDate().toString())
                + ",\"deadlineText\":" + str(t.getDeadlineText())
                + ",\"timeFrom\":" + str(t.getTimeFrom().format(HOURS_MINUTES))
                + ",\"timeTo\":" + str(t.getTimeTo().format(HOURS_MINUTES))
                + ",\"timeText\":" + str(t.getTimeText())
                + ",\"slot\":" + str(t.getSlot())
                + ",\"duration\":" + str(t.getDurationText())
                + ",\"completedAt\":" + (t.getCompletedAt() == null ? "null" : str(t.getCompletedAt().format(ISO_MINUTES)))
                + ",\"status\":" + str(s.name())
                + ",\"statusText\":" + str(s.label()) + "}";
    }

    private static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    private static String error(String msg) {
        return "{\"error\":" + str(msg == null ? "Error" : msg) + "}";
    }

    // ==================================================================
    //                        HTTP HELPERS
    // ==================================================================
    private static Map<String, String> parseParams(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            String v = eq >= 0 ? pair.substring(eq + 1) : "";
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    private static int intParam(Map<String, String> p, String key, String label, int min) {
        try {
            int v = Integer.parseInt(p.getOrDefault(key, "").trim());
            if (v < min) throw new IllegalArgumentException(label + " must be at least " + min);
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    /** Looks for index.html next to the source (works from VS Code Run button and from terminal). */
    private static byte[] readIndexHtml() throws IOException {
        String[] candidates = { "web/index.html", "src/web/index.html", "index.html" };
        for (String c : candidates) {
            Path path = Paths.get(c);
            if (Files.exists(path)) return Files.readAllBytes(path);
        }
        try (InputStream in = WebServer.class.getResourceAsStream("index.html")) {
            if (in != null) return in.readAllBytes();
        }
        throw new FileNotFoundException("index.html not found - run the server from the 'src' or 'DSA project' folder");
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        send(ex, code, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        // lets the page work even if index.html is opened directly by double-click
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
