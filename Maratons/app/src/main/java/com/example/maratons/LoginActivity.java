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

import com.example.maratons.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохраненную тему перед созданием активности
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Установить начальное состояние изображений
        ThemeHelper.setInitialImageState(this);

        // Добавляем обработчик клика на лампу для смены темы
        binding.lampLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.toggleTheme(LoginActivity.this);
            }
        });

        binding.autorizeties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.pasts.getText().toString().isEmpty() || binding.parole.getText().toString().isEmpty()) {
                    CustomNotifications.showCustomToast(getApplicationContext(), "Lūdzu, aizpildiet laukumus!");
                } else {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(binding.pasts.getText().toString(), binding.parole.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                CustomNotifications.showCustomToast(getApplicationContext(), "Mēģiniet vēlreiz!");
                            });
                }
            }
        });

        binding.velNavReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        binding.atpakal3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}