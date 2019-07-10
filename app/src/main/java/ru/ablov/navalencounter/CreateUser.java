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
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class CreateUser extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        final EditText nickname = findViewById(R.id.reg_nickname);
        final EditText email = findViewById(R.id.reg_email);
        final EditText password = findViewById(R.id.reg_password);
        final EditText password2 = findViewById(R.id.reg_password2);
        Button regBtn = findViewById(R.id.reg_btn);

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String strNickname = nickname.getText().toString();
                final String strEmail = email.getText().toString();
                final String strPassword = password.getText().toString();
                String strPasswd2 = password2.getText().toString();

                if (strNickname.isEmpty() || strEmail.isEmpty() || strPassword.isEmpty() || strPasswd2.isEmpty()) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(CreateUser.this);
                    dialog.setTitle("Empty fields");
                    dialog.setMessage("Please complete the form.");
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return;
                }

                if (!strPasswd2.contentEquals(strPassword)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(CreateUser.this);
                    dialog.setTitle("Passwords not mach");
                    dialog.setMessage("Please retype passwords.");
                    dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return;
                }

                final ProgressDialog progress = new ProgressDialog(CreateUser.this);
                progress.setMessage("Loging...");
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
                progress.show();

                mAuth = FirebaseAuth.getInstance();
                mAuth.createUserWithEmailAndPassword(strEmail, strPassword)
                        .addOnCompleteListener(CreateUser.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if(task.isSuccessful()) {
                                    UserProfileChangeRequest mProfile = new UserProfileChangeRequest.Builder().setDisplayName(strNickname).build();
                                    FirebaseUser mUser = mAuth.getCurrentUser();
                                    if (mUser != null) {
                                        mUser.updateProfile(mProfile)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Сохраняем текущий e-mail и password
                                                    Context ctx = getApplicationContext();
                                                    SharedPreferences sharedPref = ctx.getSharedPreferences(getString(R.string.credentials), Context.MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = sharedPref.edit();
                                                    editor.putString(getString(R.string.saved_email), strEmail);
                                                    editor.putString(getString(R.string.saved_password), strPassword);
                                                    editor.apply();

                                                    Toast.makeText(CreateUser.this, "Gamer " + strNickname + " created.", Toast.LENGTH_SHORT).show();
                                                    startActivity( new Intent(CreateUser.this, NewGameActivity.class));
                                                    CreateUser.this.finish();
                                                } else {
                                                    Toast.makeText(CreateUser.this, "Unable to update profile with nickname.", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(CreateUser.this);
                                        dialog.setTitle("Passwords not mach");
                                        dialog.setMessage("Please retype passwords.");
                                        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                        progress.dismiss();
                                        dialog.show();
                                        return;
                                    }
                                    progress.dismiss();

                                } else {
                                    progress.dismiss();
                                    AlertDialog.Builder dialog = new AlertDialog.Builder(CreateUser.this);
                                    dialog.setTitle("Can't create user");
                                    dialog.setMessage(Objects.requireNonNull(task.getException()).getMessage());
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

    }
}
