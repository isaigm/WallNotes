package com.example.wallnotes;

import android.app.Activity;
import android.content.Intent;
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

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private List<Note> mData;
    private final List<Note> mSelectedNotes = new ArrayList<>();
    private final Activity mActivity;
    private SizeViewModel mSizeViewModel;
    private boolean mIsEnable = false;
    private boolean mIsSelectAll = false;
    private final NoteViewModel mNoteViewModel;
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item_list, parent, false);
        mSizeViewModel = new ViewModelProvider((ViewModelStoreOwner) mActivity).get(SizeViewModel.class);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = mData.get(position);
        holder.itemView.setOnLongClickListener(v -> {
            if(!mIsEnable){
               ActionMode.Callback callback = new ActionMode.Callback() {
                   @Override
                   public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                       MenuInflater menuInflater = mode.getMenuInflater();
                       menuInflater.inflate(R.menu.submenu, menu);
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
                       if(id == R.id.menu_delete){
                           for(int i = 0; i < mSelectedNotes.size(); i++){
                               Note n = mSelectedNotes.get(i);
                               n.setGoingToBeDeleted(true);
                               mNoteViewModel.update(n);
                           }
                           mode.finish();
                       }
                       else if(id == R.id.menu_select_all)
                       {
                           if(mSelectedNotes.size() == mData.size())
                           {
                               mIsSelectAll = false;
                               mSelectedNotes.clear();
                           }else{
                               mIsSelectAll = true;
                               mSelectedNotes.clear();
                               mSelectedNotes.addAll(mData);
                           }
                           mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
                           notifyDataSetChanged();
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
        holder.setData(note);
        holder.imageView.setImageDrawable(null);
        holder.itemView.setOnClickListener(v -> {
            if(mIsEnable){
                clickItem(holder);
            }else {
                Note n = mData.get(holder.getAdapterPosition());
                Intent intent = new Intent(mActivity, EditNoteActivity.class);
                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("uid", note.getUid());
                intent.putExtra("img_uri", note.getImgUri());
                intent.putExtra("created_at", note.getCreatedAt());
                intent.putExtra("remind_date", note.getRemindDate());
                mActivity.startActivity(intent);
                mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        if(mIsSelectAll){
            holder.cardView.setCardBackgroundColor(0x60000000);
        }
        else{
            holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
        }
        if(note.getImgUri() != null){
            Glide.with(holder.itemView.getContext())
                    .load(note.getImgUri())
                    .error(R.drawable.reload)
                    .into(holder.imageView);
        }
    }

    private void clickItem(ViewHolder holder) {
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
    public void setmData(List<Note> newData){
        if(mData != null){
            mData.clear();
            mData.addAll(newData);
            notifyDataSetChanged();
        }else{
            mData = newData;
        }
    }
    public Adapter(List<Note> data, Activity activity, NoteViewModel noteViewModel){
        this.mData = data;
        this.mActivity = activity;
        this.mNoteViewModel = noteViewModel;
    }
    @Override
    public int getItemCount() {
        return mData.size();
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