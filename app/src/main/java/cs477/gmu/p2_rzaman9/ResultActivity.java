package cs477.gmu.p2_rzaman9;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    private QuizAttempt attempt;

    static class ResultRow {
        public final int questionNum;
        public final String selectedAnswerText;
        public final boolean isCorrect;
        public final Integer selectedIndex;  // null if unanswered

        public ResultRow(int questionNum, String selectedAnswerText,
                         boolean isCorrect, Integer selectedIndex) {
            this.questionNum = questionNum;
            this.selectedAnswerText = selectedAnswerText;
            this.isCorrect = isCorrect;
            this.selectedIndex = selectedIndex;
        }
    }

    static class ResultAdapter extends ArrayAdapter<ResultRow> {

        public ResultAdapter(Context context, List<ResultRow> rows) {
            super(context, 0, rows);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.q_line, parent, false);
            }

            ResultRow row = getItem(position);

            TextView qNumLabel = convertView.findViewById(R.id.qNumLabel);
            TextView qAnswerLabel = convertView.findViewById(R.id.qAnswerLabel);
            TextView isCorrectLabel = convertView.findViewById(R.id.isCorrectLabel);

            assert row != null;
            qNumLabel.setText(String.format(Locale.US, "Q%d", row.questionNum));
            qAnswerLabel.setText(row.selectedAnswerText);
            if (row.selectedIndex == null || row.selectedIndex < 0) {
                isCorrectLabel.setText(getContext().getString(R.string.no_answer));
                isCorrectLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.light_gray));
            } else if (row.isCorrect) {
                isCorrectLabel.setText(getContext().getString(R.string.correct));
                isCorrectLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else {
                isCorrectLabel.setText(getContext().getString(R.string.incorrect));
                isCorrectLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        attempt = (QuizAttempt)getIntent().getSerializableExtra(QuestionActivity.QUIZ_ATTEMPT_KEY);
        if (attempt == null) { finish(); return; }  // Should not reach this

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);

        // Load questions and selections to build result rows
        List<Question> questions = dbHelper.getQuestionsForQuiz(attempt.getId());
        Map<Long, Integer> savedSelections = dbHelper.loadSelections(attempt.getId());

        // Set score display
        TextView scoreDisplay = findViewById(R.id.scoreDisplay);
        scoreDisplay.setText(String.format(Locale.US, "%s %d/%d",
                getString(R.string.score_label),
                attempt.getScore(), attempt.getTotalQuestions()
        ));

        // Build result rows
        List<ResultRow> rows = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Integer selectedIndex = savedSelections.get(q.getId());

            String answerText;
            boolean isCorrect;
            if (selectedIndex == null || selectedIndex < 0) {
                answerText = "";
                isCorrect = false;
            } else {
                answerText = q.getOptions().get(selectedIndex);
                isCorrect = (selectedIndex == q.getCorrectIndex());
            }

            rows.add(new ResultRow(i + 1, answerText, isCorrect, selectedIndex));
        }

        ListView resultListView = findViewById(R.id.recordsList);
        resultListView.setAdapter(new ResultAdapter(this, rows));

        // Review button: relaunch QuestionActivity in review mode
        Button reviewBtn = findViewById(R.id.reviewQuestionsButton);
        Button returnHomeBtn = findViewById(R.id.resultsHomeButton);
        reviewBtn.setOnClickListener(v -> goToReview(dbHelper, 1));
        returnHomeBtn.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // Question List: relaunch QuestionActivity in review mode at desired question
        resultListView.setOnItemClickListener(
                (parent, view, position, id)
                        -> goToReview(dbHelper, position+1));
    }


    private void goToReview(DatabaseHelper dbHelper, int currentQuestion) {
        // position is 0-based, questions are 1-based
        attempt.setCurrentQuestion(currentQuestion);
        dbHelper.saveCurrentQuestion(attempt.getId(), currentQuestion);
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra(QuestionActivity.QUIZ_ATTEMPT_KEY, attempt);
        intent.putExtra(QuestionActivity.REVIEW_MODE_KEY, true);
        startActivity(intent);
    }
}