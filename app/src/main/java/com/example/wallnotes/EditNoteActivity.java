package com.example.wallnotes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomappbar.BottomAppBar;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import static java.text.DateFormat.getDateInstance;

public class EditNoteActivity extends AppCompatActivity {

    private final int REQUEST_IMAGE = 0;
    private final int REQUEST_CAMERA = 1;
    private TextView mTitle;
    private TextView mContent;
    private ImageView mImageView;
    private boolean mUpdate = false;
    private Note mCurrNote;
    private String mImgUri = null;
    private String mPhotoPath = null;
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
            mCurrNote.setImgUri(data.getString("img_uri"));
            mUpdate = true;
            mTitle.setText(mCurrNote.getTitle());
            mContent.setText(mCurrNote.getContent());
            if(mCurrNote.getImgUri() != null)
            {
                Glide.with(this)
                        .load(mCurrNote.getImgUri())
                        .error(R.drawable.reload)
                        .into(mImageView);
                mImgUri = mCurrNote.getImgUri();
            }
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if(id == R.id.delete){
                if(mUpdate)
                {
                    String title = mTitle.getText().toString();
                    String content = mContent.getText().toString();
                    Intent intent = new Intent("DATA");
                    intent.putExtra("uid", mCurrNote.getUid());
                    intent.putExtra("title", title);
                    intent.putExtra("content", content);
                    intent.putExtra("must_delete", true);
                    intent.putExtra("img_uri", mImgUri);
                    sendBroadcast(intent);
                    finish();
                }
            }else if(id == R.id.add_img){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/");
                startActivityForResult(intent, REQUEST_IMAGE);
            }else if(id == R.id.take_photo){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                        openCamera();
                    }else{
                        ActivityCompat.requestPermissions(this, new String []{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                    }
                }
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
                Glide.with(this)
                        .load(mImgUri)
                        .error(R.drawable.reload)
                        .into(mImageView);
            }
        }else if(requestCode == REQUEST_CAMERA){
            if(resultCode == RESULT_OK)
            {
                Glide.with(this)
                        .load(mPhotoPath)
                        .error(R.drawable.reload)
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
            if(mPhotoPath != null)
            {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                mImgUri = contentUri.toString();
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
    public void openCamera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = getDateInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir =  getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            mPhotoPath = image.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this,  "com.example.wallnotes.fileprovider", image);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA){
            if(permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                openCamera();
            }else{
                Toast.makeText(this, "Necesitas aceptar los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}