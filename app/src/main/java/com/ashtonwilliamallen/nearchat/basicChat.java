package com.ashtonwilliamallen.nearchat;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class basicChat extends AppCompatActivity {


    String userLocation = "";
    FirebaseDatabase database;
    DatabaseReference userRef;
    DatabaseReference allRef;
    DatabaseReference numUserRef;
    DatabaseReference locRef;
    String username;
    String locTag;
    boolean isLoggedIn = true;
    EditText text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat_DayNight_DarkActionBar);
        setContentView(R.layout.activity_basic_chat);

        Intent intent = getIntent();

        final String location = intent.getStringExtra("location");
        username = intent.getStringExtra("username");
        locTag = intent.getStringExtra("locTag");
        boolean isPeeking = intent.getExtras().getBoolean("isPeeking");

        database = FirebaseDatabase.getInstance();
        locRef = database.getReference("allMessages").child(locTag);
        allRef = database.getReference("allMessages");
        userRef = database.getReference("userLobby");
        numUserRef = locRef.child("activeUsers");

        text = (EditText) findViewById(R.id.chatEntry);
        text.setHorizontallyScrolling(false);
        text.setImeOptions(EditorInfo.IME_ACTION_DONE);
        text.setRawInputType(InputType.TYPE_CLASS_TEXT);

        text.setHintTextColor(Color.GRAY);

        final ImageButton button = findViewById(R.id.messageButton);

        text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    /* Write your logic here that will be executed when user taps next button */
                    button.callOnClick();
                }

                return handled;
            }
        });

        button.setBackgroundResource(0);
        EditText e = findViewById(R.id.chatEntry);
        TextView peekText = findViewById(R.id.peekmode);

        button.setVisibility(View.VISIBLE);
        e.setVisibility(View.VISIBLE);

        if (isPeeking) {
            peekText.setAlpha(.4f);
            button.setVisibility(View.GONE);
            e.setVisibility(View.GONE);
        } else {
            numUserRef.child(username).setValue("active");
            peekText.setAlpha(0);
        }


        setTitle(location + " chat");
        userLocation = location;

        final ListView messageBox = (ListView) findViewById(R.id.messageBox);
        messageBox.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        messageBox.setStackFromBottom(true);

        final ArrayList<String> messages = new ArrayList<String>();
        final ArrayList<String> keys = new ArrayList<String>();


        locRef.addValueEventListener(new ValueEventListener() {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(basicChat.this, android.R.layout.simple_list_item_1, messages);


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                String datetime;
                String username;
                String messageContent;
                if (dataSnapshot.exists()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (!keys.toString().contains(ds.getKey())) {
                            datetime = ds.child("datetime").getValue(String.class);
                            username = ds.child("username").getValue(String.class);
                            messageContent = ds.child("message").getValue(String.class);

                            if (username != null && messageContent != null) {
                                String completeMessage = username + ": " + messageContent;
                                messages.add(completeMessage);
                                keys.add(ds.getKey());

                                arrayAdapter.notifyDataSetChanged();
                                messageBox.setAdapter(arrayAdapter);
                            }
                        }
                    }
                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("onCancelled called :", "Failed to read value.", error.toException());
            }
        });

        numUserRef.addValueEventListener(new ValueEventListener() {
            TextView userCount = findViewById(R.id.userCount);

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int i = 0;

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        i++;
                    }
                    if (i == 0)
                        userCount.setText("no active users");
                    else if (i == 1) {
                        userCount.setText(String.valueOf(i) + " user here now");
                    } else
                        userCount.setText(String.valueOf(i) + " users here now");
                } else
                    userCount.setText("no active users");


            }


            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("onCancelled called :", "Failed to read value.", error.toException());
            }
        });

    }

    @Override
    protected void onResume() {
        if (!isLoggedIn) {
            Intent intent = new Intent(getApplicationContext(), loginUser.class);
            startActivity(intent);
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (checkIfBackground(getApplicationContext())) {

            locRef.child("activeUsers").child(username).removeValue();

            isLoggedIn = false;
        } else {
            userRef.child(username).setValue("lobby");
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (checkIfBackground(getApplicationContext())) {
            userRef.child(username).removeValue();

            isLoggedIn = false;
        }

        super.onStop();
    }


    @Override
    protected void onDestroy() {
        locRef.child("activeUsers").child(username).removeValue();

        super.onDestroy();
    }

    public static boolean checkIfBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }


    public void sendMessage(View view) {
        String message = text.getText().toString();


        if (message != "" && message != null && message.length() > 0 && message.length() < 500) {
            Long key = System.currentTimeMillis() / 1000;
            String ts = key.toString();
            Date datetime = Calendar.getInstance().getTime();
            String date = datetime.toString();
            Message messageObj = new Message(username, message, date, locTag);

            locRef.child(ts).setValue(messageObj);
            text.setText("");
        } else if (message.length() < 1)
            Toast.makeText(getApplicationContext(), "Messages must not be empty", Toast.LENGTH_LONG).show();
        else if (message.length() > 500) {
            Toast.makeText(getApplicationContext(), "Messages must be less than 500 characters", Toast.LENGTH_LONG).show();
        }
    }
}




