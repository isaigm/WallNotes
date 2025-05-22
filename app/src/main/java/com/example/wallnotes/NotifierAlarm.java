package com.example.wallnotes;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

public class NotifierAlarm extends BroadcastReceiver {
    private static final String TAG = "NotifierAlarm"; // Para logs
    public static final String CHANNEL_ID = "NOTIFICATION_REMINDERS"; // Hacerlo público si se crea en Application
    public static final String CHANNEL_NAME = "Recordatorios de Notas"; // Nombre legible para el canal

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida. Intent: " + intent);

        if (intent == null) {
            Log.e(TAG, "Intent recibido es null. No se puede procesar la alarma.");
            return;
        }

        int uid = intent.getIntExtra("uid", -1); // Usar -1 o algún valor inválido como default
        if (uid == -1) {
            Log.e(TAG, "No se recibió un UID válido en la alarma.");
            return;
        }
        Log.d(TAG, "UID recibido: " + uid);

        // Considerar ejecutar esto en un hilo de fondo si es lento
        NoteRepository mNoteRepository = new NoteRepository(context.getApplicationContext());
        Note n = mNoteRepository.getById(uid);

        if (n == null) {
            Log.e(TAG, "No se encontró la nota con UID: " + uid + ". No se mostrará notificación.");
            return; // Salir si la nota no existe
        }

        // Actualizar la nota (quitar la fecha de recordatorio)
        // Esto también debería idealmente estar en un hilo de fondo.
        // Por simplicidad, lo dejamos aquí, pero tenlo en cuenta.
        n.setRemindDate(null);
        mNoteRepository.update(n);
        Log.d(TAG, "Fecha de recordatorio eliminada para la nota UID: " + uid);

        String title = n.getTitle();
        String content = n.getContent(); // O un mensaje de recordatorio más genérico si prefieres

        // Intent para cuando el usuario pulse la notificación
        Intent editNoteIntent = new Intent(context, EditNoteActivity.class);
        editNoteIntent.putExtra("uid", uid);
        // FLAG_ACTIVITY_NEW_TASK es necesario si inicias una actividad desde un BroadcastReceiver
        // FLAG_ACTIVITY_CLEAR_TOP si EditNoteActivity ya está en la pila, la trae al frente y limpia lo que esté encima.
        // FLAG_ACTIVITY_SINGLE_TOP si EditNoteActivity es la actividad superior, recibe el nuevo intent en onNewIntent.
        editNoteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Añade la pila de "padres" de EditNoteActivity.
        // Si MainActivity es el padre lógico de EditNoteActivity (declarado en el Manifest),
        // el sistema puede construir la pila correctamente.
        // O puedes añadirlo explícitamente:
        stackBuilder.addNextIntentWithParentStack(new Intent(context, MainActivity.class)); // Asegura que MainActivity esté debajo
        stackBuilder.addNextIntent(editNoteIntent);

        int pendingIntentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        // Usar un requestCode diferente para el PendingIntent de la notificación
        // para evitar conflictos con el requestCode de la alarma (si usaras el mismo).
        // Usar el uid aquí puede ser una buena opción si quieres que cada notificación
        // tenga un PendingIntent actualizable basado en la nota.
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(uid, pendingIntentFlags);

        // Crear canal de notificación (mejor hacerlo en Application#onCreate)
        createNotificationChannel(context);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round) // ¡ASEGÚRATE DE QUE ESTE ICONO SEA VÁLIDO!
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Para que aparezca como heads-up
                .setSound(alarmSound)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true); // La notificación se quita cuando el usuario la pulsa

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // ANTES DE NOTIFICAR (Android 13+): Verifica el permiso POST_NOTIFICATIONS
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        // notificationManager.notify(uid, builder.build()); // Usar uid como ID de notificación
        // } else {
        // Log.e(TAG, "Permiso POST_NOTIFICATIONS no concedido. No se puede mostrar la notificación.");
        // // Aquí podrías enviar un broadcast a tu actividad para que solicite el permiso,
        // // o simplemente loguearlo si la app ya debería tener el permiso.
        // }
        // } else {
        notificationManager.notify(uid, builder.build()); // Usar uid como ID de notificación para unicidad
        // }
        Log.d(TAG, "Notificación mostrada para UID: " + uid);
    }

    // Método para crear el canal de notificación.
    // Es mejor llamar a esto desde Application#onCreate una sola vez.
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Usar HIGH o DEFAULT según la criticidad
            );
            // channel.setDescription("Canal para recordatorios de notas"); // Opcional
            // Puedes configurar más propiedades del canal aquí (vibración, luz, etc.)

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificación creado/actualizado: " + CHANNEL_ID);
            }
        }
    }
}