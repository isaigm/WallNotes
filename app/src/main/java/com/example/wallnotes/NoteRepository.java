package com.example.wallnotes;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {
    private final NoteDAO mNoteDao;
    private final ExecutorService mExecutor;
    NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        mExecutor = Executors.newSingleThreadExecutor();
        mNoteDao = db.noteDAO();
    }
    NoteRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        mNoteDao = db.noteDAO();
        mExecutor = Executors.newSingleThreadExecutor();
    }
    public LiveData<List<Note>> getCurrNotes() {
        return  mNoteDao.getCurrentNotes();
    }
    public LiveData<List<Note>> getNotesToBeDeleted() {
        return mNoteDao.getNotesToBeDeleted();
    }
    public LiveData<List<Note>> getReminders() { return mNoteDao.getReminders(); }
    public void addNote(Note note)
    {
        mExecutor.execute(() -> mNoteDao.insertAll(note));
    }
    public void update(Note note){
        mExecutor.execute(() -> mNoteDao.update(note));
    }
    public void delete(Note note)
    {
        mExecutor.execute(() -> mNoteDao.delete(note));
    }
    public LiveData<List<Note>> search(String text)  {
        try {
            return mExecutor.submit(() -> mNoteDao.getSearchResults("%" + text + "%")).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    public Note getById(int id)  {
        try {
            return mExecutor.submit(() -> mNoteDao.getCurrNoteById(id)).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    public LiveData<Note> getNoteByIdLiveData(int noteId) {
        return mNoteDao.getNoteByIdLiveData(noteId);
    }
}
