package com.example.wallnotes;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder; // Import nativo
import android.os.Build;
import android.os.Bundle;
import java.io.File;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
// Se elimina org.jetbrains.annotations.NotNull si no es estrictamente necesario por otra dependencia
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// Se eliminan las importaciones de OmRecorder
// import omrecorder.AudioRecordConfig;
// import omrecorder.OmRecorder;
// import omrecorder.PullTransport;
// import omrecorder.PullableSource;
// import omrecorder.Recorder;

public class RecordDialog extends DialogFragment {
    private static final String LOG_TAG = "RecordDialog";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200; // Para permisos si los manejas aquí

    private String _strTitle;
    private String _strMessage;
    private String _strPositiveButtonText;
    private FloatingActionButton _recordButton;
    private String STATE_BUTTON = "INIT";
    private String _AudioSavePathInDevice = null;
    private File audioFile = null; // Para tener el objeto File
    private TextView _timerView;
    private Timer _timer;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;

    private ClickListener _clickListener;
    // Se elimina la variable recorder de OmRecorder
    // Recorder recorder;
    private MediaRecorder mediaRecorder; // Instancia del MediaRecorder nativo
    MediaPlayer mediaPlayer; // Para reproducción
    MediaPlayer mPlayerSoundEffects; // Para efectos de sonido (antes mPlayer)

    public RecordDialog() {
        // Constructor vacío requerido
    }

    public static RecordDialog newInstance(String title) {
        RecordDialog frag = new RecordDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Podrías solicitar permisos aquí si es necesario, aunque es mejor en la Activity
        // if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        //     ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        // }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        // setupRecorder(); // Ya no se llama aquí de la misma forma
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.record_dialog, null);

        String strMessage = (_strMessage == null) ? "Presiona para grabar" : _strMessage;
        _timerView = rootView.findViewById(R.id._txtTimer);
        _timerView.setText(strMessage);
        _recordButton = rootView.findViewById(R.id.btnRecord);

        _recordButton.setOnClickListener(v -> {
            scaleAnimation();
            switch (STATE_BUTTON) {
                case "INIT":
                    // Verificar permisos antes de grabar
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                        // No continuar hasta que se otorguen los permisos.
                        // Podrías mostrar un Toast o deshabilitar el botón.
                        Log.w(LOG_TAG, "Permiso de grabación no otorgado.");
                        return; // Salir del onClickListener
                    }
                    _recordButton.setImageResource(R.drawable.ic_stop);
                    STATE_BUTTON = "RECORD";
                    startRecording();
                    playSoundEffect(R.raw.hangouts_message, this::startTimerAfterSound); // Reproduce sonido y luego inicia timer
                    break;
                case "RECORD":
                    stopRecording();
                    playSoundEffect(R.raw.pop, null); // Solo reproduce sonido
                    _recordButton.setImageResource(R.drawable.ic_play);
                    STATE_BUTTON = "STOP";
                    if (_timerView != null) _timerView.setText("00:00:00");
                    recorderSecondsElapsed = 0;
                    // El path del audio ya debería estar en _AudioSavePathInDevice desde createAudioFile()
                    break;
                case "STOP":
                    if (_AudioSavePathInDevice != null) {
                        startMediaPlayer();
                    } else {
                        Log.e(LOG_TAG, "No hay archivo de audio para reproducir.");
                        // Quizás mostrar un mensaje al usuario
                    }
                    break;
                case "PLAY":
                    pauseMediaPlayer();
                    break;
                case "PAUSE":
                    resumeMediaPlayer();
                    break;
            }
        });

        alertDialogBuilder.setView(rootView);

        String strPositiveButton = (_strPositiveButtonText == null) ? "GUARDAR" : _strPositiveButtonText; // Cambiado "CLOSE" a "GUARDAR" como sugerencia
        alertDialogBuilder.setPositiveButton(strPositiveButton, (dialog, which) -> {
            if (STATE_BUTTON.equals("RECORD")) {
                stopRecording(); // Asegúrate de detener la grabación si está activa
                // stopTimer(); // stopRecording ya debería llamar a stopTimer
            }
            if (_clickListener != null) {
                _clickListener.OnClickListener(_AudioSavePathInDevice);
            }
        });

        String strDialogTitle = (_strTitle == null) ? "Grabar Audio" : _strTitle;
        alertDialogBuilder.setTitle(strDialogTitle);

        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;

        final AlertDialog dialog = alertDialogBuilder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        return dialog;
    }


    private void startRecording() {
        audioFile = createAudioFile(); // Crea el archivo primero
        _AudioSavePathInDevice = audioFile.getAbsolutePath();

        
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // Cambiado a MPEG_4
        mediaRecorder.setOutputFile(_AudioSavePathInDevice);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // Cambiado a AAC para mejor calidad

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            // El timer se inicia después del efecto de sonido
            Log.d(LOG_TAG, "Grabación iniciada.");
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() falló para MediaRecorder", e);
            releaseMediaRecorder();
            STATE_BUTTON = "INIT"; // Volver al estado inicial
            _recordButton.setImageResource(R.drawable.ic_mic);
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "start() falló para MediaRecorder", e);
            releaseMediaRecorder();
            STATE_BUTTON = "INIT"; // Volver al estado inicial
            _recordButton.setImageResource(R.drawable.ic_mic);
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                Log.d(LOG_TAG, "Grabación detenida.");
            } catch (IllegalStateException e) {
                Log.e(LOG_TAG, "stop() falló para MediaRecorder. ¿Ya estaba detenido o no iniciado?", e);
                // Esto puede ocurrir si se llama a stop repetidamente o en un estado incorrecto.
                // Usualmente no es crítico si el objetivo era detenerlo y ya lo estaba.
            } finally {
                releaseMediaRecorder(); // Llama a release en finally para asegurar que se ejecuta
            }
        }
        stopTimer(); // Detener el timer después de que la grabación realmente se detuvo
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            // mediaRecorder.reset(); // reset() es útil si vas a reutilizar el objeto, pero aquí creamos uno nuevo cada vez.
            mediaRecorder.release();
            mediaRecorder = null;
            Log.d(LOG_TAG, "MediaRecorder liberado.");
        }
    }


    private void playSoundEffect(int soundResId, MediaPlayer.OnCompletionListener completionListener) {
        if (mPlayerSoundEffects != null) {
            mPlayerSoundEffects.release();
        }
        mPlayerSoundEffects = MediaPlayer.create(getContext(), soundResId);
        if (mPlayerSoundEffects != null) {
            mPlayerSoundEffects.setOnCompletionListener(mp -> {
                if (completionListener != null) {
                    completionListener.onCompletion(mp);
                }
                mp.release(); // Liberar después de completar
                if (mPlayerSoundEffects == mp) { // Evitar null si se llama a playSoundEffect rápidamente otra vez
                    mPlayerSoundEffects = null;
                }
            });
            mPlayerSoundEffects.start();
        } else {
            Log.e(LOG_TAG, "No se pudo crear MediaPlayer para efecto de sonido: " + soundResId);
            // Si el sonido no se puede reproducir, ejecuta el listener de completado inmediatamente
            // para que la lógica dependiente (como startTimer) no se bloquee.
            if (completionListener != null) {
                completionListener.onCompletion(null); // Pasa null o un mock si es necesario
            }
        }
    }

    // Método para ser llamado después de que el sonido inicial termine
    private void startTimerAfterSound(MediaPlayer mp) {
        startTimer();
    }


    // El método setupRecorder() original ya no es necesario con esta estructura.
    // private void setupRecorder() { ... }

    // El método mic() original ya no es necesario.
    // private PullableSource mic() { ... }

    @NonNull
    private File createAudioFile() {
        File createdFile = null;
        try {
            @SuppressLint("SimpleDateFormat")
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileNamePrefix = "AUDIO_" + timeStamp + "_"; // Cambiado de WAV_
            File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC);

            if (storageDir == null) {
                Log.e(LOG_TAG, "Directorio de música externo no disponible.");
                // Podrías intentar con getExternalCacheDir() o el almacenamiento interno como fallback
                // storageDir = requireActivity().getCacheDir();
                // O mostrar un error al usuario
                return null; // No se puede crear el archivo si no hay directorio
            }
            if (!storageDir.exists()){
                if(!storageDir.mkdirs()){
                    Log.e(LOG_TAG, "No se pudo crear el directorio: " + storageDir.getAbsolutePath());
                    return null;
                }
            }

            createdFile = File.createTempFile(
                    fileNamePrefix,  /* prefijo */
                    ".mp4",         /* sufijo / extensión */
                    storageDir      /* directorio */
            );
            _AudioSavePathInDevice = createdFile.getAbsolutePath(); // Actualizar aquí también
            Log.d(LOG_TAG, "Archivo de audio creado en: " + _AudioSavePathInDevice);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error al crear el archivo de audio", e);
            // Podrías propagar la excepción o devolver null y manejarlo arriba
        }
        return createdFile;
    }


    public String getAudioPath() {
        return _AudioSavePathInDevice;
    }

    // --- Métodos del MediaPlayer para reproducción (sin cambios importantes) ---
    private void startMediaPlayer() {
        if (_AudioSavePathInDevice == null || !(new File(_AudioSavePathInDevice).exists())) {
            Log.e(LOG_TAG, "Ruta de audio inválida o archivo no existe: " + _AudioSavePathInDevice);
            _recordButton.setImageResource(R.drawable.ic_play); // Volver al estado visual de 'STOP'
            STATE_BUTTON = "STOP"; // Asegurar estado lógico
            if (_timerView != null) _timerView.setText("00:00:00"); // Resetear timer visualmente
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(_AudioSavePathInDevice);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopMediaPlayer()); // Reutilizar tu método stopMediaPlayer
            mediaPlayer.start();

            _recordButton.setImageResource(R.drawable.ic_pause);
            STATE_BUTTON = "PLAY";
            playerSecondsElapsed = 0; // Reiniciar contador para reproducción
            startTimer(); // Iniciar timer para la reproducción
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error al preparar MediaPlayer", e);
            stopMediaPlayer(); // Limpiar si hay error
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "Error de estado al iniciar MediaPlayer", e);
            stopMediaPlayer();
        }
    }

    private void resumeMediaPlayer() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            _recordButton.setImageResource(R.drawable.ic_pause);
            STATE_BUTTON = "PLAY";
            // El timer debería continuar si ya estaba corriendo para 'PLAY'
        }
    }

    private void pauseMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            _recordButton.setImageResource(R.drawable.ic_play);
            STATE_BUTTON = "PAUSE";
            // El timer debería pausarse o al menos dejar de actualizar para 'PLAY'
            // stopTimer(); // O una lógica más fina para pausar la actualización del timer
        }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        _recordButton.setImageResource(R.drawable.ic_play); // Ícono para indicar que se puede volver a reproducir
        STATE_BUTTON = "STOP"; // Estado después de detener/completar reproducción
        if (_timerView != null) _timerView.setText("00:00:00"); // Resetear timer visualmente
        stopTimer(); // Detener timer si estaba corriendo para la reproducción
        playerSecondsElapsed = 0; // Resetear contador
    }

    // --- Métodos del Timer (sin cambios importantes, pero Utils.formatSeconds debe existir) ---
    private void startTimer() {
        stopTimer(); // Asegura que no haya timers duplicados
        _timer = new Timer();
        _timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (_timer != null) {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
    }

    private void updateTimer() {
        if (getActivity() == null) { // Es DialogFragment, usar requireActivity() o getContext() con cuidado del ciclo de vida
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (_timerView == null) return; // Chequeo extra
            if (STATE_BUTTON.equals("RECORD")) {
                recorderSecondsElapsed++;
                _timerView.setText(Utils.formatSeconds(recorderSecondsElapsed)); // Asume que tienes esta clase Utils
            } else if (STATE_BUTTON.equals("PLAY")) {
                playerSecondsElapsed++;
                // Asegurarse de que el mediaplayer no haya terminado antes de actualizar el timer
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int duration = mediaPlayer.getDuration() / 1000; // Duración en segundos
                    if(playerSecondsElapsed <= duration){ // Solo actualiza si no hemos pasado la duración
                        _timerView.setText(Utils.formatSeconds(playerSecondsElapsed));
                    } else {
                        // A veces el onCompletion puede tardar un poco, así que forzamos la detención visual del timer
                        // _timerView.setText(Utils.formatSeconds(duration));
                        // stopMediaPlayer(); // Podría ser muy agresivo llamarlo aquí
                    }
                } else if (mediaPlayer == null && playerSecondsElapsed > 0) {
                    // Si el mediaPlayer es null pero playerSecondsElapsed > 0, es probable que haya terminado la reproducción
                    _timerView.setText(Utils.formatSeconds(playerSecondsElapsed)); // Muestra el último tiempo
                }

            }
        });
    }


    // --- Animación (sin cambios) ---
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scaleAnimation() {
        if (getContext() == null) return; // Chequeo para evitar NPE
        final Interpolator interpolador = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in);
        _recordButton.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setInterpolator(interpolador)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {}
                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        _recordButton.animate().scaleX(1f).scaleY(1f).setListener(null).start(); // Evitar listener recursivo
                    }
                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {}
                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {}
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Considera qué hacer con la grabación o reproducción si el diálogo se pausa
        if (STATE_BUTTON.equals("RECORD")) {
            stopRecording();
            // Quizás quieras guardar el estado o descartar la grabación
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pauseMediaPlayer(); // Pausa la reproducción
        }
        // dismiss(); // Dismiss podría ser muy agresivo en onPause, depende del comportamiento deseado
    }

    @Override
    public void onDestroyView() {
        // Liberar recursos importantes aquí
        releaseMediaRecorder();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mPlayerSoundEffects != null) {
            mPlayerSoundEffects.release();
            mPlayerSoundEffects = null;
        }
        stopTimer();
        super.onDestroyView(); // Asegúrate de llamar a super al final
    }


    // --- Interfaz ClickListener (sin cambios) ---
    public interface ClickListener {
        void OnClickListener(String path);
    }

    public void setTitle(String strTitle) {
        _strTitle = strTitle;
    }

    public void setMessage(String strMessage) {
        _strMessage = strMessage;
    }

    public void setPositiveButton(String strPositiveButtonText, ClickListener onClickListener) {
        _strPositiveButtonText = strPositiveButtonText;
        _clickListener = onClickListener;
    }

    // Es importante manejar el resultado de la solicitud de permisos si la haces desde el DialogFragment
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes intentar iniciar la grabación de nuevo si el usuario
                // hizo clic en grabar y se le denegó el permiso.
                // Podrías llamar a _recordButton.performClick() o directamente a la lógica de inicio.
                Log.i(LOG_TAG, "Permiso de grabación concedido.");
                // Si el estado era INIT y el usuario intentó grabar, ahora podría funcionar.
                // Considera llamar a _recordButton.performClick() si es apropiado
                // o simplemente permitir que el usuario vuelva a hacer clic.
            } else {
                Log.w(LOG_TAG, "Permiso de grabación denegado.");
                // Informar al usuario que el permiso es necesario.
                // Podrías mostrar un Toast o un Snackbar.
            }
        }
    }
}