package com.sebswebs.barcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class BaseMenus extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.top_bar);
    }

   public void settingsClicked(View v) {
        Log.e("xxxx", "clicked");
        Intent switchActivityIntent = new Intent(BaseMenus.this, Settings.class);
        startActivity(switchActivityIntent);
    }

    public void menuClicked(View v){
        Log.e("xxxx", "menu clicked");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        Log.e("xxxx", "menu created");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void goToDatabase(){
        Intent switchActivityIntent = new Intent(BaseMenus.this, SyncData.class);
        startActivity(switchActivityIntent);
    }

    public void goToSettings(){
        Intent switchActivityIntent = new Intent(BaseMenus.this, Settings.class);
        startActivity(switchActivityIntent);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.e("xxxx", "item selected");
        switch (item.getItemId()) {
            case R.id.menu_update:
                goToDatabase();
                return true;
            case R.id.menu_settings:
                goToSettings();
                return true;
        }
        return false;
    }
}