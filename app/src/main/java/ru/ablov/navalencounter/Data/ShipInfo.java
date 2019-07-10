package ru.ablov.navalencounter.Data;

import java.io.Serializable;
import java.util.ArrayList;

import static ru.ablov.navalencounter.Data.Constants.*;

/*
    Описание состояния корабля - координаты верхней левой клетки (в клетках), размер и расположение
 */

public class ShipInfo implements Serializable {
    private int X, Y; // Левый верхний угол
    private int size; // Размер корабля 1...4
    private boolean isVertical; // Вертикально: true; горизонтально: false
    private ArrayList<Boolean> intact = new ArrayList<>(size); // true - клетка цела; false - подбита

    public ShipInfo() {
        X = SIZE; // За полем
        Y = SIZE; // За полем
    }

    public int getX() {
        return X;
    }

    public void setX(int x) {
        X = x;
    }

    public int getY() {
        return Y;
    }

    public void setY(int y) {
        Y = y;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isVertical() {
        return isVertical;
    }

    public void setVertical(boolean vertical) {
        isVertical = vertical;
    }

    public void setHealth() { // Инициализация нового (целого) корабля
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                intact.add(i, true);
            }
        }
    }

    // Цела ли данная клетка?
    public boolean isIntact(int x, int y) {
        // Проверяем, что клетка принадлежит кораблю
        int index;

        if (isVertical) {
            index = y - Y;
        } else {
            index = x - X;
        }

        if (index < size) {
            return intact.get(index);
        }

        return false;
    }

    // Обработка попадания
    public void killCell(int x, int y) {
        int index;

        if (isVertical) {
            index = y - Y;
        } else {
            index = x - X;
        }

        if (index < size) {
            intact.set(index, false);
        }
    }

    // Жив ли корабль?
    public boolean isLive() {
        boolean live = false;

        for (int i = 0; i < size; i++) {
            live |= intact.get(i);
        }

        return live;
    }

    // Возвращает константу, описывающую тип убитого корабля
    public int getConstant() {
        switch (size) {
            case 1:
                return NM_KILLED_1;
            case 2:
                if (isVertical) return NM_KILLED_2V;
                else return NM_KILLED_2H;
            case 3:
                if (isVertical) return NM_KILLED_3V;
                else return NM_KILLED_3H;
            case 4:
                if (isVertical) return NM_KILLED_4V;
                else return NM_KILLED_4H;
        }
        return NM_UNKNOWN;
    }
}
