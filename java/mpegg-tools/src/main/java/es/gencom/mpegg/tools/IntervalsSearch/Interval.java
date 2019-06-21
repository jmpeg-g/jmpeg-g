package es.gencom.mpegg.tools.IntervalsSearch;

import java.util.Objects;

public class Interval<T> implements Comparable<Interval>{
    final private long start;
    final private long end;
    final T value;

    public Interval(long start, long end, T value) {
        this.start = start;
        this.end = end;
        this.value = value;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public T getValue() {
        return value;
    }

    public boolean intersects(long start, long end){
        return this.start < end && start < this.end;
    }

    public boolean intersects(Interval testInterval){
        return start < testInterval.end && testInterval.start < end;
    }

    @Override
    public int compareTo(Interval interval) {
        if(interval == null){
            System.err.println("found a null");
        }
        if(start == interval.start){
            return Long.compare(end, interval.end);
        }
        return Long.compare(start, interval.start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interval)) return false;
        Interval interval = (Interval) o;
        return getStart() == interval.getStart() &&
                getEnd() == interval.getEnd() &&
                Objects.equals(getValue(), interval.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd(), getValue());
    }
}
