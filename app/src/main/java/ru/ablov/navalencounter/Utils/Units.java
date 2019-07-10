package ru.ablov.navalencounter.Utils;

/*
    Различные константы и величины, связанные с размерами экрана и поля
 */

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import static ru.ablov.navalencounter.Data.Constants.MAX_UNIT_SIZE;
import static ru.ablov.navalencounter.Data.Constants.SIZE;

public class Units {
    private float dpHeight, dpWidth; // размеры экрана в условных точках
    private int dHeight, dWidth; // размеры экрана в реальных точках
    private int belowSpace; // минимальный размер свободного места под основным игровым полем, не менее 3 юнитов
    private int mapX, mapY; // положение левого верхнего края игрового поля на экране
    private int unitSize; // размер клетки в точках
    private float density; // плотность
    private float dty; // реальная плотность
    private int gameFieldSize; // размер игрового поля в точках

    public Units(Context ctx, View gameField) {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        dty = displayMetrics.density;
        density = 1; // Не совсем понятно, нужна ли нам эта величина...
        dpHeight = displayMetrics.heightPixels / density;
        dpWidth = displayMetrics.widthPixels / density;

        dHeight = Math.round(dpHeight);
        dWidth = Math.round(dpWidth);
        int belowSpace = (int) Math.round(dWidth / (float) SIZE * 3);  // 0.3 поля оставляем для управления
        int gameSpace = dHeight - belowSpace; // максимальный размер игрового поля по высоте
        gameFieldSize = (dWidth <= gameSpace) ? dWidth : gameSpace;
        if (gameFieldSize > MAX_UNIT_SIZE * SIZE) {
            gameFieldSize = MAX_UNIT_SIZE * SIZE;
        }

        mapX = (int) Math.round((dWidth-gameFieldSize)/2.0); // Обычно просто 0, но если игровое поле чуть уже экрана, то половина разницы
        mapY = 155;

        gameField.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mapX = left;
                mapY = top;
                float fX = v.getX();
                float fY = v.getY();
                int[] xy = new int[2];
                v.getLocationOnScreen(xy);
            }
        });

/*
        float width = gameField.getLayoutParams().width / density;
        float height = gameField.getLayoutParams().height / density;


        if (dpWidth > width && dpHeight > height) {
            unitSize = Math.max(width, height) / SIZE;
        } else {
            unitSize = Math.min(dpHeight, dpWidth) / SIZE;
        }
*/
        unitSize = Math.round(gameFieldSize / (float) SIZE);
        unitSize *= density;

    }

    public float getDpHeight() {
        return dpHeight;
    }

    public float getDpWidth() {
        return dpWidth;
    }

    public int getUnitSize() {
        return unitSize;
    }

    public float getDensity() {
        return dty;
    }

    public int getGameFieldSize() {
        return gameFieldSize;
    }

    // Возвращает номер строки или столбца по координате
    // относительно левого верхнего края поля
    public short toNumber(float a) {
        return (short) Math.round(a / unitSize);
    }

    // Возвращает координату относительно левого верхнего края поля в пикселях
    // по номеру строки или столбца
    public float toCoord(int a) {
        return unitSize * a;
    }

    // Возвращает координаты левого верхнего края поля
    public int getMapX() {
        return mapX;
    }

    public int getMapY() {
        return mapY;
    }
}
