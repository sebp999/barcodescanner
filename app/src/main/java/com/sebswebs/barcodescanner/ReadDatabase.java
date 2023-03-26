package com.sebswebs.barcodescanner;

import static com.sebswebs.barcodescanner.PatientDbEntries.SQL_CREATE_ENTRIES;
import static com.sebswebs.barcodescanner.PatientDbEntries.SQL_DELETE_ENTRIES;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


public class ReadDatabase extends AppCompatActivity {
    String myUrl = "http://10.0.2.2:8000/patients/";
    TextView resultsTextView;
    ProgressDialog progressDialog;
    Button displayData;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_database);
        resultsTextView = (TextView) findViewById(R.id.textView);
        displayData = (Button) findViewById(R.id.displayData);

        displayData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create object of MyAsyncTasks class and execute it
                MyAsyncTasks myAsyncTasks = new MyAsyncTasks(ReadDatabase.this);
                myAsyncTasks.execute();
            }
        });
    }


    private class MyAsyncTasks extends AsyncTask<String, String, String> {
        private Context myContext;
        public MyAsyncTasks(Context aContext) {
            myContext = aContext; // TODO weak reference prevents mem leaks
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // display a progress dialog for good user experiance
            progressDialog = new ProgressDialog(ReadDatabase.this);
            progressDialog.setMessage("processing results");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(String... params) {
            // Fetch data from the API in the background.
            Log.e("BarcodeScanner", "Reading data from API " + new Date());
            String result = "";
            try {
                URL url;
                HttpURLConnection urlConnection = null;
                try {
                    url = new URL(myUrl);
                    //open a URL coonnection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isw = new InputStreamReader(in);
                    int data = isw.read();
                    while (data != -1) {
                        result += (char) data;
                        data = isw.read();
                    }
                    Log.e("BarcodeScanner", "Finished reading data from API " + new Date());
                    // return the data to onPostExecute method
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Exception: " + e.getMessage();
            }
            return result;
        }
        private boolean addPatientToDb(JSONObject some_patient)  {
            try {
                PatientDbHelper dbHelper = new PatientDbHelper(myContext);
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
            PatientDbHelper dbHelper = new PatientDbHelper(myContext);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL(SQL_DELETE_ENTRIES);
            Log.e("BarcodeScanner", "Finished deleting data from database");
            return true;
        }

        @Override
        protected void onPostExecute(String s) {

            // dismiss the progress dialog after receiving data from API
            progressDialog.dismiss();

            Log.e("BarcodeScanner", "Adding data to database from API "+new Date());
            try {
//                JSONObject jsonObject = new JSONObject(s);
                JSONArray patients = new JSONArray(s);
                if (patients.length() > 0 ){
                    recreateDatabase();
                }
                String patientsList = "";
                for (int i = 0; i < patients.length(); i++) {
                    JSONObject patient = patients.getJSONObject(i);
                    addPatientToDb(patient);
                    if (i % 100 == 0) {
                        Log.e("BarcodeScanner", i + " records added to database" + new Date());
                    }
                }
                resultsTextView.setVisibility(View.VISIBLE);

                //Display data with the Textview
                resultsTextView.setText(patientsList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

