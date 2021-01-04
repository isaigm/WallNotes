package com.example.wallnotes.ui.notes;

import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.wallnotes.NoteAdapter;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteFragment extends Fragment{

    private NoteViewModel mNoteViewModel;
    private RecyclerView.AdapterDataObserver mObserver;
    private NoteAdapter mAdapter;
    private boolean mUseLinearLayout = true;
    private RecyclerView mRecyclerview;
    private boolean addAtEnd = false;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        View root = inflater.inflate(R.layout.fragment_note, container, false);
        mRecyclerview = root.findViewById(R.id.recycler_view);
        List<Note> data = new ArrayList<>();
        mAdapter = new NoteAdapter(data, getActivity(), mNoteViewModel);
        mNoteViewModel.getCurrNotes().observe(getViewLifecycleOwner(), mAdapter::setmData);
        mRecyclerview.setAdapter(mAdapter);
        final TextView textView = root.findViewById(R.id.text_home);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        addAtEnd = sharedPreferences.getBoolean("add_at_end", false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        if(addAtEnd)
        {
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
        }
        mRecyclerview.setLayoutManager(linearLayoutManager);
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
        return root;
    }
    void getNotesFromDb(String text) {
        mNoteViewModel.search(text).observe(this, notes -> {
            mAdapter.setmData(notes);
        });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.change_layout)
        {
            mUseLinearLayout = !mUseLinearLayout;
            if(mUseLinearLayout) {
                item.setIcon(R.drawable.ic_baseline_view_list_24);
                LinearLayoutManager linearLayout = new LinearLayoutManager(getActivity());
                if(addAtEnd)
                {
                    linearLayout.setReverseLayout(true);
                    linearLayout.setStackFromEnd(true);
                }
                mRecyclerview.setLayoutManager(linearLayout);

            }else{
                item.setIcon(R.drawable.ic_baseline_view_grid);
                mRecyclerview.setLayoutManager( new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
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