package cs477.gmu.p2_rzaman9;

public class RecordRow {
    private final long recordId;  // _id from records table
    private final String score;  // "Score # / Total" e.g. "7/10"
    private final String timestamp;  // "MM/DD/YYYY HH:MM *M" (AM/PM)

    private final int scoreInt;
    private final int totalInt;

    public RecordRow(long recordId, String score, String timestamp, int scoreInt, int totalInt) {
        this.recordId = recordId;
        this.score = score;
        this.timestamp = timestamp;
        this.scoreInt = scoreInt;
        this.totalInt = totalInt;
    }

    public long getRecordId() { return recordId; }
    public String getScore() { return score; }
    public String getTimestamp() { return timestamp; }
    public int getScoreInt() { return scoreInt; }
    public int getTotalInt() { return totalInt; }
}