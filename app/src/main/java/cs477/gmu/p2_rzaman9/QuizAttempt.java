package cs477.gmu.p2_rzaman9;

import java.io.Serializable;

// Implement Serializable to let it to pass between activities through Intents / savedInstanceState
public class QuizAttempt implements Serializable {

    private final long quizId;  // Quiz ID in the DATABASE
    private final boolean shuffled;  // Can't get changed later
    private boolean submitted = false;
    private int currentQuestion = 1;
    private int score;

    private final int totalQuestions;


    public QuizAttempt(long quizId, int totalQuestions, boolean shuffled) {
        this.quizId = quizId;
        this.totalQuestions = totalQuestions;
        this.shuffled = shuffled;
    }


    public void setCurrentQuestion(int questionNum) {
        // Clamp currentQuestion value in range [1, totalQuestions]
        this.currentQuestion = Math.max(1, Math.min(questionNum, totalQuestions));
    }

    public void submit(int score) {
        if (!this.submitted) {
            this.score = score;
            this.submitted = true;
        }
    }


    public long getId() { return quizId; }
    public boolean isShuffled() { return shuffled; }
    public int getCurrentQuestion() { return currentQuestion; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getScore() { return score; }
}
