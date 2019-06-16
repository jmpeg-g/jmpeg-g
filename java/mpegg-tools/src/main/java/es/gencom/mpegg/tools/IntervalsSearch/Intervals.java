package es.gencom.mpegg.tools.IntervalsSearch;

import java.util.Arrays;

public class Intervals<T> {
    private final Interval<T>[] intervals;
    public final long[] maxEnd;

    public Intervals(Interval<T>[] intervals) {
        this.intervals = intervals;
        Arrays.sort(intervals);
        maxEnd = new long[intervals.length];

        if(intervals.length == 0){
            return;
        }
        setMax(0, intervals.length);

    }

    private long setMax(int start, int end){
        if(end <= start){
            throw new IllegalArgumentException();
        }
        int center = (int) Math.ceil((end-start)/2) + start;
        long maxLeft = intervals[center].getEnd();
        long maxRight = intervals[center].getEnd();

        if(start != center){
            maxLeft = setMax(start, center);
        }
        if(center != end-1){
            maxRight = setMax(center+1, end);
        }
        maxEnd[center] = Math.max(maxLeft, maxRight);
        return maxEnd[center];
    }

    public Interval[] getIntersects(long startInterval, long endInterval){
        Interval[] intervalsResult = new Interval[this.intervals.length];
        int sizeResult = searchIntersects(intervalsResult, startInterval, endInterval, 0, intervals.length, 0);
        return Arrays.copyOf(intervalsResult, sizeResult);
    }

    private int searchIntersects(
            Interval[] intervalsResult,
            long startInterval,
            long endInterval,
            int start,
            int end,
            int previousResults
    ) {
        if(end <= start){
            throw new IllegalArgumentException();
        }
        int center = (int) Math.ceil((end-start)/2) + start;
        if(maxEnd[center] < startInterval){
            return previousResults;
        }


        if(intervals[center].intersects(startInterval, endInterval)){
            intervalsResult[previousResults] = intervals[center];
            previousResults++;
        }

        if(start != center){
            previousResults = searchIntersects(
                    intervalsResult,
                    startInterval,
                    endInterval,
                    start,
                    center,
                    previousResults
            );
        }
        if(center != end-1 && intervals[center].getStart() <= endInterval ){
            previousResults = searchIntersects(
                    intervalsResult,
                    startInterval,
                    endInterval,
                    center+1,
                    end,
                    previousResults
            );
        }

        return previousResults;
    }


}
