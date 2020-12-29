package com.example.wallnotes;

import android.app.Activity;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class RecycleBinAdapter extends RecyclerView.Adapter<RecycleBinAdapter.ViewHolder>{
    private List<Note> mData;
    private final List<Note> mSelectedNotes = new ArrayList<>();
    private final Activity mActivity;
    private SizeViewModel mSizeViewModel;
    private final NoteViewModel mNoteViewModel;
    private boolean mIsEnable = false;
    private boolean mIsSelectAll = false;
    @NonNull
    @Override
    public RecycleBinAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item_list, parent, false);
        mSizeViewModel = new ViewModelProvider((ViewModelStoreOwner) mActivity).get(SizeViewModel.class);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull RecycleBinAdapter.ViewHolder holder, int position) {
        Note note = mData.get(position);
        holder.itemView.setOnLongClickListener(v -> {
            if(!mIsEnable){
                ActionMode.Callback callback = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.bin_menu, menu);
                        return true;
                    }
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        mIsEnable = true;
                        clickItem(holder);
                        mSizeViewModel.getText().observe((LifecycleOwner) mActivity,
                                s -> mode.setTitle(s + " seleccionados"));
                        return true;
                    }
                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        int id = item.getItemId();
                        if(id == R.id.menu_effective_delete){
                            for(int i = 0; i < mSelectedNotes.size(); i++){
                                Note n = mSelectedNotes.get(i);
                                mNoteViewModel.delete(n);

                            }
                            mode.finish();
                        }
                        else if(id == R.id.menu_restore)
                        {
                            for(int i = 0; i < mSelectedNotes.size(); i++){
                                Note n = mSelectedNotes.get(i);
                                n.setGoingToBeDeleted(false);
                                mNoteViewModel.update(n);
                            }
                            mode.finish();
                        }
                        return true;
                    }
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        mIsEnable = false;
                        mIsSelectAll = false;
                        mSelectedNotes.clear();
                        notifyDataSetChanged();
                    }
                };
                ((AppCompatActivity)v.getContext()).startActionMode(callback);
            }else{
                clickItem(holder);
            }
            return true;
        });
        holder.itemView.setOnClickListener(v -> {
            if(mIsEnable){
                clickItem(holder);
            }
        });
        if(mIsSelectAll){
            holder.cardView.setCardBackgroundColor(0x60000000);
        }
        else{
            holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
        }
        holder.setData(note);
        holder.imageView.setImageDrawable(null);
        if(note.getImgUri() != null){
            Glide.with(holder.itemView.getContext())
                    .load(note.getImgUri())
                    .error(R.drawable.reload)
                    .into(holder.imageView);
        }
    }
    private void clickItem(RecycleBinAdapter.ViewHolder holder) {
        Note note = mData.get(holder.getAdapterPosition());
        if(holder.cardView.getCardBackgroundColor().getDefaultColor() != 0x60000000){
            holder.cardView.setCardBackgroundColor(0x60000000);
            mSelectedNotes.add(note);
        }else{
            holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
            mSelectedNotes.remove(note);
        }
        mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
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
    public List<Note> getmData(){
        return mData;
    }
    public RecycleBinAdapter(List<Note> data, Activity activity, NoteViewModel noteViewModel){
        this.mActivity = activity;
        this.mData = data;
        this.mNoteViewModel = noteViewModel;
    }
    static public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final ImageView imageView;
        private final CardView cardView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            imageView = itemView.findViewById(R.id.img);
            cardView = itemView.findViewById(R.id.cv);
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
