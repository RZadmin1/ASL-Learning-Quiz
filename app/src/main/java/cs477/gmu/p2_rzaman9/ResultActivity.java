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
    private List<Question> questions;
    private Map<Long, Integer> savedSelections;

    class ResultRow {
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

    class ResultAdapter extends ArrayAdapter<ResultRow> {

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

        attempt = (QuizAttempt)getIntent().getSerializableExtra("currentAttempt");
        if (attempt == null) { finish(); return; }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);

        // Load questions and selections to build result rows
        questions = dbHelper.getQuestionsForQuiz(attempt.getId());
        savedSelections = dbHelper.loadSelections(attempt.getId());

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

        ListView listView = findViewById(R.id.recordsList);
        listView.setAdapter(new ResultAdapter(this, rows));

        // Review button: relaunch QuestionActivity in review mode
        Button reviewButton = findViewById(R.id.reviewQuestionsButton);
        reviewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuestionActivity.class);
            intent.putExtra("currentAttempt", attempt);
            intent.putExtra("reviewMode", true);  // flag for QuestionActivity to handle
            startActivity(intent);
        });
    }
}