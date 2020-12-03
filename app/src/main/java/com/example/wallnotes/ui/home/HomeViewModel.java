package com.example.wallnotes.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.wallnotes.AppDatabase;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteDAO;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private NoteDAO noteDAO;
    private ExecutorService executorService;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        noteDAO = AppDatabase.getInstance(application).noteDAO();
        executorService = Executors.newSingleThreadExecutor();
    }
    LiveData<List<Note>> getAllNotes() {
        return noteDAO.getAll();
    }
    void addNote(Note note){
        executorService.execute(() -> noteDAO.insertAll(note));
    }
    void deleteNote(Note note){
        executorService.execute(() -> noteDAO.delete(note));
    }
    void updateNote(Note note){
        executorService.execute(() -> noteDAO.update(note));
    }
}