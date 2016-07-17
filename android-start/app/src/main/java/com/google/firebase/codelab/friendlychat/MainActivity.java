/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.codelab.friendlychat.viewholder.ImageMessageViewHolder;
import com.google.firebase.codelab.friendlychat.viewholder.MessageViewHolder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ChatActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String ANONYMOUS = "anonymous";
    public static final int REQUEST_CODE = 100;
    public static final int IMAGE_SIZE = 1000;
    private String mUsername;
    private String avatarUrl;

    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMessage, RecyclerView.ViewHolder> mFirebaseAdapter;

    private String userName;
    private String myUserName;
    private ImageView selectPic;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading");
        progressDialog.setCanceledOnTouchOutside(false);

        Intent extras = getIntent();
        if (extras != null) {
            userName = extras.getExtras().getString(Constant.userName);
            myUserName = extras.getExtras().getString(Constant.myUserName);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set default username is anonymous.
        mUsername = ANONYMOUS;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);


        initInputForm();
        initSelectPicIcon();
        initSendMessageButton();
        initFirebaseAuth();
        setUpRecyclerView();

    }

    /**
     * Get Firebase User Info
     */
    private void initFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                avatarUrl = firebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void initSelectPicIcon() {
        selectPic = (ImageView) findViewById(R.id.img_take_file);
        selectPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadClicked(v);
            }
        });
    }

    /**
     * Go to take a pic when tap icon
     *
     * @param view
     */
    public void uploadClicked(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            progressDialog.show();
            Uri uri = data.getData();
            Bitmap imageBitmap = getBitmap(uri);
            if (imageBitmap == null) {
            } else {
                uploadBitmapToStorage(imageBitmap);
            }
        }
    }

    /**
     * Upload to storage
     */

    private void uploadBitmapToStorage(Bitmap imageBitmap) {
        imageBitmap = Bitmap.createScaledBitmap(imageBitmap, IMAGE_SIZE, IMAGE_SIZE, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(firebaseUser.getUid() + "/" + "/" + System.currentTimeMillis());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
        UploadTask uploadTask = ref.putBytes(imageData, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("", "onFailure", exception);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String imageUrl = taskSnapshot.getDownloadUrl().toString();
                postMessage("", imageUrl);
            }
        });
    }

    /**
     * for post message
     *
     * @param mes
     * @param imageUrl
     */
    private void postMessage(String mes, String imageUrl) {
        FriendlyMessage friendlyMessage = new
                FriendlyMessage(
                mes,
                avatarUrl,
                userName,
                myUserName,
                imageUrl
        );

        mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(initChatRoomName(myUserName, userName)).push().setValue(friendlyMessage);
        mMessageEditText.setText("");
        mMessageRecyclerView.scrollToPosition(mFirebaseAdapter.getItemCount());
    }

    /**
     * For get bitmap from uri
     *
     * @param uri
     * @return
     */
    private Bitmap getBitmap(Uri uri) {
        try {
            if (uri != null) {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap imageBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                return imageBitmap;
            }
        } catch (Exception e) {
            Log.e("", e.toString(), e);
        }

        return null;
    }

    enum UserType {
        MY_TEXT_MESSAGE,
        MY_IMAGE_MESSAGE,
        USER_TEXT_MESSAGE,
        USER_IMAGE_MESSAGE
    }

    /**
     * Get data from firebase database and set to recycler View
     */
    private void setUpRecyclerView() {
        // New child entries
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, RecyclerView.ViewHolder>(
                FriendlyMessage.class,
                0,
                RecyclerView.ViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(initChatRoomName(myUserName, userName))) {

            @Override
            protected void populateViewHolder(RecyclerView.ViewHolder viewHolder,
                                              FriendlyMessage friendlyMessage, int position) {

                if (friendlyMessage.getPhotoUrl().isEmpty()) {
                    bindingTextViewHolder((MessageViewHolder) viewHolder, friendlyMessage);
                } else {
                    bindingImageViewHolder((ImageMessageViewHolder) viewHolder, friendlyMessage);
                }

                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                switch (UserType.values()[viewType]) {
                    case MY_TEXT_MESSAGE:
                        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.item_my_message_text, parent, false));

                    case MY_IMAGE_MESSAGE:
                        return new ImageMessageViewHolder(LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.item_my_message_image, parent, false));

                    case USER_TEXT_MESSAGE:
                        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.item_message_text_user_layout, parent, false));


                    case USER_IMAGE_MESSAGE:
                        return new ImageMessageViewHolder(LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.item_message_image_user_layout, parent, false));

                }
                return null;
            }

            @Override
            public int getItemViewType(int position) {
                FriendlyMessage friendlyMessage = getItem(position);
                if (TextUtils.equals(friendlyMessage.getFromName(), myUserName)) {
                    return friendlyMessage.getPhotoUrl().isEmpty() ?
                            UserType.MY_TEXT_MESSAGE.ordinal() : UserType.MY_IMAGE_MESSAGE.ordinal();
                } else {
                    return friendlyMessage.getPhotoUrl().isEmpty() ?
                            UserType.USER_TEXT_MESSAGE.ordinal() : UserType.MY_IMAGE_MESSAGE.ordinal();
                }
            }

        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
                progressDialog.hide();
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    private void bindingTextViewHolder(MessageViewHolder viewHolder, FriendlyMessage friendlyMessage) {

        viewHolder.messageTextView.setText(friendlyMessage.getText());

        if (friendlyMessage.getAvatarUrl() == null || friendlyMessage.getAvatarUrl().isEmpty()) {
            viewHolder.messengerImageView
                    .setImageDrawable(ContextCompat
                            .getDrawable(MainActivity.this,
                                    R.drawable.ic_account_circle_black_36dp));
        } else {
            Glide.with(MainActivity.this)
                    .load(friendlyMessage.getAvatarUrl())
                    .into(viewHolder.messengerImageView);
        }
    }

    private void bindingImageViewHolder(ImageMessageViewHolder viewHolder, FriendlyMessage friendlyMessage) {
        if (!friendlyMessage.getPhotoUrl().isEmpty()) {
            Glide.with(MainActivity.this)
                    .load(friendlyMessage.getPhotoUrl())
                    .into(viewHolder.imageView);
        }

        if (friendlyMessage.getAvatarUrl() == null || friendlyMessage.getAvatarUrl().isEmpty()) {
            viewHolder.messengerImageView
                    .setImageDrawable(ContextCompat
                            .getDrawable(MainActivity.this,
                                    R.drawable.ic_account_circle_black_36dp));
        } else {
            Glide.with(MainActivity.this)
                    .load(friendlyMessage.getAvatarUrl())
                    .into(viewHolder.messengerImageView);
        }
    }

    /**
     * Set up input form
     */
    private void initInputForm() {
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mSendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }


    /**
     * Init send message button
     */
    private void initSendMessageButton() {
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
                postMessage(mMessageEditText.getText().toString(), "");
            }
        });

    }

    /**
     * Create chat room name
     *
     * @param userName1
     * @param userName2
     * @return
     */
    private String initChatRoomName(String userName1, String userName2) {
        if (userName1.compareTo(userName2) < 0) {
            return "room" + userName1 + userName2;
        } else {
            return "room" + userName2 + userName1;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
