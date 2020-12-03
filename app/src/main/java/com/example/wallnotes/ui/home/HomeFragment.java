package com.example.wallnotes.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallnotes.Adapter;
import com.example.wallnotes.EditNoteActivity;
import com.example.wallnotes.Note;
import com.example.wallnotes.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment implements Adapter.OnClickNoteListener {

    private HomeViewModel homeViewModel;
    private RecyclerView.AdapterDataObserver observer;
    private Adapter adapter;
    private BroadcastReceiver noteReceiver;
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Note> data = new ArrayList<>();
        adapter = new Adapter(data, this);
        homeViewModel.getAllNotes().observe(getViewLifecycleOwner(), adapter::setData);
        recyclerView.setAdapter(adapter);
        final TextView textView = root.findViewById(R.id.text_home);
        observer = new RecyclerView.AdapterDataObserver() {
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
        adapter.registerAdapterDataObserver(observer);
        noteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null){
                    boolean mustUpdateNote = intent.getBooleanExtra("must_update_note", false);
                    Note note = new Note(intent.getStringExtra("title"));
                    if(mustUpdateNote){
                        note.uid = intent.getIntExtra("uid", 0);
                        homeViewModel.updateNote(note);
                    }else homeViewModel.addNote(note);
                }
            }
        };
        IntentFilter filter = new IntentFilter("DATA");
        requireActivity().registerReceiver(noteReceiver, filter);
        return root;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(adapter != null && observer != null){
            adapter.unregisterAdapterDataObserver(observer);
        }
        if(noteReceiver != null){
            requireActivity().unregisterReceiver(noteReceiver);
        }
    }
    @Override
    public void onClick(int pos) {
        Note note = adapter.getData().get(pos);
        Intent intent = new Intent(getActivity(), EditNoteActivity.class);
        intent.putExtra("title", note.text);
        intent.putExtra("uid", note.uid);
        startActivity(intent);
    }
}