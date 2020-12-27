package com.example.wallnotes;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class RecycleBinAdapter extends RecyclerView.Adapter<RecycleBinAdapter.ViewHolder>{
    private List<Note> mData;
    @NonNull
    @Override
    public RecycleBinAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item_list, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull RecycleBinAdapter.ViewHolder holder, int position) {
        Note note = mData.get(position);
        holder.setData(note);
        holder.imageView.setImageDrawable(null);
        if(note.getImgUri() != null){
            Glide.with(holder.itemView.getContext())
                    .load(note.getImgUri())
                    .error(R.drawable.reload)
                    .into(holder.imageView);
        }
    }
    @Override
    public int getItemCount() {
        return mData.size();
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
    public RecycleBinAdapter(List<Note> data){
        this.mData = data;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
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
        }
        public void setData(Note note) {
            String t = note.getTitle() + "\n" + note.getContent();
            text.setText(t);
        }
    }
}
