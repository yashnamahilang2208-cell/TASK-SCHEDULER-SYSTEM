package ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import model.Task;

/**
 * Our OWN binary Min-Heap (no java.util.PriorityQueue used here).
 *
 * Stored in an array (ArrayList). For a node at index i:
 *     parent = (i - 1) / 2
 *     left   = 2 * i + 1
 *     right  = 2 * i + 2
 *
 * The Comparator decides what "smallest" means, so the same heap is used
 * for SJF (smallest burst time) and EDF (earliest deadline).
 *
 * Complexities:
 *     insert     : O(log n)
 *     extractMin : O(log n)
 *     peek       : O(1)
 *     size/empty : O(1)
 */
public class MinHeap {

    private final List<Task> heap = new ArrayList<>();
    private final Comparator<Task> cmp;

    public MinHeap(Comparator<Task> cmp) {
        this.cmp = cmp;
    }

    private int parent(int i) { return (i - 1) / 2; }
    private int left(int i)   { return 2 * i + 1; }
    private int right(int i)  { return 2 * i + 2; }

    /** Add at the end, then bubble it UP to its correct place. O(log n) */
    public void insert(Task t) {
        heap.add(t);
        heapifyUp(heap.size() - 1);
    }

    /** Remove the root (minimum), move last element to root, bubble it DOWN. O(log n) */
    public Task extractMin() {
        if (heap.isEmpty()) {
            return null;
        }
        Task min = heap.get(0);
        Task last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return min;
    }

    /** Look at the minimum without removing it. O(1) */
    public Task peek() {
        return heap.isEmpty() ? null : heap.get(0);
    }

    public boolean isEmpty() { return heap.isEmpty(); }

    public int size() { return heap.size(); }

    /** Copy of the internal array, level by level (used by the website to draw the heap tree). */
    public List<Task> toList() { return new ArrayList<>(heap); }

    private void heapifyUp(int i) {
        while (i > 0 && cmp.compare(heap.get(i), heap.get(parent(i))) < 0) {
            Collections.swap(heap, i, parent(i));
            i = parent(i);
        }
    }

    private void heapifyDown(int i) {
        int smallest = i;
        int l = left(i);
        int r = right(i);

        if (l < heap.size() && cmp.compare(heap.get(l), heap.get(smallest)) < 0) {
            smallest = l;
        }
        if (r < heap.size() && cmp.compare(heap.get(r), heap.get(smallest)) < 0) {
            smallest = r;
        }
        if (smallest != i) {
            Collections.swap(heap, i, smallest);
            heapifyDown(smallest);
        }
    }
}
