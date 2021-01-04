package com.example.wallnotes;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository noteRepository;
    private final LiveData<List<Note>> mCurrNotes;
    private final LiveData<List<Note>> mNotesToBeDeleted;
    private final LiveData<List<Note>> mReminders;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
        mCurrNotes = noteRepository.getCurrNotes();
        mNotesToBeDeleted = noteRepository.getNotesToBeDeleted();
        mReminders = noteRepository.getReminders();
    }

    public LiveData<List<Note>> getCurrNotes() {
        return mCurrNotes;
    }
    public LiveData<List<Note>> getNotesToBeDeleted() {
        return mNotesToBeDeleted;
    }
    public LiveData<List<Note>> getReminders() { return mReminders; }
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