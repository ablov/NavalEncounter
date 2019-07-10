package ru.ablov.navalencounter.Data;

import java.io.Serializable;

import static ru.ablov.navalencounter.Data.Constants.NM_EMPTY;
import static ru.ablov.navalencounter.Data.Constants.NM_TAKEN;
import static ru.ablov.navalencounter.Data.Constants.NM_UNKNOWN;
import static ru.ablov.navalencounter.Data.Constants.SIZE;

/*
    "Стратегическая карта"
    - При расстановке кораблей:
        NM_EMPTY и NM_TAKEN
    - При обороне:
        NM_EMPTY, NM_TAKEN, NM_KNOCKED или индекс корабля в списке + 1
    - При нападении:
        NM_UNKNOWN, NM_EMPTY или разные варианты попаданий из NM_* (все меньше нуля)
 */
public class NavalMap implements Serializable {
    private short[] rawData = new short[SIZE * SIZE];

    public NavalMap() {
        for (int i = 0; i < SIZE * SIZE; i++) {
            rawData[i] = NM_UNKNOWN;
        }
    }

    public NavalMap(short initState) {
        for (int i = 0; i < SIZE * SIZE; i++) {
            rawData[i] = initState;
        }
    }

    public NavalMap(short[] rd) {
        System.arraycopy(rd, 0, rawData, 0, rawData.length);
    }

    public void setCell(int X, int Y) {
        int i = X * 10 + Y;
        if (i >= 0 && i < (SIZE * SIZE)) rawData[i] = (short)NM_TAKEN;
    }

    // Сохраняем состояние клетки (по-разному в разных ситуациях, см описание класса
    public void setCell(int X, int Y, short state) {
        if (X < 0 || X >= SIZE || Y < 0 || Y >= SIZE) return;

        int i = X * 10 + Y;
        if (i >= 0 && i < (SIZE * SIZE)) rawData[i] = state;
    }

    public void clearCell(int X, int Y) {
        if (X < 0 || X >= SIZE || Y < 0 || Y >= SIZE) return;

        int i = X * 10 + Y;
        if (i >= 0 && i < (SIZE * SIZE)) rawData[i] = (short)NM_EMPTY;
    }

    public boolean isCell(int X, int Y) {
        if (X < 0 || X >= SIZE || Y < 0 || Y >= SIZE) return false;
        int i = X * 10 + Y;
        if (i >= 0 && i < (SIZE * SIZE))
            return rawData[i] == NM_TAKEN;
        else
            return false;
    }

    public short getCell(int X, int Y) {
        if (X < 0 || X >= SIZE || Y < 0 || Y >= SIZE) return -1;
        int i = X * 10 + Y;
        if (i >= 0 && i < (SIZE * SIZE))
            return rawData[i];
        else
            return -1;
    }

}
