package ru.ablov.navalencounter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView email;
    private TextView passwd;
    private Button login_btn;
    private Button register_btn;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email = findViewById(R.id.start_email);
        passwd = findViewById(R.id.start_password);
        login_btn = findViewById(R.id.login_btn);
        register_btn = findViewById(R.id.create_btn);

        // Проверяем, есть ли сохранённые email и password и инициализируем поля
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        String savedEmail = sharedPref.getString(getString(R.string.saved_email), "");
        String savedPassword = sharedPref.getString(getString(R.string.saved_password), "");
        email.setText(savedEmail);
        passwd.setText(savedPassword);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get authorization
                final String emailS = email.getText().toString();
                final String passwdS = passwd.getText().toString();

                if (emailS.isEmpty() || passwdS.isEmpty()) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("Empty fields");
                    dialog.setMessage("Please complete the form");
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return;
                }

                final ProgressDialog progress = new ProgressDialog(MainActivity.this);
                progress.setMessage("Loging...");
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
                progress.show();
                mAuth = FirebaseAuth.getInstance();
                if (mAuth == null) {
                    Toast.makeText(MainActivity.this, "Can't connect to Firebase. Check internet connection.", Toast.LENGTH_LONG).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(emailS, passwdS).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                        // Вход в игру
                            progress.dismiss();
                            // Сохраняем текущий e-mail и password
                            SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getString(R.string.saved_email), emailS);
                            editor.putString(getString(R.string.saved_password), passwdS);
                            editor.apply();
                            // Показываем тост и запускаем игру
                            Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, NewGameActivity.class));
                        } else {
                            progress.dismiss();
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("Can't login");
                            dialog.setMessage(task.getException().getMessage());
                            dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        }
                    }
                });
            }
        });

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CreateUser.class));
            }
        });
    }
}
