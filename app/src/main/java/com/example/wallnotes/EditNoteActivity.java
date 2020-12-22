package com.example.wallnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.google.android.material.bottomappbar.BottomAppBar;
import java.util.Objects;

public class EditNoteActivity extends AppCompatActivity {

    private TextView mTitle;
    private TextView mContent;
    private boolean mUpdate = false;
    private Note mCurrNote;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = findViewById(R.id.note_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        mTitle = findViewById(R.id.title);
        mContent = findViewById(R.id.content);
        mTitle.setFocusableInTouchMode(true);
        mTitle.requestFocus();
        mContent.setFocusableInTouchMode(true);
        mContent.requestFocus();
        mContent.setMovementMethod(new ScrollingMovementMethod());
        Bundle data = getIntent().getExtras();
        if(data != null){
            mCurrNote = new Note(data.getString("title"), data.getString("content"));
            mCurrNote.uid = data.getInt("uid");
            mUpdate = true;
            mTitle.setText(mCurrNote.title);
            mContent.setText(mCurrNote.content);
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.delete){
                if(mUpdate)
                {
                    String title = mTitle.getText().toString();
                    String content = mContent.getText().toString();
                    Intent intent = new Intent("DATA");
                    intent.putExtra("uid", mCurrNote.uid);
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("must_delete", true);
                    sendBroadcast(intent);
                    finish();
                }
            }
            return false;
        });
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
        String content = mContent.getText().toString();
        if(title.length() > 0){
            Intent intent = new Intent("DATA");
            if(mUpdate){
                intent.putExtra("uid", mCurrNote.uid);
            }
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            intent.putExtra("must_update_note", mUpdate);
            sendBroadcast(intent);
        }
        finish();
        return super.onSupportNavigateUp();
    }
}