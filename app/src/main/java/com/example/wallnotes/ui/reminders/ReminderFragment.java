package com.example.wallnotes.ui.reminders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteAdapter;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import com.example.wallnotes.Utils;
import java.util.ArrayList;
import java.util.List;

public class ReminderFragment extends Fragment {

    private RecyclerView mRecyclerview;
    private NoteViewModel mNoteViewModel;
    private NoteAdapter mAdapter = null;
    private boolean mUseLinearLayout = true;
    private RecyclerView.AdapterDataObserver mObserver = null;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_reminder, container, false);
        mRecyclerview = root.findViewById(R.id.recycler_view_reminder);
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        List<Note> data = new ArrayList<>();
        mAdapter = new NoteAdapter(data, getActivity(), mNoteViewModel);
        mNoteViewModel.getReminders().observe(getViewLifecycleOwner(), notes -> mAdapter.setDataList(notes));
        mRecyclerview.setAdapter(mAdapter);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        final TextView tvReminder = root.findViewById(R.id.text_reminder);
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(mAdapter.getItemCount() > 0){
                    tvReminder.setVisibility(View.INVISIBLE);
                }else if (mAdapter.getItemCount() == 0){
                    tvReminder.setVisibility(View.VISIBLE);
                }
            }
        };
        mAdapter.registerAdapterDataObserver(mObserver);
        setHasOptionsMenu(true);
        return root;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAdapter != null && mObserver != null){
            mAdapter.unregisterAdapterDataObserver(mObserver);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.change_layout)
        {
            mUseLinearLayout = !mUseLinearLayout;
            if(mUseLinearLayout) {
                item.setIcon(R.drawable.ic_baseline_view_list_24);
                mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

            }else{
                item.setIcon(R.drawable.ic_baseline_view_grid);
                mRecyclerview.setLayoutManager( new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            }
            mRecyclerview.setAdapter(mAdapter);
            Utils.runLayoutAnimation(mRecyclerview);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.delete_notes).setVisible(false);
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
    }
    void getNotesFromDb(String text) {
        mNoteViewModel.search(text).observe(this, notes -> {
            if(!text.isEmpty())
            {
                mAdapter.setDataList(notes);
            }else{
                mAdapter.setDataList(mNoteViewModel.getReminders().getValue());
            }
        });
    }
}