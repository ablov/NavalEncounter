package ru.ablov.navalencounter.Data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Gamer {
    private String uid="";
    private String nickname="";
    private String state ="";
    private long timestamp;
    private String partner;
    private ArrayList<Integer> request = new ArrayList<>(2); // Координаты выстрела
    private ArrayList<Integer> answer = new ArrayList<>(3); // [0] - код результата (см. Constants.java NM_*), [1] и [2] - доп. параметры

    public Gamer() {
        this.timestamp = System.currentTimeMillis();
    }

    public Gamer(@NonNull String uid, String nickname, String state) {
        this.uid = uid;
        this.nickname = nickname;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
        this.partner = null;
        this.request = null;
        this.answer = null;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void resetTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public ArrayList<Integer> getRequest() {
        return request;
    }

    public void setRequest(ArrayList<Integer> request) {
        this.request = request;
    }

    public void clearRequest() {
        this.request = null;
    }

    public ArrayList<Integer> getAnswer() {
        return answer;
    }

    public void setAnswer(ArrayList<Integer> answer) {
        this.answer = answer;
    }

    public void clearAnswer() {
        this.answer = null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Gamer) {
            Gamer g = (Gamer) obj;
            return (this.uid.equals(g.uid)) && (this.nickname.equals(g.nickname)) && (this.state.equals(g.state));
        }
        return false;
    }

}
