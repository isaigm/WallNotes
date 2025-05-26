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
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
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
    boolean mPause = false;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;
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
        Log.d("COLOR",  String.format("#%08X", bottomAppBar.getBackgroundTint().getDefaultColor()));
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
            mCurrentNoteId = data.getInt("uid", -1);
            if (mCurrentNoteId != -1) {
                mCurrentNoteLiveData = mNoteViewModel.getNoteByIdLiveData(mCurrentNoteId);
                observeNote();
            }
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

            setRemindDate();

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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
            } else if (id == R.id.cancel_alarm) {
                if (mCurrNote != null) {
                    if (mCurrNote.getRemindDate() != null) {
                        cancelAlarm();
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
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) { //Añadido data.getData() != null
                    Uri selectedImageUri = data.getData();
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);

                        mImgUri = selectedImageUri.toString();
                        loadImage(mImgUri);

                    } catch (SecurityException e) {
                        Toast.makeText(this, "No se pudo obtener permiso para la imagen.", Toast.LENGTH_SHORT).show();
                        mImgUri = null;
                    }
                }
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (mPhotoPath != null) {
                        File imageFile = new File(mPhotoPath);
                        if (imageFile.exists()) {
                            Uri photoFileUri = FileProvider.getUriForFile(this,
                                    "com.example.wallnotes.fileprovider",
                                    imageFile);
                            mImgUri = photoFileUri.toString();
                            loadImage(mImgUri);
                        } else {
                            Toast.makeText(this, "Error al cargar la foto tomada.", Toast.LENGTH_SHORT).show();
                            mImgUri = null;
                        }
                    } else {
                        Toast.makeText(this, "Error al procesar la foto tomada.", Toast.LENGTH_SHORT).show();
                        mImgUri = null;
                    }
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
                    checkNotificationPermissionAndCreateAlarm();
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
            case REQUEST_CODE_POST_NOTIFICATIONS:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG_PERMISSION, "POST_NOTIFICATIONS permission granted by user after request.");
                    if (mCurrNote != null && mRemindDate == null) {
                        Log.d(TAG_PERMISSION, "Permission granted. Attempting to create alarm.");
                        createAlarm();
                    } else {
                        Log.d(TAG_PERMISSION, "Permission granted, but conditions for alarm not met now.");
                    }
                } else {
                    Log.w(TAG_PERMISSION, "POST_NOTIFICATIONS permission denied by user.");
                    Toast.makeText(this, "Permiso de notificación denegado. Los recordatorios no se mostrarán.", Toast.LENGTH_LONG).show();
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
        if (!title.isEmpty()) {
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
            if (mUpdate && mCurrNote != null) {
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
    private void checkNotificationPermissionAndCreateAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permiso ya concedido
                Log.d(TAG_PERMISSION, "POST_NOTIFICATIONS permission already granted. Creating alarm.");
                createAlarm();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                // Muestra una explicación al usuario
                new AlertDialog.Builder(this)
                        .setTitle("Permiso de Notificación Necesario")
                        .setMessage("WallNotes necesita mostrar notificaciones para los recordatorios de tus notas. Por favor, concede el permiso.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            ActivityCompat.requestPermissions(EditNoteActivity.this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_CODE_POST_NOTIFICATIONS);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            Log.w(TAG_PERMISSION, "User cancelled rationale for POST_NOTIFICATIONS.");
                            Toast.makeText(this, "Permiso de notificación es necesario para los recordatorios.", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            } else {
                // Solicitar directamente el permiso
                Log.d(TAG_PERMISSION, "Requesting POST_NOTIFICATIONS permission.");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        } else {
            // Versiones anteriores a Android 13 no necesitan este permiso en tiempo de ejecución
            Log.d(TAG_PERMISSION, "Pre-Tiramisu: No runtime permission needed for notifications. Creating alarm.");
            createAlarm();
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
        Glide.with(this)
                .load(uri)
                .error(R.drawable.reload)
                .into(mImageView);
    }

    void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(getApplicationContext(), NotifierAlarm.class);
        // Asegúrate de que mCurrNote y su Uid no sean nulos antes de usarlos.
        if (mCurrNote == null) {
            // Manejar el caso donde mCurrNote es null, quizás mostrar un mensaje o log.
            // No se puede cancelar una alarma sin su identificador.
            Log.e("EditNoteActivity", "mCurrNote es null en cancelAlarm, no se puede cancelar.");
            return;
        }

        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_NO_CREATE;
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
        }
        mRemindDate = null;
    }
    void createAlarm() {
        final Calendar newCalender = Calendar.getInstance();
        Context context = this; // o getActivity()

// Obtener el ID del estilo del DatePickerDialog desde el tema actual de la actividad
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.appDatePickerDialogTheme, outValue, true);
        int datePickerDialogThemeResId = outValue.resourceId;

        DatePickerDialog dialog = new DatePickerDialog(EditNoteActivity.this, datePickerDialogThemeResId, (view, year, month, dayOfMonth) -> {
            final Calendar newDate = Calendar.getInstance();
            Calendar newTime = Calendar.getInstance();
            TimePickerDialog time = new TimePickerDialog(EditNoteActivity.this, (view1, hourOfDay, minute) -> {
                newDate.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                Calendar tem = Calendar.getInstance(); // Para comparar con la hora actual

                if (newDate.getTimeInMillis() > tem.getTimeInMillis()) { // Asegúrate de que la fecha/hora sea en el futuro
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(newDate.getTime());
                    calendar.set(Calendar.SECOND, 0); // No necesitamos precisión de segundos para la alarma

                    // Asegurarse de que mCurrNote no sea null antes de usarlo para el UID
                    if (mCurrNote == null) {
                        Log.e("CreateAlarm", "mCurrNote es null, no se puede crear alarma sin UID.");
                        Utils.showMessage(EditNoteActivity.this, "Error: No se pudo identificar la nota para el recordatorio.");
                        return;
                    }

                    Intent intent = new Intent(this, NotifierAlarm.class);
                    intent.putExtra("uid", mCurrNote.getUid());

                    int flags;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                    } else {
                        flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    }
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, mCurrNote.getUid(), intent, flags);

                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                    if (alarmManager == null) { // Buena práctica verificar si el servicio está disponible
                        Utils.showMessage(this, "No se pudo acceder al servicio de alarmas.");
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            // Configuración exitosa de la alarma
                            mRemindDate = calendar.getTime();
                            if (mCurrNote != null) { // Doble verificación, aunque ya se hizo arriba
                                mCurrNote.setRemindDate(mRemindDate);
                            }
                            setRemindDate(); // Actualiza la UI
                            Utils.showMessage(this, "Recordatorio agregado");
                        } else {
                            // Permiso no concedido, guiar al usuario
                            new AlertDialog.Builder(this)
                                    .setTitle("Permiso Necesario")
                                    .setMessage("Para establecer recordatorios precisos, WallNotes necesita permiso para programar alarmas exactas. ¿Deseas ir a la configuración para concederlo?")
                                    .setPositiveButton("Ir a Configuración", (dialogInterface, i) -> {
                                        Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);

                                        try {
                                            startActivity(permissionIntent);
                                            Utils.showMessage(EditNoteActivity.this, "Por favor, activa el permiso para 'WallNotes' y luego intenta agregar el recordatorio de nuevo.");
                                        } catch (Exception e) {
                                            Log.e("CreateAlarm", "Error al abrir la configuración de SCHEDULE_EXACT_ALARM", e);
                                            Utils.showMessage(EditNoteActivity.this, "No se pudo abrir la configuración de permisos. Por favor, habilita manualmente el permiso de 'Alarmas y recordatorios' para WallNotes desde la configuración de la aplicación.");
                                        }
                                    })
                                    .setNegativeButton("Cancelar", (dialogInterface, i) -> {
                                        Utils.showMessage(this, "El recordatorio no se pudo agregar sin el permiso.");
                                    })
                                    .show();
                        }
                    } else {
                        // Para versiones anteriores a Android S (API 31)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        // Configuración exitosa de la alarma
                        mRemindDate = calendar.getTime();
                        if (mCurrNote != null) {
                            mCurrNote.setRemindDate(mRemindDate);
                        }
                        setRemindDate(); // Actualiza la UI
                        Utils.showMessage(this, "Recordatorio agregado");
                    }

                } else {
                    Utils.showMessage(this, "Tiempo inválido. Por favor, selecciona una fecha y hora en el futuro.");
                }
            }, newTime.get(Calendar.HOUR_OF_DAY), newTime.get(Calendar.MINUTE), true);
            time.show();
        }, newCalender.get(Calendar.YEAR), newCalender.get(Calendar.MONTH), newCalender.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }
    private void observeNote() {
        if (mCurrentNoteLiveData == null) return;

        mCurrentNoteLiveData.observe(this, note -> {
            if (note != null) {
                mCurrNote = note;
                mTitle.setText(note.getTitle());
                mContent.setText(note.getContent());
                mRemindDate = note.getRemindDate();
                setRemindDate();
                mUpdate = true;
            } else {
                Log.w("EditNoteActivity", "La nota con ID " + mCurrentNoteId + " ya no existe.");
                finish();
            }
        });
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
            mPlayer.prepareAsync();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void getPos() {
        mTvLocation.setVisibility(View.VISIBLE);
        mTvLocation.setText("Cargando ubicación...");
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
                    if(!addresses.isEmpty())
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