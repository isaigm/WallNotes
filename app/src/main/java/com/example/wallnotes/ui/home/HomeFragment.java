package com.example.wallnotes.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wallnotes.Adapter;
import com.example.wallnotes.EditNoteActivity;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import java.util.ArrayList;
import java.util.List;
public class HomeFragment extends Fragment implements Adapter.OnClickNoteListener, Adapter.OnLongClickListener {

    private NoteViewModel mNoteViewModel;
    private BroadcastReceiver mBroadcastReceiver;
    private RecyclerView.AdapterDataObserver mObserver;
    private Adapter adapter;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Note> data = new ArrayList<>();
        adapter = new Adapter(data, this, this);
        mNoteViewModel.getAllNotes().observe(getViewLifecycleOwner(), adapter::setmData);
        recyclerView.setAdapter(adapter);
        final TextView textView = root.findViewById(R.id.text_home);
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(adapter.getItemCount() > 0){
                    textView.setVisibility(View.INVISIBLE);
                }else if (adapter.getItemCount() == 0){
                    textView.setVisibility(View.VISIBLE);
                }
            }
        };
        adapter.registerAdapterDataObserver(mObserver);
        setHasOptionsMenu(true);
        IntentFilter filter = new IntentFilter("DATA");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null){
                    boolean mustUpdateNote = intent.getBooleanExtra("must_update_note", false);
                    boolean mustDeleteNote = intent.getBooleanExtra("must_delete", false);
                    String imgUri = intent.getStringExtra("img_uri");
                    Note note = new Note(intent.getStringExtra("title"), intent.getStringExtra("content"), null);
                    if(imgUri != null){
                        note.setImgUri(imgUri);
                    }
                    if(mustUpdateNote){
                        note.setUid(intent.getIntExtra("uid", 0));
                        mNoteViewModel.update(note);
                    }else if (mustDeleteNote){
                        note.setUid(intent.getIntExtra("uid", 0));
                        mNoteViewModel.delete(note);
                    }else {
                        mNoteViewModel.addNote(note);
                    }
                }
            }
        };
        requireActivity().registerReceiver(mBroadcastReceiver, filter);
        return root;
    }
    void getNotesFromDb(String text){
        mNoteViewModel.search(text).observe(this, notes -> {
            adapter.setmData(notes);
        });
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem mSearchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(query != null){
                    getNotesFromDb(query);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText != null)
                {
                    getNotesFromDb(newText);
                }
                return true;
            }
        });
        super.onPrepareOptionsMenu(menu);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(adapter != null && mObserver != null){
            adapter.unregisterAdapterDataObserver(mObserver);
        }
        if(mBroadcastReceiver != null)
        {
            requireActivity().unregisterReceiver(mBroadcastReceiver);
        }
    }
    @Override
    public void onClick(int pos) {
        Note note = adapter.getmData().get(pos);
        Intent intent = new Intent(getActivity(), EditNoteActivity.class);
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        intent.putExtra("uid", note.getUid());
        startActivity(intent);
    }
    @Override
    public boolean onLongClick(int n) {
        return false;
    }
}