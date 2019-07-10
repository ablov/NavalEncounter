package ru.ablov.navalencounter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import ru.ablov.navalencounter.Utils.Units;

import static ru.ablov.navalencounter.Data.Constants.*;

public class AttackActivity extends AppCompatActivity {

    private FirebaseUser mUser;
    private String currentUserID;
    private String partnerID;
    private DatabaseReference mGamersDatabase;
    private Gamer currentGamer;
    private Gamer partnerGamer;

    private Units u;

    // Обработка сдвига прицела
    private float dX, dY, lastX, lastY;
    private int lastAction;

    private boolean isSet = false; // Прицел установлен?
    private int fsX, fsY; // Координаты прицела (если он установлен

    private NavalMap tacticMap; // Карта боя, обслуживется через LocalData

    private ProgressDialog progress;
    private boolean isBusy = false;

    Button fireBtn;
    ImageView gameField;
    ImageView foresight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack);

        initGamers(); // Инициализируем игроков - текущего и на кого нападаем

        resetTimestamp(currentGamer); // Обновляем timestamp текущего игрока

        initData(); // Загрузка и инициализация карты и кораблей

        prepareElements(); // Устанавливаем размеры графических элементов на экране

        setTacticMap(); // Заполняем карту

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

    // Устанавливаем размеры графических элементов на экране
    @SuppressLint("ClickableViewAccessibility")
    private void prepareElements() {

        gameField = findViewById(R.id.a_game_field);
        foresight = findViewById(R.id.a_foresight);
        fireBtn = findViewById(R.id.a_fireBtn);

        u = new Units(AttackActivity.this, gameField);

        gameField.getLayoutParams().width = u.getGameFieldSize();
        gameField.getLayoutParams().height = u.getGameFieldSize();
        gameField.requestLayout();

        foresight.getLayoutParams().width = u.getUnitSize();
        foresight.getLayoutParams().height = u.getUnitSize();
        foresight.requestLayout();

        foresight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x, y, delta;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        dX = v.getX() - lastX;
                        dY = v.getY() - lastY;
                        lastAction = MotionEvent.ACTION_DOWN;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        x = event.getRawX();
                        y = event.getRawY();
                        delta = Math.abs(x - lastX) + Math.abs(y - lastY);
                        if (delta > DELTA) {
                            x += dX; y += dY;
                            v.setX(x); v.setY(y); // Возможный выход за границы видимой части поля обрабатываем в setForesight
                            lastAction = MotionEvent.ACTION_MOVE;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_MOVE) {
                            setForesight(v);
                        }
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });

        fireBtn.getLayoutParams().height = u.getUnitSize();
        fireBtn.requestLayout();

        fireBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSet) {
                    Toast.makeText(AttackActivity.this, "The foresight not set!", Toast.LENGTH_SHORT).show();
                } else {
                    fireBtn.setEnabled(false); // Отключаем кнопку выстрела до получения ответа
                    // Открываем ProgressDialog
                    progress = new ProgressDialog(AttackActivity.this);
                    progress.setMessage("Please wait for answer...");
                    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progress.setIndeterminate(true);
                    progress.show();
                    // Производим выстрел
                    makeFire();
                }
            }
        });
    }

    // Загрузка и инициализация карты
    private void initData() {
        LocalData localData = new LocalData();
        tacticMap = localData.getTacticMap();
        if (tacticMap == null) tacticMap = new NavalMap();
    }

    // Заполняем карту боя
    private void setTacticMap() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                setImage(x, y);
            }
        }
    }

    // Устанавливаем картинку в клетку на основании содержимого TacticMap
    private void setImage(int x, int y) {

        short state = tacticMap.getCell(x, y);
        if (state == NM_UNKNOWN) return;

        // Подключаем картинку
        RelativeLayout rl = findViewById(R.id.a_main);
        ImageView iv;

        // Теперь точки попаданий
        iv = new ImageView(getApplicationContext());
        if (state == NM_EMPTY) {
            iv.setImageDrawable(getDrawable(R.drawable.tried));
        } else if (state == NM_KNOCKED) {
            iv.setImageDrawable(getDrawable(R.drawable.hitted));
        }
        rl.addView(iv);

        // Позиционируем картинку
        iv.setX(u.toCoord(x));
        iv.setY(u.toCoord(y));

        // Размер картинки
        iv.getLayoutParams().height = u.getUnitSize();
        iv.getLayoutParams().width = u.getUnitSize();
        iv.requestLayout();

        // Теперь аналогично размещаем убитые корабли
        iv = null;
        if (state == NM_KILLED_1) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.one));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize();
            iv.getLayoutParams().width = u.getUnitSize();
        } else if (state == NM_KILLED_2H) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.twohr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize();
            iv.getLayoutParams().width = u.getUnitSize() * 2;
        } else if (state == NM_KILLED_2V) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.twovr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize() * 2;
            iv.getLayoutParams().width = u.getUnitSize();
        } else if (state == NM_KILLED_3H) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.threehr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize();
            iv.getLayoutParams().width = u.getUnitSize() * 3;
        } else if (state == NM_KILLED_3V) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.threevr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize() * 3;
            iv.getLayoutParams().width = u.getUnitSize();
        } else if (state == NM_KILLED_4H) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.fourhr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize();
            iv.getLayoutParams().width = u.getUnitSize() * 4;
        } else if (state == NM_KILLED_4V) {
            iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getDrawable(R.drawable.fourvr));
            rl.addView(iv);
            iv.getLayoutParams().height = u.getUnitSize() * 4;
            iv.getLayoutParams().width = u.getUnitSize();
        }

        if (iv != null) {
            iv.setX(u.toCoord(x));
            iv.setY(u.toCoord(y));
            iv.requestLayout();
        }
    }

    // Установка мишени с выравниванием и проверкой
    private void setForesight(View v) {
        short X, Y;

        // Координаты левого верхнего угла игрового поля
        int left = u.getMapX();
        int top = u.getMapY();

        X = u.toNumber(v.getX() - left);
        Y = u.toNumber(v.getY() - top);

        if (X >= SIZE) X = SIZE - 1;
        if (Y == SIZE) Y = SIZE - 1;
        if (X < 0) X = 0;
        if (Y < 0) Y = 0;
        if (Y > SIZE) { // Если увели мишень сильно вниз - возвращаем на место
            X = 0;
            Y = SIZE;
        }

        v.setX(u.toCoord(X) + left);
        v.setY(u.toCoord(Y) + top);

        // Проверка корректности установки прицела
        if (tacticMap.getCell(X, Y) == NM_UNKNOWN) {
            ((ImageView)v).setImageDrawable(getDrawable(R.drawable.foresight_red));
            fsX = X; fsY = Y;
            isSet = true;
        } else {
            ((ImageView)v).setImageDrawable(getDrawable(R.drawable.foresight));
            fsX = -1; fsY = -1;
            isSet = false;
        }
    }

    // Обработка выстрела
    private void makeFire() {
        resetTimestamp(currentGamer);
        ArrayList<Integer> req = new ArrayList<>(2);
        req.add(0, fsX);
        req.add(1, fsY);
        partnerGamer.setRequest(req);
        mGamersDatabase.child(partnerID).setValue(partnerGamer);
    }

    // Сброс значения timestamp у данного игрока
    private void resetTimestamp(Gamer gamer) {
        if (gamer != null) {
            gamer.resetTimestamp();
            mGamersDatabase.child(gamer.getUid()).child("timestamp").setValue(gamer.getTimestamp());
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

                Gamer gamer = dataSnapshot.getValue(Gamer.class);

                //
                // Обработка собственного состояния
                //
                if (gamer != null && gamer.getUid().equals(currentUserID)) {
                    //
                    // Обработка разультатов нападения
                    //
                    if (gamer.getState().equals(GAMER_IN_GAME_ATTACKING) && gamer.getAnswer().size() != 0) {

                        // Блокируем повторную обработку и убираем ответ
                        isBusy = true;
                        mGamersDatabase.child(currentUserID).child("answer").setValue(null);

                        ArrayList<Integer> result = gamer.getAnswer();
                        int answer = result.get(0);

                        if (answer == NM_EMPTY) { // Обработка ответа "мимо"

                            Toast.makeText(AttackActivity.this, "You missed!", Toast.LENGTH_SHORT).show();

                            // отключаем обработчик событий
                            mGamersDatabase.removeEventListener(this);

                            // записываем в тактическую карту промах и сохраняем в файл
                            tacticMap.setCell(fsX, fsY, (short) NM_EMPTY);
                            new LocalData().storeTacticMap(tacticMap);

                            // переводим свой статус в "Оборона"
                            mGamersDatabase.child(currentUserID).child("state").setValue(GAMER_IN_GAME_DEFENDING);

                            // запускаем оборонное активити и завершаем эту
                            startActivity(new Intent(AttackActivity.this, DefenceActivity.class));
                            finish();

                        } else if (answer == NM_KNOCKED) { // Обработка ответа "попал"

                            Toast.makeText(AttackActivity.this, "The enemy ship is damaged!", Toast.LENGTH_SHORT).show();

                            // освобождаем кнопку выстрела
                            fireBtn.setEnabled(true);

                            // записываем в тактическую карту попадание и изображаем его
                            tacticMap.setCell(fsX, fsY, (short) NM_KNOCKED);
                            setImage(fsX, fsY);

                            // отгоняем прицел на нейтральную позицию
                            foresight.setX(u.toCoord(SIZE + 1));
                            foresight.setY(u.toCoord(SIZE + 1));
                            setForesight(foresight);

                            // убираем диалог "Waiting..."
                            if (progress != null) progress.dismiss();

                        } else if (answer != NM_UNKNOWN) { // Обработка ответа "убил"

                            Toast.makeText(AttackActivity.this, "Enemy ship destroyed!", Toast.LENGTH_SHORT).show();

                            // освобождаем кнопку выстрела
                            fireBtn.setEnabled(true);

                            // записываем в тактическую карту попадание и изображаем его
                            tacticMap.setCell(fsX, fsY, (short) NM_KNOCKED);
                            setImage(fsX, fsY);

                            // записываем в тактическую карту убитый корабль и изображаем его
                            int x = result.get(1), y = result.get(2);
                            tacticMap.setCell(x, y, (short) answer);
                            setImage(x, y);

                            // помечаем клетки вокруг корабля как NM_EMPTY
                            setEmptyCellsAround(x, y, answer);

                            // отгоняем прицел на нейтральную позицию
                            foresight.setX(u.toCoord(SIZE + 1));
                            foresight.setY(u.toCoord(SIZE + 1));
                            setForesight(foresight);

                            // убираем диалог "Waiting..."
                            if (progress != null) progress.dismiss();

                        }
                        isBusy = false;
                    } else
                        //
                        // Обработка выигрыша
                        //
                    if (gamer.getState().equals(GAMER_WIN)) {
                        // ToDo: обработка выигрыша
                    }
                }
            }

            private void setEmptyCellsAround(int X, int Y, int answer) {
                int Xmax = X, Ymax = Y;

                switch (answer) {
                    case NM_KILLED_1:
                        break;
                    case NM_KILLED_2H:
                        Xmax = X + 1;
                        break;
                    case NM_KILLED_2V:
                        Ymax = Y + 1;
                        break;
                    case NM_KILLED_3H:
                        Xmax = X + 2;
                        break;
                    case NM_KILLED_3V:
                        Ymax = Y + 2;
                        break;
                    case NM_KILLED_4H:
                        Xmax = X + 3;
                        break;
                    case NM_KILLED_4V:
                        Ymax = Y + 3;
                        break;
                }

                int i, j;
                // сверху
                i = X-1; j = Y-1;
                do {
                    tacticMap.setCell(i, j, (short) NM_EMPTY);
                    setImage(i, j);
                    i++;
                } while (i <= Xmax + 1);

                // справа
                i--;
                do {
                    tacticMap.setCell(i, j, (short) NM_EMPTY);
                    setImage(i, j);
                    j++;
                } while (j <= Ymax + 1);

                // снизу
                j--;
                do {
                    tacticMap.setCell(i, j, (short) NM_EMPTY);
                    setImage(i, j);
                    i--;
                } while (i >= X - 1);

                // слева
                i++;
                do {
                    tacticMap.setCell(i, j, (short) NM_EMPTY);
                    setImage(i, j);
                    j--;
                } while (j >= Y - 1);
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

    private void showError(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(AttackActivity.this);
        dialog.setTitle("Error");
        dialog.setMessage(message);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                AttackActivity.this.finish();
            }
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        // ToDo: Сменить состояния игроков
        super.onDestroy();
    }
}
