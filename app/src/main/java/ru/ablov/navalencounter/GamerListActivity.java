package ru.ablov.navalencounter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import ru.ablov.navalencounter.Data.Gamer;
import ru.ablov.navalencounter.Data.GamerAdapter;

import static ru.ablov.navalencounter.Data.Constants.GAMER_ESCAPED;
import static ru.ablov.navalencounter.Data.Constants.GAMER_IN_GAME_ATTACKING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_IN_GAME_DEFENDING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_READY;
import static ru.ablov.navalencounter.Data.Constants.GAMER_UNDER_ATTACK;
import static ru.ablov.navalencounter.Data.Constants.TIMEOUT;

public class GamerListActivity extends AppCompatActivity {

    private FirebaseUser mUser;
    private String currentUserID = "";
    private DatabaseReference mGamersDatabase;
    private ArrayList<Gamer> mGamers;
    private GamerAdapter mAdapter;
    private static boolean onChildChangedProcessRunFlag = false;
    private static AlertDialog alertDialog = null;

    private boolean mIsRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamer_list);

        // Определяем текущего пользователя
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            mUser = mAuth.getCurrentUser();
            if (mUser != null) {
                currentUserID = mUser.getUid();
                this.setTitle("Naval Encounter - " + mUser.getDisplayName());
            }
        }

        // Определяем точку входа в базу со списком игроков
        mGamersDatabase = FirebaseDatabase.getInstance().getReference().child("Gamers");
        mGamersDatabase.keepSynced(true);

        // Элемент вывода списка игроков
        ListView listView = findViewById(R.id.gamers_list);

        mGamers = new ArrayList<>(); // Текущий список игроков; формируется динамически в onStart()

        // Адаптер для ListView
        mAdapter = new GamerAdapter(this, currentUserID, mGamers);
        listView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGamersDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Gamer gamer = dataSnapshot.getValue(Gamer.class);
                if (gamer != null) {
                    // Проверяем тайм-аут
                    long timestamp = gamer.getTimestamp();
                    if (Math.abs(System.currentTimeMillis() - timestamp) > TIMEOUT) {
                        // Удалить из базы
                        mGamersDatabase.child(gamer.getUid()).setValue(null);
                    }
                    // Не добавляем в список самих себя
                    if (!gamer.getUid().equals(currentUserID)) {
                        int i;
                        if ((i = mGamers.indexOf(gamer)) == -1) { // новая запись
                            mGamers.add(gamer);
                        } else { // замена текущей
                            mGamers.set(i, gamer);
                        }
                    }
                    mAdapter.notifyDataSetChanged();

                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (onChildChangedProcessRunFlag) return;
                onChildChangedProcessRunFlag = true;
                final Gamer gamer = dataSnapshot.getValue(Gamer.class);
                final String currentUserID = mUser.getUid();
                String partnerNickname = "";

                if(alertDialog != null) {
                    alertDialog.dismiss();
                    alertDialog = null;
                }

                if (gamer != null) {
                    // Обновление изменённой записи в списке игроков и получаем имя "партнёра"
                    for (Gamer g : mGamers) {
                        String GUID = gamer.getUid();
                        if (!GUID.isEmpty() && GUID.equals(g.getUid())) {
                            mGamers.set(mGamers.indexOf(g), gamer);
                        }
                        if (g.getUid().equals(gamer.getPartner())) {
                            partnerNickname = g.getNickname();
                        }
                    }

                    // Обработка реакции на нападение
                    if (gamer.getUid().equals(currentUserID) && gamer.getState().equals(GAMER_UNDER_ATTACK)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(GamerListActivity.this);
                        builder.setTitle("YOU ARE UNDER ATTACK!");
                        builder.setMessage("You are attacked by " + partnerNickname);
                        builder.setPositiveButton("Engage", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Вступаем в бой
                                // Выбираем случайным образом чей ход первый
                                String myState, partnerState;
                                Class activity;
                                if (Math.random() < 1.0) { // ToDo: для отладки - кто согласился, тот защищается (должно стоять 0.5
                                    myState = GAMER_IN_GAME_DEFENDING;
                                    partnerState = GAMER_IN_GAME_ATTACKING;
                                    activity = DefenceActivity.class;
                                } else {
                                    myState = GAMER_IN_GAME_ATTACKING;
                                    partnerState = GAMER_IN_GAME_DEFENDING;
                                    activity = AttackActivity.class;
                                }
                                // Меняем состояние противника на игру-нападение
                                String partner = gamer.getPartner();
                                mGamersDatabase.child(partner).child("state").setValue(partnerState);
                                // Теперь входим в игру сами
                                gamer.setState(myState);
                                mGamersDatabase.child(gamer.getUid()).setValue(gamer);
                                startActivity(new Intent(GamerListActivity.this, activity));
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("Escape", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Отказ от боя
                                // Сначала меняем состояние противника на "Ready"
                                String partner = gamer.getPartner();
                                mGamersDatabase.child(partner).child("state").setValue(GAMER_READY);
                                mGamersDatabase.child(partner).child("partner").setValue(null);
                                // Теперь обновляем своё состояние
                                gamer.setState(GAMER_ESCAPED);
                                gamer.setPartner(null);
                                mGamersDatabase.child(gamer.getUid()).setValue(gamer);
                                dialog.dismiss();
                            }
                        });
                        alertDialog = builder.create();
                        if (mIsRunning) {
                            alertDialog.show(); // ToDo: Unable to add window -- token android.os.BinderProxy@7f3e8d5 is not valid; is your activity running?
                        }
                    }

                    //
                    // Обработка случая, когда противник принял игру
                    //    Сначла запускаем DefenceActivity, далее оттуда уже запускаем AttackActivity для нападающего
                    //
                    if (gamer.getUid().equals(currentUserID)) {
                        if (gamer.getState().equals(GAMER_IN_GAME_ATTACKING)) {
                            Intent intent = new Intent(GamerListActivity.this, AttackActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            mGamersDatabase.removeEventListener(this);
                            startActivity(intent);
                            GamerListActivity.this.finish();
                        } else if (gamer.getState().equals(GAMER_IN_GAME_DEFENDING)) {
                            Intent intent = new Intent(GamerListActivity.this, DefenceActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            mGamersDatabase.removeEventListener(this);
                            startActivity(intent);
                            GamerListActivity.this.finish();
                        }
                    }

                }
                mAdapter.notifyDataSetChanged();
                onChildChangedProcessRunFlag = false;
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Gamer gamer = dataSnapshot.getValue(Gamer.class);
                mGamers.remove(gamer);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsRunning = true;
    }
}
