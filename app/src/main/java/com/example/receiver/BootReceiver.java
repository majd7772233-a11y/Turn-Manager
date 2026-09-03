package com.example.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.database.DatabaseModule;
import com.example.database.SessionEntity;
import com.example.database.TurnDatabase;
import com.example.database.UserEntity;
import com.example.engine.TurnEngine;
import com.example.service.TurnService;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * BootReceiver: Receives system boot completed broadcast to initialize
 * users, sessions, widget, and background services on device startup.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    TurnDatabase database = DatabaseModule.INSTANCE.getDatabase(context);
                    List<UserEntity> users = database.turnDao().getAllUsersSync();
                    List<SessionEntity> sessions = database.turnDao().getAllSessionsSync();

                    Log.d(TAG, "Boot loaded users count: " + (users != null ? users.size() : 0));

                    if (users != null && !users.isEmpty()) {
                        // Setup TurnEngine if session exists
                        if (sessions != null && !sessions.isEmpty()) {
                            SessionEntity activeSession = sessions.get(0);
                            TurnEngine.INSTANCE.setupSession(activeSession, users);
                        }

                        // Update all Home Screen Widgets
                        QuickTurnWidgetProvider.Companion.updateAllWidgets(context);

                        // Start Foreground TurnService
                        Intent serviceIntent = new Intent(context, TurnService.class);
                        serviceIntent.setAction(TurnService.ACTION_START_SERVICE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(context, serviceIntent);
                        } else {
                            context.startService(serviceIntent);
                        }
                        Log.d(TAG, "TurnService started and widgets updated on boot successfully.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing on boot: ", e);
                }
            });
        }
    }
}
