package ru.ablov.navalencounter.Data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import ru.ablov.navalencounter.R;
import ru.ablov.navalencounter.Utils.Units;

import static ru.ablov.navalencounter.Data.Constants.DELTA;
import static ru.ablov.navalencounter.Data.Constants.SIZE;

// Общие свойства корабля при создании карты
public class Ship {
    final private Units u;
    final private MyMap map;
    final private Context ctx;

    private ShipInfo si = new ShipInfo();
    private boolean isSet = false; // Корабль на поле?

    int width, height; // Ширина и высота корабля в единицах
    float unit; // размер единицы корабля в пикселях

    private ImageView v; // on activity_new_game.xml
    private Drawable hr, vr; // Horizontal and vertical images

    // Обработка сдвига
    private float dX, dY, lastX, lastY;
    private int lastAction;
    private float BOTTOM_BAR_HEIGHT;


    @SuppressLint("ClickableViewAccessibility")
    public Ship(View view, @NonNull final Units units, MyMap myMap, Context ctx) {
        u = units;
        v = (ImageView) view;
        map = myMap;
        this.ctx = ctx;
        BOTTOM_BAR_HEIGHT = u.getUnitSize() * 2; // Учёт нижней панели

        // Определяем размеры корабля (в клеточках) и его ориентацию
        width = v.getDrawable().getIntrinsicWidth();
        height = v.getDrawable().getIntrinsicHeight();
        unit = Math.min(width, height);
        width = Math.round(width / unit);
        height = Math.round(height / unit);
        si.setSize(Math.max(width, height));
        si.setVertical(height > width);
        si.setHealth(); // Только после установки размеров мы можем инициализировать intact-лист

        // Устанавливаем размеры корабля в точках
        v.getLayoutParams().width = width * u.getUnitSize();
        v.getLayoutParams().height = height * u.getUnitSize();
        v.requestLayout();

        // Даём стартовый фон
        v.setBackgroundColor(ctx.getResources().getColor(R.color.colorInit));

        // Инициализируем рисунок корабля
        switch (si.getSize()) {
            case 1:
                hr = ctx.getDrawable(R.drawable.one);
                vr = ctx.getDrawable(R.drawable.one);
                break;
            case 2:
                hr = ctx.getDrawable(R.drawable.twohr);
                vr = ctx.getDrawable(R.drawable.twovr);
                break;
            case 3:
                hr = ctx.getDrawable(R.drawable.threehr);
                vr = ctx.getDrawable(R.drawable.threevr);
                break;
            case 4:
                hr = ctx.getDrawable(R.drawable.fourhr);
                vr = ctx.getDrawable(R.drawable.fourvr);
                break;
            default:
                hr = vr = null;
        }

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float x, y, delta;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isSet) map.clearFromMap(si.getX(), si.getY(), si.getSize(), si.isVertical());
                        v.bringToFront();
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
                            x += dX;
                            if (x > 0 && x < (u.getDpWidth() * u.getDensity() - u.getUnitSize()))
                                v.setX(x);

                            y += dY;
                            if (y > 0 && (y < u.getDpHeight() * u.getDensity() - u.getUnitSize() - BOTTOM_BAR_HEIGHT))
                                v.setY(y);
                            lastAction = MotionEvent.ACTION_MOVE;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            v.performClick();
                            rotate();
                        } else {
                            align();
                        }
                        isSet = map.setToMap(si.getX(), si.getY(), si.getSize(), si.isVertical());
                        setColor(isSet);
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private void setColor(boolean isSet) {
        if (isSet) {
            v.setBackgroundColor(ctx.getResources().getColor(R.color.colorOk));
        } else {
            v.setBackgroundColor(ctx.getResources().getColor(R.color.colorBad));
        }
    }

    // Вращение корабля
    private void rotate() {
        if (si.isVertical()) {
            v.setImageDrawable(hr);
            si.setVertical(false);
        } else {
            v.setImageDrawable(vr);
            si.setVertical(true);
        }
        // Делаем всё наоборот - высоту меняем на ширину
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
        int width = params.width;
        int height = params.height;
        params.width = height;
        params.height = width;
        v.setLayoutParams(params);

    }

    // Выравнивает положение коробля по сетке карты
    private void align() {
        si.setX(u.toNumber(v.getX()-u.getMapX()));
        si.setY(u.toNumber(v.getY()-u.getMapY()));

        // Учтём возможность выхода за пределы поля не очень далеко
        if (si.getX() == SIZE) si.setX(SIZE-1);
        if (si.getY() == SIZE) si.setY(SIZE-1);

        if (si.getX() >= 0 && si.getX() < SIZE && si.getY() >= 0 && si.getY() < SIZE){
            v.setX(u.toCoord(si.getX()) + u.getMapX());
            v.setY(u.toCoord(si.getY()) + u.getMapY());
        }
    }

    // Проверяет, установлен ли корабль
    public boolean isShipReady() {
        return isSet;
    }

    // Сообщает положение корабля
    public ShipInfo getSI() {
        return si;
    }

    // Возвращает положение корабля
    public void setSI(ShipInfo si) {
        this.si = si;
    }
}