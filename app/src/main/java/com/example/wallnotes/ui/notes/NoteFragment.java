package com.example.wallnotes.ui.notes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.wallnotes.Adapter;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class NoteFragment extends Fragment{

    private NoteViewModel mNoteViewModel;
    private BroadcastReceiver mBroadcastReceiver;
    private RecyclerView.AdapterDataObserver mObserver;
    private Adapter mAdapter;
    private boolean mUseLinearLayout = true;
    private RecyclerView mRecyclerview;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        View root = inflater.inflate(R.layout.fragment_note, container, false);
        mRecyclerview = root.findViewById(R.id.recycler_view);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<Note> data = new ArrayList<>();
        mAdapter = new Adapter(data, getActivity(), mNoteViewModel);
        mNoteViewModel.getCurrNotes().observe(getViewLifecycleOwner(), mAdapter::setmData);
        mRecyclerview.setAdapter(mAdapter);
        final TextView textView = root.findViewById(R.id.text_home);
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(mAdapter.getItemCount() > 0){
                    textView.setVisibility(View.INVISIBLE);
                }else if (mAdapter.getItemCount() == 0){
                    textView.setVisibility(View.VISIBLE);
                }
            }
        };
        mAdapter.registerAdapterDataObserver(mObserver);
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
                    note.setCreatedAt((Date) intent.getSerializableExtra("created_at"));
                    if(imgUri != null){
                        note.setImgUri(imgUri);
                    }
                    if(mustUpdateNote){
                        note.setUid(intent.getIntExtra("uid", 0));
                        mNoteViewModel.update(note);
                    }else if (mustDeleteNote){
                        note.setUid(intent.getIntExtra("uid", 0));
                        note.setGoingToBeDeleted(true);
                        mNoteViewModel.update(note);
                    }else {
                        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US);
                        Date currDate = new Date();
                        note.setCreatedAt(currDate);
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
            mAdapter.setmData(notes);
        });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.change_layout)
        {
            mUseLinearLayout = !mUseLinearLayout;
            if(mUseLinearLayout)
            {
                item.setIcon(R.drawable.ic_baseline_view_list_24);
                mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

            }else{
                item.setIcon(R.drawable.ic_baseline_view_grid);
                mRecyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            }
            mRecyclerview.setAdapter(mAdapter);
            runLayoutAnimation(mRecyclerview);
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem mSearchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        menu.findItem(R.id.delete_notes).setVisible(false);
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
        if(mAdapter != null && mObserver != null){
            mAdapter.unregisterAdapterDataObserver(mObserver);
        }
        if(mBroadcastReceiver != null)
        {
            requireActivity().unregisterReceiver(mBroadcastReceiver);
        }
    }
    private void runLayoutAnimation(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.animation_layout);

        recyclerView.setLayoutAnimation(controller);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }
}