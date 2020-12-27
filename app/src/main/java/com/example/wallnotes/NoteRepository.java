package com.example.wallnotes;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NoteRepository {
    private final NoteDAO mNoteDao;
    private final LiveData<List<Note>> mCurrNotes;
    private final LiveData<List<Note>> mNotesToBeDeleted;
    NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        mNoteDao = db.noteDAO();
        mCurrNotes = mNoteDao.getCurrentNotes();
        mNotesToBeDeleted = mNoteDao.getNotesToBeDeleted();
    }
    public LiveData<List<Note>> getCurrNotes() {
        return mCurrNotes;
    }
    public LiveData<List<Note>> getNotesToBeDeleted() {
        return mNotesToBeDeleted;
    }
    public void addNote(Note note)
    {
        new insertAsyncTask(mNoteDao).execute(note);
    }
    public void update(Note note)
    {
        new updateAsyncTask(mNoteDao).execute(note);
    }
    public void delete(Note note)
    {
        new deleteAsyncTask(mNoteDao).execute(note);
    }
    public LiveData<List<Note>> search(String text)  {
        searchAsyncTask asyncTask = new searchAsyncTask(mNoteDao);
        LiveData<List<Note>> res = null;
        try {
            res = asyncTask.execute(text).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }
    private static class insertAsyncTask extends AsyncTask<Note, Void, Void> {
        private final NoteDAO mAsyncTaskDao;
        public insertAsyncTask(NoteDAO dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(final Note... params) {
            mAsyncTaskDao.insertAll(params[0]);
            return null;
        }
    }
    private static class updateAsyncTask extends AsyncTask<Note, Void, Void> {
        private final NoteDAO mAsyncTaskDao;
        public updateAsyncTask(NoteDAO dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(final Note... params) {
            mAsyncTaskDao.update(params[0]);
            return null;
        }
    }
    private static class deleteAsyncTask extends AsyncTask<Note, Void, Void> {
        private final NoteDAO mAsyncTaskDao;
        public deleteAsyncTask(NoteDAO dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected Void doInBackground(final Note... params) {
            mAsyncTaskDao.delete(params[0]);
            return null;
        }
    }
    private static class searchAsyncTask extends AsyncTask<String, Void, LiveData<List<Note>>> {
        private final NoteDAO mAsyncTaskDao;
        public searchAsyncTask(NoteDAO dao) {
            mAsyncTaskDao = dao;
        }
        @Override
        protected LiveData<List<Note>> doInBackground(final String... params) {
            return mAsyncTaskDao.getSearchResults("%" + params[0] + "%");
        }
    }
}
