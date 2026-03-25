package cs477.gmu.p2_rzaman9;

import java.util.List;

public class Question {

    private final long id;

    private final String questionText;
    private final String videoName;
    private final List<String> options;
    private final int correctIndex;

    // Constructor
    public Question(long id, String questionText, String videoName,
                    List<String> options, int correctIndex) {
        this.id = id;
        this.questionText = questionText;
        this.videoName = videoName;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    // Getters
    public long getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getVideoName() { return videoName; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
}