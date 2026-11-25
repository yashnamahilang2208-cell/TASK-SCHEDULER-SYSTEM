package ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import model.Task;

/**
 * Our OWN sorting algorithms (no Collections.sort used).
 *
 *   Merge Sort : O(n log n) best/avg/worst, O(n) extra space, STABLE
 *   Quick Sort : O(n log n) average, O(n^2) worst, O(log n) stack space, NOT stable
 */
public class SortUtils {

    // ---------- ready-made comparators (ties broken so the order is always the same) ----------
    public static final Comparator<Task> BY_ID =
            Comparator.comparingInt(Task::getId);

    public static final Comparator<Task> BY_NAME =
            Comparator.comparing(Task::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(BY_ID);

    /** Priority 1 first; same priority -> earlier deadline first. */
    public static final Comparator<Task> BY_PRIORITY =
            Comparator.comparingInt(Task::getPriority).thenComparing(Task::getDeadline).thenComparing(BY_ID);

    /** Earliest deadline first; same deadline -> higher priority first. */
    public static final Comparator<Task> BY_DEADLINE =
            Comparator.comparing(Task::getDeadline).thenComparingInt(Task::getPriority).thenComparing(BY_ID);

    /** By start of the time slot (morning tasks first). */
    public static final Comparator<Task> BY_TIME =
            Comparator.comparing(Task::getTimeFrom).thenComparing(BY_ID);

    /** Shortest time slot first. */
    public static final Comparator<Task> BY_DURATION =
            Comparator.comparingLong(Task::getDurationMinutes).thenComparing(Task::getDeadline).thenComparing(BY_ID);

    private SortUtils() { }

    // =====================================================================
    //                              MERGE SORT
    // =====================================================================
    public static void mergeSort(List<Task> list, Comparator<Task> cmp) {
        if (list.size() < 2) {
            return;
        }
        List<Task> temp = new ArrayList<>(list);   // helper array
        mergeSort(list, temp, 0, list.size() - 1, cmp);
    }

    private static void mergeSort(List<Task> a, List<Task> temp, int low, int high, Comparator<Task> cmp) {
        if (low >= high) {
            return;
        }
        int mid = low + (high - low) / 2;
        mergeSort(a, temp, low, mid, cmp);        // sort left half
        mergeSort(a, temp, mid + 1, high, cmp);   // sort right half
        merge(a, temp, low, mid, high, cmp);      // merge both halves
    }

    private static void merge(List<Task> a, List<Task> temp, int low, int mid, int high, Comparator<Task> cmp) {
        for (int k = low; k <= high; k++) {
            temp.set(k, a.get(k));
        }
        int i = low, j = mid + 1, k = low;
        while (i <= mid && j <= high) {
            // "<=" keeps equal elements in original order -> stable
            if (cmp.compare(temp.get(i), temp.get(j)) <= 0) {
                a.set(k++, temp.get(i++));
            } else {
                a.set(k++, temp.get(j++));
            }
        }
        while (i <= mid) {
            a.set(k++, temp.get(i++));
        }
        while (j <= high) {
            a.set(k++, temp.get(j++));
        }
    }

    // =====================================================================
    //                              QUICK SORT
    // =====================================================================
    public static void quickSort(List<Task> list, Comparator<Task> cmp) {
        quickSort(list, 0, list.size() - 1, cmp);
    }

    private static void quickSort(List<Task> a, int low, int high, Comparator<Task> cmp) {
        if (low < high) {
            int p = partition(a, low, high, cmp);
            quickSort(a, low, p - 1, cmp);
            quickSort(a, p + 1, high, cmp);
        }
    }

    /** Lomuto partition: last element is the pivot. */
    private static int partition(List<Task> a, int low, int high, Comparator<Task> cmp) {
        Task pivot = a.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(a.get(j), pivot) <= 0) {
                i++;
                Collections.swap(a, i, j);
            }
        }
        Collections.swap(a, i + 1, high);
        return i + 1;
    }
}
