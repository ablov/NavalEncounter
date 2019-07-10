package ru.ablov.navalencounter.Data;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static ru.ablov.navalencounter.Application.GlobalApplication.getAppContext;

// Управляет хранением локальных данных во время игры
public class LocalData {
    private static final String TACTIC_MAP_FILE_NAME = "fMyTMap";
    private static final String MAP_FILE_NAME = "fMyMap";
    private static final String SHIPS_FILE_NAME = "fShips";
    private static final String SCORE_FILE_NAME = "fScore"; // Количество "живых" клеток (в начале игры - 20)

    // Сохраняем список информации о кораблях
    public boolean storeShipList(ArrayList<ShipInfo> siList) {
        FileOutputStream outputStream;
        ObjectOutputStream oos;

        try {
            if (getFile(SHIPS_FILE_NAME) != null) getFile(SHIPS_FILE_NAME).delete();
            outputStream = getAppContext().openFileOutput(SHIPS_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(siList);
            oos.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Сохраняем все корабли
    public boolean storeShips(ArrayList<Ship> ships) {
        FileOutputStream outputStream;
        ObjectOutputStream oos;
        ArrayList<ShipInfo> siList = new ArrayList<>();

        try {
            if (getFile(SHIPS_FILE_NAME) != null) getFile(SHIPS_FILE_NAME).delete();
            outputStream = getAppContext().openFileOutput(SHIPS_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(outputStream);
            for (Ship ship : ships) {
                siList.add(ship.getSI());
            }
            oos.writeObject(siList);
            oos.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Сохраняем тактическую карту (нападение)
    public boolean storeTacticMap(NavalMap map) {
        FileOutputStream outputStream;
        ObjectOutputStream oos;

        try {
            if (getFile(TACTIC_MAP_FILE_NAME) != null) getFile(TACTIC_MAP_FILE_NAME).delete();
            outputStream = getAppContext().openFileOutput(TACTIC_MAP_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(map);
            oos.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Сохраняем количество живых клеток
    public boolean storeScore(int N) {
        FileOutputStream outputStream;
        ObjectOutputStream oos;

        try {
            if (getFile(SCORE_FILE_NAME) != null) getFile(SCORE_FILE_NAME).delete();
            outputStream = getAppContext().openFileOutput(SCORE_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(outputStream);
            oos.writeInt(N);
            oos.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Конструируем карту для DefenceActivity
    // на месте корабля ставим его индекс в ShipInfoList + 1, на месте попаданий NM_KNOCKED,
    // остальное забиваем NM_EMPTY
    public NavalMap prepareMap() {
        NavalMap m = new NavalMap();
        ArrayList<ShipInfo> shipInfos;
        int X, Y;

        try {
            FileInputStream inputStream = getAppContext().openFileInput(SHIPS_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            shipInfos = (ArrayList<ShipInfo>) ois.readObject();
            ois.close();
            inputStream.close();

            for (short i = 0; i < shipInfos.size(); i++) {
                ShipInfo si = shipInfos.get(i);
                X = si.getX();
                Y = si.getY();
                if (si.isVertical()) {
                    for (int Yi = Y; Yi < Y + si.getSize(); Yi++) {
                        if (si.isIntact(X, Yi)) {
                            m.setCell(X, Yi, (short)(i+1));
                        } else {
                            m.setCell(X, Yi, (short) -1);
                        }
                    }
                } else {
                    for (int Xi = X; Xi < X + si.getSize(); Xi++) {
                        if (si.isIntact(Xi, Y)) {
                            m.setCell(Xi, Y, (short)(i+1));
                        } else {
                            m.setCell(Xi, Y, (short) -1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return m;
    }
    // Сохраняем карту (оборона)
    public boolean storeMap(NavalMap map) {
        FileOutputStream outputStream;
        ObjectOutputStream oos;

        try {
            if (getFile(MAP_FILE_NAME) != null) getFile(MAP_FILE_NAME).delete();
            outputStream = getAppContext().openFileOutput(MAP_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(outputStream);
            oos.writeObject(map);
            oos.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Считываем список кораблей
    public ArrayList<ShipInfo> getShips() {
        Context ctx = getAppContext();
        if (getFile(SHIPS_FILE_NAME) == null) return null;
        try {
            FileInputStream inputStream = ctx.openFileInput(SHIPS_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            ArrayList<ShipInfo> s = (ArrayList<ShipInfo>) ois.readObject();
            ois.close();
            inputStream.close();
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Считываем тактическую карту
    public NavalMap getTacticMap() {
        Context ctx = getAppContext();
        if (getFile(TACTIC_MAP_FILE_NAME) == null) return null;
        try {
            FileInputStream inputStream = ctx.openFileInput(TACTIC_MAP_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            NavalMap map = (NavalMap) ois.readObject();
            ois.close();
            inputStream.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NavalMap();
    }

    // Считываем карту обороны
    public NavalMap getMap() {
        if (getFile(MAP_FILE_NAME) == null) return prepareMap();
        try {
            FileInputStream inputStream = getAppContext().openFileInput(MAP_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            NavalMap map = (NavalMap) ois.readObject();
            ois.close();
            inputStream.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prepareMap();
    }

    // Считываем количество "живых клеток"
    public int getScore() {
        try {
            FileInputStream inputStream = getAppContext().openFileInput(SCORE_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            int N = ois.readInt();
            ois.close();
            inputStream.close();
            return N;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private File getFile(String fileName) {
        Context ctx = getAppContext();
        File file = ctx.getFileStreamPath(fileName);
        if (file == null || !file.exists()) {
            return null;
        }
        return file;
    }

    public void clearData() {
        File file;

        if ((file = getFile(MAP_FILE_NAME)) != null) {
            file.delete();
        }
        if ((file = getFile(TACTIC_MAP_FILE_NAME)) != null) {
            file.delete();
        }
        if ((file = getFile(SHIPS_FILE_NAME)) != null) {
            file.delete();
        }
        if ((file = getFile(SCORE_FILE_NAME)) != null) {
            file.delete();
        }
    }
}


