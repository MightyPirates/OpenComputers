package li.cil.oc.util;

public final class MovingAverage {
    private final int[] data;
    private int head;
    private int cachedAverage;
    private boolean dirty = true;

    // ----------------------------------------------------------------------- //

    public MovingAverage(final int size) {
        data = new int[size];
    }

    public int get() {
        if (dirty) {
            int sum = 0;
            for (final int value : data) {
                sum += value;
            }
            cachedAverage = sum / data.length;
            dirty = false;
        }
        return cachedAverage;
    }

    public void put(final int value) {
        data[head] = value;
        head = (head + 1) % data.length;
        dirty = true;
    }
}
