package com.mtariqkakar.papp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

public class PappDatabase extends SQLiteOpenHelper {
    SQLiteDatabase mSqLiteDatabase;
    Polygons polygonsObject;
    private static final String databaseName = "PAPPDatabase";

    //database version
    private static final int databaseVersion = 3;
    /*===============================================================================================
     creating Geofence Table with its columns
     ============================================================================================== */
    private static final String Geofence_Table = "Geofence_Table";
    //columns name for Geofence Table
    public static final String Geofence_ID = "geofence_id";
    public static final String Center_Point_Latitude = "Center_Point_Latitude";
    public static final String Center_Point_Longitude = "Center_Point_Longitude";
    public static final String Geofence_Radius = "radius";
    /*==============================================================================================
    Creating Polygon Table with its columns
    ================================================================================================*/
    private static final String Polygon_Table = "Polygon_Table";
    public static final String Polygon_ID = "polygon_id";
    public static final String Polygon_Points = "polygon_points";
    //public static final String Geofence_ID = "geofence_id"; //as a foreign key in this table
    /*==============================================================================================
    Creating Markers Table with its columns
     ==============================================================================================*/
    public static final String Markers_Table = "Markers_Table";
    public static final String Marker_Position_Latitude = "marker_position_latitude";
    public static final String Marker_Position_Longitude = "marker_position_longitude";
    public static final String Marker_Title = "marker_title";
    //public static final String Geofence_ID = "geofence_id"; //as a foreign key in this table

    public PappDatabase(Context context) {
        super(context, databaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GEOFENCE_TABLE = " CREATE TABLE IF NOT EXISTS " + Geofence_Table + "(" + Geofence_ID + " TEXT PRIMARY KEY, " +
                Center_Point_Latitude + " REAL , " + Center_Point_Longitude + " REAL, " + Geofence_Radius + " INTEGER);";
        db.execSQL(CREATE_GEOFENCE_TABLE);
        String CREATE_POLYGON_TABLE = " CREATE TABLE IF NOT EXISTS " + Polygon_Table + "(" + Polygon_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Polygon_Points + " TEXT, " + Geofence_ID + " TEXT, " +
                "FOREIGN KEY(" + Geofence_ID + ") REFERENCES " + Geofence_Table + "(" + Geofence_ID + "));";
        db.execSQL(CREATE_POLYGON_TABLE);
        String CREATE_MARKER_TABLE = " CREATE TABLE IF NOT EXISTS " + Markers_Table + "(" + Marker_Position_Latitude + " REAL, " +
                Marker_Position_Longitude + " REAL, " +
                Marker_Title + " TEXT, " + Geofence_ID + " TEXT, " +
                "FOREIGN KEY(" + Geofence_ID + ") REFERENCES " + Geofence_Table + "(" + Geofence_ID + "));";
        db.execSQL(CREATE_MARKER_TABLE);

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            mSqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Geofence_Table);
            mSqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Markers_Table);
            mSqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Polygon_Table);
            onCreate(db);
    }

    public long saveGeofence(String Geofence_id, double lat, double lng, int radius) {
        mSqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Geofence_ID, Geofence_id);
        contentValues.put(Center_Point_Latitude, lat);
        contentValues.put(Center_Point_Longitude, lng);
        contentValues.put(Geofence_Radius, radius);
        return mSqLiteDatabase.insert(Geofence_Table, null, contentValues);
    }

    public long saveMarker(double lat, double lng, String marker_title, String Geofence_id) {
        mSqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Marker_Position_Latitude, lat);
        contentValues.put(Marker_Position_Longitude, lng);
        contentValues.put(Marker_Title, marker_title);
        contentValues.put(Geofence_ID, Geofence_id);
        return mSqLiteDatabase.insert(Markers_Table, null, contentValues);
    }

    public long savePolygon(String polygonPoints, String Geofence_id) {
        mSqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Polygon_Points, polygonPoints);
        contentValues.put(Geofence_ID, Geofence_id);
        return mSqLiteDatabase.insert(Polygon_Table, null, contentValues);

    }

    private List<Geofence> geofencesList = new ArrayList<Geofence>();

    public List<Geofence> getGeofence() {
        mSqLiteDatabase = this.getReadableDatabase();
        String columns[] = new String[]{Geofence_ID, Center_Point_Latitude, Center_Point_Longitude, Geofence_Radius};
        Cursor cursor = mSqLiteDatabase.query(Geofence_Table, columns, null, null, null, null, null);
        String result = "";

        int _id = cursor.getColumnIndex(Geofence_ID);
        int icenter_lat = cursor.getColumnIndex(Center_Point_Latitude);
        int icenter_lng = cursor.getColumnIndex(Center_Point_Longitude);
        int iradius = cursor.getColumnIndex(Geofence_Radius);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
           geofencesList.add(addGeofence(cursor.getString(_id), cursor.getDouble(icenter_lat), cursor.getDouble(icenter_lng), cursor.getInt(iradius)));
        }
        return geofencesList;
    }
    /*
    *
    * */
    private ArrayList<PolygonOptions> polygonOptionsList=new ArrayList<PolygonOptions>();

    public ArrayList<String> getpolygon(){
        mSqLiteDatabase = this.getReadableDatabase();

        String columns[] = new String[]{Polygon_Points};
        Cursor cursor = mSqLiteDatabase.query(Polygon_Table, columns, null, null, null, null, null);
        ArrayList<String> pointsOfPolygon = new ArrayList<String>();
        int ipolygon_points = cursor.getColumnIndex(Polygon_Points);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
           pointsOfPolygon.add(cursor.getString(ipolygon_points));

        }

        return pointsOfPolygon;

    }
/*
*
* */
    private Geofence addGeofence(String geofenceId, double lat, double lng, int radius) {
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
    private ArrayList<MarkerOptions> markersList = new ArrayList<MarkerOptions>();

    public List<MarkerOptions> getMarkersList() {
        mSqLiteDatabase = this.getReadableDatabase();
        String columns[] = new String[]{Marker_Position_Latitude, Marker_Position_Longitude, Marker_Title};
        Cursor cursor = mSqLiteDatabase.query( Markers_Table, columns, null, null, null, null, null);
        int imlat = cursor.getColumnIndex(Marker_Position_Latitude);
        int imlng = cursor.getColumnIndex(Marker_Position_Longitude);
        int imarker_title = cursor.getColumnIndex(Marker_Title);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            LatLng ll= new LatLng(cursor.getDouble(imlat),cursor.getDouble(imlng));
           MarkerOptions markerOptions = new MarkerOptions()
                   .position(ll)
                   .title(cursor.getString(imarker_title));
            markersList.add(markerOptions);
        }
        return markersList;
    }
    public void deletePolygon(String _id){
        mSqLiteDatabase = this.getWritableDatabase();


    }
    ArrayList<CircleOptions> circleOptionsList = new ArrayList<>();
    public ArrayList<CircleOptions> getCircleOptionsList() {
        mSqLiteDatabase = this.getReadableDatabase();
        String columns[] = new String[]{Geofence_ID, Center_Point_Latitude, Center_Point_Longitude, Geofence_Radius};
        Cursor cursor = mSqLiteDatabase.query(Geofence_Table, columns, null, null, null, null, null);
        String result = "";

        int _id = cursor.getColumnIndex(Geofence_ID);
        int icenter_lat = cursor.getColumnIndex(Center_Point_Latitude);
        int icenter_lng = cursor.getColumnIndex(Center_Point_Longitude);
        int iradius = cursor.getColumnIndex(Geofence_Radius);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            circleOptionsList.add(new CircleOptions()
                            .center(new LatLng(cursor.getDouble(icenter_lat), cursor.getDouble(icenter_lng)))
                            .radius(cursor.getDouble(iradius)).strokeWidth(5));
        }
        return circleOptionsList;
    }

}
/* public List<PolygonOptions> getPolygons() {
        mSqLiteDatabase = this.getReadableDatabase();
        String columns[] = new String[]{Polygon_Points};
        Cursor cursor = mSqLiteDatabase.query(Polygon_Table, columns, null, null, null, null, null);
       int ipolygon_points = cursor.getColumnIndex(Polygon_Points);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            List<LatLng>  polygonLatLngs = polygonsObject.splitPolygonPointsString(cursor.getString(ipolygon_points));
            PolygonOptions polygonOptions = new PolygonOptions().addAll(polygonLatLngs);
            polygonOptionsList.add(polygonOptions);
            polygonLatLngs = null;
        }
        return polygonOptionsList;
    }*/