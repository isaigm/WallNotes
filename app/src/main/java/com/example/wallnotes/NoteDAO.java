package com.example.wallnotes;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface NoteDAO {
    @Query("SELECT * FROM notes")
    LiveData<List<Note>> getAll();

    @Query("SELECT * FROM notes WHERE uid IN (:noteIds)")
    LiveData<List<Note>> loadAllByIds(int[] noteIds);

    @Insert
    void insertAll(Note... notes);

    @Delete
    void delete(Note note);

    @Update
    void update(Note note);

    @Query("SELECT * FROM notes WHERE title LIKE :text AND NOT is_going_to_be_deleted")
    LiveData<List<Note>> getSearchResults(String text);

    @Query("SELECT * FROM notes WHERE is_going_to_be_deleted")
    LiveData<List<Note>> getNotesToBeDeleted();

    @Query("SELECT * FROM notes WHERE NOT is_going_to_be_deleted")
    LiveData<List<Note>> getCurrentNotes();

    @Query("SELECT * FROM notes WHERE NOT is_going_to_be_deleted AND uid = :id")
    Note getCurrNoteById(int id);

    @Query("SELECT * FROM notes WHERE remind_date IS NOT NULL")
    LiveData<List<Note>> getReminders();


    @Query("SELECT * FROM notes WHERE uid = :noteId")
    LiveData<Note> getNoteByIdLiveData(int noteId);

    // En tu NoteRepository.java

}
