package com.example.wallnotes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

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
    private String mImgUri = null; // URI of the image selected (gallery or camera)
    private String mPhotoPath = null; // Absolute path for photos taken with camera
    private NoteRepository mNoteRepository;
    private Date mRemindDate = null;
    private TextView mTv = null;
    private TextView mTvLocation = null;
    private String mAudioPath = null;
    private LinearLayout mLinearLayout = null;
    private String mLocation;
    boolean mPause = false;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS = 102; // For returning from settings
    private static final String TAG_PERMISSION = "EditNoteActivityPerm";
    private LiveData<Note> mCurrentNoteLiveData;
    private int mCurrentNoteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NoteViewModel mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPreferences.getString("theme", "");
        mLocation = null;
        switch (theme) {
            case "morado":
                getTheme().applyStyle(R.style.morado, true);
                break;
            case "verde":
                getTheme().applyStyle(R.style.verde, true);
                break;
            case "azul":
                getTheme().applyStyle(R.style.azul, true);
                break;
            case "oscuro":
                getTheme().applyStyle(R.style.oscuro, true);
                break;
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_edit_note);
        mProgress = findViewById(R.id.sbProgress);
        Handler updateHandler = new Handler(Looper.getMainLooper());
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
            mPhotoPath = savedInstanceState.getString("photo_path"); // Restore photo path

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
            mCurrentNoteId = data.getInt("uid", -1);
            if (mCurrentNoteId != -1) {
                // Use LiveData for observing changes
                mCurrentNoteLiveData = mNoteViewModel.getNoteByIdLiveData(mCurrentNoteId);
                observeNote(); // This will populate mCurrNote and UI fields
            } else {
                // This case implies a new note, so mCurrNote might remain null until saved.
                // Initialize mCurrNote to a new Note object or handle null appropriately.
                mCurrNote = new Note("", "", null); // Or however you handle new notes
                mUpdate = false;
            }
        } else {
            // New note if no data
            mCurrNote = new Note("", "", null);
            mUpdate = false;
        }


        bottomAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.delete) {
                if (mUpdate && mCurrNote != null && mCurrNote.getUid() > 0) { // Ensure note is valid and saved
                    if (mRemindDate != null) {
                        cancelAlarm();
                    }
                    mCurrNote.setGoingToBeDeleted(true);
                    mNoteRepository.update(mCurrNote);
                    finish();
                } else {
                    // If it's a new note not yet saved, just finish
                    finish();
                }
            } else if (id == R.id.add_img) {
                mPhotoPath = null; // Clear photopath when selecting from gallery
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE);

            } else if (id == R.id.take_photo) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
            } else if (id == R.id.cancel_alarm) {
                if (mCurrNote != null && mCurrNote.getUid() > 0) { // Check if note is valid
                    if (mCurrNote.getRemindDate() != null) {
                        cancelAlarm(); // This will set mRemindDate to null
                        setRemindDate(); // Update UI
                        // Persist this change
                        if (mUpdate) {
                            mCurrNote.setRemindDate(null);
                            mNoteRepository.update(mCurrNote);
                        }
                        Utils.showMessage(this, "Recordatorio cancelado");
                    } else {
                        Utils.showMessage(this, "Esta nota no tiene recordatorio");
                    }
                } else {
                    Utils.showMessage(this, "Guarda la nota primero para gestionar recordatorios");
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

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(mp -> {
            mPlayer.reset();
            mPause = false;
        });
        mPlayer.setOnPreparedListener(mediaPlayer -> {
            mProgress.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
        });
        btnPlay.setOnClickListener(v -> {
            if (mAudioPath != null) {
                if(mPlayer.isPlaying())
                {
                    mPlayer.pause();
                    mPause = true;
                }else if(mPause){
                    mPlayer.start();
                    mPause = false;
                }else{
                    playAudio(mAudioPath);
                }
            }
        });
        mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer != null && fromUser) {
                    // Assuming progress is now in milliseconds, not seconds
                    mPlayer.seekTo(progress);
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
                        mProgress.setProgress(mPlayer.getCurrentPosition());
                    }
                }
                updateHandler.postDelayed(this, 250);
            }
        });
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("img_uri", mImgUri);
        outState.putString("location", mLocation);
        outState.putString("audio", mAudioPath);
        outState.putString("photo_path", mPhotoPath); // Save photo path
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
                        mImgUri = selectedImageUri.toString();
                        mPhotoPath = null; // Not a camera photo
                        loadImage(mImgUri);
                    } catch (SecurityException e) {
                        Toast.makeText(this, "No se pudo obtener permiso para la imagen.", Toast.LENGTH_SHORT).show();
                        mImgUri = (mCurrNote != null) ? mCurrNote.getImgUri() : null; // Revert
                        loadImage(mImgUri);
                    }
                } else { // Gallery selection cancelled or failed
                    mImgUri = (mCurrNote != null) ? mCurrNote.getImgUri() : null; // Revert to original or null
                    loadImage(mImgUri); // Refresh view
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (mPhotoPath != null) {
                        File imageFile = new File(mPhotoPath);
                        if (imageFile.exists() && imageFile.length() > 0) { // Check if file is valid
                            Uri photoFileUri = FileProvider.getUriForFile(this,
                                    "com.example.wallnotes.fileprovider",
                                    imageFile);
                            mImgUri = photoFileUri.toString(); // This is the URI for Glide
                            loadImage(mImgUri);
                        } else {
                            Toast.makeText(this, "Error al cargar la foto tomada.", Toast.LENGTH_SHORT).show();
                            mPhotoPath = null; // Invalidate path
                            mImgUri = (mCurrNote != null) ? mCurrNote.getImgUri() : null; // Revert
                            loadImage(mImgUri);
                        }
                    } else {
                        Toast.makeText(this, "Error al procesar la foto tomada (ruta no disponible).", Toast.LENGTH_SHORT).show();
                        mPhotoPath = null; // Invalidate path
                        mImgUri = (mCurrNote != null) ? mCurrNote.getImgUri() : null; // Revert
                        loadImage(mImgUri);
                    }
                } else { // Camera cancelled or failed
                    mPhotoPath = null; // Critically, nullify mPhotoPath if camera was cancelled
                    // Revert mImgUri to what it was before attempting camera, or null if new note.
                    // If mCurrNote exists and had an image, mImgUri should reflect that.
                    // If mCurrNote is new or had no image, mImgUri should be null.
                    if (mCurrNote != null && mCurrNote.getImgUri() != null) {
                        mImgUri = mCurrNote.getImgUri();
                    } else {
                        mImgUri = null;
                    }
                    loadImage(mImgUri); // Refresh ImageView
                    Toast.makeText(this, "Captura de foto cancelada.", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS:
                // User has returned from exact alarm settings.
                // Re-check and potentially show date/time pickers if they click "Remind" again.
                // Or, you could try to directly proceed if you stashed the intent.
                // For now, let them click "Remind" again.
                Log.d(TAG_PERMISSION, "Returned from Exact Alarm settings. User should try setting reminder again.");
                Toast.makeText(this, "Por favor, intente establecer el recordatorio de nuevo.", Toast.LENGTH_LONG).show();
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
            if (mCurrNote != null) { // Ensure mCurrNote is initialized
                // Allow modifying existing reminder or setting a new one
                checkPermissionsAndThenCreateAlarmFlow();
            } else {
                // This should ideally not happen if mCurrNote is initialized in onCreate
                Utils.showMessage(this, "Error: La nota no está disponible. Intente de nuevo.");
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
            }
            mPlayer.release();
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
    public void onBackPressed() {
        updateOrSaveNote();
        super.onBackPressed();
    }

    void updateOrSaveNote() {
        String title = mTitle.getText().toString();
        String content = mContent.getText().toString();

        if (title.isEmpty() && content.isEmpty() && mImgUri == null && mAudioPath == null && mLocation == null) {
            // If the note is completely empty and it's a new note (not an update of an existing one)
            // or an existing note is made completely empty, consider not saving or deleting it.
            // For simplicity, we'll save it if it was an update, or not save if new and empty.
            if (mUpdate && mCurrNote != null && mCurrNote.getUid() > 0) {
                // If it was an update, and it's now empty, maybe mark for deletion or save as empty.
                // Current logic will save it as empty.
            } else {
                Log.d("EditNoteActivity", "Not saving empty new note.");
                return; // Don't save an entirely empty new note
            }
        }


        // Ensure mCurrNote is not null if we are updating
        if (mUpdate && mCurrNote == null) {
            Log.e("EditNoteActivity", "Attempting to update a null mCurrNote. This should not happen.");
            // Potentially fetch the note again or handle error
            if (mCurrentNoteId != -1) {
                mCurrNote = mNoteRepository.getById(mCurrentNoteId); // Try to recover
                if (mCurrNote == null) {
                    Utils.showMessage(this, "Error al guardar: no se encontró la nota original.");
                    return;
                }
            } else {
                Utils.showMessage(this, "Error al guardar: ID de nota inválido.");
                return;
            }
        }


        if (mUpdate) { // Existing note
            mCurrNote.setTitle(title);
            mCurrNote.setContent(content);
            mCurrNote.setImgUri(mImgUri);
            mCurrNote.setRemindDate(mRemindDate);
            mCurrNote.setAudio(mAudioPath);
            mCurrNote.setLocation(mLocation);
            mNoteRepository.update(mCurrNote);
        } else { // New note
            if (!title.isEmpty() || !content.isEmpty() || mImgUri != null || mAudioPath != null || mLocation != null) {
                Note newNote = new Note(title, content, mImgUri);
                newNote.setImgUri(mImgUri);
                newNote.setAudio(mAudioPath);
                newNote.setLocation(mLocation);
                mNoteRepository.addNote(newNote);
                // After adding, we might want to get the UID for future operations if the user stays on screen
                // This part is tricky without observing the insertion result.
                // For now, we assume the activity will be finished or reloaded for an existing note.
            }
        }

        // Scan file if a new photo was taken via camera
        if (mPhotoPath != null && mImgUri != null && new File(mPhotoPath).exists()) {
            // Check if mImgUri corresponds to mPhotoPath (i.e., it's a FileProvider URI)
            if (mImgUri.startsWith("content://") && mImgUri.contains("com.example.wallnotes.fileprovider")) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mPhotoPath);
                Uri photoFileUriForScanner = Uri.fromFile(f); // Use file URI for media scanner
                mediaScanIntent.setData(photoFileUriForScanner);
                sendBroadcast(mediaScanIntent);
                Log.d("EditNoteActivity", "Media scanner broadcast sent for: " + mPhotoPath);
            }
        }
    }

    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            mPhotoPath = photoFile.getAbsolutePath(); // Save file path
        } catch (IOException ex) {
            Log.e("EditNoteActivity", "Error creating image file", ex);
            Toast.makeText(this, "Error al crear archivo de imagen.", Toast.LENGTH_SHORT).show();
            mPhotoPath = null; // Ensure path is null on error
            return;
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.wallnotes.fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private void checkPermissionsAndThenCreateAlarmFlow() {
        if (mCurrNote == null || (mUpdate && mCurrNote.getUid() == 0 && mCurrentNoteId == -1) ) {
            Utils.showMessage(this, "Guarda la nota primero para agregar un recordatorio.");
            // Prompt to save the note first if it's a new, unsaved note
            new AlertDialog.Builder(this)
                    .setTitle("Guardar Nota")
                    .setMessage("Debes guardar la nota antes de poder agregar un recordatorio. ¿Deseas guardarla ahora?")
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        updateOrSaveNote();
                        // After saving, mCurrNote should be updated or a new ID available.
                        // This is complex as addNote is async. For now, tell user to try again.
                        if (mCurrNote != null && mCurrNote.getUid() > 0) {
                            Log.d(TAG_PERMISSION, "Nota guardada, procediendo con permisos de recordatorio.");
                            proceedWithNotificationPermissionCheck();
                        } else {
                            // If it's a new note, its ID isn't immediately available here without more complex handling.
                            // Fetching it or observing LiveData would be needed.
                            Toast.makeText(EditNoteActivity.this, "Nota guardada. Por favor, presiona 'Recordatorio' de nuevo.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        proceedWithNotificationPermissionCheck();
    }

    private void proceedWithNotificationPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_PERMISSION, "POST_NOTIFICATIONS permission already granted.");
                proceedWithExactAlarmPermissionCheck();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permiso de Notificación Necesario")
                        .setMessage("WallNotes necesita mostrar notificaciones para los recordatorios. Por favor, concede el permiso.")
                        .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(EditNoteActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS))
                        .setNegativeButton("Cancelar", (dialog, which) -> Toast.makeText(this, "Permiso de notificación denegado. No se pueden crear recordatorios.", Toast.LENGTH_SHORT).show())
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        } else {
            // Pre-Tiramisu, no runtime permission for notifications needed
            Log.d(TAG_PERMISSION, "Pre-Tiramisu: No POST_NOTIFICATIONS runtime permission needed.");
            proceedWithExactAlarmPermissionCheck();
        }
    }

    private void proceedWithExactAlarmPermissionCheck() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Utils.showMessage(this, "No se pudo acceder al servicio de alarmas.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(TAG_PERMISSION, "SCHEDULE_EXACT_ALARM permission granted.");
                createAlarm(); // All permissions good, show date/time picker
            } else {
                Log.d(TAG_PERMISSION, "SCHEDULE_EXACT_ALARM permission NOT granted. Showing dialog.");
                new AlertDialog.Builder(this)
                        .setTitle("Permiso para Alarmas Exactas Necesario")
                        .setMessage("Para asegurar que los recordatorios funcionen correctamente, WallNotes necesita permiso para programar alarmas exactas. Por favor, activa este permiso en la siguiente pantalla.")
                        .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            // Optionally, direct to your app's specific settings
                            // intent.setData(Uri.parse("package:" + getPackageName()));
                            try {
                                startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM_SETTINGS);
                            } catch (Exception e) {
                                Log.e(TAG_PERMISSION, "Error starting ACTION_REQUEST_SCHEDULE_EXACT_ALARM", e);
                                Toast.makeText(this, "No se pudo abrir la configuración. Habilita 'Alarmas y recordatorios' manualmente para WallNotes.", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> Toast.makeText(this, "Permiso para alarmas exactas denegado. No se pueden crear recordatorios precisos.", Toast.LENGTH_SHORT).show())
                        .show();
            }
        } else {
            // Pre-S, no special permission for exact alarms needed beyond manifest
            Log.d(TAG_PERMISSION, "Pre-S: No SCHEDULE_EXACT_ALARM runtime permission needed.");
            createAlarm(); // Show date/time picker
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Utils.showMessage(this, "Permisos de cámara denegados");
                }
                break;
            case REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        (permissions.length < 2 || grantResults[1] == PackageManager.PERMISSION_GRANTED)) { // Check both if requested
                    openRecordDialog();
                } else {
                    Utils.showMessage(this, "Debes aceptar los permisos de audio y almacenamiento");
                }
                break;
            case REQUEST_GPS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPos();
                } else {
                    Utils.showMessage(this, "Debes aceptar los permisos de ubicación");
                }
                break;
            case REQUEST_CODE_POST_NOTIFICATIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG_PERMISSION, "POST_NOTIFICATIONS permission granted by user.");
                    proceedWithExactAlarmPermissionCheck(); // Notification perm granted, now check exact alarm
                } else {
                    Log.w(TAG_PERMISSION, "POST_NOTIFICATIONS permission denied by user.");
                    Toast.makeText(this, "Permiso de notificación denegado. Los recordatorios no se mostrarán.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    private void setRemindDate() {
        if (mRemindDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            mTv.setText(String.format("Recordatorio %s", dateFormat.format(mRemindDate)));
            mTv.setVisibility(View.VISIBLE);
        } else {
            mTv.setText("");
            mTv.setVisibility(View.GONE);
        }
    }
    public void loadImage(String uri) {
        if (uri != null && !uri.isEmpty()) {
            mImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(Uri.parse(uri)) // Ensure it's parsed to Uri if it's a string
                    .error(R.drawable.reload) // Shown if Glide fails to load the valid URI
                    .into(mImageView);
        } else {
            mImageView.setImageDrawable(null); // Clear current image
            mImageView.setVisibility(View.GONE); // Hide if no image
        }
    }

    void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || mCurrNote == null || mCurrNote.getUid() == 0) {
            Log.e("EditNoteActivity", "Cannot cancel alarm: AlarmManager null, mCurrNote null, or note not saved.");
            if (mCurrNote != null && mCurrNote.getUid() == 0) { // If note exists but not saved
                // This implies it's a new note, so no alarm to cancel anyway
            }
            return;
        }

        Intent myIntent = new Intent(getApplicationContext(), NotifierAlarm.class);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // FLAG_IMMUTABLE from API 23 with S, but safer here
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                mCurrNote.getUid(),
                myIntent,
                flags
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d("EditNoteActivity", "Alarm cancelled for note UID: " + mCurrNote.getUid());
        } else {
            Log.d("EditNoteActivity", "No alarm found to cancel for note UID: " + mCurrNote.getUid());
        }
        mRemindDate = null; // Clear the remind date model
        // UI update will be handled by setRemindDate() called after this
    }

    void createAlarm() {
        // This method is called AFTER all necessary permissions are confirmed.

        if (mCurrNote == null || (mUpdate && mCurrNote.getUid() == 0 && mCurrentNoteId == -1)) {
            Utils.showMessage(this, "La nota debe guardarse primero para establecer un recordatorio.");
            // This check is mostly redundant now due to checks in checkPermissionsAndThenCreateAlarmFlow
            // but kept as a safeguard.
            return;
        }


        MaterialDatePicker.Builder<Long> datePickerBuilder = MaterialDatePicker.Builder.datePicker();
        datePickerBuilder.setTitleText("Seleccionar Fecha");

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());
        datePickerBuilder.setCalendarConstraints(constraintsBuilder.build());

        // Set initial selection to current reminder date if exists, else today
        long initialSelection = MaterialDatePicker.todayInUtcMilliseconds();
        if (mRemindDate != null) {
            Calendar initialCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            initialCal.setTime(mRemindDate);
            // Clear time part for date picker, MaterialDatePicker expects midnight UTC for date selection
            initialCal.set(Calendar.HOUR_OF_DAY, 0);
            initialCal.set(Calendar.MINUTE, 0);
            initialCal.set(Calendar.SECOND, 0);
            initialCal.set(Calendar.MILLISECOND, 0);
            initialSelection = initialCal.getTimeInMillis();
        }
        datePickerBuilder.setSelection(initialSelection);


        final MaterialDatePicker<Long> datePicker = datePickerBuilder.build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedDateCalendarUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedDateCalendarUtc.setTimeInMillis(selection);

            Calendar newDateTimeCalendar = Calendar.getInstance();
            newDateTimeCalendar.set(
                    selectedDateCalendarUtc.get(Calendar.YEAR),
                    selectedDateCalendarUtc.get(Calendar.MONTH),
                    selectedDateCalendarUtc.get(Calendar.DAY_OF_MONTH)
            );

            Calendar currentTime = Calendar.getInstance();
            if (mRemindDate != null) { // If modifying, prefill with old time
                Calendar existingTimeCal = Calendar.getInstance();
                existingTimeCal.setTime(mRemindDate);
                currentTime.set(Calendar.HOUR_OF_DAY, existingTimeCal.get(Calendar.HOUR_OF_DAY));
                currentTime.set(Calendar.MINUTE, existingTimeCal.get(Calendar.MINUTE));
            }


            MaterialTimePicker.Builder timePickerBuilder = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(currentTime.get(Calendar.HOUR_OF_DAY))
                    .setMinute(currentTime.get(Calendar.MINUTE))
                    .setTitleText("Seleccionar Hora");

            final MaterialTimePicker timePicker = timePickerBuilder.build();

            timePicker.addOnPositiveButtonClickListener(dialog -> {
                int hourOfDay = timePicker.getHour();
                int minute = timePicker.getMinute();

                newDateTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                newDateTimeCalendar.set(Calendar.MINUTE, minute);
                newDateTimeCalendar.set(Calendar.SECOND, 0);
                newDateTimeCalendar.set(Calendar.MILLISECOND, 0);

                Calendar nowCalendar = Calendar.getInstance();

                if (newDateTimeCalendar.getTimeInMillis() > nowCalendar.getTimeInMillis()) {
                    // Ensure mCurrNote has a valid UID. This should be true if we reached here
                    // due to the checks in checkPermissionsAndThenCreateAlarmFlow
                    if (mCurrNote == null || mCurrNote.getUid() == 0) {
                        if (mCurrentNoteId != -1) { // Try to recover if it's an existing note
                            mCurrNote = mNoteRepository.getById(mCurrentNoteId);
                        }
                        if (mCurrNote == null || mCurrNote.getUid() == 0) { // Still no valid UID
                            Log.e("CreateAlarm", "mCurrNote UID is 0 or mCurrNote is null. Cannot set alarm without saving note first.");
                            Utils.showMessage(EditNoteActivity.this, "Error: Guarda la nota primero para establecer el recordatorio.");
                            return;
                        }
                    }


                    Intent intent = new Intent(EditNoteActivity.this, NotifierAlarm.class);
                    intent.putExtra("uid", mCurrNote.getUid());
                    intent.putExtra("title", mCurrNote.getTitle()); // Pass title for notification

                    int flagsPi = PendingIntent.FLAG_UPDATE_CURRENT;

                    flagsPi |= PendingIntent.FLAG_IMMUTABLE;
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            EditNoteActivity.this,
                            mCurrNote.getUid(),
                            intent,
                            flagsPi
                    );

                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (alarmManager == null) {
                        Utils.showMessage(EditNoteActivity.this, "No se pudo acceder al servicio de alarmas.");
                        return;
                    }

                    // The canScheduleExactAlarms check is primarily done *before* showing this dialog flow.
                    // This is a final safeguard.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Utils.showMessage(EditNoteActivity.this, "Permiso para alarmas exactas no concedido. El recordatorio no se puede establecer.");
                        // Optionally re-prompt to go to settings, but user should have done this already.
                        return;
                    }

                    try {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, newDateTimeCalendar.getTimeInMillis(), pendingIntent);
                        mRemindDate = newDateTimeCalendar.getTime();
                        if (mCurrNote != null) { // mCurrNote should be valid here
                            mCurrNote.setRemindDate(mRemindDate);
                        }
                        setRemindDate(); // Update UI
                        Utils.showMessage(EditNoteActivity.this, "Recordatorio agregado/actualizado");
                        // Note: updateOrSaveNote() will persist this mRemindDate when activity closes/navigates up
                    } catch (SecurityException se) {
                        Log.e("CreateAlarm", "SecurityException setting exact alarm. Missing SCHEDULE_EXACT_ALARM?", se);
                        Utils.showMessage(EditNoteActivity.this, "Error de seguridad al establecer la alarma. Asegúrate de tener los permisos necesarios.");
                    }


                } else {
                    Utils.showMessage(EditNoteActivity.this, "Tiempo inválido. Por favor, selecciona una fecha y hora en el futuro.");
                }
            });
            timePicker.show(getSupportFragmentManager(), "MATERIAL_TIME_PICKER_TAG");
        });
        datePicker.addOnNegativeButtonClickListener(dialog -> Utils.showMessage(EditNoteActivity.this, "Selección de fecha cancelada."));
        datePicker.addOnCancelListener(dialog -> Utils.showMessage(EditNoteActivity.this, "Selección de fecha cerrada."));
        datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER_TAG");
    }

    private void observeNote() {
        if (mCurrentNoteLiveData == null) return;

        mCurrentNoteLiveData.observe(this, note -> {
            if (note != null) {
                mCurrNote = note; // This is the authoritative Note object
                mTitle.setText(note.getTitle());
                mContent.setText(note.getContent());
                mRemindDate = note.getRemindDate();
                mImgUri = note.getImgUri(); // Load image URI from note
                mAudioPath = note.getAudio();
                mLocation = note.getLocation();

                setRemindDate();
                loadImage(mImgUri); // Load image using the URI from the note

                if (mAudioPath != null) {
                    mLinearLayout.setVisibility(View.VISIBLE);
                } else {
                    mLinearLayout.setVisibility(View.GONE);
                }
                if (mLocation != null) {
                    mTvLocation.setVisibility(View.VISIBLE);
                    mTvLocation.setText(mLocation);
                } else {
                    mTvLocation.setVisibility(View.GONE);
                    mTvLocation.setText("");
                }
                mUpdate = true;
            } else {
                // Note might have been deleted from elsewhere
                Log.w("EditNoteActivity", "La nota con ID " + mCurrentNoteId + " ya no existe o no se pudo cargar.");
                Toast.makeText(this, "La nota ya no está disponible.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    void openRecordDialog() {
        RecordDialog recordDialog = RecordDialog.newInstance("Record Audio");
        recordDialog.setMessage("Presiona para grabar");
        recordDialog.show(getSupportFragmentManager(), "TAG_RECORD_DIALOG");
        recordDialog.setPositiveButton("Guardar", path -> {
            if (mLinearLayout.getVisibility() == View.GONE) {
                mLinearLayout.setVisibility(View.VISIBLE);
            }
            mAudioPath = path;
        });
    }

    void playAudio(String path) {
        if (mPlayer == null) mPlayer = new MediaPlayer(); // Re-initialize if released

        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        mPlayer.reset(); // Reset before setting new data source
        mProgress.setProgress(0);

        Uri mediaUri = Uri.fromFile(new File(path));
        try {
            mPlayer.setDataSource(getBaseContext(), mediaUri);
            mPlayer.prepareAsync(); // Asynchronous preparation
        } catch (IOException ex) {
            Log.e("EditNoteActivity", "Error setting data source for audio", ex);
            Toast.makeText(this, "Error al reproducir audio.", Toast.LENGTH_SHORT).show();
        }
    }

    void getPos() {
        mTvLocation.setVisibility(View.VISIBLE);
        mTvLocation.setText("Cargando ubicación...");
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            mTvLocation.setText("Servicio de ubicación no disponible.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mTvLocation.setText("Permiso de ubicación denegado.");
            return;
        }

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (location == null) {
                    mTvLocation.setText("No se pudo obtener la ubicación.");
                    return;
                }
                // double altitude = location.getAltitude(); // Not typically used for address
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                Geocoder geo = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geo.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address ads = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        if (ads.getFeatureName() != null) sb.append(ads.getFeatureName());
                        if (ads.getLocality() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(ads.getLocality());
                        }
                        if (ads.getAdminArea() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(ads.getAdminArea());
                        }
                        if (ads.getCountryName() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(ads.getCountryName());
                        }
                        mLocation = sb.toString();
                        mTvLocation.setText(mLocation);
                    } else {
                        mTvLocation.setText("Ubicación no encontrada.");
                        mLocation = "Lat: " + String.format(Locale.US, "%.4f", latitude) + ", Lon: " + String.format(Locale.US, "%.4f", longitude); // Fallback to coordinates
                        mTvLocation.setText(mLocation);
                    }
                } catch (IOException e) {
                    Log.e("EditNoteActivity", "Geocoder error", e);
                    mTvLocation.setText("Error al obtener dirección.");
                    mLocation = null; // Or fallback to coordinates
                } finally {
                    locationManager.removeUpdates(this); // Remove listener after one update
                }
            }
            public void onProviderEnabled(String provider) {
                mTvLocation.setText("Proveedor GPS habilitado. Obteniendo ubicación...");
            }
            public void onProviderDisabled(String provider) {
                Utils.showMessage(EditNoteActivity.this, "Por favor, active el GPS y/o la red para la ubicación.");
                mTvLocation.setText("GPS/Red desactivado.");
                mLocation = null;
            }
        };

        // Try Network provider first, then GPS
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            mTvLocation.setText("Ningún proveedor de ubicación activo.");
            Utils.showMessage(this, "Por favor, active el GPS o la ubicación por red.");
        }
    }
}