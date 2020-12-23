package com.example.wallnotes;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private List<Note> mData;
    private final OnClickNoteListener mOnClickNoteListener;
    private final OnLongClickListener mOnLongClickListener;
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, null, false);
        return new ViewHolder(view, mOnClickNoteListener, mOnLongClickListener);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = mData.get(position);
        holder.setData(note);
        if(note.getImgUri() != null){
            Picasso.get().load(Uri.parse(note.getImgUri()))
                    .into(holder.imageView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            System.out.println(e.toString());
                        }
                    });
            System.out.println("HERE");
            System.out.println(note.getImgUri());
        }
    }
    public void setmData(List<Note> newData){
        if(mData != null){
            mData.clear();
            mData.addAll(newData);
            notifyDataSetChanged();
        }else{
            mData = newData;
        }
    }
    public List<Note> getmData(){
        return mData;
    }
    public Adapter(List<Note> data, OnClickNoteListener onClickNoteListener, OnLongClickListener onLongClickListener){
        this.mData = data;
        this.mOnClickNoteListener = onClickNoteListener;
        this.mOnLongClickListener = onLongClickListener;
    }
    @Override
    public int getItemCount() {
        return mData.size();
    }
    static public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final TextView text;
        private final ImageView imageView;
        private final OnClickNoteListener onClickNoteListener;
        private final OnLongClickListener onLongClickListener;
        public ViewHolder(@NonNull View itemView, OnClickNoteListener onClickNoteListener, OnLongClickListener onLongClickListener) {
            super(itemView);
            this.onClickNoteListener = onClickNoteListener;
            this.onLongClickListener = onLongClickListener;
            text = itemView.findViewById(R.id.text);
            imageView = itemView.findViewById(R.id.img);
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
            itemView.setOnLongClickListener(this);
        }
        public void setData(Note note) {
            String t = note.getTitle() + "\n" + note.getContent();
            text.setText(t);
        }
        @Override
        public void onClick(View v) {
            onClickNoteListener.onClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            onLongClickListener.onLongClick(getAdapterPosition());
            return true;
        }
    }
    public interface OnClickNoteListener{
        void onClick(int n);
    }
    public interface OnLongClickListener{
        boolean onLongClick(int n);
    }
}