package cs477.gmu.p2_rzaman9;

import java.util.List;

public class Question {
    private final int originalOrder;
    private final String questionText;
    private final String videoName;
    private final List<String> options;
    private final int correctIndex;

    // Constructor
    public Question(int originalOrder, String questionText, String videoName,
                    List<String> options, int correctIndex) {
        this.originalOrder = originalOrder;
        this.questionText = questionText;
        this.videoName = videoName;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    // Getters
    public int getOrder() { return originalOrder; }
    public String getQuestionText() { return questionText; }
    public String getVideoName() { return videoName; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
}