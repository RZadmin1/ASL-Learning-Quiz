package cs477.gmu.p2_rzaman9;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;


public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences(
                Settings.PREFS_NAME, Context.MODE_PRIVATE);

        CheckBox progBarToggle, shuffleToggle, colorScoresToggle, videoSpeedToggle;

        progBarToggle = view.findViewById(R.id.progBarToggle);
        shuffleToggle = view.findViewById(R.id.shuffleToggle);
        colorScoresToggle = view.findViewById(R.id.colorScoresToggle);
        videoSpeedToggle = view.findViewById(R.id.videoSpeedToggle);

        // Load current saved state into checkboxes
        progBarToggle.setChecked(prefs.getBoolean(Settings.PROGRESS_BAR_KEY, false));
        shuffleToggle.setChecked(prefs.getBoolean(Settings.SHUFFLE_KEY, false));
        colorScoresToggle.setChecked(prefs.getBoolean(Settings.COLOR_SCORES_KEY, false));
        videoSpeedToggle.setChecked(prefs.getBoolean(Settings.VIDEO_SPEED_KEY, false));

        // Save immediately on each toggle change
        progBarToggle.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(Settings.PROGRESS_BAR_KEY, isChecked).apply());
        shuffleToggle.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(Settings.SHUFFLE_KEY, isChecked).apply());
        colorScoresToggle.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(Settings.COLOR_SCORES_KEY, isChecked).apply());
        videoSpeedToggle.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(Settings.VIDEO_SPEED_KEY, isChecked).apply());

        // X button closes the settings panel
        view.findViewById(R.id.settingsExitButton).setOnClickListener(v ->
                ((MainActivity)requireActivity()).onSettingsButtonClicked(v));
    }
}