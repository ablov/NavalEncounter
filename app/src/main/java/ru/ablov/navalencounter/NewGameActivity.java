package ru.ablov.navalencounter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import ru.ablov.navalencounter.Data.Gamer;
import ru.ablov.navalencounter.Data.LocalData;
import ru.ablov.navalencounter.Data.MyMap;
import ru.ablov.navalencounter.Data.Ship;
import ru.ablov.navalencounter.Utils.Units;

import static ru.ablov.navalencounter.Data.Constants.GAMERS;
import static ru.ablov.navalencounter.Data.Constants.GAMER_PREPARING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_READY;
import static ru.ablov.navalencounter.Data.Constants.TIMEOUT;


public class NewGameActivity extends AppCompatActivity {

    private ArrayList<Ship> ships;

    private DatabaseReference mData;
    private String nickname, currentUserId;
    private Gamer gamer;

    private boolean checkDataChange = true; // Запрещает зацикливание onDataChange

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        // Обнуляем файлы данных
        new LocalData().clearData();

        // Проверка нет ли уже такого игрока
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            FirebaseUser mUser = mAuth.getCurrentUser();

            if (mUser != null) {
                currentUserId = mUser.getUid();
                nickname = mUser.getDisplayName();
            }
        }

        mData = FirebaseDatabase.getInstance().getReference().child(GAMERS);
        mData.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(checkDataChange && snapshot.hasChild(currentUserId)) {
                    checkDataChange = false;
                    // Проверка на таймаут
                    Long timestamp = snapshot.child(currentUserId).child("timestamp").getValue(Long.class);
                    long time = System.currentTimeMillis();
                    if (Math.abs(time - timestamp) > TIMEOUT) {
                        new LocalData().clearData();
                        mData.child(currentUserId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                startNewGame();
                            }
                        });
                        return;
                    }

                    // Предупреждение о смене игрока
                    AlertDialog.Builder askForNewGame = new AlertDialog.Builder(NewGameActivity.this);
                    askForNewGame.setTitle("The Gamer Already Playing!");
                    askForNewGame.setMessage("The gamer " + nickname + " already registered as active player. " +
                            "Would you like to reset him?");
                    askForNewGame.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new LocalData().clearData();
                            FirebaseDatabase.getInstance().getReference().child(GAMERS).
                                    child(currentUserId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startNewGame();
                                }
                            });
                            dialog.dismiss();
                        }
                    });
                    askForNewGame.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            NewGameActivity.this.finish();
                        }
                    });
                    askForNewGame.show();
                } else {
                    startNewGame();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                final AlertDialog.Builder error = new AlertDialog.Builder(NewGameActivity.this);
                error.setTitle("databaseError");
                error.setMessage(databaseError.getMessage());
                error.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                error.show();
            }
        });

    }

    @SuppressLint("ClickableViewAccessibility")
    private void startNewGame() {
        this.setTitle("Naval Encounter - " + nickname);

        mData = FirebaseDatabase.getInstance().getReference().child(GAMERS).child(currentUserId);
        mData.keepSynced(true);

        gamer = new Gamer(currentUserId, nickname, GAMER_PREPARING);
        mData.setValue(gamer)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(NewGameActivity.this);
                        alert.setTitle("Data error");
                        alert.setMessage(e.getMessage());
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alert.show();
                    }
                });

        // Элементы на экране
        final ImageView gameField = findViewById(R.id.game_field);
        Button submit = findViewById(R.id.submit_btn);

        Units u = new Units(this, gameField);

        final MyMap myMap = new MyMap();
        ships = new ArrayList<>();

        // Готовим графические элементы
        gameField.getLayoutParams().width = u.getGameFieldSize();
        gameField.getLayoutParams().height = u.getGameFieldSize();
        submit.getLayoutParams().height = u.getUnitSize();
        gameField.requestLayout();

        ships.add(new Ship(findViewById(R.id.imageOne1), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageOne2), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageOne3), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageOne4), u, myMap, this));

        ships.add(new Ship(findViewById(R.id.imageTwo1), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageTwo2), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageTwo3), u, myMap, this));

        ships.add(new Ship(findViewById(R.id.imageThree1), u, myMap, this));
        ships.add(new Ship(findViewById(R.id.imageThree2), u, myMap, this));

        ships.add(new Ship(findViewById(R.id.imageFour), u, myMap, this));

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверяем, все ли корабли расставлены
                boolean isReady = true;

                for (Ship ship : ships) {
                    isReady = isReady && ship.isShipReady();
                    if (!isReady) break;
                }

                if (isReady) {
                    // Регистрируем игру
                    gamer.setState(GAMER_READY);
                    gamer.resetTimestamp();
                    mData.setValue(gamer)
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(NewGameActivity.this);
                                    alert.setTitle("Data error");
                                    alert.setMessage(e.getMessage());
                                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    alert.show();
                                }
                            });
                    LocalData data = new LocalData();
                    if (data.storeShips(ships) && data.storeScore(20)){
                        startActivity(new Intent(NewGameActivity.this, GamerListActivity.class));
                        NewGameActivity.this.finish();
                    } else {
                        Toast.makeText(NewGameActivity.this, "Unable to save your data", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(NewGameActivity.this, "Ships is not ready yet!", Toast.LENGTH_LONG).show();
                }
            }
        });

        gameField.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gamer.resetTimestamp();
                mData.child("timestamp").setValue(gamer.getTimestamp());

                return false;
            }

        });

    }
}
