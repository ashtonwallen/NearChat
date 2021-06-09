package com.ashtonwilliamallen.nearchat;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gmap;
    LocationManager locationManager;
    LocationListener locationListener;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference userRef = database.getReference("userLobby");


    boolean isPeeking = false;
    final ArrayList<String> currentLocations = new ArrayList<String>();
    String username;
    boolean isLoggedIn = true;
    EditText peekEntry;
    Button peekButton;
    LatLng ny = new LatLng(40.748817, -73.985428);


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 10000, 50, locationListener);
            } else
                Toast.makeText(getApplicationContext(), "Location services must be enabled for proper functionality", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat_DayNight_DarkActionBar); // (for Custom theme)
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        final ListView currentChats = findViewById(R.id.currentChatsList);

        Intent intent = getIntent();
        peekEntry = findViewById(R.id.peekEntry);
        peekButton = findViewById(R.id.peekButton);
        peekEntry.setHintTextColor(Color.GRAY);

        peekEntry.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            peekButton.callOnClick();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });


        username = intent.getStringExtra("username");
        isLoggedIn = true;


        currentChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // When clicked, show a toast with the TextView text or do whatever you need.
                ArrayList<String> appendedList = new ArrayList<String>(currentLocations.subList(position, currentLocations.size()));
                String locTag = appendedList.toString().replaceAll("[\\[\\] , ]", "");

                Log.i("listString: ", locTag);

                Intent intent = new Intent(getApplicationContext(), basicChat.class);
                intent.putExtra("location", ((TextView) view).getText());
                intent.putExtra("username", username);
                intent.putExtra("locTag", locTag);
                intent.putExtra("isPeeking", isPeeking);

                userRef.child(username).setValue(locTag);

                startActivity(intent);
            }
        });


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (isPeeking == false) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    setByLocation(userLatLng);
                    Log.i("LOCATIONHERE", "HERE");
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 10000, 50, locationListener);
            Location startLoc = getLocation();
            LatLng userLatLng;

            try {
                userLatLng = new LatLng(startLoc.getLatitude(), startLoc.getLongitude());
                setByLocation(userLatLng);
            } catch (NullPointerException e) {
                Toast.makeText(getApplicationContext(), "Please ensure location services are enabled", Toast.LENGTH_SHORT).show();
                setByLocation(ny);
            }
        }
    }

    @Override
    protected void onResume() {
        if (!isLoggedIn) {
            Intent intent = new Intent(getApplicationContext(), loginUser.class);
            startActivity(intent);
        }

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
        if (checkIfBackground(getApplicationContext())) {
            userRef.child(username).removeValue();

            isLoggedIn = false;
        }

        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        if (checkIfBackground(getApplicationContext())) {
            userRef.child(username).removeValue();

            isLoggedIn = false;
        }

        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        userRef.child(username).removeValue();

        super.onDestroy();
        mapView.onDestroy();
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
        Location userLoc = getLocation();
        if (userLoc != null) {
            LatLng userLatLng = new LatLng(userLoc.getLatitude(), userLoc.getLongitude());
            gmap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
            gmap.animateCamera(CameraUpdateFactory.zoomTo(5), 2000, null);
        } else {
            gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
            gmap.animateCamera(CameraUpdateFactory.zoomTo(5), 2000, null);
        }


        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                // Creating a marker
                setByLocation(latLng);
                isPeeking = true;
                peekButton.setText("stop peeking");
                peekEntry.setVisibility(View.INVISIBLE);


            }
        });


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

    public void setByLocation(LatLng location) {
        if (location != null) {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
                setListView(addresses);
                gmap.clear();
                gmap.addMarker(new MarkerOptions().position(location));
                CameraPosition cameraPosition = new CameraPosition.Builder().
                        target(location).
                        tilt(60).
                        zoom(10).
                        bearing(0).
                        build();

                gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "something went wrong", Toast.LENGTH_LONG);
            }
        }
    }

    public void onPeek(View view) {
        if (isPeeking) {
            gmap.clear();
            peekEntry.setVisibility(View.VISIBLE);
            peekButton.setText("peek");
            isPeeking = false;
            Location newLoc = getLocation();
            LatLng newLatLng = new LatLng(newLoc.getLatitude(), newLoc.getLongitude());
            setByLocation(newLatLng);
        } else {
            isPeeking = true;
            peekButton.setText("stop peeking");
            String locEntry = peekEntry.getText().toString();

            if (locEntry != "" && locEntry.length() > 0) {

                try {
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocationName(locEntry, 1);
                    if (addresses != null && addresses.size() > 0) {
                        peekEntry.setVisibility(View.GONE);
                        setListView(addresses);
                        LatLng enteredLatLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                        gmap.clear();
                        gmap.addMarker(new MarkerOptions().position(enteredLatLng));
                        CameraPosition cameraPosition = new CameraPosition.Builder().
                                target(enteredLatLng).
                                tilt(60).
                                zoom(10).
                                bearing(0).
                                build();
                        gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);

                    } else {
                        isPeeking = false;
                        peekButton.setText("PEEK");
                        Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_LONG).show();
                        peekEntry.setVisibility(View.VISIBLE);

                    }



                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "something went wrong", Toast.LENGTH_LONG).show();
                    isPeeking = false;
                    peekButton.setText("peek");
                    peekEntry.setVisibility(View.VISIBLE);

                }
            }
            else {
                isPeeking = false;
                peekButton.setText("PEEK");
                Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_LONG).show();
                peekEntry.setVisibility(View.VISIBLE);

            }
        }
    }

    public void setListView(List<Address> listAddress) {
        String loc;
        final ListView currentChats = (ListView) findViewById(R.id.currentChatsList);
        currentLocations.clear();


        if (listAddress.get(0).getThoroughfare() != null) {
            //loc = listAddress.get(0).getSubThoroughfare().toString();
            String loc2 = listAddress.get(0).getThoroughfare().toString();
            currentLocations.add(loc2);
        }

        if (listAddress.get(0).getLocality() != null) {
            loc = listAddress.get(0).getLocality().toString();
            currentLocations.add(loc);
        }
        if (listAddress.get(0).getSubAdminArea() != null) {
            loc = listAddress.get(0).getSubAdminArea().toString();
            currentLocations.add(loc);
        }
        if (listAddress.get(0).getAdminArea() != null) {
            loc = listAddress.get(0).getAdminArea().toString();
            currentLocations.add(loc);
        }
        if (listAddress.get(0).getCountryName() != null) {
            loc = listAddress.get(0).getCountryName().toString();
            currentLocations.add(loc);
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, currentLocations);
        arrayAdapter.notifyDataSetChanged();
        currentChats.setAdapter(arrayAdapter);
    }


    public Location getLocation() {
        Location location = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return location;
    }
}


