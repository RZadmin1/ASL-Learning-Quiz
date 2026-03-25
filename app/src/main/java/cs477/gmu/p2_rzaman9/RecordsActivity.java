package cs477.gmu.p2_rzaman9;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class RecordsActivity extends AppCompatActivity {

    private static final String TAG = "RecordsActivity";
    private RecordsAdapter adapter;
    private List<RecordRow> rows;
    private DatabaseHelper dbHelper;
    private AlertDialog currentDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_records);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = DatabaseHelper.getInstance(this);
        ListView recordsList = findViewById(R.id.recordsList);
        Button deleteAllBtn = findViewById(R.id.deleteAllButton);
        Button returnHomeBtn = findViewById(R.id.recordsHomeButton);

        rows = new ArrayList<>();
        adapter = new RecordsAdapter(this, rows);
        recordsList.setAdapter(adapter);

        loadRecords();

        // Long press on a row to delete that record
        recordsList.setOnItemLongClickListener((parent, view, position, id) -> {
            RecordRow row = rows.get(position);
            currentDialog = new AlertDialog.Builder(this)
                    .setTitle("Delete Record")
                    .setMessage("Delete attempt #" + row.getRecordId() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteRecord(row.getRecordId());
                        loadRecords();  // refresh list
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        // Deleting all records
        deleteAllBtn.setOnClickListener(v -> {
            if (rows.isEmpty()) {
                Toast.makeText(this, "No records to delete.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentDialog = new AlertDialog.Builder(this)
                    .setTitle("Delete All Records")
                    .setMessage("Are you sure you want to delete all quiz records? This cannot be undone.")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        dbHelper.deleteAllRecords();
                        loadRecords();  // refresh list
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Returning to MainActivity
        returnHomeBtn.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }


    private void loadRecords() {
        rows.clear();
        rows.addAll(dbHelper.getAllRecords());
        adapter.notifyDataSetChanged();
    }


    // ADAPTER
    private static class RecordsAdapter extends ArrayAdapter<RecordRow> {
        public RecordsAdapter(Context context, List<RecordRow> rows) {
            super(context, 0, rows);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.record_line, parent, false);
            }

            RecordRow row = getItem(position);

            TextView quizNameRowLabel = convertView.findViewById(R.id.quizNameRowLabel);
            TextView attemptRowLabel = convertView.findViewById(R.id.attemptRowLabel);
            TextView scoreRowLabel = convertView.findViewById(R.id.scoreRowLabel);
            TextView timestampRowLabel = convertView.findViewById(R.id.timestampRowLabel);

            if (Settings.get(getContext(), Settings.COLOR_SCORES_KEY)) {
                assert row != null;
                double percent = (row.getTotalInt() > 0)
                        ? (double) row.getScoreInt() / row.getTotalInt() : 0;

                int color;
                if (percent > 0.75) {
                    color = ContextCompat.getColor(getContext(), R.color.green);
                } else if (percent > 0.5) {
                    color = ContextCompat.getColor(getContext(), R.color.yellow);
                } else {
                    color = ContextCompat.getColor(getContext(), R.color.red);
                }
                scoreRowLabel.setTypeface(null, Typeface.BOLD);
                scoreRowLabel.setTextColor(color);
            } else {
                // Reset to default text color when feature is off
                scoreRowLabel.setTypeface(null, Typeface.NORMAL);
                scoreRowLabel.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.black));
            }

            quizNameRowLabel.setText(MainActivity.QUIZ);
            assert row != null;
            attemptRowLabel.setText(String.valueOf(row.getRecordId()));
            scoreRowLabel.setText(row.getScore());
            timestampRowLabel.setText(row.getTimestamp());

            return convertView;
        }
    }
}