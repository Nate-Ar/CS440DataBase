package anti.sus.database;

public final class FilterWord {
    private final String filterWord;
    private int numViolations;

    public FilterWord(final String filterWord, final int numViolations) {
        this.filterWord = filterWord;
        this.numViolations = numViolations;
    }

    public String getFilterWord() {
        return filterWord;
    }

    public int getNumViolations() {
        return numViolations;
    }

    public void setNumViolations(final int numViolations) {
        this.numViolations = numViolations;
    }
}
