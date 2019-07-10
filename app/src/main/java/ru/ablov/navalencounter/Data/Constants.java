package ru.ablov.navalencounter.Data;

public class Constants {
    public static final int SIZE = 10; // Размеры поля
    public static final float DELTA = 10; // чуствительность к сдвигу
    public static final long TIMEOUT = 900000; // таймаут для игрока (15 мин)
    public static final int GAMEFIELD_SIZE = 592; // размер стороны gamefield.png
    public static final int MAX_UNIT_SIZE = 110; // максимальный размер клетки в точках

    // Состояния игроков
    public static final String GAMER_PREPARING = "Preparing...";
    public static final String GAMER_READY = "Ready!";
    public static final String GAMER_UNDER_ATTACK = "Under attack!";
    public static final String GAMER_ATTACKING = "Attacking";
    public static final String GAMER_ESCAPED = "Escaped";
    public static final String GAMER_IN_GAME_ATTACKING = "In game: attacking";
    public static final String GAMER_IN_GAME_DEFENDING = "In game: defending";
    public static final String GAMER_WIN = "Winner!";
    public static final String GAMER_LOST = "Loser :(";

    // Названия веток базы данных
    public static final String GAMERS = "Gamers";

    // Передача данных между активностями
    public static final String INTENT_TACTIC_MAP = "TacticMap";

    // NavalMap - расшифровка полей
    // Для убитых кораблей - левый верхний угол
    public static final int NM_UNKNOWN = -99; // нет или неизвестно
    public static final int NM_EMPTY = 0; // пусто
    public static final int NM_TAKEN = -98; // занято кораблём или противник промазал
    public static final int NM_KNOCKED = -2; // попал
    public static final int NM_KILLED_1 = -3; // одноклеточный
    public static final int NM_KILLED_2H = -4; // двухклеточный горизонтальный
    public static final int NM_KILLED_2V = -5; // и т.д.
    public static final int NM_KILLED_3H = -6;
    public static final int NM_KILLED_3V = -7;
    public static final int NM_KILLED_4H = -8;
    public static final int NM_KILLED_4V = -9;
}
