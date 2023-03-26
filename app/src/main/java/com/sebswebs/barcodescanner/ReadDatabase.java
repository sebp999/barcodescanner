package com.sebswebs.barcodescanner;

import static com.sebswebs.barcodescanner.PatientDbEntries.SQL_DELETE_ENTRIES;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;


public class ReadDatabase extends AppCompatActivity {
    private final String URL = "http://10.0.2.2:8000/patients/";
    private TextView resultsTextView;
    private Button updateDatabaseButton;
    private int myProgressNum = 0;

    private ProgressBar myProgressBar;
    private TextView myProgressText;
    private Handler handler = new Handler();

    private boolean addPatientToDb(JSONObject some_patient) {
        try {
            PatientDbHelper dbHelper = new PatientDbHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(PatientDbEntries.PatientEntry.CLIENT_NAME_COLUMN, some_patient.getString("Client_Name"));
            values.put(PatientDbEntries.PatientEntry.HOUSEHOLD_ID_COLUMN, some_patient.getString("HouseholdId"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_GENDER_COLUMN, some_patient.getString("MemberGender"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_DATE_OF_BIRTH_COLUMN, some_patient.getString("MemberDateOfBirth"));
            values.put(PatientDbEntries.PatientEntry.CURRENT_SUBSCRIPTION_DATE_COLUMN, some_patient.getString("CurrentSubscriptionDate"));
            values.put(PatientDbEntries.PatientEntry.SUBSCRIPTION_DURATION_COLUMN, some_patient.getString("SubscriptionDuration"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_IMAGE_PATH, some_patient.getString("MemberImagePath"));
            long newRowId = db.insert(PatientDbEntries.PatientEntry.TABLE_NAME, null, values);
            db.close();
            return true;
        } catch (Exception e) {
            Log.e("argh", e.toString());
        }
        return false;
    }

    private boolean recreateDatabase() {
        Log.e("BarcodeScanner", "Deleting data from database");
        PatientDbHelper dbHelper = new PatientDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        Log.e("BarcodeScanner", "Finished deleting data from database");
        return true;
    }

    private String readFeedContents() throws InterruptedException, MalformedURLException {
        FeedThread t = new FeedThread(new URL(URL));
        t.start();
        t.join();
        return t.getFeedContents();
    }


    protected void onCreate(Bundle savedInstanceState) {
        Log.e("DEBUGX", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_database);
        resultsTextView = (TextView) findViewById(R.id.textView);
        updateDatabaseButton = (Button) findViewById(R.id.displayData);
        updateDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create object of MyAsyncTasks class and execute it
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Log.e("DEBUGX", "first thread");
                        myProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                        myProgressBar.setMax(10000);
                        myProgressText = (TextView) findViewById(R.id.updatedText);
//                        myProgressText.setText("Updated " + myProgressNum + "/" + myProgressBar.getMax());
                        Log.e("BarcodeScanner", "Reading data from API " + new Date());
                        String feedContents = null;
                        try {
                            feedContents = readFeedContents();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (MalformedURLException m) {
                            throw new RuntimeException(m);
                        }

                        Log.e("BarcodeScanner", "Adding data to database from API " + new Date());
                        try {
                            JSONArray patients = new JSONArray(feedContents);
                            if (patients.length() > 0) {
                                recreateDatabase();
                            }
                            for (int i = 0; i < patients.length(); i++) {
                                JSONObject patient = patients.getJSONObject(i);
                                addPatientToDb(patient);
                                if (i % 100 == 0) {
                                    myProgressNum = i;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Log.e("BarcodeScanner", myProgressNum + " records added to database" + new Date());
                                            myProgressBar.setProgress(myProgressNum);
                                            myProgressText.setText(myProgressNum + "/" + myProgressBar.getMax());
                                        }
                                    });
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                ).start();
            }
        });
    }
}



//    private class MyAsyncTasks extends AsyncTask<String, String, String> {
//        private Context myContext;
//        private ProgressBar myProgressBar;
//        private TextView myProgressText;
//        private Handler handler = new Handler();
//
//        public MyAsyncTasks(Context aContext) {
//            myContext = aContext; // TODO weak reference prevents mem leaks
//        }
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            myProgressBar = (ProgressBar) findViewById(R.id.progressBar);
//            myProgressBar.setMax(10000);
//            myProgressText = (TextView) findViewById(R.id.updatedText) ;
//            myProgressText.setText("Updated "+myProgressNum+"/"+myProgressBar.getMax());
//        }
//        @Override
//        protected String doInBackground(String... params) {
//            // Fetch data from the API in the background.
//            Log.e("BarcodeScanner", "Reading data from API " + new Date());
//            String result = "";
//            try {
//                URL url;
//                HttpURLConnection urlConnection = null;
//                try {
//                    url = new URL(myUrl);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    InputStream in = urlConnection.getInputStream();
//                    InputStreamReader isw = new InputStreamReader(in);
//                    BufferedReader bufferedReader = new BufferedReader(isw);
//                    String line;
//                    while ((line = bufferedReader.readLine()) != null) {
//                        result += line;
//                    }
//                    Log.e("BarcodeScanner", "Finished reading data from API " + new Date());
//                    // return the data to onPostExecute method
//                    return result;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    if (urlConnection != null) {
//                        urlConnection.disconnect();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                return "Exception: " + e.getMessage();
//            }
//            return result;
//        }
//        private boolean addPatientToDb(JSONObject some_patient)  {
//            try {
//                PatientDbHelper dbHelper = new PatientDbHelper(myContext);
//                SQLiteDatabase db = dbHelper.getWritableDatabase();
//
//                ContentValues values = new ContentValues();
//                values.put(PatientDbEntries.PatientEntry.CLIENT_NAME_COLUMN, some_patient.getString("Client_Name"));
//                values.put(PatientDbEntries.PatientEntry.HOUSEHOLD_ID_COLUMN, some_patient.getString("HouseholdId"));
//                values.put(PatientDbEntries.PatientEntry.MEMBER_GENDER_COLUMN, some_patient.getString("MemberGender"));
//                values.put(PatientDbEntries.PatientEntry.MEMBER_DATE_OF_BIRTH_COLUMN, some_patient.getString("MemberDateOfBirth"));
//                values.put(PatientDbEntries.PatientEntry.CURRENT_SUBSCRIPTION_DATE_COLUMN, some_patient.getString("CurrentSubscriptionDate"));
//                values.put(PatientDbEntries.PatientEntry.SUBSCRIPTION_DURATION_COLUMN, some_patient.getString("SubscriptionDuration"));
//                values.put(PatientDbEntries.PatientEntry.MEMBER_IMAGE_PATH, some_patient.getString("MemberImagePath"));
//                long newRowId = db.insert(PatientDbEntries.PatientEntry.TABLE_NAME, null, values);
//                db.close();
//                return true;
//            } catch (Exception e) {
//                Log.e("argh", e.toString());
//            }
//            return false;
//        }
//
//        private boolean recreateDatabase() {
//            Log.e("BarcodeScanner", "Deleting data from database");
//            PatientDbHelper dbHelper = new PatientDbHelper(myContext);
//            SQLiteDatabase db = dbHelper.getWritableDatabase();
//            db.execSQL(SQL_DELETE_ENTRIES);
//            Log.e("BarcodeScanner", "Finished deleting data from database");
//            return true;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//
//            // dismiss the progress dialog after receiving data from API
//
//            Log.e("BarcodeScanner", "Adding data to database from API "+new Date());
//            try {
////                JSONObject jsonObject = new JSONObject(s);
//                JSONArray patients = new JSONArray(s);
//                if (patients.length() > 0 ){
//                    recreateDatabase();
//                }
//                for (int i = 0; i < patients.length(); i++) {
//                    JSONObject patient = patients.getJSONObject(i);
//                    addPatientToDb(patient);
//                    if (i % 100 == 0) {
//                        myProgressNum = i;
//                        handler.post(new Runnable() {
//                            public void run() {
//                                Log.e("BarcodeScanner", myProgressNum + " records added to database" + new Date());
//                                myProgressBar.setProgress(myProgressNum);
//                                myProgressText.setText(myProgressNum + "/" + myProgressBar.getMax());
//                            }
//                        });
//                    }
//                }
//                resultsTextView.setVisibility(View.VISIBLE);
//
//                //Display data with the Textview
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
//
