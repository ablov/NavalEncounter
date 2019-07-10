package ru.ablov.navalencounter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import ru.ablov.navalencounter.Data.Gamer;
import ru.ablov.navalencounter.Data.LocalData;
import ru.ablov.navalencounter.Data.NavalMap;
import ru.ablov.navalencounter.Data.ShipInfo;
import ru.ablov.navalencounter.Utils.Units;

import static ru.ablov.navalencounter.Data.Constants.*;

public class DefenceActivity extends AppCompatActivity {

    private FirebaseUser mUser;
    private String currentUserID;
    private String partnerID;
    private DatabaseReference mGamersDatabase;
    private Gamer currentGamer;
    private Gamer partnerGamer;

    private Units u;
    private NavalMap myMap;
    private ArrayList<ShipInfo> siList;
    private int score;

    private boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defence);

        initGamers(); // Инициализируем игроков - текущего и на кого нападаем

        resetTimestamp(currentGamer); // Обновляем timestamp

        initData(); // Загрузка и инициализация карты и кораблей

        prepareElements(); // Устанавливаем корабли на экране

        setDatabaseListeners();  // Устанавливаем обработчики изменения данных
    }

    // Инициализируем игроков - текущего и на кого нападаем
    private void initGamers() {
        // Определяем игрока
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            mUser = mAuth.getCurrentUser();
            if (mUser != null) {
                currentUserID = mUser.getUid();
                this.setTitle("Naval Encounter - " + mUser.getDisplayName());
            } else {
                // ToDo: showError();
            }
        } else {
            // ToDo: showError();
        }

        // Инициализируем игроков
        mGamersDatabase = FirebaseDatabase.getInstance().getReference().child("Gamers");
        mGamersDatabase.keepSynced(true);

        mGamersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(currentUserID)) {
                    currentGamer = dataSnapshot.child(currentUserID).getValue(Gamer.class);
                    if (currentGamer != null && dataSnapshot.hasChild(currentGamer.getPartner())) {
                        partnerGamer = dataSnapshot.child(currentGamer.getPartner()).getValue(Gamer.class);
                        if (partnerGamer != null) partnerID = partnerGamer.getUid();
                    }
                }

                if (currentGamer == null || partnerGamer == null) showError("Gamers not found in database");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError(databaseError.getMessage());
            }
        });
    }

    // Загрузка и инициализация карты и кораблей
    private  void initData() {
        // Восстанавливаем расположение кораблей на поле из файла
        LocalData data = new LocalData();
        myMap = data.getMap();
        siList = data.getShips();
        score = data.getScore();
    }

    // Устанавливаем размеры графических элементов на экране
    private void prepareElements() {

        ImageView gameField = findViewById(R.id.d_game_field);

        u = new Units(DefenceActivity.this, gameField);

        gameField.getLayoutParams().width = u.getGameFieldSize();
        gameField.getLayoutParams().height = u.getGameFieldSize();
        gameField.requestLayout();

        FrameLayout fl = findViewById(R.id.d_main);
        ImageView iv;

        // Расставляем корабли
        for (ShipInfo si : siList) {
            iv = new ImageView(getApplicationContext());
            int XL=0, YL=0; // длина и высота корабля в клеточках
            switch (si.getSize()) {
                case 1:
                    iv.setImageDrawable(getDrawable(R.drawable.one));
                    XL = YL = 1;
                    break;
                case 2:
                    if (si.isVertical()){
                        iv.setImageDrawable(getDrawable(R.drawable.twovr));
                        XL = 1; YL = 2;
                    }
                    else {
                        iv.setImageDrawable(getDrawable(R.drawable.twohr));
                        XL = 2; YL = 1;
                    }
                    break;
                case 3:
                    if (si.isVertical()) {
                        iv.setImageDrawable(getDrawable(R.drawable.threevr));
                        XL = 1; YL = 3;
                    }
                    else {
                        iv.setImageDrawable(getDrawable(R.drawable.threehr));
                        XL = 3; YL = 1;
                    }
                    break;
                case 4:
                    if (si.isVertical()) {
                        iv.setImageDrawable(getDrawable(R.drawable.fourvr));
                        XL = 1; YL = 4;
                    }
                    else {
                        iv.setImageDrawable(getDrawable(R.drawable.fourhr));
                        XL = 4; YL = 1;
                    }
                    break;
            }
            fl.addView(iv);

            int X, Y;
            X = si.getX();
            Y = si.getY();
            iv.setX(u.toCoord(si.getX()));
            iv.setY(u.toCoord(si.getY()));
            iv.getLayoutParams().height = u.getUnitSize() * YL;
            iv.getLayoutParams().width = u.getUnitSize() * XL;
            iv.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.colorOk));
            iv.requestLayout();
        }

        // Расставляем точки попаданий
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (myMap.getCell(i, j) == NM_TAKEN) {
                    setPastImage(i, j);
                } else if (myMap.getCell(i, j) == NM_KNOCKED) {
                    setHitImage(i, j);
                }
            }
        }


    }

    // Устанавливаем обработчики изменения данных
    private void setDatabaseListeners() {
        if (mGamersDatabase == null) {
            showError("mGamersDatabase is null");
            return;
        }

        mGamersDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (isBusy) return;
                isBusy = true;

                Gamer gamer = dataSnapshot.getValue(Gamer.class);

                // Обработка нападения
                if (gamer != null && gamer.getUid().equals(currentUserID) && gamer.getRequest().size() > 0) {
                    // Удаляем request
                    removeRequest();

                    // Определяем параметры нападения
                    int X = gamer.getRequest().get(0);
                    int Y = gamer.getRequest().get(1);

                    // Обрабатываем нападение, формируем ответ и отражаем это в myMap
                    ArrayList<Integer> answer = checkMap(gamer.getRequest());
                    if (answer == null) return;

                    // Ставим картинку на своей карте и записываем в myMap
                    if (answer.get(0) == NM_EMPTY) {
                        setPastImage(X, Y);
                        myMap.setCell(X, Y, (short) NM_TAKEN);
                    }
                    else {
                        setHitImage(X, Y);
                        myMap.setCell(X, Y, (short) NM_KNOCKED);
                    }

                    // Посылаем ответ в запись партнёра
                    mGamersDatabase.child(partnerID).child("answer").setValue(answer);

                    // Если противник промазал, меняем активити
                    if (answer.get(0) == NM_EMPTY) {
                        // Снимаем обработчик событий
                        mGamersDatabase.removeEventListener(this);

                        // Сохраняем данные
                        LocalData data = new LocalData();
                        if (! (data.storeMap(myMap) && data.storeScore(score) && data.storeShipList(siList))) {
                            showError("Can't save the data!");
                        }

                        // Меняем свой статус в базе
                        gamer.clearRequest();
                        gamer.clearAnswer();
                        gamer.resetTimestamp();
                        gamer.setState(GAMER_IN_GAME_ATTACKING);
                        mGamersDatabase.child(currentUserID).setValue(gamer);

                        // Запускаем активити
                        startActivity(new Intent(DefenceActivity.this, AttackActivity.class));
                        finish();
                    }

                    else { // Противник попал - его ход продолжается
                        score--;
                        if (score == 0) finita(); // проигрыш
                        gamer.clearRequest();
                        gamer.clearAnswer();
                        gamer.resetTimestamp();
                        mGamersDatabase.child(currentUserID).setValue(gamer);
                    }
                }

                isBusy = false;
            }

            // Обработка проигрыша
            private void finita() {
                // Снимаем обработчик событий
                mGamersDatabase.removeEventListener(this);

                // Устанавливаем выигрыш партнёра и свой проигрыш
                mGamersDatabase.child(partnerID).child("state").setValue(GAMER_WIN);
                mGamersDatabase.child(currentUserID).child("state").setValue(GAMER_LOST);
                mGamersDatabase.keepSynced(true);

                // Обнуляем файлы
                new LocalData().clearData();

                // Переходим на прощальную Activity
                Intent intent = new Intent(DefenceActivity.this, LostActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            private void removeRequest() {
                mGamersDatabase.child(currentUserID).child("request").setValue(null);
            }

            // Возвращает список из 3 элементов [0] - код результата (см. Constants.java NM_*),
            // [1] и [2] - доп. параметры
            private ArrayList<Integer> checkMap(ArrayList<Integer> request) {
                ArrayList<Integer> answer = new ArrayList<>(3);
                if (request != null && request.size() > 0) {
                    int X = request.get(0);
                    int Y = request.get(1);

                    int index = myMap.getCell(X, Y);

                    if (index != NM_UNKNOWN && index != NM_EMPTY && index != NM_TAKEN) { // обрабатываем попадание
                        myMap.setCell(X, Y, (short)NM_KNOCKED);
                        ShipInfo si = siList.get(index - 1);
                        if (si.isIntact(X, Y)) {
                            si.killCell(X, Y);
                            if (si.isLive()) { // "Попал!"
                                answer.add(0, NM_KNOCKED);
                                answer.add(1, X);
                                answer.add(2, Y);
                            } else {            // "Убил!" (в этом случае аргументы - координаты левого верхнего и правого нижнего угла корабля
                                answer.add(0, si.getConstant());
                                answer.add(1, si.getX());
                                answer.add(2, si.getY());
                            }
                        }
                    } else {
                        myMap.setCell(X, Y, (short) NM_EMPTY);
                        answer.add(0, NM_EMPTY);
                        answer.add(1, X);
                        answer.add(2, Y);
                    }
                    return answer;
                }

                return null;
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Устанавливаем "мимо" в клетку
    private void setPastImage(int x, int y) {

        // Подключаем картинку
        FrameLayout rl = findViewById(R.id.d_main);
        ImageView iv;

        iv = new ImageView(getApplicationContext());
        iv.setImageDrawable(getDrawable(R.drawable.tried));
        rl.addView(iv);

        // Позиционируем картинку
        iv.setX(u.toCoord(x));
        iv.setY(u.toCoord(y));

        // Размер картинки
        iv.getLayoutParams().height = u.getUnitSize();
        iv.getLayoutParams().width = u.getUnitSize();
        iv.requestLayout();
    }

    // Устанавливаем "попал" в клетку
    private void setHitImage(int x, int y) {

        // Подключаем картинку
        FrameLayout rl = findViewById(R.id.d_main);
        ImageView iv;

        iv = new ImageView(getApplicationContext());
        iv.setImageDrawable(getDrawable(R.drawable.hitted));
        rl.addView(iv);

        // Позиционируем картинку
        iv.setX(u.toCoord(x));
        iv.setY(u.toCoord(y));

        // Размер картинки
        iv.getLayoutParams().height = u.getUnitSize();
        iv.getLayoutParams().width = u.getUnitSize();
        iv.requestLayout();
    }

    // Сброс значения timestamp у данного игрока
    private void resetTimestamp(Gamer gamer) {
        if (gamer != null) {
            gamer.resetTimestamp();
            mGamersDatabase.child(gamer.getUid()).child("timestamp").setValue(gamer.getTimestamp());
        }
    }

    private void showError(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(DefenceActivity.this);
        dialog.setTitle("Error");
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                DefenceActivity.this.finish();
            }
        });
        dialog.show();
    }
}
