package es.gencom.mpegg.coder.tokens;

public class SearchDiffResult {
    private final int bestIndex;
    private final long cost;

    SearchDiffResult(int bestIndex, long cost) {
        this.bestIndex = bestIndex;
        this.cost = cost;
    }

    public int getBestIndex() {
        return bestIndex;
    }

    public long getCost() {
        return cost;
    }
}
