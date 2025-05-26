package com.example.wallnotes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.List;

// Asumimos que BaseAdapter extiende o es un RecyclerView.Adapter
// import androidx.recyclerview.widget.RecyclerView;

public class NoteAdapter extends BaseAdapter<NoteAdapter.NoteViewHolder> {

    public NoteAdapter(List<Note> data, Activity activity, NoteViewModel noteViewModel) {
        super(data, activity, noteViewModel);
    }

    @Override
    protected boolean handleActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotesCopy, List<Note> allNotes) {
        if (item.getItemId() == R.id.menu_select_all) { // Example: common select all ID
            if (mSelectedNotes.size() == mData.size()) { // All are selected, so deselect all
                mIsSelectAll = false;
                mSelectedNotes.clear();
            } else { // Not all (or none) are selected, so select all
                mIsSelectAll = true;
                mSelectedNotes.clear();
                if (mData != null) {
                    mSelectedNotes.addAll(mData);
                }
            }
            if (mSizeViewModel != null) {
                mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
            }

            // Reemplazar notifyDataSetChanged()
            // notifyDataSetChanged(); // Método anterior, desaconsejado para RecyclerView

            // Nuevo método: notificar que el rango de elementos ha cambiado.
            // Esto es más eficiente y permite animaciones.
            // Asumimos que mData contiene todos los elementos actualmente en el adaptador.
            // Si tu BaseAdapter tiene un método como getItemCount(), es preferible usarlo.
            if (mData != null) {
                notifyItemRangeChanged(0, mData.size());
            } else {
                // Si mData es null, podría significar una lista vacía.
                // Notificar un rango de 0 o manejar según la lógica de tu app.
                notifyItemRangeChanged(0, 0);
            }
            // Alternativamente, si tu BaseAdapter implementa getItemCount()
            // y este refleja correctamente el tamaño de la lista (incluso si mData es null):
            // notifyItemRangeChanged(0, getItemCount());

            return true;
        }
        return handleSpecificActionItemClick(mode, item, selectedNotesCopy, allNotes);
    }

    @Override
    protected int getListItemLayoutResId() {
        return R.layout.note_item_list;
    }

    @Override
    protected NoteViewHolder createViewHolder(View view) {
        return new NoteViewHolder(view);
    }

    @Override
    protected int getActionModeMenuResId() {
        return R.menu.submenu;
    }

    @Override
    protected String getActionModeTitleSuffix() {
        return " seleccionados";
    }

    @Override
    protected boolean handleSpecificActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotes, List<Note> allNotes) {
        int id = item.getItemId();
        if (id == R.id.menu_delete) {
            for (Note note : selectedNotes) { // Iterate over the copy passed
                note.setGoingToBeDeleted(true);
                cancelAlarm(mActivity, note);
                note.setRemindDate(null);
                mNoteViewModel.update(note);
            }
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    protected void handleRegularItemClick(Note note, int position) {
        Intent intent = new Intent(mActivity, EditNoteActivity.class);
        intent.putExtra("uid", note.getUid());
        mActivity.startActivity(intent);
        mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void bindSpecificData(NoteViewHolder holder, Note note) {
        holder.location.setVisibility(View.GONE);
        holder.icon.setVisibility(View.GONE);
        holder.remindDate.setVisibility(View.GONE);

        if (note.getRemindDate() != null) {
            SimpleDateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy hh:mm");
            Log.d("DATE", dateFor.format(note.getRemindDate()));
            holder.remindDate.setVisibility(View.VISIBLE);
            holder.remindDate.setText(dateFor.format(note.getRemindDate()));
        }
        if (note.getLocation() != null) {
            holder.location.setVisibility(View.VISIBLE);
            holder.location.setText(note.getLocation());
        }
        if (note.getAudio() != null) {
            holder.icon.setVisibility(View.VISIBLE);
        }
    }

    void cancelAlarm(Activity activity, Note note) {
        if (note.getRemindDate() == null) return;
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(activity.getApplicationContext(), NotifierAlarm.class);

        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = PendingIntent.FLAG_NO_CREATE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                activity.getApplicationContext(),
                note.getUid(),
                myIntent,
                flags
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        } else {
            Log.w("NoteAdapter", "PendingIntent to cancel alarm for note UID " + note.getUid() + " not found.");
        }
    }

    public static class NoteViewHolder extends BaseAdapter.BaseViewHolder {
        private final TextView remindDate;
        private final TextView location;
        private final ImageView icon;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.audio_icon);
            location = itemView.findViewById(R.id.loc);
            remindDate = itemView.findViewById(R.id.remind_date);
        }
    }
}