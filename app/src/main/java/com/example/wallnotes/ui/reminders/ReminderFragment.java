package com.example.wallnotes.ui.reminders; // Asegúrate que el package sea el correcto

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.wallnotes.NoteAdapter;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import com.example.wallnotes.Utils;
import java.util.ArrayList;

public class ReminderFragment extends Fragment {

    private RecyclerView mRecyclerview;
    private NoteViewModel mNoteViewModel;
    private NoteAdapter mAdapter;
    private boolean mUseLinearLayout = true;
    private RecyclerView.AdapterDataObserver mObserver;
    private TextView tvReminder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupRecyclerView();
        setupObserver();
        setupMenu();
    }

    private void setupViews(View root) {
        mRecyclerview = root.findViewById(R.id.recycler_view_reminder);
        tvReminder = root.findViewById(R.id.text_reminder);
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
    }
    private void setupRecyclerView() {
        mAdapter = new NoteAdapter(new ArrayList<>(), getActivity(), mNoteViewModel);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerview.setAdapter(mAdapter);
        mNoteViewModel.getReminders().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                mAdapter.setDataList(notes);
            }
        });
    }

    private void setupObserver() {
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mAdapter.getItemCount() > 0) {
                    tvReminder.setVisibility(View.INVISIBLE);
                } else {
                    tvReminder.setVisibility(View.VISIBLE);
                }
            }
        };
        mAdapter.registerAdapterDataObserver(mObserver);
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // No es necesario si el menú ya se infla en la Activity
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.change_layout) {
                    mUseLinearLayout = !mUseLinearLayout;
                    if (mUseLinearLayout) {
                        menuItem.setIcon(R.drawable.ic_baseline_view_list_24);
                        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
                    } else {
                        menuItem.setIcon(R.drawable.ic_baseline_view_grid);
                        mRecyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                    }
                    // La animación se puede aplicar directamente al LayoutManager
                    Utils.runLayoutAnimation(mRecyclerview);
                    return true; // Evento manejado
                }
                return false; // Evento no manejado
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                menu.findItem(R.id.delete_notes).setVisible(false);

                MenuItem mSearchMenuItem = menu.findItem(R.id.search);
                SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        getNotesFromDb(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        getNotesFromDb(newText);
                        return true;
                    }
                });
            }
        }, getViewLifecycleOwner(), Lifecycle.State.STARTED);
    }

    void getNotesFromDb(String text) {
        // Usamos getViewLifecycleOwner() para que la observación sea segura
        mNoteViewModel.search(text).observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                if (!text.isEmpty()) {
                    mAdapter.setDataList(notes);
                } else {
                    // Si el texto está vacío, restaurar la lista original de recordatorios
                    mAdapter.setDataList(mNoteViewModel.getReminders().getValue());
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Es la práctica correcta anular el registro aquí.
        if (mAdapter != null && mObserver != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
        }
    }
}