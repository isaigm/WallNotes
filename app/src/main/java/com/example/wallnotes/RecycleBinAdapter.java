package com.example.wallnotes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
// Import other necessary classes like Note, NoteViewModel, SizeViewModel, R, etc.
// These should be available from the package or common Android/androidx libraries.
import androidx.annotation.NonNull;

import java.util.List;

public class RecycleBinAdapter extends BaseAdapter<RecycleBinAdapter.RecycleBinViewHolder> {

    public RecycleBinAdapter(List<Note> data, Activity activity, NoteViewModel noteViewModel) {
        super(data, activity, noteViewModel);
    }

    @Override
    protected boolean handleActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotesCopy, List<Note> allNotes) {
        return handleSpecificActionItemClick(mode, item, selectedNotesCopy, allNotes);
    }
    @Override
    protected int getListItemLayoutResId() {
        return R.layout.note_item_list; // Uses the same layout
    }

    @Override
    protected RecycleBinViewHolder createViewHolder(View view) {
        return new RecycleBinViewHolder(view);
    }

    @Override
    protected int getActionModeMenuResId() {
        return R.menu.bin_menu;
    }

    @Override
    protected String getActionModeTitleSuffix() {
        return " seleccionados"; // Or specific suffix for recycle bin
    }

    @Override
    protected boolean handleSpecificActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotes, List<Note> allNotes) {
        int id = item.getItemId();
        if (id == R.id.menu_effective_delete) {
            for (Note n : selectedNotes) { // Iterate over the copy
                mNoteViewModel.delete(n);
            }
            mode.finish();
            return true;
        } else if (id == R.id.menu_restore) {
            for (Note n : selectedNotes) { // Iterate over the copy
                n.setGoingToBeDeleted(false);
                mNoteViewModel.update(n);
            }
            mode.finish();
            return true;
        }

        return false;
    }

    @Override
    protected void handleRegularItemClick(Note note, int position) {
        // No action for regular click in RecycleBinAdapter
    }

    @Override
    protected void bindSpecificData(RecycleBinViewHolder holder, Note note) {
        // If R.layout.note_item_list contains views specific to NoteAdapter (like reminder date, location icon),
        // ensure they are hidden for RecycleBinAdapter items.
        // BaseViewHolder only binds title, content, and image.
        // If specific views like audio_icon, loc, remind_date from R.layout.note_item_list
        // are not part of RecycleBinViewHolder, they won't be touched.
        // If they ARE part of the layout and should be hidden:
        View audioIcon = holder.itemView.findViewById(R.id.audio_icon);
        if (audioIcon != null) audioIcon.setVisibility(View.GONE);

        View locationView = holder.itemView.findViewById(R.id.loc);
        if (locationView != null) locationView.setVisibility(View.GONE);

        View remindDateView = holder.itemView.findViewById(R.id.remind_date);
        if (remindDateView != null) remindDateView.setVisibility(View.GONE);
    }

    // --- Inner ViewHolder for RecycleBinAdapter ---
    public static class RecycleBinViewHolder extends BaseAdapter.BaseViewHolder {
        public RecycleBinViewHolder(@NonNull View itemView) {
            super(itemView);
            // No additional views beyond what BaseViewHolder provides
        }
    }
}