package com.ashtonwilliamallen.nearchat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class loginUser extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap gmap;
    String username = "";
    DatabaseReference myAllRef;
    DatabaseReference userRef;
    FirebaseDatabase database;
    EditText userText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat_NoActionBar);
        setContentView(R.layout.activity_login_user);

        userText = (EditText) findViewById(R.id.userEntry);
        userText.setHintTextColor(Color.GRAY);
        final ImageButton submitButton = findViewById(R.id.submitButton);
        submitButton.setBackgroundResource(0);

        userText.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            submitButton.callOnClick();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        ImageView imgView = findViewById(R.id.logo);

        database = FirebaseDatabase.getInstance();
        myAllRef = database.getReference();

        mapView = findViewById(R.id.mapsView1);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        imgView.animate().alpha(1).setDuration(1500).setStartDelay(700);

        userText.animate().alpha(1).setDuration(1500).setStartDelay(1500);

        submitButton.animate().alpha(1).setDuration(1500).setStartDelay(2500);



    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(8);
        LatLng ny = new LatLng(40.7128, -74.0060);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
        CameraPosition cameraPosition = new CameraPosition.Builder().
                target(ny).
                tilt(75).
                zoom(17).
                bearing(0).
                build();

        gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }





    public void validateUser(View view) {

        username = userText.getText().toString();


        userRef = database.getReference("userLobby").child(username);

        if (username != "" && username.length() >= 5 && username.length() <= 25) {
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {

                        myAllRef.child("userLobby").child(username).setValue("lobby");
                        Toast.makeText(getApplicationContext(), "Logged in as: " + username, Toast.LENGTH_LONG);
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("username", username);
                        finish();
                        startActivity(intent);


                    } else {
                        Toast.makeText(getApplicationContext(), "A user is logged with that name!", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            userRef.addListenerForSingleValueEvent(eventListener);

        } else if (username.length() < 5) {
            Toast.makeText(getApplicationContext(), "Username must exceed 5 characters", Toast.LENGTH_LONG).show();
        }
        else if (username.length() > 25)
        {
            Toast.makeText(getApplicationContext(), "Username can be up to 25 characters", Toast.LENGTH_LONG).show();
        }
    }
}