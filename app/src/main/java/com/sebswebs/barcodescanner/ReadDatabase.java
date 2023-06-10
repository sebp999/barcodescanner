package com.sebswebs.barcodescanner;

import static com.sebswebs.barcodescanner.PatientDbEntries.SQL_DELETE_ENTRIES;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.List;


public class ReadDatabase extends AppCompatActivity {
    private static final String FILENAME = "last_updated";
    private final String URL = "http://10.0.2.2:8000/patients/";
    private final String URL_UPDATE = "http://10.0.2.2:8000/patients/?since=";

    private final String IMG_DOWNLOAD_URL = "http://10.0.2.2/";
    private TextView resultsTextView;
    private Button fullSyncButton;
    private Button updateSyncButton;
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
        // Remove database and start again
        Log.e("BarcodeScanner", "Deleting data from database");
        PatientDbHelper dbHelper = new PatientDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        Log.e("BarcodeScanner", "Finished deleting data from database");
        return true;
    }



    private class UpdateThread extends Thread {
        // A thread that updates the database
        private String myURL = null;
        private boolean thisIsUpdate = false;
        public UpdateThread(String url, boolean isUpdate){
            // constructor
            myURL = url;
            thisIsUpdate = isUpdate;
        }

        @Override
        public void run() {

            if (thisIsUpdate){
                try {
                    Context context = ReadDatabase.this;
                    FileInputStream fis = context.openFileInput(FILENAME);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                    String line = reader.readLine();

                    String justDate = line.substring(0,10);
                    myURL+=justDate;
                    Log.e("BarcodeScanner", "this is an update so I'm using url "+myURL);

                } catch (FileNotFoundException e) {
                    //This is the first time it has been run, so report that you have to do a full sync first.
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myProgressText = (TextView) findViewById(R.id.updatedText);
                            myProgressText.setText("Please run a full sync before updating");
                        }
                    });
                    return;
                } catch (IOException ioe){
                    throw new RuntimeException(ioe);
                }
            }
            String feedContents = null;
            try {
                feedContents = readFeedContents();
                if (feedContents.trim().length() == 0){
                    Log.e("barcodescanner", "zero length feed");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myProgressText = (TextView) findViewById(R.id.updatedText);
                            myProgressText.setText("No new records since last update");
                        }
                    });
                    return;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException m) {
                throw new RuntimeException(m);
            }

            Log.e("BarcodeScanner", "Adding data to database from API " + Instant.now().toString());
            try {
                JSONArray patients = new JSONArray(feedContents);
                if (!thisIsUpdate && patients.length() > 0) {
                    recreateDatabase();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        myProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                        myProgressBar.setMax(patients.length());
                        myProgressText = (TextView) findViewById(R.id.updatedText);
                        myProgressText.setText("0/"+myProgressBar.getMax());
                    }
                });
                int num_patients = patients.length();
                for (int i = 0; i < num_patients; i++) {
                    JSONObject patient = patients.getJSONObject(i);
                    addPatientToDb(patient);
                    if (thisIsUpdate) {
                        try {
                            downloadPatientImg(patient);
                        } catch (IOException failed) {
                            TextView warnings = (TextView) findViewById(R.id.textView);
                            String existing_text = warnings.getText().toString();
                            String error = existing_text + "\nUnable to find image on server " + patient.getString("MemberId") + ".jpg";
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    warnings.setText(error);
                                }
                            });
                        }
                    }
                    if (i % 100 == 0) {
                        myProgressNum = i;
                        update_progress_display(myProgressNum, myProgressBar);
                    }
                }
                update_progress_display(myProgressBar.getMax(), myProgressBar); // final
                String filename = "last_updated";
                String fileContents = Instant.now().toString();
                Context context = ReadDatabase.this;
                try {
                    FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
                    fos.write(fileContents.getBytes());
                    FileInputStream fis = context.openFileInput(filename);
                    String contents = String.valueOf(fis.read());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void downloadPatientImg(JSONObject patient) throws JSONException, IOException {
            int count;
            String patientId = patient.getString("MemberId");
            URL imgDownloadUrl = new URL(IMG_DOWNLOAD_URL + patientId + ".jpg");
            URLConnection conn = imgDownloadUrl.openConnection();
            InputStream input = new BufferedInputStream(imgDownloadUrl.openStream(), 8192);
            OutputStream output = new FileOutputStream(getFilesDir().toString() + "/patient_images/"+ patientId + ".jpg");
            Log.e("", "outputting file" + getFilesDir().toString()+"/patient_images/");
            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
        }

        private void update_progress_display(int progressNum, ProgressBar progressBar) {
            handler.post(new Runnable() {
                public void run() {
                    Log.e("BarcodeScanner", (progressNum + 1) + " records added to database" + Instant.now().toString());
                    myProgressBar.setProgress(progressNum);
                    myProgressText.setText(progressNum + "/" + progressBar.getMax());
                }
            });
        }

        private String readFeedContents() throws InterruptedException, MalformedURLException {
            FeedThread t = new FeedThread(new URL(myURL));
            t.start();
            t.join();
            return t.getFeedContents();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.e("DEBUGX", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_database);
        resultsTextView = (TextView) findViewById(R.id.textView);
        fullSyncButton = (Button) findViewById(R.id.displayData);

        fullSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateThread(URL, false).start();
            }
        });

        updateSyncButton = (Button) findViewById(R.id.updateSync);
        updateSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)  {
                try{
                    new UpdateThread(URL_UPDATE, true).start();
                } catch (RuntimeException e) {  // No last updated file found.  You have to run full sync first.
                    Throwable cause = e.getCause();
                    if (cause instanceof FileNotFoundException) {
                        throw new RuntimeException("You have to run full sync before you can update");
                    } else {
                        throw e;
                    }
                }
            }
        });
    }
}
