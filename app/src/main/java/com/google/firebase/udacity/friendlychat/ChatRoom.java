package com.google.firebase.udacity.friendlychat;
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.EXTRA_LOCAL_ONLY;
import static com.google.firebase.udacity.friendlychat.MainActivity.DEFAULT_MSG_LENGTH_LIMIT;
import static com.google.firebase.udacity.friendlychat.MainActivity.FRIENDLY_MSG_LENGTH_KEY;

public class ChatRoom extends AppCompatActivity {

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private static final int RC_PHOTO_PICKER = 2;
    private String userName;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotoStorageReference;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        attachDatabaseReadListener();
        userName = "anon";
//        userName = (String) savedInstanceState.getSerializable(EXTRA_LOCAL_ONLY);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mChatPhotoStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("ChatA");

        setContentView(R.layout.chat_layout);

        mMessageListView = (ListView) findViewById(R.id.messageListView);

        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.chat_layout, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mMessageEditText.setFilters(new InputFilter[]
                {
                        new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)
                });


        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), userName, null);

//                mMessagesDatabaseReferencea.push().setValue(friendlyMessage);
//                mMessagesDatabaseReferenceb.push().setValue(friendlyMessage);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        attachDatabaseReadListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            // Get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = mChatPhotoStorageReference.child(selectedImageUri.getLastPathSegment());
            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener
                    (this,
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // When the image has successfully uploaded, we get its download URL
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                    // Set the download URL to the message box, so that the user can send it to the database
                                    FriendlyMessage friendlyMessage =
                                            new FriendlyMessage(null, userName, downloadUrl.toString());
                                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
                                }
                            }
                    );

        }
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);

//                    ChatRoom chat = dataSnapshot.getValue(ChatRoom.class);
//                    mChatAdapter.add(chat);
                    mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/jpeg");
                            intent.putExtra(EXTRA_LOCAL_ONLY, true);
                            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                        }
                    });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
//            mChatRoomDatabaseReference.addChildEventListener(mChildEventListener);
//            mMessagesDatabaseReferencea.addChildEventListener(mChildEventListener);
//            mMessagesDatabaseReferenceb.addChildEventListener(mChildEventListener);

        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
//            mChatRoomDatabaseReference.removeEventListener(mChildEventListener);

            mChildEventListener = null;
//
//            mChatRoomDatabaseReference.removeEventListener(mChildEventListener);
//            mChildEventListener = null;
        }
    }


}
