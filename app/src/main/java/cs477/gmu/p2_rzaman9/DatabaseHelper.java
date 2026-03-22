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
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    @SuppressLint("StaticFieldLeak")
    private static DatabaseHelper instance;
    final private static String QUIZ_DB_NAME = "asl_quiz_db";
    final static String _ID = "_id";
    final private static Integer VERSION = 5;

    final private Context context;
    final private String quizDataFile;

    final static String QUIZZES_TABLE = "quizzes";
    final static String QUESTIONS_TABLE = "questions";
    final static String ANSWERS_TABLE = "answers";
    final static String RECORDS_TABLE = "records";

    final static String[] quizzesColumns = {"title", "num_questions", "current_question", "submitted"};
    final static String[] questionColumns = {"quiz_id", "question_text", "video", "order_num", "correct_index"};
    final static String[] optionColumns = {"question_id", "option_index", "answer_text"};
    final static String[] recordColumns = {"quiz_id", "score", "total", "date", "time"};

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

    // TODO: Implement Records Table & Functionality


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
                        "%s INTEGER NOT NULL DEFAULT 0 CHECK (%s IN (0, 1)))",  // submitted
                        // *****  0=FALSE, 1=TRUE  ***** //
                QUIZZES_TABLE, _ID,
                quizzesColumns[0], quizzesColumns[1], quizzesColumns[2],
                quizzesColumns[3], quizzesColumns[3]
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // QUESTIONS_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // quiz_id
                        "%s TEXT NOT NULL, " +  // question_text
                        "%s TEXT NOT NULL, " +  // video
                        "%s INTEGER NOT NULL, " +  // order_num
                        "%s INTEGER NOT NULL, " +  // correct_index
                        "FOREIGN KEY (%s) REFERENCES %s(id))",  // QUIZZES_TABLE
                QUESTIONS_TABLE, _ID,
                questionColumns[0], questionColumns[1], questionColumns[2],
                questionColumns[3], questionColumns[4], questionColumns[0],
                QUIZZES_TABLE
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // ANSWER_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // question_id
                        "%s INTEGER NOT NULL, " +  // option_index
                        "%s TEXT NOT NULL, " +  // answer_text
                        "FOREIGN KEY (%s) REFERENCES %s(id))",  // QUESTIONS_TABLE
                ANSWERS_TABLE, _ID,
                optionColumns[0], optionColumns[1], optionColumns[2], optionColumns[0],
                QUESTIONS_TABLE
        ));

        db.execSQL(String.format(
                "CREATE TABLE %s (" +  // RECORDS_TABLE
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +  // _ID
                        "%s INTEGER NOT NULL, " +  // quiz_id
                        "%s INTEGER NOT NULL, " +  // score
                        "%s INTEGER NOT NULL, " +  // total
                        "%s TEXT NOT NULL, " +  // date
                        "%s TEXT NOT NULL, " +  // time
                        "FOREIGN KEY (%s) REFERENCES %s(id))",  // QUIZZES_TABLE
                RECORDS_TABLE, _ID,
                recordColumns[0], recordColumns[1], recordColumns[2],
                recordColumns[3], recordColumns[4], recordColumns[0],
                QUIZZES_TABLE
        ));

        fillData(db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS answers");
        db.execSQL("DROP TABLE IF EXISTS records");
        db.execSQL("DROP TABLE IF EXISTS questions");
        db.execSQL("DROP TABLE IF EXISTS quizzes");
        onCreate(db);
    }


    private void fillData(SQLiteDatabase db) {
        try {
            InputStream qData = context.getAssets().open(quizDataFile);
            int size = qData.available();
            byte[] buffer = new byte[size];
            if (qData.read(buffer) < 0) { throw new JSONException("No data found"); }
            qData.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(json);
            JSONArray quizzes = root.getJSONArray("quizzes");

            for (int i = 0; i < quizzes.length(); i++) {
                JSONObject quizObj = quizzes.getJSONObject(i);

                ContentValues quizVals = new ContentValues();
                quizVals.put(quizzesColumns[0], quizObj.getString("title"));
                quizVals.put(quizzesColumns[1], quizObj.getJSONArray("questions").length());
                quizVals.put(quizzesColumns[2], 1);  // Start at question 1
                quizVals.put(quizzesColumns[3], 0);  // submitted = 0 (FALSE)
                long quizId = db.insert(QUIZZES_TABLE, null, quizVals);

                JSONArray questions = quizObj.getJSONArray("questions");
                for (int j = 0; j < questions.length(); j++) {
                    JSONObject qObj = questions.getJSONObject(j);
                    JSONArray optArr = qObj.getJSONArray("options");
                    List<String> options = new ArrayList<>();

                    for (int k = 0; k < optArr.length(); k++) {
                        options.add(optArr.getString(k));
                    }
                    Question q = new Question(
                            qObj.getInt("order_num"),
                            qObj.getString("question_text"),
                            qObj.getString("video"),
                            options,
                            qObj.getInt("correct_index")
                    );
                    insertQuestionIntoDB(db, quizId, q);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "fillData: Input stream error occurred.", e);
        } catch (JSONException e) {
            Log.e(TAG, "fillData: Error trying to read JSON file.", e);
        }
    }


    /**
     * Helper method for inserting question data into the database
     * @param db The database to add a question to
     * @param quizId The ID value of the quiz
     * @param q The actual Question object and its data
     */
    private void insertQuestionIntoDB(SQLiteDatabase db, long quizId, Question q) {
        ContentValues qVals = new ContentValues();
        qVals.put(questionColumns[0], quizId);  // quiz_id
        qVals.put(questionColumns[1], q.getQuestionText());  // question_text
        qVals.put(questionColumns[2], q.getVideoName());  // video
        qVals.put(questionColumns[3], q.getOrder());  // order_num
        qVals.put(questionColumns[4], q.getCorrectIndex());  // correct_index
        long questionId = db.insert(QUESTIONS_TABLE, null, qVals);

        List<String> options = q.getOptions();
        for (int i = 0; i < options.size(); i++) {
            ContentValues aVals = new ContentValues();
            aVals.put(optionColumns[0], questionId);   // question_id
            aVals.put(optionColumns[1], i);            // option_index
            aVals.put(optionColumns[2], options.get(i)); // answer_text
            db.insert(ANSWERS_TABLE, null, aVals);
        }
    }


    /**
     * Insert a record of a quiz attempt into the database.
     * recordColumns = {"quiz_id", "score", "total", "date", "time"}
     * @param db The database to insert a record into
     * @param quizId The quiz ID
     * @param score The score of the attempt
     * @param total The total number of questions
     * @param date A String representation of the date of the attempt
     * @param time A String representation of the time of the attempt
     */
    private void recordScoreIntoDB(
            SQLiteDatabase db, int quizId, int score, int total, String date, String time) {
        ContentValues qVals = new ContentValues();
        qVals.put(recordColumns[0], quizId);
        qVals.put(recordColumns[1], score);
        qVals.put(recordColumns[2], total);
        qVals.put(recordColumns[3], date);
        qVals.put(recordColumns[4], time);
        long questionId = db.insert("records", null, qVals);
    }

    /**
     * Get the first quiz in the database that has not yet been submitted.
     * (Assume there will always only be up to one quiz that is not marked as submitted)
     * @return QuizAttempt object with data of the quiz that hasn't been submitted
     */
    public QuizAttempt getInProgressAttempt() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(QUIZZES_TABLE,
                new String[]{_ID, quizzesColumns[1], quizzesColumns[2]},  // id, num_questions
                quizzesColumns[3] + " = 0",            // WHERE submitted = 0
                null, null, null, null);

        if (cursor.moveToFirst()) {
            int quizId = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            int numQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[1]));
            int currentQuestion = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[2]));
            cursor.close();
            QuizAttempt q = new QuizAttempt(quizId, numQuestions);
            q.setCurrentQuestion(currentQuestion);
            return q;
        }
        cursor.close();
        return null;
    }

    /**
     * Creates a new quiz attempt in the database and returns the data for easy access.
     * @return QuizAttempt object with the relevant data that was inserted into the database
     */
    public QuizAttempt createNewAttempt() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(QUIZZES_TABLE,
                new String[]{_ID, quizzesColumns[1]},  // _ID, num_questions
                null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int quizId = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            int numQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(quizzesColumns[1]));
            cursor.close();

            SQLiteDatabase wDb = getWritableDatabase();
            ContentValues vals = new ContentValues();
            vals.put(quizzesColumns[3], 0);  // submitted = false
            wDb.update(QUIZZES_TABLE, vals, _ID + " = ?", new String[]{String.valueOf(quizId)});

            return new QuizAttempt(quizId, numQuestions);
        }
        cursor.close();
        return null;
    }
}
