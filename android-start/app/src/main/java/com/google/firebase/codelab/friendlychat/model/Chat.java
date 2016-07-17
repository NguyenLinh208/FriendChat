package com.google.firebase.codelab.friendlychat.model;

/**
 * Created by usr0200475 on 2016/06/30.
 */
public class Chat {
    private int count;
    private FriendlyMessage friendlyMessage;

    public Chat(int count, FriendlyMessage friendlyMessage) {
        this.count = count;
        this.friendlyMessage = friendlyMessage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public FriendlyMessage getFriendlyMessage() {
        return friendlyMessage;
    }

    public void setFriendlyMessage(FriendlyMessage friendlyMessage) {
        this.friendlyMessage = friendlyMessage;
    }
}
