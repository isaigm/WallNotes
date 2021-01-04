package com.example.wallnotes;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

public class NotifierAlarm extends BroadcastReceiver {
    private static final String CHANNEL_ID = "NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Intent intent1 = new Intent(context, EditNoteActivity.class);
        Intent intent2 = new Intent(context, MainActivity.class);
        NoteRepository mNoteRepository = new NoteRepository(context);
        int uid = intent.getIntExtra("uid", 0);
        Note n = mNoteRepository.getById(uid);
        n.setRemindDate(null);
        mNoteRepository.update(n);
        String title = n.getTitle();
        String content = n.getContent();
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent1.putExtra("uid", uid);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(intent2);
        taskStackBuilder.addNextIntent(intent1);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Notification";
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setSound(alarmSound).setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1, builder.build());
    }
}
