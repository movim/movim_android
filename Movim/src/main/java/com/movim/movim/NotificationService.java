package com.movim.movim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationService extends FirebaseMessagingService {
    private HashMap<String, List<String>> notifs;

    @Override
    protected Intent getStartCommandIntent(Intent intent) {
        handleIntent(intent);
        return super.getStartCommandIntent(intent);
    }

    @Override
    public void handleIntent(Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("update")) {
                updateNotification();
            }

            if (intent.getAction().equals("clear")) {
                clearNotifications(intent.getStringExtra("action"));
            }

            // Coming from within the application
            if (intent.getAction().equals("notify")) {
                showNotification(
                    intent.getStringExtra("title"),
                    intent.getStringExtra("body"),
                    intent.getStringExtra("image"),
                    intent.getStringExtra("action")
                );
            }

            super.handleIntent(intent);
        }
    }

    @Override
    public void onCreate() {
        this.notifs = new HashMap<String, List<String>>();
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            showNotification(
                    remoteMessage.getData().get("title"),
                    remoteMessage.getData().get("body"),
                    remoteMessage.getData().get("image"),
                    remoteMessage.getData().get("action")
            );
        } else if (remoteMessage.getNotification() != null) {
            /*showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getNotification().getImageUrl().toString(),
                remoteMessage.getNotification().getClickAction()
            );*/
        }

        //super.onMessageReceived(remoteMessage);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                //MainActivity.getInstance().notifs.remove(intent.getAction());
            }
            unregisterReceiver(this);
        }
    };

    public void showNotification(String title, String body, String picture, String action) {
        Bitmap pictureBitmap = getBitmapFromURL(picture);

        Intent i = new Intent(this, MainActivity.class);
        if (action != null) {
            i.setAction(action);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // The deleteIntent declaration
        Intent deleteIntent = new Intent(action);
        PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(receiver, new IntentFilter(action));

        // Integer counter;
        List<String> messages = null;

        // There is already pending notifications
        if (this.notifs.get(action) != null) {
            messages = this.notifs.get(action);
        } else {
            messages = new ArrayList<String>();
        }

        messages.add(body);

        if (messages.size() > 5) {
            messages.remove(0);
        }

        this.notifs.put(action, messages);

        // We create the inbox
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (int j = 0; j < messages.size(); j++) {
            style.addLine(messages.get(j));
        }

        style.setBigContentTitle(title);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelId = "channel-movim";
        String channelName = "Movim";
        String groupId = "movim";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(mChannel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(pictureBitmap)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pi)
                .setDeleteIntent(pendingDeleteIntent).setAutoCancel(true).setColor(Color.parseColor("#3F51B5"))
                .setLights(Color.parseColor("#3F51B5"), 1000, 5000)
                .setNumber(messages.size())
                .setStyle(style)
                .setGroup(groupId)
                .build();
        notificationManager.notify(action, 0, notification);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification summaryNotification = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_vectorial))
                    .setGroup(groupId)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build();
            notificationManager.notify("summary", 0, summaryNotification);
        }
    }

    public void clearNotifications(String action) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(action, 0);

        if (this.notifs.get(action) != null) {
            this.notifs.remove(action);
        }
    }

    public void updateNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            int counter = notificationManager.getActiveNotifications().length;

            if (counter <= 1) {
                notificationManager.cancel("summary", 0);
            }
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
