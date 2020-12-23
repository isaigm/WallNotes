package com.example.wallnotes;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private NoteRepository noteRepository;
    private LiveData<List<Note>> mAllNotes;


    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
        mAllNotes = noteRepository.getAllNotes();
    }

    public LiveData<List<Note>> getAllNotes() {
        return mAllNotes;
    }

    public void addNote(Note note) {
        noteRepository.addNote(note);
    }

    public void update(Note note) {
        noteRepository.update(note);
    }

    public void delete(Note note)
    {
        noteRepository.delete(note);
    }
    public LiveData<List<Note>> search(String text)
    {
        return noteRepository.search(text);
    }
}