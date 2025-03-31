package com.example.maratons;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maratons.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохраненную тему перед созданием активности
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Правильная инициализация binding
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Установить начальное состояние изображений
        ThemeHelper.setInitialImageState(this);

        // Добавляем обработчик клика на лампу для смены темы
        binding.lampLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.toggleTheme(RegisterActivity.this);
            }
        });

        // Обработчик для кнопки registreties
        binding.registreties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.pasts.getText().toString().isEmpty() ||
                        binding.parole.getText().toString().isEmpty() ||
                        binding.lietotajs.getText().toString().isEmpty()) {

                    CustomNotifications.showCustomToast(getApplicationContext(), "Lūdzu, aizpildiet laukumus!");
                } else {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                                    binding.pasts.getText().toString(),
                                    binding.parole.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        HashMap<String, String> userInfo = new HashMap<>();
                                        userInfo.put("email", binding.pasts.getText().toString());
                                        userInfo.put("username", binding.lietotajs.getText().toString());

                                        FirebaseDatabase.getInstance().getReference()
                                                .child("Users")
                                                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                                                .setValue(userInfo);

                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    } else {
                                        CustomNotifications.showCustomSnackbar(
                                                findViewById(android.R.id.content),
                                                "Reģistrācijas kļūda: " + task.getException().getMessage()
                                        );
                                    }
                                }
                            });
                }
            }
        });

        binding.atpakal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}