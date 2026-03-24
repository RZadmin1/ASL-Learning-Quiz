package cs477.gmu.p2_rzaman9;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;


public class QuestionFragment extends Fragment {

    // UI ELEMENTS
    private TextView questionText;
    private RadioGroup answerGroup;
    private RadioButton option1, option2, option3, option4;
    private Button prevButton, nextButton;

    private QuestionActivity host;

    private long currentQuestionId = -1;


    public QuestionFragment() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        host = (QuestionActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_question, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        questionText = view.findViewById(R.id.questionText);
        answerGroup = view.findViewById(R.id.answerSelection);
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        prevButton = view.findViewById(R.id.prevButton);
        nextButton = view.findViewById(R.id.nextButton);

        // Listeners defined in QuestionActivity to stay organized
        prevButton.setOnClickListener(v -> host.onPrevClicked());
        nextButton.setOnClickListener(v -> host.onNextClicked());

        answerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedIndex = getIndexForId(checkedId);
            if (selectedIndex >= 0 && currentQuestionId > -1) {
                host.onAnswerSelected(currentQuestionId, selectedIndex);
            }
        });
    }

    // Called by QuestionActivity to populate the fragmentUI
    void displayQuestion(Question q, int index, int total, int savedSelection, boolean reviewMode) {
        currentQuestionId = q.getId();
        questionText.setText(q.getQuestionText());

        List<String> options = q.getOptions();
        RadioButton[] buttons = {option1, option2, option3, option4};

        // Reset all buttons to default state before applying new state
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setText(options.get(i));
            buttons[i].setBackground(null);
            buttons[i].setEnabled(!reviewMode);  // disable clicks in review mode
        }

        // Restore or apply selection
        answerGroup.setOnCheckedChangeListener(null);
        if (savedSelection >= 0) {
            answerGroup.check(getIdForIndex(savedSelection));
            answerGroup.jumpDrawablesToCurrentState();
        } else {
            answerGroup.clearCheck();
            answerGroup.jumpDrawablesToCurrentState();
        }

        if (reviewMode) { applyReviewHighlights(buttons, q.getCorrectIndex(), savedSelection); }
        else {  // Re-attach listener only if NOT in review mode
            answerGroup.setOnCheckedChangeListener((group, checkedId) -> {
                int selectedIndex = getIndexForId(checkedId);
                if (selectedIndex >= 0 && currentQuestionId != -1) {
                    host.onAnswerSelected(currentQuestionId, selectedIndex);
                }
            });
        }

        prevButton.setVisibility(index > 0 ? View.VISIBLE : View.INVISIBLE);
        if (reviewMode) { nextButton.setText(getString(R.string.next_question)); }
        else {
            nextButton.setText(index == total-1
                    ? getString(R.string.submit_quiz) : getString(R.string.next_question));
        }
    }


    // HELPER METHODS
    private int getIdForIndex(int index) {
        switch (index) {
            case 0: return R.id.option1;
            case 1: return R.id.option2;
            case 2: return R.id.option3;
            case 3: return R.id.option4;
            default: return -1;
        }
    }

    private int getIndexForId(int id) {
        if (id == R.id.option1) return 0;
        if (id == R.id.option2) return 1;
        if (id == R.id.option3) return 2;
        if (id == R.id.option4) return 3;
        return -1;
    }

    private void applyReviewHighlights(RadioButton[] buttons,
                                       int correctIndex, int selectedIndex) {
        // Always highlight the correct answer green
        buttons[correctIndex].setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green_transparent));
        buttons[correctIndex].setText(
                String.format("%s - CORRECT!", buttons[correctIndex].getText()));

        // If user selected wrong answer, highlight it red
        if (selectedIndex >= 0 && selectedIndex != correctIndex) {
            buttons[selectedIndex].setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.red_transparent));
            buttons[selectedIndex].setText(
                    String.format("%s - INCORRECT", buttons[selectedIndex].getText()));
        }
    }
}