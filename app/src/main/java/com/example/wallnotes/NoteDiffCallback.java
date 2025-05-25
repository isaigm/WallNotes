package com.example.wallnotes;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;
import java.util.Objects;

public class NoteDiffCallback extends DiffUtil.Callback {

    private final List<Note> oldList;
    private final List<Note> newList;

    public NoteDiffCallback(List<Note> oldList, List<Note> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Implement this based on a unique, stable ID in your Note class
        // Assuming Note has a getUid() method that returns a unique identifier (e.g., long, String, int)
        return oldList.get(oldItemPosition).getUid() == newList.get(newItemPosition).getUid();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Note oldNote = oldList.get(oldItemPosition);
        Note newNote = newList.get(newItemPosition);

        // Compare all relevant fields that affect the visual representation of the Note.
        // Using Objects.equals for null-safety.
        return Objects.equals(oldNote.getTitle(), newNote.getTitle()) &&
                Objects.equals(oldNote.getContent(), newNote.getContent()) &&
                Objects.equals(oldNote.getImgUri(), newNote.getImgUri());
        // Add comparisons for any other fields that, if changed, should trigger a rebind.
    }

    // Optional: Implement getChangePayload for more granular updates if specific fields change.
    // This allows onBindViewHolder(VH holder, int position, List<Object> payloads) to handle
    // partial updates more efficiently.
    // @Nullable
    // @Override
    // public Object getChangePayload(int oldItemPosition, int newItemPosition) {
    //     // Example: Bundle changes and return it.
    //     // If null is returned, it's a full rebind for that item.
    //     return super.getChangePayload(oldItemPosition, newItemPosition);
    // }
}