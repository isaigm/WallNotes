package com.example.wallnotes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private List<Note> mData;
    private final List<Note> mSelectedNotes = new ArrayList<>();
    private final Activity mActivity;
    private SizeViewModel mSizeViewModel;
    private boolean mIsEnable = false;
    private boolean mIsSelectAll = false;
    private final NoteViewModel mNoteViewModel;
    void cancelAlarm(Activity activity, Note note)
    {
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(activity.getApplicationContext(), NotifierAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity.getApplicationContext(), note.getUid(), myIntent, 0);
        alarmManager.cancel(pendingIntent);
    }
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
                           for(Note note : mSelectedNotes){
                               note.setGoingToBeDeleted(true);
                               cancelAlarm(mActivity, note);
                               note.setRemindDate(null);
                               mNoteViewModel.update(note);
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
                intent.putExtra("uid", note.getUid());
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
    public NoteAdapter(List<Note> data, Activity activity, NoteViewModel noteViewModel){
        this.mData = data;
        this.mActivity = activity;
        this.mNoteViewModel = noteViewModel;
    }
    @Override
    public int getItemCount() {
        return mData.size();
    }
    static public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView content;
        private final TextView remindDate;
        private final TextView location;
        private final ImageView imageView;
        private final CardView cardView;
        private final ImageView icon;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.audio_icon);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            location = itemView.findViewById(R.id.loc);
            imageView = itemView.findViewById(R.id.img);
            cardView = itemView.findViewById(R.id.cv);
            remindDate = itemView.findViewById(R.id.remind_date);
        }
        public void setData(Note note) {
            if(note.getRemindDate() != null){
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy hh:mm");
                remindDate.setVisibility(View.VISIBLE);
                remindDate.setText(dateFor.format(note.getRemindDate()));
            }
            if(note.getLocation() != null)
            {
                location.setVisibility(View.VISIBLE);
                location.setText(note.getLocation());
            }
            if(note.getAudio() != null){
                icon.setVisibility(View.VISIBLE);
            }
            title.setText(note.getTitle());
            content.setText(note.getContent());
        }
    }
}