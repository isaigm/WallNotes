package com.example.wallnotes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class EditNoteActivity extends AppCompatActivity {

    private TextView mTitle;
    private TextView mContent;
    private ImageView mImageView;
    private boolean mUpdate = false;
    private Note mCurrNote;
    private String mImgUri = null;
    private final int REQUEST_IMAGE = 0;
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
        mImageView = findViewById(R.id.image);
        mTitle.setFocusableInTouchMode(true);
        mTitle.requestFocus();
        mContent.setFocusableInTouchMode(true);
        mContent.requestFocus();
        mContent.setMovementMethod(new ScrollingMovementMethod());
        Bundle data = getIntent().getExtras();
        if(data != null){
            mCurrNote = new Note(data.getString("title"), data.getString("content"), null);
            mCurrNote.setUid(data.getInt("uid"));
            mUpdate = true;
            mTitle.setText(mCurrNote.getTitle());
            mContent.setText(mCurrNote.getContent());
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.delete){
                if(mUpdate)
                {
                    String title = mTitle.getText().toString();
                    String content = mContent.getText().toString();
                    Intent intent = new Intent("DATA");
                    intent.putExtra("uid", mCurrNote.getUid());
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("must_delete", true);
                    sendBroadcast(intent);
                    finish();
                }
            }else if(item.getItemId() == R.id.add_img){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
            return false;
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE){
            if(resultCode == RESULT_OK && data != null){
                mImgUri = data.getDataString();
                Picasso.get().load(data.getData())
                        .into(mImageView);
            }
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
        String content = mContent.getText().toString();
        if(title.length() > 0){
            Intent intent = new Intent("DATA");
            if(mUpdate){
                intent.putExtra("uid", mCurrNote.getUid());
            }
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            intent.putExtra("must_update_note", mUpdate);
            intent.putExtra("img_uri", mImgUri);
            sendBroadcast(intent);
        }
        finish();
        return super.onSupportNavigateUp();
    }
}