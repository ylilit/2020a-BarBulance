package android.technion.com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sucho.placepicker.AddressData;
import com.sucho.placepicker.Constants;
import com.sucho.placepicker.MapType;
import com.sucho.placepicker.PlacePicker;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddFosterActivity extends AppCompatActivity {
    private TextInputEditText addFosterNameText;
    private TextInputEditText addFosterPhoneText;
    private TextInputEditText addFosterLocationText;
    private TextInputEditText addFosterLocationCity;
    private TextInputEditText addFosterFromDateText;
    private TextInputEditText addFosterFromTimeText;
    private TextInputEditText addFosterUntilDateText;
    private TextInputEditText addFosterUntilTimeText;
    private Toolbar toolbar;
    private Foster foster;
    private Event event;
    private TimePickerDialog timePicker;
    private DatePickerDialog datePicker;
    private Location userLastKnownLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private int locationRequestCode = 1000;
    private double latitude=32.776437;
    private double longitude=35.022515;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_foster);

        Database db = new Database();
        foster = (Foster) getIntent().getSerializableExtra("foster");
        event = (Event) getIntent().getSerializableExtra("event");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(foster != null){
            toolbar.setTitle("Edit Foster");
        } else {
            toolbar.setTitle("Add Foster");
        }
        toolbar.inflateMenu(R.menu.send_menu);

        toolbar.setNavigationIcon(R.drawable.back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onSendFoster(item);
            }
        });
        // check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    locationRequestCode);
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            userLastKnownLocation = location;
                        }else {
                            locationRequest = LocationRequest.create();
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationRequest.setInterval(20 * 1000);
                            locationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                    if (locationResult == null) {
                                        return;
                                    }
                                    for (Location location : locationResult.getLocations()) {
                                        if (location != null) {
                                            userLastKnownLocation=location;
                                        }
                                    }
                                }
                            };
                        }
                    }
                });


        addFosterNameText = findViewById(R.id.addFosterNameText);
        addFosterPhoneText = findViewById(R.id.addFosterPhoneText);
        addFosterLocationText = findViewById(R.id.addFosterLocationText);
        addFosterLocationCity = new TextInputEditText(AddFosterActivity.this);
        addFosterFromDateText = findViewById(R.id.addFosterFromDateText);
        addFosterFromTimeText = findViewById(R.id.addFosterFromTimeText);
        addFosterUntilDateText = findViewById(R.id.addFosterUntilDateText);
        addFosterUntilTimeText = findViewById(R.id.addFosterUntilTimeText);

        addFosterFromTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minutes = calendar.get(Calendar.MINUTE);
                // time picker dialog
                timePicker = new TimePickerDialog(AddFosterActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker tp, int sHour, int sMinute) {
                                String str;
                                if(sHour < 10 && sMinute < 10) {
                                    str = "0" + sHour + ":0" + sMinute;
                                } else if(sHour < 10 && sMinute >= 10){
                                    str = "0" + sHour + ":" + sMinute;
                                } else if(sHour >= 10 && sMinute < 10){
                                    str = sHour + ":0" + sMinute;
                                } else {
                                    str = sHour + ":" + sMinute;
                                }
                                addFosterFromTimeText.setText(str);
                            }
                        }, hour, minutes, true);
                timePicker.show();
            }
        });

        addFosterFromDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                // date picker dialog
                datePicker = new DatePickerDialog(AddFosterActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                addFosterFromDateText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                            }
                        }, year, month, day);
                datePicker.show();
            }
        });

        addFosterUntilTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minutes = calendar.get(Calendar.MINUTE);
                // time picker dialog
                timePicker = new TimePickerDialog(AddFosterActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker tp, int sHour, int sMinute) {
                                String str;
                                if(sHour < 10 && sMinute < 10) {
                                    str = "0" + sHour + ":0" + sMinute;
                                } else if(sHour < 10 && sMinute >= 10){
                                    str = "0" + sHour + ":" + sMinute;
                                } else if(sHour >= 10 && sMinute < 10){
                                    str = sHour + ":0" + sMinute;
                                } else {
                                    str = sHour + ":" + sMinute;
                                }
                                addFosterUntilTimeText.setText(str);
                            }
                        }, hour, minutes, true);
                timePicker.show();
            }
        });

        addFosterUntilDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                // date picker dialog
                datePicker = new DatePickerDialog(AddFosterActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                addFosterUntilDateText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                            }
                        }, year, month, day);
                datePicker.show();
            }
        });

        addFosterLocationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userLastKnownLocation!=null) {
                    latitude=userLastKnownLocation.getLatitude();
                    longitude=userLastKnownLocation.getLongitude();
                }
                Intent intent = new PlacePicker.IntentBuilder()
                        .setLatLong(latitude, longitude)  // Initial Latitude and Longitude the Map will load into
                        .showLatLong(true)  // Show Coordinates in the Activity
                        .setMapZoom(18.0f)  // Map Zoom Level. Default: 14.0
                        .setAddressRequired(true) // Set If return only Coordinates if cannot fetch Address for the coordinates. Default: True
                        .hideMarkerShadow(true) // Hides the shadow under the map marker. Default: False
                        .setMarkerDrawable(R.drawable.map_marker) // Change the default Marker Image
                        .setMarkerImageImageColor(R.color.colorPrimary)
                        .setFabColor(R.color.colorAccent)
                        .setPrimaryTextColor(R.color.colorPrimaryText) // Change text color of Shortened Address
                        .setSecondaryTextColor(R.color.colorSecondaryText) // Change text color of full Address
                        .setMapRawResourceStyle(R.raw.style_json)  //Set Map Style (https://mapstyle.withgoogle.com/)
                        .setMapType(MapType.NORMAL)
                        .build(AddFosterActivity.this);

                startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST);
            }
        });

        // in case of EDITING an existing foster
        if(foster != null){
            addFosterNameText.setText(foster.getFosterFullName());
            addFosterPhoneText.setText(foster.getFosterPhoneNumber());
            addFosterLocationText.setText(foster.getLocation());
            addFosterLocationCity.setText(foster.getLocationCity());
            addFosterFromDateText.setText(foster.getFromDate());
            addFosterFromTimeText.setText(foster.getFromTime());
            addFosterUntilDateText.setText(foster.getUntilDate());
            addFosterUntilTimeText.setText(foster.getUntilTime());
        } else {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser != null){
                db.getUserToTextViews(currentUser.getUid(), addFosterNameText, new TextView(AddFosterActivity.this), addFosterPhoneText);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                AddressData addressData = data.getParcelableExtra(Constants.ADDRESS_INTENT);
                if(!addressData.getAddressList().isEmpty()) {
                    addFosterLocationText.setText(addressData.getAddressList().get(0).getAddressLine(0));
                    addFosterLocationCity.setText(addressData.getAddressList().get(0).getLocality());
                }
            }
        }
    }

    public boolean onSendFoster(MenuItem item) {
        String name = addFosterNameText.getText().toString();
        String phone = addFosterPhoneText.getText().toString();
        String location = addFosterLocationText.getText().toString();
        String city = addFosterLocationCity.getText().toString();
        String fromDate = addFosterFromDateText.getText().toString();
        String fromTime = addFosterFromTimeText.getText().toString();
        String untilDate = addFosterUntilDateText.getText().toString();
        String untilTime = addFosterUntilTimeText.getText().toString();

        Toast toast;
        if(name.isEmpty() || phone.isEmpty() || location.isEmpty() || fromDate.isEmpty() ||
                fromTime.isEmpty() || untilDate.isEmpty() || untilTime.isEmpty()){
            if(name.isEmpty()){
                addFosterNameText.setError("Required");
            }
            if(phone.isEmpty()) {
                addFosterPhoneText.setError("Required");
            }
            if(location.isEmpty()) {
                addFosterLocationText.setError("Required");
            }
            if(fromDate.isEmpty()){
                addFosterFromDateText.setError("Required");
            }
            if(fromTime.isEmpty()){
                addFosterFromTimeText.setError("Required");
            }
            if(untilDate.isEmpty()){
                addFosterUntilDateText.setError("Required");
            }
            if(untilTime.isEmpty()){
                addFosterUntilTimeText.setError("Required");
            }

            toast = Toast.makeText(getApplicationContext(),"Required fields are empty!", Toast.LENGTH_LONG);
            toast.show();
            return true;
        }

        if (!isValidPhone(phone)) {
            addFosterPhoneText.setError("Invalid phone number!");
            return true;
        }

        Database db = new Database();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser currentUser = mAuth.getCurrentUser();

        // in case of adding ride to specific event
        if(event != null){
            Foster newFoster = new Foster(currentUser.getUid(), currentUser.getPhotoUrl().toString(),
                    name, phone, location, city, fromDate, fromTime, untilDate, untilTime, event.getDatabaseID());

            if(foster != null) {
                db.updateFosterInDatabase(foster, newFoster);
                toast = Toast.makeText(getApplicationContext(), "Foster Updated!", Toast.LENGTH_SHORT);
            } else {
                db.addFosterToDatabase(newFoster);
                toast = Toast.makeText(getApplicationContext(), "Foster Sent!", Toast.LENGTH_SHORT);
            }
            toast.show();
        }

        Intent intent = new Intent(AddFosterActivity.this, FosterActivity.class);
        intent.putExtra("event", event);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                userLastKnownLocation = location;
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
    private boolean isValidPhone(String phone) {
        String PHONE_PATTERN = "^\\d{9,10}$";
        Pattern pattern = Pattern.compile(PHONE_PATTERN);
        Matcher matcher = pattern.matcher(phone);
        return matcher.matches();
    }
}
