package cs477.gmu.p2_rzaman9;

import android.content.Context;

public class Settings {

    public static final String PREFS_NAME = "settings";

    public static final String PROGRESS_BAR_KEY = "progressBar";
    public static final String COLOR_SCORES_KEY = "colorScores";
    public static final String VIDEO_SPEED_KEY = "videoSpeed";
    public static final String SHUFFLE_KEY = "shuffleQuestions";

    public static boolean get(Context context, String key) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, false);  // default off
    }

    /*public static void set(Context context, String key, boolean value) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(key, value).apply();
    }*/
}
