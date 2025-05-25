package com.example.wallnotes.ui.notes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater; // Required for MenuProvider
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost; // Required for MenuHost
import androidx.core.view.MenuProvider; // Required for MenuProvider
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle; // Required for Lifecycle.State
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
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

// Implement MenuProvider
public class NoteFragment extends Fragment implements MenuProvider {

    private NoteViewModel mNoteViewModel;
    private RecyclerView.AdapterDataObserver mObserver;
    private NoteAdapter mAdapter;
    private boolean mUseLinearLayout = true;
    private RecyclerView mRecyclerview;
    private boolean addAtEnd = false;
    private TextView mEmptyStateTextView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        View root = inflater.inflate(R.layout.fragment_note, container, false);
        mRecyclerview = root.findViewById(R.id.recycler_view);
        mEmptyStateTextView = root.findViewById(R.id.text_home);

        List<Note> data = new ArrayList<>();
        mAdapter = new NoteAdapter(data, getActivity(), mNoteViewModel);
        mNoteViewModel.getCurrNotes().observe(getViewLifecycleOwner(), mAdapter::setDataList);
        mRecyclerview.setAdapter(mAdapter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        addAtEnd = sharedPreferences.getBoolean("add_at_end", false);
        setupLayoutManager();

        mObserver = new RecyclerView.AdapterDataObserver() {
            private void updateEmptyViewVisibility() {
                if (mAdapter == null || mEmptyStateTextView == null) {
                    return;
                }
                if (mAdapter.getItemCount() > 0) {
                    mEmptyStateTextView.setVisibility(View.INVISIBLE);
                } else {
                    mEmptyStateTextView.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onChanged() {
                super.onChanged();
                updateEmptyViewVisibility();
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                updateEmptyViewVisibility();
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                updateEmptyViewVisibility();
            }
            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                updateEmptyViewVisibility();
            }
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                updateEmptyViewVisibility();
            }
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                super.onItemRangeChanged(positionStart, itemCount, payload);
                updateEmptyViewVisibility();
            }
        };
        mAdapter.registerAdapterDataObserver(mObserver);
        // Initial check for empty state
        if (mAdapter.getItemCount() > 0) {
            mEmptyStateTextView.setVisibility(View.INVISIBLE);
        } else {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
        }
        // setHasOptionsMenu(true); // DEPRECATED: This line is removed.
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Obtain the MenuHost and add the MenuProvider.
        // 'this' (the Fragment) acts as the MenuHost and MenuProvider.
        // The MenuProvider will be active when the Fragment's view is RESUMED.
        // It will be automatically removed when the getViewLifecycleOwner() is destroyed.
        MenuHost menuHost = requireActivity(); // Or requireActivity() if more appropriate for your app structure
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.STARTED);
    }

    private void setupLayoutManager() {
        if (mRecyclerview == null) return; // Guard against null RecyclerView
        if (mUseLinearLayout) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
            if (addAtEnd) {
                linearLayoutManager.setReverseLayout(true);
                linearLayoutManager.setStackFromEnd(true);
            }
            mRecyclerview.setLayoutManager(linearLayoutManager);
        } else {
            mRecyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        }
    }

    void getNotesFromDb(String text) {
        mNoteViewModel.search(text).observe(getViewLifecycleOwner(), notes -> {
            if (mAdapter != null) {
                mAdapter.setDataList(notes);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdapter != null && mObserver != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
        }
        mRecyclerview = null;
        mAdapter = null;
        mEmptyStateTextView = null;
        mObserver = null;
        // No need to explicitly call removeMenuProvider(this) if added with getViewLifecycleOwner(),
        // as it's automatically handled.
    }

    // --- MenuProvider Implementation ---

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // This method replaces the old onPrepareOptionsMenu.
        // If this fragment was responsible for inflating its own menu items, you would call:
        // menuInflater.inflate(R.menu.your_fragment_menu, menu);
        // However, the original onPrepareOptionsMenu modified existing menu items,
        // so we replicate that behavior here.

        MenuItem mSearchMenuItem = menu.findItem(R.id.search);
        if (mSearchMenuItem != null && mSearchMenuItem.getActionView() instanceof SearchView) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();

            // Ensure delete_notes is hidden if it exists in this menu
            MenuItem deleteNotesItem = menu.findItem(R.id.delete_notes);
            if (deleteNotesItem != null) {
                deleteNotesItem.setVisible(false);
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null) {
                        getNotesFromDb(query);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null) {
                        getNotesFromDb(newText);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // This method replaces the old onOptionsItemSelected.
        int itemId = menuItem.getItemId();
        if (itemId == R.id.change_layout) {
            mUseLinearLayout = !mUseLinearLayout;
            if (mUseLinearLayout) {
                menuItem.setIcon(R.drawable.ic_baseline_view_list_24);
            } else {
                menuItem.setIcon(R.drawable.ic_baseline_view_grid);
            }
            setupLayoutManager();
            if (mRecyclerview != null) { // Guard against null RecyclerView
                Utils.runLayoutAnimation(mRecyclerview);
            }
            return true; // Indicate that the menu item selection was handled.
        }
        // Return false if the item was not handled by this provider,
        // allowing other MenuProviders or the Activity to handle it.
        return false;
    }

    // The deprecated methods 'onPrepareOptionsMenu' and 'onOptionsItemSelected'
    // should be removed from this class as their functionality is now in
    // 'onCreateMenu' and 'onMenuItemSelected' respectively.
}