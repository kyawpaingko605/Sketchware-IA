package pro.sketchware.activities.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import java.util.Locale;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityIaSettingsBinding;
import pro.sketchware.utility.TranslationFunction;

public class IaSettingsActivity extends BaseAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);

        var binding = ActivityIaSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.topAppBar.setTitle(R.string.ia_settings_title);
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        // Show device language and an English notice that AI will respond in the detected language
        {
            Locale locale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? getResources().getConfiguration().getLocales().get(0)
                    : getResources().getConfiguration().locale;
            String deviceLanguageName = locale.getDisplayLanguage(locale);
            if (binding.tvDeviceLanguageNotice != null) {
                binding.tvDeviceLanguageNotice.setText(getString(R.string.ia_device_language_notice_template, deviceLanguageName));
                binding.tvDeviceLanguageNotice.setVisibility(View.VISIBLE);
            }
        }

        {
            View view = binding.appBarLayout;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top + insets.top, right + insets.right, bottom);
                return i;
            });
        }

        {
            View view = binding.contentScroll;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(left, top, right, bottom + insets.bottom);
                return i;
            });
        }

        // Persistência simples via SharedPreferences
        final SharedPreferences prefs = getSharedPreferences("ia_settings", MODE_PRIVATE);
        final String K_GROQ = "groq_api_key";
        final String K_MORPH = "morph_api_key";
        final String K_GROQ_ENABLED = "groq_enabled";
        final String K_MORPH_ENABLED = "morph_enabled";

        // Carregar valores salvos - Groq
        if (binding.etGroqApiKey != null) {
            binding.etGroqApiKey.setText(prefs.getString(K_GROQ, ""));
            binding.etGroqApiKey.addTextChangedListener(saveWatcher(prefs, K_GROQ));
        }

        // Carregar valores salvos - Morph
        if (binding.etMorphApiKey != null) {
            binding.etMorphApiKey.setText(prefs.getString(K_MORPH, ""));
            binding.etMorphApiKey.addTextChangedListener(saveWatcher(prefs, K_MORPH));
        }

        // Inicializar switches (sempre habilitados; OFF por padrão se não houver preferência)
        boolean groqEnabled = prefs.getBoolean(K_GROQ_ENABLED, false);
        boolean morphEnabled = prefs.getBoolean(K_MORPH_ENABLED, false);

        if (binding.switchGroq != null) {
            binding.switchGroq.setEnabled(true);
            binding.switchGroq.setChecked(groqEnabled);
            binding.switchGroq.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(K_GROQ_ENABLED, isChecked).apply();
                setGroqEnabled(binding, isChecked);
            });
            setGroqEnabled(binding, binding.switchGroq.isChecked());
        }

        if (binding.switchMorph != null) {
            binding.switchMorph.setEnabled(true);
            binding.switchMorph.setChecked(morphEnabled);
            binding.switchMorph.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(K_MORPH_ENABLED, isChecked).apply();
                setMorphEnabled(binding, isChecked);
            });
            setMorphEnabled(binding, binding.switchMorph.isChecked());
        }

        // Ações dos botões de ajuda/links
        if (binding.btnGroqGetKey != null) {
            binding.btnGroqGetKey.setOnClickListener(v -> openUrl(getString(R.string.url_groq_api_keys)));
        }
        if (binding.btnMorphGetKey != null) {
            binding.btnMorphGetKey.setOnClickListener(v -> openUrl(getString(R.string.url_morph_api_keys)));
        }
    }

    private void setGroqEnabled(ActivityIaSettingsBinding binding, boolean enabled) {
        if (binding.tilGroq != null) binding.tilGroq.setEnabled(enabled);
        if (binding.etGroqApiKey != null) binding.etGroqApiKey.setEnabled(enabled);
        if (binding.btnGroqGetKey != null) binding.btnGroqGetKey.setEnabled(true);
    }

    private void setMorphEnabled(ActivityIaSettingsBinding binding, boolean enabled) {
        if (binding.tilMorph != null) binding.tilMorph.setEnabled(enabled);
        if (binding.etMorphApiKey != null) binding.etMorphApiKey.setEnabled(enabled);
        if (binding.btnMorphGetKey != null) binding.btnMorphGetKey.setEnabled(true);
    }

    private TextWatcher saveWatcher(SharedPreferences prefs, String key) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString(key, s.toString()).apply();
            }
        };
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.ia_open_link_error), Toast.LENGTH_SHORT).show();
        }
    }
}