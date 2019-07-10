package ru.ablov.navalencounter.Data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import ru.ablov.navalencounter.GamerListActivity;
import ru.ablov.navalencounter.R;

import static ru.ablov.navalencounter.Data.Constants.GAMERS;
import static ru.ablov.navalencounter.Data.Constants.GAMER_ATTACKING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_ESCAPED;
import static ru.ablov.navalencounter.Data.Constants.GAMER_IN_GAME_ATTACKING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_IN_GAME_DEFENDING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_PREPARING;
import static ru.ablov.navalencounter.Data.Constants.GAMER_READY;
import static ru.ablov.navalencounter.Data.Constants.GAMER_UNDER_ATTACK;

public class GamerAdapter extends ArrayAdapter<Gamer> {

    private String mUserID;
    private Context ctx;
    private static AlertDialog dialog = null;

    public GamerAdapter(@NonNull Context context, String userID, @NonNull List<Gamer> gamers) {
        super(context, 0, gamers);
        mUserID = userID;
        ctx = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Если такой игрок не зарегистрирован в списке - добавляем его
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.gamer, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.nicknameTV = convertView.findViewById(R.id.gr_nickname);
            holder.stateTV = convertView.findViewById(R.id.gr_state);
            holder.activityBtn = convertView.findViewById(R.id.gr_activity_btn);

            convertView.setTag(holder);
        }

        // Теперь настраиваем конкретного игрока
        final Gamer gamer = getItem(position); // ToDo: Не синхронизирован в момент "Under Attack!" = показывает "Ready!"
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);;

        if (gamer != null) {

            holder.nicknameTV.setText(gamer.getNickname());
            String gState = gamer.getState();
            holder.stateTV.setText(gState);

            if (!(gState.equals(GAMER_READY) || gState.equals(GAMER_ESCAPED))) {
                holder.activityBtn.setVisibility(View.INVISIBLE);
            }
            else { // Игрок готов или сбежал - можно нападать
                holder.activityBtn.setVisibility(View.VISIBLE);
                holder.activityBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //
                        // Нападение
                        //
                        final DatabaseReference refGamers = FirebaseDatabase.getInstance().getReference().child(GAMERS);

                        // Обновляем своё состояние
                        refGamers.child(mUserID).child("state").setValue(GAMER_ATTACKING);
                        refGamers.child(mUserID).child("partner").setValue(gamer.getUid());

                        // Обновляем информацию того, на кого напали
                        final String pState = gamer.getState();
                        gamer.setState(GAMER_UNDER_ATTACK);
                        gamer.setPartner(mUserID);
                        refGamers.child(gamer.getUid()).setValue(gamer);

                        // Блокируем доступ к списку
                        builder.setTitle("Waiting...");
                        builder.setMessage("Waiting for gamer response.");
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Если нажат escape - возвращаем всё в исходное состояние и закрываем предупреждение
                                refGamers.child(mUserID).child("state").setValue(GAMER_READY);
                                gamer.setState(pState);
                                gamer.setPartner(null);
                                refGamers.child(gamer.getUid()).setValue(gamer);
                                dialog.dismiss();
                            }
                        });
                        dialog = builder.create();
                        dialog.show();

                        // Ждём ответ
                        refGamers.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                            }

                            @Override
                            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                // Закрываем диалог ожидания если противник принял бой или сбежал
                                Gamer g = dataSnapshot.getValue(Gamer.class);
                                if (g != null && g.getUid().equals(gamer.getUid()) &&
                                        (g.getState().equals(GAMER_ESCAPED) || g.getState().equals(GAMER_IN_GAME_DEFENDING)
                                        || g.getState().equals(GAMER_IN_GAME_ATTACKING))) {
                                    dialog.dismiss();
                                }
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
                });
            }
        }

        return convertView;
    }

    private class ViewHolder {
        TextView nicknameTV;
        TextView stateTV;
        Button activityBtn;
    }
}
