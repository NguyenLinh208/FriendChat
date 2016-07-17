package com.google.firebase.codelab.friendlychat.viewholder;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.google.firebase.codelab.friendlychat.OnItemClickInterface;
import com.google.firebase.codelab.friendlychat.R;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by usr0200475 on 2016/06/30.
 */
public class UserVIewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView userName;
    public CircleImageView avatar;
    public AppCompatActivity appCompatActivity;

    public OnItemClickInterface onItemClickInterface;

    public UserVIewHolder(View v) {
        super(v);
        userName = (TextView) itemView.findViewById(R.id.userName);
        avatar = (CircleImageView) itemView.findViewById(R.id.avatar);
        v.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        onItemClickInterface.onItemClick(v, getPosition());
    }

    public void setOnItemClickInterface(OnItemClickInterface onItemClickInterface) {
        this.onItemClickInterface = onItemClickInterface;
    }


}
