package cs477.gmu.p2_rzaman9;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Date;


public class DatabaseHelper extends SQLiteOpenHelper {

    @SuppressLint("StaticFieldLeak")
    private static DatabaseHelper instance;
    final private static String QUIZ_DB_NAME = "asl_quiz_db";
    final static String _ID = "_id";
    final private static Integer VERSION = 6;

    final private Context context;
    final private String quizDataFile;

    final static String QUIZZES_TABLE = "quizzes";
    final static String QUESTIONS_TABLE = "questions";
    final static String ANSWERS_TABLE = "answers";
    final static String RECORDS_TABLE = "records";
    final static String SELECTIONS_TABLE = "selections";

    final static String[] quizzesColumns = {"title", "num_questions", "current_question", "submitted", "shuffled"};
    final static String[] questionColumns = {"quiz_id", "question_text", "video", "order_num", "correct_index"};
    final static String[] optionColumns = {"question_id", "option_index", "answer_text"};
    final static String[] recordColumns = {"quiz_id", "score", "total", "date", "time"};
    final static String[] selectionColumns = {"quiz_id", "question_id", "selected_index"};

    final private static String TAG = "DatabaseHelper";  // For debugging/error messages


    // Avoid multiple open connections
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) { instance = new DatabaseHelper(context.getApplicationContext()); }
        return instance;
    }

    public DatabaseHelper(Context context, String quizDataFile) {
        super(context, QUIZ_DB_NAME, null, VERSION);
        this.context = context;
        this.quizDataFile = quizDataFile;
    }
    public DatabaseHelper(Context context) { this(context, "quiz_questions.json"); }


    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // QUIZZES_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s TEXT NOT NULL, " +  // title
                        "%s INTEGER NOT NULL, " +  // num_questions
                        "%s INTEGER NOT NULL DEFAULT 1, " +  // current_question
                        "%s INTEGER NOT NULL DEFAULT 0 CHECK (%s IN (0, 1)), " +  // submitted
                        "%s INTEGER NOT NULL DEFAULT 0 CHECK (%s IN (0, 1)))",  // shuffled
                        // *****  0=FALSE, 1=TRUE  ***** //
                QUIZZES_TABLE, _ID,
                quizzesColumns[0], quizzesColumns[1], quizzesColumns[2],
                quizzesColumns[3], quizzesColumns[3], quizzesColumns[4], quizzesColumns[4]
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // QUESTIONS_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // quiz_id
                        "%s TEXT NOT NULL, " +  // question_text
                        "%s TEXT NOT NULL, " +  // video
                        "%s INTEGER NOT NULL, " +  // order_num
                        "%s INTEGER NOT NULL, " +  // correct_index
                        "FOREIGN KEY (%s) REFERENCES %s(%s))",  // QUIZZES_TABLE
                QUESTIONS_TABLE, _ID,
                questionColumns[0], questionColumns[1], questionColumns[2],
                questionColumns[3], questionColumns[4], questionColumns[0],
                QUIZZES_TABLE, _ID
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // ANSWER_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // question_id
                        "%s INTEGER NOT NULL, " +  // option_index
                        "%s TEXT NOT NULL, " +  // answer_text
                        "FOREIGN KEY (%s) REFERENCES %s(%s))",  // QUESTIONS_TABLE
                ANSWERS_TABLE, _ID,
                optionColumns[0], optionColumns[1], optionColumns[2], optionColumns[0],
                QUESTIONS_TABLE, _ID
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // RECORDS_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // quiz_id
                        "%s INTEGER NOT NULL, " +  // score
                        "%s INTEGER NOT NULL, " +  // total
                        "%s TEXT NOT NULL, " +  // date
                        "%s TEXT NOT NULL, " +  // time
                        "FOREIGN KEY (%s) REFERENCES %s(%s))",  // QUIZZES_TABLE
                RECORDS_TABLE, _ID,
                recordColumns[0], recordColumns[1], recordColumns[2],
                recordColumns[3], recordColumns[4], recordColumns[0],
                QUIZZES_TABLE, _ID
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // SELECTIONS_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // quiz_id
                        "%s INTEGER NOT NULL, " +  // question_id
                        "%s INTEGER NOT NULL, " +  // selected_index
                        "FOREIGN KEY (%s) REFERENCES %s(%s), " +
                        "FOREIGN KEY (%s) REFERENCES %s(%s), " +
                        "UNIQUE (%s, %s))",  // one selection per question per quiz
                SELECTIONS_TABLE, _ID,
                selectionColumns[0], selectionColumns[1], selectionColumns[2],
                selectionColumns[0], QUIZZES_TABLE, _ID,
                selectionColumns[1], QUESTIONS_TABLE, _ID,
                selectionColumns[0], selectionColumns[1]
        ));
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", ANSWERS_TABLE));
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", RECORDS_TABLE));
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", QUESTIONS_TABLE));
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", QUIZZES_TABLE));
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", SELECTIONS_TABLE));
        onCreate(db);
    }


    private void seedQuestionsForQuiz(SQLiteDatabase db, long quizId) {
        try {
            InputStream qData = context.getAssets().open(quizDataFile);
            byte[] buffer = new byte[qData.available()];
            int bytes = qData.read(buffer);
            qData.close();
            if (bytes < 0) {
                Log.e(TAG, "seedQuestionsForQuiz: ", new JSONException("No data found"));
            }

            JSONObject root = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            // Just get first quiz's questions since there's only one quiz in this implementation.
            JSONArray questions = root.getJSONArray("quizzes")
                    .getJSONObject(0)
                    .getJSONArray("questions");

            // Build a shuffleable index list
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < questions.length(); i++) { indices.add(i); }

            // Shuffle only if setting is enabled
            if (Settings.get(context, Settings.SHUFFLE_KEY)) { Collections.shuffle(indices); }

            // Insert in the (possibly shuffled) order, using position as new order_num
            for (int i = 0; i < indices.size(); i++) {
                JSONObject qObj = questions.getJSONObject(indices.get(i));
                List<String> options = new ArrayList<>();
                JSONArray optArr = qObj.getJSONArray("options");
                for (int j = 0; j < optArr.length(); j++) { options.add(optArr.getString(j)); }
                insertQuestionIntoDB(db, quizId,
                        i+1,  // new order_num based on shuffled position
                        qObj.getString("question_text"),
                        qObj.getString("video"),
                        options,
                        qObj.getInt("correct_index")
                );
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "seedQuestionsForQuiz: failed to seed.", e);
        }
    }


    /**
     * Helper method for inserting question data into the database.
     * @param db           The database to insert the question into
     * @param quizId       The ID of the quiz attempt to associate the question with
     * @param orderNum     Which order the question would show up in by default
     * @param questionText The actual question text itself
     * @param video        The name of the video file associated with the question (without any extension)
     * @param options      The list of option strings for the answer choices
     * @param correctIndex The index of the correct answer choice
     */
    private void insertQuestionIntoDB(SQLiteDatabase db, long quizId, int orderNum,
                                      String questionText, String video,
                                      List<String> options, int correctIndex) {
        ContentValues qVals = new ContentValues();
        qVals.put(questionColumns[0], quizId);
        qVals.put(questionColumns[1], questionText);
        qVals.put(questionColumns[2], video);
        qVals.put(questionColumns[3], orderNum);
        qVals.put(questionColumns[4], correctIndex);
        long questionId = db.insert(QUESTIONS_TABLE, null, qVals);

        for (int i = 0; i < options.size(); i++) {
            ContentValues aVals = new ContentValues();
            aVals.put(optionColumns[0], questionId);
            aVals.put(optionColumns[1], i);
            aVals.put(optionColumns[2], options.get(i));
            db.insert(ANSWERS_TABLE, null, aVals);
        }
    }


    /**
     * Get the first quiz in the database that has not yet been submitted.
     * (Assume there will always only be up to one quiz that is not marked as submitted)
     * @return QuizAttempt object with data of the quiz that hasn't been submitted
     */
    public QuizAttempt getInProgressAttempt() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(QUIZZES_TABLE,
                // id, num_questions, shuffled
                new String[]{_ID, quizzesColumns[1], quizzesColumns[2], quizzesColumns[4]},
                quizzesColumns[3] + " = 0",            // WHERE submitted = 0
                null, null, null, null);

        if (cursor.moveToFirst()) {
            long quizId = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
            int numQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[1]));
            int currentQuestion = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[2]));
            int shuffled = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[4]));
            cursor.close();
            QuizAttempt q = new QuizAttempt(quizId, numQuestions, (shuffled==1));
            q.setCurrentQuestion(currentQuestion);
            return q;
        }
        cursor.close();
        return null;
    }

    /**
     * Creates a new quiz attempt by storing the questions in the database.
     * @return QuizAttempt object representation of the data inserted into the database
     */
    public QuizAttempt createNewAttempt() {
        SQLiteDatabase db = getWritableDatabase();

        // Clear any previous attempt state
        resetAttempt(db);

        // Insert fresh quiz row
        ContentValues quizVals = new ContentValues();
        quizVals.put(quizzesColumns[0], MainActivity.QUIZ);  // title
        quizVals.put(quizzesColumns[1], 0);  // num_questions - updated after insertions
        quizVals.put(quizzesColumns[2], 1);  // current_question = 1
        quizVals.put(quizzesColumns[3], 0);  // submitted = false

        // shuffle = 0 if SHUFFLE_KEY is false, otherwise shuffle = 1
        quizVals.put(quizzesColumns[4], Settings.get(context, Settings.SHUFFLE_KEY) ? 1 : 0);
        long quizId = db.insert(QUIZZES_TABLE, null, quizVals);

        // Seed questions and answers for this quiz
        seedQuestionsForQuiz(db, quizId);

        // Update num_questions now that we know how many were inserted
        int count = getQuestionCount(db, quizId);
        ContentValues update = new ContentValues();
        update.put(quizzesColumns[1], count);
        db.update(QUIZZES_TABLE, update, _ID + "=?", new String[]{String.valueOf(quizId)});

        return new QuizAttempt(quizId, count, Settings.get(context, Settings.SHUFFLE_KEY));
    }

    private void resetAttempt(SQLiteDatabase db) {
        // Delete previous quiz data so we start fresh
        db.delete(SELECTIONS_TABLE, null, null);
        db.delete(ANSWERS_TABLE, null, null);
        db.delete(QUESTIONS_TABLE, null, null);
        db.delete(QUIZZES_TABLE, quizzesColumns[3] + "=?", new String[]{"0"});
    }

    private int getQuestionCount(SQLiteDatabase db, long quizId) {
        Cursor c = db.query(QUESTIONS_TABLE,
                new String[]{"COUNT(*)"},
                questionColumns[0] + "=?",
                new String[]{String.valueOf(quizId)},
                null, null, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }


    /**
     * Get the list of questions associated with a given quiz.
     * @param quizId The database ID of the quiz to retrieve questions for
     * @return An ArrayList of Question objects
     */
    public List<Question> getQuestionsForQuiz(long quizId) {
        List<Question> questions = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Get all questions for this quiz in order
        Cursor qCursor = db.query(
                QUESTIONS_TABLE,
                new String[]{_ID, questionColumns[1], questionColumns[2],
                        questionColumns[3], questionColumns[4]},
                // question_text, video, order_num, correct_index
                questionColumns[0] + "=?",  // WHERE quiz_id=?
                new String[]{String.valueOf(quizId)},
                null, null,
                questionColumns[3]  // ORDER BY order_num
        );

        while (qCursor.moveToNext()) {
            long id = qCursor.getLong(qCursor.getColumnIndexOrThrow(_ID));
            String text = qCursor.getString(qCursor.getColumnIndexOrThrow(questionColumns[1]));
            String video = qCursor.getString(qCursor.getColumnIndexOrThrow(questionColumns[2]));
            int orderNum = qCursor.getInt(qCursor.getColumnIndexOrThrow(questionColumns[3]));
            int correctIdx = qCursor.getInt(qCursor.getColumnIndexOrThrow(questionColumns[4]));

            // Get answers for this question in option order
            List<String> options = new ArrayList<>();
            Cursor aCursor = db.query(
                    ANSWERS_TABLE,
                    new String[]{optionColumns[2]},  // answer_text
                    optionColumns[0] + "=?",  // WHERE question_id=?
                    new String[]{String.valueOf(id)},
                    null, null,
                    optionColumns[1]  // ORDER BY option_index
            );
            while (aCursor.moveToNext()) {
                options.add(aCursor.getString(0));
            }
            aCursor.close();

            questions.add(new Question(id, orderNum, text, video, options, correctIdx));
        }
        qCursor.close();
        return questions;
    }


    // METHODS FOR SAVING ANSWER SELECTION FOR IN-PROGRESS QUIZZES
    public void saveSelection(long quizId, long questionId, int selectedIndex) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(selectionColumns[0], quizId);
        vals.put(selectionColumns[1], questionId);
        vals.put(selectionColumns[2], selectedIndex);
        // INSERT OR REPLACE handles both new and updated selections cleanly
        db.insertWithOnConflict(SELECTIONS_TABLE, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Load all selections for a quiz into a HashMap.
     * @param quizId The ID of the quiz attempt to retrieve data for
     * @return Map of the selected indexes and their associated question IDs
     */
    public Map<Long, Integer> loadSelections(long quizId) {
        Map<Long, Integer> selections = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(SELECTIONS_TABLE,
                new String[]{selectionColumns[1], selectionColumns[2]},  // question_id, selected_index
                selectionColumns[0] + "=?",
                new String[]{String.valueOf(quizId)},
                null, null, null);
        while (c.moveToNext()) {
            selections.put(
                    c.getLong(c.getColumnIndexOrThrow(selectionColumns[1])),
                    c.getInt(c.getColumnIndexOrThrow(selectionColumns[2]))
            );
        }
        c.close();
        return selections;
    }

    /**
     * Saves current question to the database.
     * @param quizId The ID of the quiz attempt being taken
     * @param currentQuestion The current question index
     */
    public void saveCurrentQuestion(long quizId, int currentQuestion) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(quizzesColumns[2], currentQuestion);
        db.update(QUIZZES_TABLE, vals, _ID + "=?", new String[]{String.valueOf(quizId)});
    }

    /**
     * Saves quiz attempt data to the records table in the database.
     * @param attempt The QuizAttempt object to save data for
     */
    public void saveRecord(QuizAttempt attempt) {
        SQLiteDatabase db = getWritableDatabase();

        // Get current date and time as strings
        String date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                .format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date());

        ContentValues vals = new ContentValues();
        vals.put(recordColumns[0], attempt.getId());  // quiz_id
        vals.put(recordColumns[1], attempt.getScore());  // score
        vals.put(recordColumns[2], attempt.getTotalQuestions());  // total
        vals.put(recordColumns[3], date);  // date
        vals.put(recordColumns[4], time);  // time
        db.insert(RECORDS_TABLE, null, vals);
    }

    /**
     * Marks a quiz as submitted in the database.
     * (Does NOT do anything with any QuizAttempt object - handle that elsewhere)
     * @param quizId The ID of the quiz attempt to mark as submitted.
     */
    public void markQuizSubmitted(long quizId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put(quizzesColumns[3], 1);  // submitted = true
        db.update(QUIZZES_TABLE, vals, _ID + "=?",
                new String[]{String.valueOf(quizId)});
    }

    /**
     * Get the list of quiz attempt records from the database.
     * @return List of RecordRow objects
     */
    public List<RecordRow> getAllRecords() {
        List<RecordRow> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(RECORDS_TABLE,
                new String[]{_ID, recordColumns[1], recordColumns[2],
                        recordColumns[3], recordColumns[4]},
                // score, total, date, time
                null, null, null, null,
                _ID + " DESC");  // most recent first

        while (c.moveToNext()) {
            long id      = c.getLong(c.getColumnIndexOrThrow(_ID));
            int score    = c.getInt(c.getColumnIndexOrThrow(recordColumns[1]));
            int total    = c.getInt(c.getColumnIndexOrThrow(recordColumns[2]));
            String date  = c.getString(c.getColumnIndexOrThrow(recordColumns[3]));
            String time  = c.getString(c.getColumnIndexOrThrow(recordColumns[4]));

            records.add(new RecordRow(id,
                    score + "/" + total,
                    date + " " + time,
                    score, total));
        }
        c.close();
        return records;
    }

    /**
     * Delete a single quiz attempt record from the database.
     * @param recordId The ID of the record to delete
     */
    public void deleteRecord(long recordId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(RECORDS_TABLE, _ID + "=?",
                new String[]{String.valueOf(recordId)});
    }

    /**
     * Deletes ALL quiz attempt record entries in the database.
     * (WARNING: Does not handle confirmation - ensure safety/confirmation elsewhere)
     */
    public void deleteAllRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(RECORDS_TABLE, null, null);
    }

    /**
     * Get the number of questions that the user has answered in the quiz from the database.
     * @param quizId The ID of the quiz to check for
     * @return The number of selections made in the quiz
     */
    public int getSelectionCount(long quizId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(SELECTIONS_TABLE,
                new String[]{"COUNT(*)"},
                selectionColumns[0] + "=?",
                new String[]{String.valueOf(quizId)},
                null, null, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
}
