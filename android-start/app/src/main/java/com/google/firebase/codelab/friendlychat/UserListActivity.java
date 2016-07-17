package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.codelab.friendlychat.model.User;
import com.google.firebase.codelab.friendlychat.viewholder.UserVIewHolder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by usr0200475 on 2016/06/30.
 */
public class UserListActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "UserListActivity";
    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private String mPhotoUrl;

    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<User, UserVIewHolder> userListAdapter;
    private RecyclerView userListRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private String myUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userlist);
        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        initFirebaseAuth();
        setUpRecyclerView();

    }

    private void initFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            startActivity(new Intent(UserListActivity.this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = firebaseUser.getPhotoUrl().toString();
            }
        }

        myUserId = firebaseUser.getDisplayName();

    }

    private void setUpRecyclerView() {
        // New child entries
        userListRecyclerView = (RecyclerView) findViewById(R.id.userListRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        userListAdapter = new FirebaseRecyclerAdapter<User,
                UserVIewHolder>(
                User.class,
                R.layout.item_user,
                UserVIewHolder.class,
                mFirebaseDatabaseReference.child(SignInActivity.USER_CHILD)) {

            @Override
            protected void populateViewHolder(UserVIewHolder viewHolder,
                                              final User user, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.userName.setText(user.getUserName());
                viewHolder.avatar
                        .setImageDrawable(ContextCompat
                                .getDrawable(UserListActivity.this,
                                        R.drawable.ic_account_circle_black_36dp));

                viewHolder.setOnItemClickInterface(new OnItemClickInterface() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (!TextUtils.equals(user.getUserName(), myUserId)) {
                            Intent intent = new Intent(UserListActivity.this, MainActivity.class);
                            intent.putExtra(Constant.userName, user.getUserName());
                            intent.putExtra(Constant.myUserName, mUsername);
                            startActivity(intent);
                        }
                    }
                });
            }
        };

        userListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = userListAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    userListRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        userListRecyclerView.setLayoutManager(mLinearLayoutManager);
        userListRecyclerView.setAdapter(userListAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                firebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}
