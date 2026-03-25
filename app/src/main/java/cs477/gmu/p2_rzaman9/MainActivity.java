package cs477.gmu.p2_rzaman9;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    // QUIZ (Just the one quiz in this implementation)
    public static final String QUIZ = "ASL Basics: Lesson 1 Quiz";

    // BUTTONS
    private Button resumeBtn;

    // SETTINGS (Additional Feature settings elements)
    private SettingsFragment settingsFragment;
    private boolean settingsVisible = false;

    // QUIZ ATTEMPT (For transferring to Quiz activity)
    private QuizAttempt currentAttempt = null;

    // FOR SAVING VALUES DURING APP UPDATE/ROTATING & SENDING VIA INTENT
    public static final String QUIZ_ATTEMPT_KEY = "currentAttempt";
    private static final String SETTINGS_KEY = "settingsVisible";


    private final String TAG = "MainActivity";  // For debugging/error messages


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        resumeBtn = findViewById(R.id.resumeButton);
        settingsFragment = (SettingsFragment)getSupportFragmentManager()
                .findFragmentById(R.id.settingsFragment);

        if (settingsFragment != null) {
            getSupportFragmentManager().beginTransaction().hide(settingsFragment).commit();
        }

        if (savedInstanceState != null) {
            currentAttempt = (QuizAttempt)savedInstanceState.getSerializable(QUIZ_ATTEMPT_KEY);
            settingsVisible = savedInstanceState.getBoolean(SETTINGS_KEY, false);
            if (settingsVisible) {
                getSupportFragmentManager().beginTransaction().show(settingsFragment).commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SETTINGS_KEY, settingsVisible);
        if (currentAttempt != null) {
            outState.putSerializable(QUIZ_ATTEMPT_KEY, currentAttempt);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always re-check DB on resume
        // Only query DB if we don't already have an attempt in memory
        if (currentAttempt == null) {
            currentAttempt = DatabaseHelper.getInstance(this).getInProgressAttempt();
        }
        // Set resumeBtn to invisible if there's no quiz attempt to resume
        resumeBtn.setVisibility(currentAttempt != null ? View.VISIBLE : View.INVISIBLE);
    }



    // BUTTON LISTENERS

    public void onStartButtonClicked(View view) {
        if (currentAttempt != null) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Quiz In Progress")
                    .setMessage("Starting a new quiz will abandon your current attempt. Continue?")
                    .setPositiveButton("Start New", (dialog, which) -> startNewQuiz())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            startNewQuiz();
        }
    }

    public void onResumeButtonClicked(View view) {
        if (currentAttempt != null) { launchQuizActivity(); }
    }

    public void onRecordsButtonClicked(View view) {
        Intent intent = new Intent(this, RecordsActivity.class);
        startActivity(intent);
    }

    public void onSettingsButtonClicked(View view) {
        if (settingsFragment == null) { return; }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (settingsVisible) { transaction.hide(settingsFragment); }
        else { transaction.show(settingsFragment); }

        transaction.commit();
        settingsVisible = !settingsVisible;
    }


    // Helper Methods
    private void launchQuizActivity() {
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra(QUIZ_ATTEMPT_KEY, currentAttempt);
        startActivity(intent);
        // Clear in-memory attempt (onResume will re-check DB when we return)
        currentAttempt = null;
    }
    private void startNewQuiz() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        currentAttempt = dbHelper.createNewAttempt();
        launchQuizActivity();
    }
}