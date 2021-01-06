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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomappbar.BottomAppBar;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static java.text.DateFormat.getDateInstance;

public class EditNoteActivity extends AppCompatActivity {
    static MediaPlayer mPlayer = null;
    private SeekBar mProgress;
    private final int REQUEST_IMAGE = 0;
    private final int REQUEST_CAMERA = 1;
    private final int REQUEST_RECORD_AUDIO = 2;
    private final int REQUEST_GPS = 3;
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
    private TextView mTvLocation = null;
    private String mAudioPath = null;
    private LinearLayout mLinearLayout = null;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        mProgress = findViewById(R.id.sbProgress);
        Handler updateHandler = new Handler();
        Toolbar toolbar = findViewById(R.id.note_toolbar);
        ImageButton btnPlay = findViewById(R.id.play);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mTv = findViewById(R.id.remind_date);
        mTvLocation = findViewById(R.id.location);
        mLinearLayout = findViewById(R.id.player);
        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        bottomAppBar.setElevation(0);
        mNoteRepository = new NoteRepository(getApplication());
        mTitle = findViewById(R.id.title);
        mContent = findViewById(R.id.content);
        mImageView = findViewById(R.id.image);
        mContent.setMovementMethod(new ScrollingMovementMethod());
        if (savedInstanceState != null) {
            mImgUri = savedInstanceState.getString("img_uri");
            mLocation = savedInstanceState.getString("location");
            mAudioPath = savedInstanceState.getString("audio");
            if (mImgUri != null) {
                loadImage(mImgUri);
            }
            if(mLocation != null)
            {
                mTvLocation.setVisibility(View.VISIBLE);
                mTvLocation.setText(mLocation);
            }
            if(mAudioPath != null)
            {
                mLinearLayout.setVisibility(View.VISIBLE);
            }
        }
        Bundle data = getIntent().getExtras();
        if (data != null) {
            mCurrNote = mNoteRepository.getById(data.getInt("uid"));
            mRemindDate = mCurrNote.getRemindDate();
            mTitle.setText(mCurrNote.getTitle());
            mContent.setText(mCurrNote.getContent());
            if (mCurrNote.getAudio() != null) {
                mAudioPath = mCurrNote.getAudio();
                mLinearLayout.setVisibility(View.VISIBLE);
            }
            if (mCurrNote.getImgUri() != null) {
                loadImage(mCurrNote.getImgUri());
                mImgUri = mCurrNote.getImgUri();
            }
            if(mCurrNote.getLocation() != null)
            {
                mLocation = mCurrNote.getLocation();
                mTvLocation.setVisibility(View.VISIBLE);
                mTvLocation.setText(mLocation);
            }
            if (mRemindDate != null) {
                setRemindDate();
            }
            mUpdate = true;
        }
        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.delete) {
                if (mUpdate) {
                    if (mRemindDate != null) {
                        cancelAlarm();
                    }
                    mCurrNote.setGoingToBeDeleted(true);
                    mNoteRepository.update(mCurrNote);
                    finish();
                }
            } else if (id == R.id.add_img) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE);
                mPhotoPath = null;

            } else if (id == R.id.take_photo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        openCamera();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                    }
                }
            } else if (id == R.id.cancel_alarm) {
                if (mCurrNote != null) {
                    if (mCurrNote.getRemindDate() != null) {
                        cancelAlarm();
                        mRemindDate = null;
                        mTv.setVisibility(View.GONE);
                        mTv.setText("");
                    } else {
                        Utils.showMessage(this, "Esta nota no tiene recordatorio");
                    }
                } else {
                    Utils.showMessage(this, "Acción denegada");
                }
            } else if (id == R.id.add_audio) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openRecordDialog();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO);
                }
            } else if (id == R.id.add_pos) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    getPos();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GPS);
                }
            }
            return false;
        });
        mImageView.setOnClickListener(v -> {
            if (mImgUri != null) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(mImgUri), "image/");
                startActivity(intent);
            }
        });
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(mp -> mPlayer.reset());
        mPlayer.setOnPreparedListener(mediaPlayer -> {
            mProgress.setMax(mediaPlayer.getDuration() / 1000);
            mediaPlayer.start();
        });
        btnPlay.setOnClickListener(v -> {
            if (mAudioPath != null) {
                playAudio(mAudioPath);
            }
        });
        mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer != null && fromUser) {
                    mPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        EditNoteActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if (mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mProgress.setProgress(mPlayer.getCurrentPosition() / 1000);
                    }
                }
                updateHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("img_uri", mImgUri);
        outState.putString("location", mLocation);
        outState.putString("audio", mAudioPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    mImgUri = data.getDataString();
                    loadImage(mImgUri);
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
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
        if (item.getItemId() == R.id.remind) {
            if (mCurrNote != null) {
                if (mRemindDate == null) {
                    createAlarm();
                } else {
                    Utils.showMessage(this, "Esta nota ya tiene un recordatorio");
                }
            } else {
                Utils.showMessage(this, "Debes agregar esta nota primero");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
                mPlayer.release();
            }
            mPlayer = null;
        }
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
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Utils.showMessage(this, "Debes aceptar los permisos");
                }
                break;
            case REQUEST_RECORD_AUDIO:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openRecordDialog();
                } else {
                    Utils.showMessage(this, "Debes aceptar los permisos");
                }
                break;
            case REQUEST_GPS:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPos();
                } else {
                    Utils.showMessage(this, "Debes aceptar los permisos");
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        updateOrSaveNote();
    }

    void updateOrSaveNote() {
        String title = mTitle.getText().toString();
        String content = mContent.getText().toString();
        if (title.length() > 0) {
            Note note = new Note(title, content, null);
            note.setAudio(mAudioPath);
            note.setLocation(mLocation);
            if (mPhotoPath != null) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                mImgUri = contentUri.toString();
            }
            if (mUpdate) {
                mCurrNote.setContent(content);
                mCurrNote.setTitle(title);
                mCurrNote.setImgUri(mImgUri);
                mCurrNote.setRemindDate(mRemindDate);
                mCurrNote.setAudio(mAudioPath);
                mCurrNote.setLocation(mLocation);
                mNoteRepository.update(mCurrNote);
            } else {
                note.setImgUri(mImgUri);
                mNoteRepository.addNote(note);
            }
        }
    }

    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = getDateInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            mPhotoPath = image.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this, "com.example.wallnotes.fileprovider", image);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRemindDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy hh:mm");
        String t = "Recordatorio: " + dateFor.format(mRemindDate);
        mTv.setVisibility(View.VISIBLE);
        mTv.setText(t);
    }

    public void loadImage(String uri) {
        Glide.with(this)
                .load(uri)
                .error(R.drawable.reload)
                .into(mImageView);
    }

    void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(getApplicationContext(), NotifierAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), mCurrNote.getUid(), myIntent, 0);
        alarmManager.cancel(pendingIntent);
        mRemindDate = null;
    }

    void createAlarm() {
        final Calendar newCalender = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(EditNoteActivity.this, (view, year, month, dayOfMonth) -> {
            final Calendar newDate = Calendar.getInstance();
            Calendar newTime = Calendar.getInstance();
            TimePickerDialog time = new TimePickerDialog(EditNoteActivity.this, (view1, hourOfDay, minute) -> {
                newDate.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                Calendar tem = Calendar.getInstance();
                if (newDate.getTimeInMillis() - tem.getTimeInMillis() > 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(newDate.getTime());
                    calendar.set(Calendar.SECOND, 0);
                    Intent intent = new Intent(this, NotifierAlarm.class);
                    intent.putExtra("uid", mCurrNote.getUid());
                    PendingIntent intent1 = PendingIntent.getBroadcast(this, mCurrNote.getUid(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), intent1);
                    mRemindDate = calendar.getTime();
                    mCurrNote.setRemindDate(mRemindDate);
                    setRemindDate();
                    Utils.showMessage(this, "Recordatorio agregado");
                } else {
                    Utils.showMessage(this, "Tiempo inválido");
                }
            }, newTime.get(Calendar.HOUR_OF_DAY), newTime.get(Calendar.MINUTE), true);
            time.show();
        }, newCalender.get(Calendar.YEAR), newCalender.get(Calendar.MONTH), newCalender.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    void openRecordDialog() {
        RecordDialog recordDialog = RecordDialog.newInstance("Record Audio");
        recordDialog.setMessage("Presiona para grabar");
        recordDialog.show(getSupportFragmentManager(), "TAG");
        recordDialog.setPositiveButton("Guardar", path -> {
            if (mLinearLayout.getVisibility() == View.GONE) {
                mLinearLayout.setVisibility(View.VISIBLE);
            }
            mAudioPath = path;
        });
    }

    void playAudio(String path) {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
            mProgress.setProgress(0);
            mPlayer.reset();
        }
        Uri mediaUri = Uri.fromFile(new File(path));
        try {
            mPlayer.setDataSource(getBaseContext(), mediaUri);
            mProgress.setProgress(0);
            mPlayer.prepare();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void getPos() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double altitude = location.getAltitude();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geo.getFromLocation(latitude, longitude, 1);
                    if(addresses.size() > 0)
                    {
                        mLocation = addresses.get(0).getFeatureName() + ", " + addresses.get(0).getLocality() +", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                        mTvLocation.setVisibility(View.VISIBLE);
                        mTvLocation.setText(mLocation);
                    }else{
                        mTvLocation.setVisibility(View.VISIBLE);
                        mTvLocation.setText("Cargando ubicación...");
                    }
                } catch (IOException e) {
                    Utils.showMessage(EditNoteActivity.this, "Ha ocurrido un error al obtener su posición");
                    e.printStackTrace();
                }
            }
            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {

                Utils.showMessage(EditNoteActivity.this, "Por favor, active el GPS y el internet");
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }
}