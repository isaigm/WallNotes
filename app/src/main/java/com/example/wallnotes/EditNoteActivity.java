package com.example.wallnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Objects;

public class EditNoteActivity extends AppCompatActivity {

    private TextView mTitle;
    private TextView mContent;
    private static boolean mUpdate = false;
    private static Note mCurrNote;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = findViewById(R.id.note_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mTitle = findViewById(R.id.title);
        mContent = findViewById(R.id.content);
        Bundle data = getIntent().getExtras();
        if(data != null){

            mCurrNote = new Note(data.getString("title"));
            mCurrNote.uid = data.getInt("uid");
            mUpdate = true;
            mTitle.setText(mCurrNote.text);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notes_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        String title = mTitle.getText().toString();
        Intent intent = new Intent("DATA");
        intent.putExtra("must_update_note", mUpdate);
        intent.putExtra("title", title);
        if(mUpdate){
            intent.putExtra("uid", mCurrNote.uid);
        }
        sendBroadcast(intent);
        finish();
        return super.onSupportNavigateUp();
    }
}