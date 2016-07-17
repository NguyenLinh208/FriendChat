package com.google.firebase.codelab.friendlychat.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.codelab.friendlychat.R;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by usr0200475 on 2016/06/30.
 */
public class ImageMessageViewHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;
    public CircleImageView messengerImageView;

    public ImageMessageViewHolder(View v) {
        super(v);
        messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        imageView = (ImageView) itemView.findViewById(R.id.chat_image);
    }
}