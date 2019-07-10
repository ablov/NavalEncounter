package ru.ablov.navalencounter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WinActivity extends AppCompatActivity {

    private DatabaseReference mGamersDatabase;
    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win);

        // Определяем игрока
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            FirebaseUser mUser = mAuth.getCurrentUser();
            if (mUser != null) {
                currentUserID = mUser.getUid();
                this.setTitle("Naval Encounter - " + mUser.getDisplayName());
            }
        }

        // Инициализируем игроков
        mGamersDatabase = FirebaseDatabase.getInstance().getReference().child("Gamers");
        mGamersDatabase.keepSynced(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Удаляем игрока
        mGamersDatabase.child(currentUserID).setValue(null);
    }
}
