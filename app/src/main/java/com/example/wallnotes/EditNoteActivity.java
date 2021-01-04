package com.example.wallnotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomappbar.BottomAppBar;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import static java.text.DateFormat.getDateInstance;

public class EditNoteActivity extends AppCompatActivity {
    private final int REQUEST_IMAGE = 0;
    private final int REQUEST_CAMERA = 1;
    private final int REQUEST_RECORD_AUDIO = 2;
    private TextView mTitle;
    private TextView mContent;
    private ImageView mImageView;
    private boolean mUpdate = false;
    private Note mCurrNote = null;
    private String mImgUri = null;
    private String mPhotoPath = null;
    private NoteRepository mNoteRepository;
    private Date mRemindDate = null;
    private TextView mTv = null;
    private CardView mCv = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = findViewById(R.id.note_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mTv = findViewById(R.id.remind_date);
        mCv = findViewById(R.id.card_vieww);
        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        bottomAppBar.setElevation(0);
        mNoteRepository = new NoteRepository(getApplication());
        mTitle = findViewById(R.id.title);
        mContent = findViewById(R.id.content);
        mImageView = findViewById(R.id.image);
        mContent.setMovementMethod(new ScrollingMovementMethod());
        if(savedInstanceState != null)
        {
            mImgUri = savedInstanceState.getString("img_uri");
            if(mImgUri != null)
            {
                loadImage(mImgUri);
            }
        }
        Bundle data = getIntent().getExtras();
        if(data != null){
            mCurrNote = mNoteRepository.getById(data.getInt("uid"));
            mRemindDate = mCurrNote.getRemindDate();
            mTitle.setText(mCurrNote.getTitle());
            mContent.setText(mCurrNote.getContent());
            if(mCurrNote.getImgUri() != null)
            {
                loadImage(mCurrNote.getImgUri());
                mImgUri = mCurrNote.getImgUri();
            }
            if(mRemindDate != null)
            {
                setRemindDate();
            }
            mUpdate = true;
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if(id == R.id.delete){
                if(mUpdate)
                {
                    mCurrNote.setGoingToBeDeleted(true);
                    mNoteRepository.update(mCurrNote);
                    finish();
                }
            }else if(id == R.id.add_img){
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE);
                mPhotoPath = null;

            }else if(id == R.id.take_photo){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                        openCamera();
                    }else{
                        ActivityCompat.requestPermissions(this, new String []{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                    }
                }
            }else if(id == R.id.cancel_alarm)
            {
                if(mCurrNote != null)
                {
                    if(mCurrNote.getRemindDate() != null)
                    {
                        cancelAlarm();
                        mRemindDate = null;
                        mCv.setVisibility(View.GONE);
                        mTv.setText("");
                    }else {
                        Utils.showMessage(this, "Esta nota no tiene recordatorio");
                    }
                }else {
                    Utils.showMessage(this, "Acción denegada");
                }
            }else if(id == R.id.add_audio)
            {
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    openRecordDialog();
                }else{
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO);
                }
            }
            return false;
        });
        mImageView.setOnClickListener(v -> {
            if(mImgUri != null)
            {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(mImgUri), "image/");
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("img_uri", mImgUri);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_IMAGE:
                if(resultCode == RESULT_OK && data != null){
                    mImgUri = data.getDataString();
                    loadImage(mImgUri);
                }
                break;
            case REQUEST_CAMERA:
                if(resultCode == RESULT_OK)
                {
                    loadImage(mPhotoPath);
                    mImgUri = mPhotoPath;
                }
                break;
            default:
                break;
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
                if(mRemindDate == null)
                {
                    createAlarm();
                }else {
                    Utils.showMessage(this, "Esta nota ya tiene un recordatorio");
                }
            }else{
                Utils.showMessage(this, "Debes agregar esta nota primero");
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        updateOrSaveNote();
        finish();
        return super.onSupportNavigateUp();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA){
            if(permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                openCamera();
            }else{
                Utils.showMessage(this, "Debes aceptar los permisos");
            }
        }else if(requestCode == REQUEST_RECORD_AUDIO)
        {
            if(permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
               openRecordDialog();
            }else{
                Utils.showMessage(this, "Debes aceptar los permisos");
            }
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        updateOrSaveNote();
    }
    void updateOrSaveNote()
    {
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
                mCurrNote.setRemindDate(mRemindDate);
                mNoteRepository.update(mCurrNote);
            }else{
                note.setImgUri(mImgUri);
                mNoteRepository.addNote(note);
            }
        }
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
    public void setRemindDate(){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy hh:mm");
        String t = "Recordatorio: " + dateFor.format(mRemindDate);
        mTv.setText(t);
        mCv.setVisibility(View.VISIBLE);
    }
    public void loadImage(String uri)
    {
        Glide.with(this)
                .load(uri)
                .error(R.drawable.reload)
                .into(mImageView);
    }
    void cancelAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(getApplicationContext(), NotifierAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), mCurrNote.getUid(), myIntent, 0);
        alarmManager.cancel(pendingIntent);
        mRemindDate = null;
    }
    void createAlarm(){
        final Calendar newCalender = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(EditNoteActivity.this, (view, year, month, dayOfMonth) -> {
            final Calendar newDate = Calendar.getInstance();
            Calendar newTime = Calendar.getInstance();
            TimePickerDialog time = new TimePickerDialog(EditNoteActivity.this, (view1, hourOfDay, minute) -> {
                newDate.set(year,month,dayOfMonth,hourOfDay,minute,0);
                Calendar tem = Calendar.getInstance();
                if(newDate.getTimeInMillis() - tem.getTimeInMillis() > 0){
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(newDate.getTime());
                    calendar.set(Calendar.SECOND, 0);
                    Intent intent = new Intent(this, NotifierAlarm.class);
                    intent.putExtra("uid", mCurrNote.getUid());
                    PendingIntent intent1 = PendingIntent.getBroadcast(this, mCurrNote.getUid(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
                    alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), intent1);
                    mRemindDate = calendar.getTime();
                    mCurrNote.setRemindDate(mRemindDate);
                    setRemindDate();
                    Utils.showMessage(this, "Recordatorio agregado");
                }else{
                    Utils.showMessage(this, "Tiempo inválido");
                }
            },newTime.get(Calendar.HOUR_OF_DAY),newTime.get(Calendar.MINUTE),true);
            time.show();
        },newCalender.get(Calendar.YEAR),newCalender.get(Calendar.MONTH),newCalender.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }
    void openRecordDialog()
    {
        RecordDialog recordDialog = RecordDialog.newInstance("Record Audio");
        recordDialog.setMessage("Presiona para grabar");
        recordDialog.show(getSupportFragmentManager(), "TAG");
        recordDialog.setPositiveButton("Guardar", System.out::println);
    }
}