package com.example.wallnotes.ui.recyclebin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.wallnotes.Note;
import com.example.wallnotes.NoteViewModel;
import com.example.wallnotes.R;
import com.example.wallnotes.RecycleBinAdapter;
import com.example.wallnotes.Utils;
import java.util.ArrayList;
import java.util.List;

public class RecycleBinFragment extends Fragment {

    private RecycleBinAdapter mRecycleBinAdapter;
    private NoteViewModel mNoteViewModel;
    private RecyclerView.AdapterDataObserver mObserver;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recyclebin, container, false);
        mNoteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        RecyclerView mRecyclerview = root.findViewById(R.id.bin_recycler_view);
        mRecyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        List<Note> data = new ArrayList<>();
        mRecycleBinAdapter = new RecycleBinAdapter(data, getActivity(), mNoteViewModel);
        mNoteViewModel.getNotesToBeDeleted().observe(getViewLifecycleOwner(), mRecycleBinAdapter::setDataList);
        mRecyclerview.setAdapter(mRecycleBinAdapter);
        final TextView tv = root.findViewById(R.id.text_bin);
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(mRecycleBinAdapter.getItemCount() > 0){
                    tv.setVisibility(View.INVISIBLE);
                }else if (mRecycleBinAdapter.getItemCount() == 0){
                    tv.setVisibility(View.VISIBLE);
                }
            }
        };
        setHasOptionsMenu(true);
        mRecycleBinAdapter.registerAdapterDataObserver(mObserver);
        return root;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.delete_notes)
        {
            Log.d("RecycleBinFragment", "onOptionsItemSelected: delete_notes");
            List<Note> data = mRecycleBinAdapter.getDataList();
            if(data != null)
            {
                if(data.isEmpty())
                {
                    Utils.showMessage(requireContext(), "No hay más notas por eliminar");
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Alerta");
                    builder.setMessage("Todas las notas se van a eliminar permanentemente");
                    builder.setNegativeButton("Cancelar", null);
                    builder.setPositiveButton("Aceptar", (dialog, which) -> {
                        for(Note n: data)
                        {
                            mNoteViewModel.delete(n);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.change_layout).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mRecycleBinAdapter != null && mObserver != null){
            mRecycleBinAdapter.unregisterAdapterDataObserver(mObserver);
        }
    }
}