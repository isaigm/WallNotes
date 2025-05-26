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
        return oldList.get(oldItemPosition).getUid() == newList.get(newItemPosition).getUid();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Note oldNote = oldList.get(oldItemPosition);
        Note newNote = newList.get(newItemPosition);

        return Objects.equals(oldNote.getTitle(), newNote.getTitle()) &&
                Objects.equals(oldNote.getContent(), newNote.getContent()) &&
                Objects.equals(oldNote.getImgUri(), newNote.getImgUri()) &&
                Objects.equals(oldNote.getLocation(), newNote.getLocation()) &&
                Objects.equals(oldNote.getAudio(), newNote.getAudio()) &&
                Objects.equals(oldNote.getRemindDate(), newNote.getRemindDate());
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