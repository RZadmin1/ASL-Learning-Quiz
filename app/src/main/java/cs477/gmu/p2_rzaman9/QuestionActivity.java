package cs477.gmu.p2_rzaman9;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestionActivity extends AppCompatActivity {

    // DB
    private DatabaseHelper dbHelper;

    // VIEWS (Host Layout)
    private VideoView videoView;
    private TextView questionLabel;

    private AlertDialog currentDialog;

    // FRAGMENT
    private QuestionFragment questionFragment;

    // QUIZ STATE
    private QuizAttempt quizAttempt;
    private List<Question> questions;
    private int currentIndex = 0;  // 0-based index into questions list
    private boolean reviewMode = false;

    // INTENT KEY
    public static final String QUIZ_ATTEMPT_KEY = "currentAttempt";
    public static final String REVIEW_MODE_KEY = "reviewMode";

    // STATE KEYS (For saving/restoring)
    private static final String CURRENT_INDEX_KEY = "currentIndex";

    // Tracks the user's selected option_index per question (-1 = unanswered)
    private Map<Long, Integer> savedSelections;


    private static final String TAG = "QuestionActivity";  // For debugging/error messages


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_question);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = DatabaseHelper.getInstance(this);
        videoView = findViewById(R.id.videoView);
        questionLabel = findViewById(R.id.questionLabel);

        // Get QuizAttempt passed from MainActivity (OR ResultActivity/RecordsActivity for review)
        quizAttempt = (QuizAttempt)getIntent().getSerializableExtra(MainActivity.QUIZ_ATTEMPT_KEY);
        if (quizAttempt == null) {  // Shouldn't reach this
            Log.e(TAG, "No QuizAttempt received - finishing.");
            finish();  return;
        }
        reviewMode = getIntent().getBooleanExtra(REVIEW_MODE_KEY, false);

        // Load questions from DB
        questions = dbHelper.getQuestionsForQuiz(quizAttempt.getId());
        if (questions == null || questions.isEmpty()) {  // Should not reach this.
            Log.e(TAG, "No questions found for quiz " + quizAttempt.getId());
            finish();  return;
        }

        // Load saved selections from DB (questionId -> selectedIndex)
        savedSelections = dbHelper.loadSelections(quizAttempt.getId());

        // Initialize or restore state
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(CURRENT_INDEX_KEY, 0);
            reviewMode = savedInstanceState.getBoolean(REVIEW_MODE_KEY, false);
        } else { currentIndex = quizAttempt.getCurrentQuestion() - 1; }
        savedSelections = dbHelper.loadSelections(quizAttempt.getId());

        // Get fragment reference
        questionFragment = (QuestionFragment)getSupportFragmentManager()
                .findFragmentById(R.id.questionFragment);

        // Display current question
        displayQuestion(currentIndex);

        // Handle back button being pressed
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (reviewMode) { finish(); } // just go back to ResultActivity
                else {
                    currentDialog = new AlertDialog.Builder(QuestionActivity.this)
                            .setTitle("Quit Quiz?")
                            .setMessage("Your progress will be saved. You can resume later.")
                            .setPositiveButton("Quit", (dialog, which) -> {
                                setEnabled(false);
                                finish();
                            })
                            .setNegativeButton("Keep Going", null)
                            .show();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_INDEX_KEY, currentIndex);
        outState.putBoolean(REVIEW_MODE_KEY, reviewMode);
    }


    // VIDEO SETUP
    private void loadVideo(String videoName) {
        Context context = QuestionActivity.this;
        String pkg = context.getPackageName();

        @SuppressLint("DiscouragedApi")
        // Not using openRawResource(R.raw.video_name) because videoName would dynamically change
        int videoId = context.getResources().getIdentifier(videoName, "raw", pkg);

        if (videoId == 0) {
            Log.e(TAG, "Video resource not found: " + videoName);  return;
        }

        Uri uri = Uri.parse("android.resource://" + pkg + "/" + videoId);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.start();
        });
        videoView.setVideoURI(uri);
    }


    // QUESTION DISPLAY
    private void displayQuestion(int index) {
        Question q = questions.get(index);
        questionLabel.setText(String.format(
                Locale.US, "Question %d of %d", index + 1, questions.size()));
        quizAttempt.setCurrentQuestion(index + 1);  // Update QuizAttempt object
        loadVideo(q.getVideoName());  // Load the video for this question

        // Tell the fragment to display this question and restore any prior selection
        if (questionFragment != null) {
            Integer saved = savedSelections.get(q.getId());
            int savedSelection = (saved != null) ? saved : -1;
            questionFragment.displayQuestion(q, index, questions.size(), savedSelection, reviewMode);
        }
        dbHelper.saveCurrentQuestion(quizAttempt.getId(), currentIndex + 1);
    }


    // FRAGMENT HELPER METHODS

    public void onAnswerSelected(long questionId, int optionIndex) {
        savedSelections.put(questionId, optionIndex);
        dbHelper.saveSelection(quizAttempt.getId(), questionId, optionIndex);
    }

    public void onPrevClicked() {
        if (currentIndex > 0) {
            currentIndex--;
            displayQuestion(currentIndex);
        }
    }

    public void onNextClicked() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            displayQuestion(currentIndex);
        } else {
            if (reviewMode) { finish(); }
            else { submitQuiz(); }
        }
    }

    private void submitQuiz() {
        int score = 0;
        for (Question q : questions) {
            Integer selected = savedSelections.get(q.getId());
            if (selected != null && selected == q.getCorrectIndex()) {
                score++;
            }
        }
        quizAttempt.submit(score);
        dbHelper.saveRecord(quizAttempt);
        dbHelper.markQuizSubmitted(quizAttempt.getId());
        Log.d(TAG, "SCORE=" + score);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(QUIZ_ATTEMPT_KEY, quizAttempt);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) { videoView.start(); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) { videoView.pause(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentDialog != null && currentDialog.isShowing()) { currentDialog.dismiss(); }
        if (videoView != null) { videoView.stopPlayback(); }
    }
}