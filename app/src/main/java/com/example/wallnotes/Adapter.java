package com.example.wallnotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private List<Note> data;
    private final OnClickNoteListener onClickNoteListener;
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, null, false);
        return new ViewHolder(view, onClickNoteListener);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(data.get(position));
    }
    public void setData(List<Note> newData){
        if(data != null){
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }else{
            data = newData;
        }
    }
    public List<Note> getData(){
        return data;
    }
    public Adapter(List<Note> data, OnClickNoteListener onClickNoteListener){
        this.data = data;
        this.onClickNoteListener = onClickNoteListener;
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
    static public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView text;
        private final OnClickNoteListener onClickNoteListener;
        public ViewHolder(@NonNull View itemView, OnClickNoteListener onClickNoteListener) {
            super(itemView);
            this.onClickNoteListener = onClickNoteListener;
            text = itemView.findViewById(R.id.title);
            ViewTreeObserver observer = text.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int maxLines = (int) text.getHeight()
                            / text.getLineHeight();
                    text.setMaxLines(maxLines);
                    text.getViewTreeObserver().removeOnGlobalLayoutListener(
                            this);
                }
            });
            itemView.setOnClickListener(this);
        }
        public void setData(Note note) {
            this.text.setText(note.text);
        }
        @Override
        public void onClick(View v) {
            onClickNoteListener.onClick(getAdapterPosition());
        }
    }
    public interface OnClickNoteListener{
        void onClick(int n);
    }
}