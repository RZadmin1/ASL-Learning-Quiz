package cs477.gmu.p2_rzaman9;

public class RecordRow {
    private final long recordId;  // _id from records table
    private final String score;  // "Score # / Total" e.g. "7/10"
    private final String timestamp;  // "MM/DD/YYYY HH:MM *M" (AM/PM)

    public RecordRow(long recordId, String score, String timestamp) {
        this.recordId = recordId;
        this.score = score;
        this.timestamp = timestamp;
    }

    public long getRecordId() { return recordId; }
    public String getScore() { return score; }
    public String getTimestamp() { return timestamp; }
}