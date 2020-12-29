package com.example.wallnotes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private NoteRepository mNoteRepository;
    public void loadImage(String uri)
    {
        Glide.with(this)
                .load(uri)
                .error(R.drawable.reload)
                .into(mImageView);
    }
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
        mNoteRepository = new NoteRepository(getApplication());
        mTitle = findViewById(R.id.title);
        mContent = findViewById(R.id.content);
        mImageView = findViewById(R.id.image);
        mContent.setMovementMethod(new ScrollingMovementMethod());
        Bundle data = getIntent().getExtras();
        if(data != null){
            mCurrNote = new Note(data.getString("title"), data.getString("content"), null);
            mCurrNote.setUid(data.getInt("uid"));
            mCurrNote.setImgUri(data.getString("img_uri"));
            mCurrNote.setCreatedAt((Date) data.getSerializable("created_at"));
            mUpdate = true;
            mTitle.setText(mCurrNote.getTitle());
            mContent.setText(mCurrNote.getContent());
            if(mCurrNote.getImgUri() != null)
            {
                loadImage(mCurrNote.getImgUri());
                mImgUri = mCurrNote.getImgUri();
            }
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if(id == R.id.delete){
                if(mUpdate)
                {
                    mCurrNote.setGoingToBeDeleted(true);
                    mNoteRepository.delete(mCurrNote);
                    finish();
                }
            }else if(id == R.id.add_img){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/");
                mPhotoPath = null;
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
                loadImage(mImgUri);
            }
        }else if(requestCode == REQUEST_CAMERA){
            if(resultCode == RESULT_OK)
            {
                loadImage(mPhotoPath);
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
        if(item.getItemId() == R.id.remind) {
            if(mCurrNote != null){
                final Calendar newCalender = Calendar.getInstance();
                DatePickerDialog dialog = new DatePickerDialog(EditNoteActivity.this, (view, year, month, dayOfMonth) -> {
                    final Calendar newDate = Calendar.getInstance();
                    Calendar newTime = Calendar.getInstance();
                    TimePickerDialog time = new TimePickerDialog(EditNoteActivity.this, (view1, hourOfDay, minute) -> {
                        newDate.set(year,month,dayOfMonth,hourOfDay,minute,0);
                        Calendar tem = Calendar.getInstance();
                        if(newDate.getTimeInMillis() - tem.getTimeInMillis() > 0){
                            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                            calendar.setTime(newDate.getTime());
                            calendar.set(Calendar.SECOND, 0);
                            Intent intent = new Intent(this, NotifierAlarm.class);
                            intent.putExtra("title", mCurrNote.getTitle());
                            intent.putExtra("content", mCurrNote.getContent());
                            intent.putExtra("id", mCurrNote.getUid());
                            PendingIntent intent1 = PendingIntent.getBroadcast(this, mCurrNote.getUid(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), intent1);
                            Toast.makeText(this,"Recordatorio agregado",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(EditNoteActivity.this,"Tiempo inválido",Toast.LENGTH_SHORT).show();
                        }
                    },newTime.get(Calendar.HOUR_OF_DAY),newTime.get(Calendar.MINUTE),true);
                    time.show();
                },newCalender.get(Calendar.YEAR),newCalender.get(Calendar.MONTH),newCalender.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(System.currentTimeMillis());
                dialog.show();
            }else{
                Toast.makeText(this,"Necesitas agregar esta nota primero",Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        String title = mTitle.getText().toString();
        String content = mContent.getText().toString();
        if(title.length() > 0){
            Note note = new Note(title, content, null);
            if(mPhotoPath != null)
            {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                mImgUri = contentUri.toString();
            }
            if(mUpdate){
                mCurrNote.setContent(content);
                mCurrNote.setTitle(title);
                mCurrNote.setImgUri(mImgUri);
                mNoteRepository.update(mCurrNote);
            }else{
                note.setImgUri(mImgUri);
                mNoteRepository.addNote(note);
            }
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