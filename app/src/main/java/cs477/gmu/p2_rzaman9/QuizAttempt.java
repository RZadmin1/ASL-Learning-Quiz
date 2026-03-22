package cs477.gmu.p2_rzaman9;

import java.io.Serializable;

// Implement Serializable to let it to pass between activities through Intents / savedInstanceState
public class QuizAttempt implements Serializable {

    private final long quizId;  // Quiz ID in the DATABASE
    private boolean submitted = false;
    private int currentQuestion = 1;
    private int score;

    private final int totalQuestions;


    public QuizAttempt(long quizId, int totalQuestions) {
        this.quizId = quizId;
        this.totalQuestions = totalQuestions;
    }
    public QuizAttempt(long quizId, int totalQuestions, int score) {
        this(quizId, totalQuestions);
        this.score = score;
        this.submitted = true;
    }


    public void nextQuestion() {
        if (currentQuestion < totalQuestions) { currentQuestion++; }
    }
    public void prevQuestion() {
        if (currentQuestion > 1) { currentQuestion--; }
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


    public long getId() {
        return this.quizId;
    }
    public boolean isSubmitted() {
        return this.submitted;
    }
    public int getCurrentQuestion() {
        return this.currentQuestion;
    }
    public int getTotalQuestions() {
        return this.totalQuestions;
    }
    public int getScore() {
        return this.score;
    }
}
