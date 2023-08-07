package com.sebswebs.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.sebswebs.barcodescanner.databinding.ActivityBarcodeIdentifiedBinding;
import org.jetbrains.annotations.Nullable;

public final class BarcodeIdentified extends AppCompatActivity {
    private ActivityBarcodeIdentifiedBinding binding;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.e("TaG", "Created");
        super.onCreate(savedInstanceState);
        this.binding = ActivityBarcodeIdentifiedBinding.inflate(this.getLayoutInflater());

        this.setContentView((View)binding.getRoot());
        this.binding.tvHooray.setText((CharSequence)this.getIntent().getStringExtra("barcodeValue"));
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
