package com.example.wallnotes;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Note.class}, version = 1)
@TypeConverters({Converters.class})
public  abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public static synchronized  AppDatabase getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "notes_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
    public abstract NoteDAO noteDAO();
}