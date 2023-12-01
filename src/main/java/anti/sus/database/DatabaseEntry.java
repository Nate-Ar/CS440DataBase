package anti.sus.database;

public final class DatabaseEntry {
    private final Object entry;

    public DatabaseEntry(final Object entry) {
        this.entry = entry;
    }

    public int asInt() {
        return (int) this.entry;
    }

    public double asDouble() {
        return (double) this.entry;
    }

    public String asString() {
        return (String) this.entry;
    }
}
