package com.sebswebs.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.sebswebs.barcodescanner.databinding.ActivityBarcodeIdentifiedBinding;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public final class BarcodeIdentified extends AppCompatActivity {
    private ActivityBarcodeIdentifiedBinding binding;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.e("TaG", "Created");
        Log.e("TaG", getFilesDir().toString()); // /data/user/0/com.sebswebs.barcodescanner/files
        super.onCreate(savedInstanceState);
        this.binding = ActivityBarcodeIdentifiedBinding.inflate(this.getLayoutInflater());

        this.setContentView((View)binding.getRoot());
        String barcodeInfo = this.getIntent().getStringExtra("barcodeValue");
        List barcodeParts = Arrays.asList(barcodeInfo.split(","));
        String patientId = (String)barcodeParts.get(0);

        PatientDbHelper databaseHelper = new PatientDbHelper(this);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] cols = {"MemberId", "HouseholdId", "Client_Name", "MemberGender", "MemberDateOfBirth", "CurrentSubscriptionDate", "SubscriptionDuration", "MemberImagePath"};
        Cursor result = database.query("patient", cols, "MemberId='"+patientId+"'", null, null, null, null);
        Log.e("result", result.getCount()+"");
        try {
            result.moveToFirst();
            String memberId = result.getString(0);
            Log.e("memberId", memberId);
            int householdIdInt = result.getInt(1);
            String householdId = householdIdInt + "";

            String clientName = result.getString(2);
            String memberGender = result.getString(3);
            String memberDateOfBirth = result.getString(4);
            String currentSubscriptionDate = result.getString(5);
            String subscriptionDuration = (result.getInt(6) + "");
            String memberImagePat = result.getString(7);

            this.binding.tvHooray.setText((CharSequence) clientName);
            //        this.binding.patientName.setText((CharSequence)clientName);
            this.binding.patientNumber.setText((CharSequence) memberId);
            this.binding.HouseholdId.setText((CharSequence) householdId);
            this.binding.MemberGenderId.setText((CharSequence) memberGender);
            this.binding.MemberDateOfBirth.setText((CharSequence) memberDateOfBirth);
            this.binding.CurrentSubscriptionDate.setText((CharSequence) currentSubscriptionDate);
            this.binding.SubscriptionDuration.setText((CharSequence) subscriptionDuration);

            File imgFile = new File(getFilesDir() + "/patient_images/" + memberImagePat);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                binding.patientImage.setImageBitmap(myBitmap);
            } else {
                Log.e("aargh", "No image" + getFilesDir() + "/patient_images/" + memberImagePat);
            }
        }
        catch (android.database.CursorIndexOutOfBoundsException noMemberInDB) {
//            Intent switchActivityIntent = new Intent((Context)this, NoSuchUser.class);
//            switchActivityIntent.putExtra("barcodeValue", barcodeInfo);
//            this.startActivity(switchActivityIntent);
            Log.e("error", noMemberInDB.toString());
        }


        this.binding.btScanAnother.setOnClickListener(new View.OnClickListener() {
            public void onClick(@Nullable View view) {Log.e("TAG", "messaged"); BarcodeIdentified.this.switchToMain();
            }
        });
    }

    private final void switchToMain() {
        Intent switchActivityIntent = new Intent((Context)this, MainActivity.class);
        this.startActivity(switchActivityIntent);
    }
}
