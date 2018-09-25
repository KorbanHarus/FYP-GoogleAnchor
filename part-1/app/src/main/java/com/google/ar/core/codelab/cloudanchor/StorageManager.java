package com.google.ar.core.codelab.cloudanchor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;


import com.google.firebase.database.DatabaseReference;

/** Helper class for managing on-device storage of cloud anchor IDs. */
public class StorageManager {

    interface CloudAnchorIdListener {
        void onCloudAnchorIdAvailable(String cloudAnchorId);
    }

    interface ShortCodeListener {
        void onShortCodeAvailable(Integer shortCode);
    }

    private static final String TAG = StorageManager.class.getName();
    private static final String KEY_ROOT_DIR = "shared_anchor_codelab_root";
    private static final String KEY_NEXT_SHORT_CODE = "next_short_code";
    private static final String NEXT_SHORT_CODE = "next_short_code";
    private static final String KEY_PREFIX = "anchor";
    private static final int INITIAL_SHORT_CODE = 142;
    private final DatabaseReference rootRef;

    StorageManager(Context context){
        FirebaseApp firebaseApp = FirebaseApp.initializeApp(context);
        rootRef = FirebaseDatabase.getInstance(firebaseApp).getReference().child(KEY_ROOT_DIR);
        DatabaseReference.goOnline();
    }

    /** Gets a new short code that can be used to store the anchor ID. */
    void nextShortCode(ShortCodeListener listener){
      rootRef
              .child(KEY_NEXT_SHORT_CODE)
              .runTransaction(
                      new Transaction.Handler() {
                          @NonNull
                          @Override
                          public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                              Integer shortCode = currentData.getValue(Integer.class);
                              if (shortCode == null) {
                                  shortCode = INITIAL_SHORT_CODE - 1;
                              }
                              currentData.setValue(shortCode + 1);
                              return Transaction.success(currentData);
                          }

                          @Override
                          public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                              if (!committed) {
                                  Log.e(TAG, "Firebase Error", error.toException());
                                  listener.onShortCodeAvailable(currentData.getValue(Integer.class));
                              }
                          }
                      });
    }

    /** Stores the cloud anchor ID in the activity's SharedPreferences. */
    void storeUsingShortCode(int shortCode, String cloudAnchorId){
      rootRef.child(KEY_PREFIX + shortCode).setValue(cloudAnchorId);
    }

  void getCloudAncorID(int shortCode, CloudAnchorIdListener listener){
       rootRef
               .child(KEY_PREFIX + shortCode)
               .addListenerForSingleValueEvent(
                       new ValueEventListener() {
                           @Override
                           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                               listener.onCloudAnchorIdAvailable(String.valueOf(dataSnapshot.getValue()));
                           }

                           @Override
                           public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "The database operation for getCloudAnchorID was cancelled.",
                                        error.toException());
                                listener.onCloudAnchorIdAvailable(null);
                           }
                       }
               );
    }
}
