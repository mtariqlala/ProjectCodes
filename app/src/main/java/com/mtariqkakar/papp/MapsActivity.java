package com.mtariqkakar.papp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,View.OnClickListener{

    public GoogleMap mGoogleMap;

    private Polygon polygonShape;
    private GoogleApiClient googleApiClient = null;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private String TAG = "MainActivity";
    private GeofencingClient mGeofencingClient;
    Intent intent = null, chooser;
    private PendingIntent mGeofencePendingIntent;
    ArrayList<Geofence> mGeofenceList = new ArrayList<>();
    private static final String[] MAP_TYPE_ITEMS =
            {"None", "Normal", "Hybrid", "Satellite", "Terrain"};
    Button maptype;
    private float radius;
    Polygons polygonObject = new Polygons();
    private Button save_Geofence;
    private Button clearButton;
    int flag=1;
    List<PolygonOptions> polygons=new ArrayList<PolygonOptions>();
    final Context context=this;
    PolygonOptions polygonOptions;
    PappDatabase pappDatabase;
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume Called");
        super.onResume();

        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (response != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "Google paly services not available please! download it ", Toast.LENGTH_LONG).show();
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=dolphin.developers.com"));
            chooser = Intent.createChooser(intent, "Launch Market");
            startActivity(chooser);
            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();
        } else {
            Log.d(TAG, "Google Play serices available no Action is required");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        pappDatabase = new PappDatabase(this);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        maptype = findViewById(R.id.mapType);
        maptype.setOnClickListener(this);
        save_Geofence = findViewById(R.id.saveGeofenc);
        save_Geofence.setOnClickListener(this);
        clearButton= findViewById(R.id.clearbtn);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMarkersList();
            }
        });
        /*Used for receiving notifications from the FusedLocationProviderApi
          when the device location has changed or can no longer be determined.*/
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                } else {
                    for (Location location : locationResult.getLocations()) {
                        LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                         //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 20));

                    }
                }
            }
        };


        //Android 6.0+ (API level 23+), users grant permissions to apps while the app is running,
        // not when they install the app.
        getUserLastKnownLocation();
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                           Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);
        startLocationMonitoring();
        /*if(!pappDatabase.getMarkersList().isEmpty()) {
            for (int i=0; i< pappDatabase.getMarkersList().size();i++) {
                markersList.add(mGoogleMap.addMarker(pappDatabase.getMarkersList().get(i)));

            }
        }*/
       /* for (int i =0;i<pappDatabase.getpolygon().size();i++) {
            polygonOptions = new PolygonOptions().addAll(polygonObject.splitPolygonPointsString(pappDatabase.getpolygon().get(i)));
            addPolgonInMap(polygonOptions);
        }*/
        System.out.println("Polygon ==============================================");



        }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.mGoogleMap = googleMap;
        mGoogleMap.setOnMapLongClickListener(this);




            if (mGoogleMap != null)
            MapSetup();

                polygonShape = googleMap.addPolygon(new PolygonOptions().add(
                new LatLng(30.252899, 67.016239),
                new LatLng(30.252921, 67.016537),
                new LatLng(30.252807, 67.016518),
                new LatLng(30.252811, 67.016411),
                new LatLng(30.252765, 67.016403),
                new LatLng(30.252787, 67.016220),
                new LatLng(30.252899, 67.016239)).strokeWidth(5).strokeColor(Color.GREEN).fillColor(Color.parseColor("#51000000")));
        //getting list of vertices polygon contains
        List<LatLng> latLng = polygonShape.getPoints();
        //getting centerpoint of polygon
        LatLng centerpoint = polygonObject.getPolygonCenterPoint(latLng);
        this.radius = polygonObject.distanceFinding(centerpoint,latLng);

        CircleOptions circleOptions;
        circleOptions = new CircleOptions()
                .center(new LatLng(centerpoint.latitude, centerpoint.longitude))
                .radius(radius).strokeWidth(5);
        // startGeofenceMonitoring(centerpoint,30);

        Circle circle = googleMap.addCircle(circleOptions);
        LatLng myHouse = new LatLng(centerpoint.latitude,centerpoint.longitude);
        googleMap.addMarker(new MarkerOptions()
                .position(myHouse)
                .title("Mohammad Tariq Lala House"));
        this.mGeofenceList.add(addGeofence("Mohammad Tariq Lala House",centerpoint.latitude,centerpoint.longitude,(int)(radius)));
        /* int i=0;
        while (!pappDatabase.getPolygons().isEmpty()){
            polygons.add(pappDatabase.getPolygons().get(i));
            i++;

        }
        addPolgonInMap(polygons);*/
       /* int i=0;
       while (!pappDatabase.getMarkersList().isEmpty()) {
           googleMap.addMarker(pappDatabase.getMarkersList().get(i));
           i++;
       }*/
        //String databaseString = pappDatabase.getpolygon().get(0).toString();
        if(pappDatabase.getpolygon()!=null) {
            ArrayList<String> listOfPolygons = pappDatabase.getpolygon();
            List<MarkerOptions> databaseMarkerOptions = pappDatabase.getMarkersList();
            mGeofenceList.addAll(pappDatabase.getGeofence());
            ArrayList<CircleOptions> databaseCircleOptioinsList = pappDatabase.getCircleOptionsList();

            for (int i = 0; i < listOfPolygons.size(); i++) {
                PolygonOptions databasePolygonOptions = new PolygonOptions().addAll(polygonObject.splitPolygonPointsString(listOfPolygons.get(i)))
                        .strokeWidth(5).strokeColor(Color.GREEN).fillColor(Color.parseColor("#51000000"));
                mGoogleMap.addPolygon(databasePolygonOptions);
                mGoogleMap.addMarker(databaseMarkerOptions.get(i));
                mGoogleMap.addCircle(databaseCircleOptioinsList.get(i));

            }
        }

    }



    //Setting up the map like adding find my location and etc
    private void MapSetup() {
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(true);
        mGoogleMap.getUiSettings().setZoomGesturesEnabled(true);
        mGoogleMap.getUiSettings().setScrollGesturesEnabled(true);
        mGoogleMap.getUiSettings().setTiltGesturesEnabled(true);
        mGoogleMap.getUiSettings().setRotateGesturesEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }
    //Location Monitoring  Method to get location updates
    private void startLocationMonitoring() {
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(10000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    &&
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }catch (SecurityException e){
            Log.d(TAG,e.getMessage());
        }
    }

    //Monitoring geofence
    private void startGeofenceMonitoring() {
        Toast.makeText(this, "The App is looking for geofence!", Toast.LENGTH_LONG).show();
        //Toast.makeText(this,mGeofenceList.get(0)+"",Toast.LENGTH_LONG).show();
        if (!googleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient is not connected", Toast.LENGTH_SHORT).show();
        } else {
          //  Toast.makeText(this, "GoogleApiClient is connected", Toast.LENGTH_SHORT).show();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }

                    }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed to add geofence
                }
            });
        }
    }
    private Geofence addGeofence(String geofenceId,double lat,double lng,int radius){
        Geofence geofence = new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(lat, lng, radius)
                //.setCircularRegion(30.252874, 67.016337,100)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(2000)
                .build();
        return geofence;
    }
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }
    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String dialogTitle = "Select Map Type";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(dialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = mGoogleMap.getMapType();

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 0:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                                break;
                            case 1:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                break;
                            case 2:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 3:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                            case 4:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            default:
                                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        dialog.dismiss();
                    }
                }
        );

        //Building Dialog and show it for changing map type
        AlertDialog mapTypeDialog = builder.create();
        mapTypeDialog.setCanceledOnTouchOutside(true);
        mapTypeDialog.show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationMonitoring();
        startGeofenceMonitoring();

    }
    @Override
    public void onConnectionSuspended(int i) {
        //startGeofenceMonitoring();


    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private void getUserLastKnownLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.i("LatLng", location.getLatitude() + "" + location.getLongitude());
                    LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
       googleApiClient.reconnect();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        googleApiClient.reconnect();
       // startGeofenceMonitoring();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //googleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        googleApiClient.disconnect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        }

    @Override
    public void onMapClick(LatLng latLng) {


    }

    private List<LatLng> polygonList=new ArrayList<>();
    private List<Marker> markersList=new ArrayList<Marker>();
    /*==============================================================================================
    When user tapped on map for long time means for 1/2 second a marker will be placed on that place
    and markers will be added while the user does not click on save geofence
      ==============================================================================================
    */
    @Override
    public void onMapLongClick(LatLng point) {
        Location location=new Location("marker");
        location.setLatitude(point.latitude);
        location.setLongitude(point.longitude);
        location.setTime(new Date().getTime());
        String latlng=location.getLatitude() + ","+location.getLongitude();
        MarkerOptions markerOptions=new MarkerOptions()
                .title(point.toString())
                .position(point);
        markersList.add(mGoogleMap.addMarker(markerOptions));
        //Toast.makeText(this,latlng,Toast.LENGTH_LONG).show();
        polygonList.add(point);
           }
     /*=============================================================================================
     * getting marker's latlong and appending it to a string, during addition a plus sign will be
     * concatented after the addition of each element.
     * This String will be stored into the database and in the retrieving we will split this string
     * by + sign and then we will assign these points to LatLng list and then will be added to map and geofences
     * =============================================================================================
     * */
        private String pointsString ="";
        private String checkStatus() {
            if(markersList.isEmpty() || markersList.size()<3) {
                return "";
            }
            else
                if (flag == 0) {
                    //PolygonOptions options = new PolygonOptions()
                    for (int i = 0; i < markersList.size(); i++) {
                      // options.add(markersList.get(i).getPosition());
                       pointsString +=markersList.get(i).getPosition().latitude+","+markersList.get(i).getPosition().longitude+"+";
                    }
               }
            for (int i = 0; i < markersList.size(); i++) {
                markersList.get(i).remove();
                markersList.remove(i);
                i= i-1;
            }
               return pointsString;
           }
           /*=======================================================================================
           * user may miss tap on map, so require to clear that marker
           * =======================================================================================
           * */
            private void removeMarkersList() {
                if(!markersList.isEmpty()) {
                    markersList.get(markersList.size() - 1).remove();
                    markersList.remove(markersList.size() - 1);
                }else {
                    Toast.makeText(this,"There is no marker to remove! ",Toast.LENGTH_SHORT).show();
                }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mapType:
                showMapTypeSelectorDialog();
                break;
            case R.id.saveGeofenc:
                flag = 0;
                String points= checkStatus();
                this.pointsString = "";
                polygonOptions = new PolygonOptions().addAll(polygonObject.splitPolygonPointsString(points))
                        .strokeWidth(5).strokeColor(Color.GREEN).fillColor(Color.parseColor("#51000000"));
                addItemInPolygonList(polygonOptions);
                List<LatLng> latLng = polygonOptions.getPoints();
                //getting centerpoint of polygon
                LatLng centerpoint = polygonObject.getPolygonCenterPoint(latLng);
                radius = polygonObject.distanceFinding(centerpoint, latLng);
                setGeofenceID(points,polygonOptions, centerpoint.latitude,centerpoint.longitude,(int) radius);
                break;
        }

    }

    private void addItemInPolygonList(PolygonOptions polygonOptions) {
                PolygonOptions polygonOptions1=polygonOptions;
                polygons.add(polygonOptions);
    }

            boolean status=false;
     String geofenceidName="";
    private void setGeofenceID(String polygonPoints,PolygonOptions polygonOption, double latitude, double longitude, final int radius){

        final PolygonOptions polygonOptions1 = polygonOptions;
        final double lat=latitude;
        final double lng=longitude;
        final int radius1=radius;
        final String polygon_points = polygonPoints;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptsView = layoutInflater.inflate(R.layout.geofenceidedittext, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        // set geofenceidedittext.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextGeofenceId);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                try {
                                    geofenceidName= userInput.getText().toString();
                                    pappDatabase.saveGeofence(geofenceidName, lat, lng, radius);
                                    pappDatabase.saveMarker(lat, lng, geofenceidName, geofenceidName);
                                    pappDatabase.savePolygon(polygon_points, geofenceidName);
                                    status = true;
                                }
                                catch (Exception exc)
                                {
                                    status = false;
                                    String error = exc.getMessage().toString();
                                    Dialog statusdialog = new Dialog(MapsActivity.this);
                                    statusdialog.setTitle("Error");
                                    TextView textView = new TextView(MapsActivity.this);
                                    textView.setText(error);
                                    statusdialog.setContentView(textView);
                                    statusdialog.show();
                                } finally {
                                    if (status) {
                                        AlertDialog.Builder alertDialog=new AlertDialog.Builder(MapsActivity.this);
                                        alertDialog.setTitle("Insertion Success Message");
                                        alertDialog.setMessage("Your Area has been included!");
                                        alertDialog.show();
                                    }
                                }
                                addGeofencePolygonMarkerinMap(polygonOptions1,geofenceidName,lat,lng,radius1);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }
    private void addGeofencePolygonMarkerinMap(PolygonOptions polygonOption,String GeofenceName, double latitude,double longitude,int radius){
        if (!GeofenceName.isEmpty()) {
            mGeofenceList.add(addGeofence(GeofenceName + "", latitude, longitude,(radius)));
            CircleOptions circleOptions;
            circleOptions = new CircleOptions()
                    .center(new LatLng(latitude, longitude))
                    .radius(radius).strokeWidth(5);
            Circle circle = mGoogleMap.addCircle(circleOptions);
            LatLng myHouse = new LatLng(latitude, longitude);
            mGoogleMap.addMarker(new MarkerOptions()
                    .position(myHouse)
                    .title(GeofenceName));
            mGoogleMap.addPolygon(polygonOption);
        }
        else {
            Toast.makeText(this,"Masjid name field cannot be left empty!",Toast.LENGTH_SHORT).show();
        }
    }
    private void addMarkersInMap(List<MarkerOptions> markerOptionsList){
        List<MarkerOptions> mMarkerOptionsList=markerOptionsList;
        for (int i = 0; i<mMarkerOptionsList.size();i++) {
            mGoogleMap.addMarker(mMarkerOptionsList.get(i));
        }
    }
    private void addPolgonInMap(List<PolygonOptions> polygonOptions) {
        List<PolygonOptions> mPolygonOptionsList = polygonOptions;
        for (int i = 0; i<mPolygonOptionsList.size();i++) {
            mGoogleMap.addPolygon(mPolygonOptionsList.get(i));
        }

    }
    private void addPolgonInMap(PolygonOptions polygonOptions) {


        }




}





