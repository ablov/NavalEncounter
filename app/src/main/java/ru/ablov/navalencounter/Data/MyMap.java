package ru.ablov.navalencounter.Data;

import java.io.Serializable;

import static ru.ablov.navalencounter.Data.Constants.NM_TAKEN;
import static ru.ablov.navalencounter.Data.Constants.SIZE;

// Свои корабли, формат X * SIZE + Y
public class MyMap extends NavalMap implements Serializable{

    // Делает попытку установить корабль на карту, возвращает результат попытки
    boolean setToMap(int X, int Y, int size, boolean isVertical) {

        int Xmin, Xmax, Ymin, Ymax;
        boolean result = true;

        if (isVertical) {
            Xmax = X;
            Ymax = Y + size - 1;
        } else {
            Xmax = X + size - 1;
            Ymax = Y;
        }
        if (X < 0 || Xmax >= SIZE || Y < 0 || Ymax >= SIZE) return false;

        Xmin = X - 1;
        Xmax = X + 1;
        Ymin = Y - 1;
        Ymax = Y + 1;

        if (isVertical) {
            Ymax = Ymax - 1 + size;
        } else {
            Xmax = Xmax - 1 + size;
        }

        // Проверка, что на месте корабля и вокруг клетки пустые
        for (int x = Xmin; (x <= Xmax) && result; x++) {
            for (int y = Ymin; (y <= Ymax) && result; y++) {
                result =  result && !isCell(x, y);
            }
        }

        // Устанавливаем корабль
        if (result) {
            for (int x = Xmin + 1; x < Xmax; x++) {
                for (int y = Ymin + 1; y < Ymax; y++) {
                    setCell(x, y);
                }
            }
            return true;
        }

        return false;
    }

    // Стирает корабль с карты
    void clearFromMap(int X, int Y, int size, boolean isVertical) {

        if (X < 0 || X >= SIZE || Y < 0 || Y >= SIZE) return; // Вне поля

        int Xmax = X;
        int Ymax = Y;

        if (isVertical) {
            Ymax = Ymax + size - 1;
        } else {
            Xmax = Xmax + size - 1;
        }

        for (int x = X; x <= Xmax; x++) {
            for (int y = Y; y <= Ymax; y++) {
                clearCell(x, y);
            }
        }
    }
}
