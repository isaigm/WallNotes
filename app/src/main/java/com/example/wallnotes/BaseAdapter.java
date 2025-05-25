package com.example.wallnotes;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<VH extends BaseAdapter.BaseViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<Note> mData;
    protected final List<Note> mSelectedNotes = new ArrayList<>();
    protected final Activity mActivity;
    protected SizeViewModel mSizeViewModel;
    protected boolean mIsEnable = false;
    protected boolean mIsSelectAll = false;
    protected final NoteViewModel mNoteViewModel;

    public BaseAdapter(List<Note> data, Activity activity, NoteViewModel noteViewModel) {
        this.mData = (data == null) ? new ArrayList<>() : new ArrayList<>(data); // Use a mutable copy
        this.mActivity = activity;
        this.mNoteViewModel = noteViewModel;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(getListItemLayoutResId(), parent, false);
        // Initialize ViewModel here if not already, or ensure it's available
        if (mSizeViewModel == null && mActivity instanceof ViewModelStoreOwner) {
            mSizeViewModel = new ViewModelProvider((ViewModelStoreOwner) mActivity).get(SizeViewModel.class);
        }
        return createViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Note note = mData.get(position);

        boolean isSelected = mIsSelectAll || mSelectedNotes.contains(note);
        holder.updateSelectionVisuals(isSelected, getDefaultItemBackgroundColor(holder.itemView.getContext()), getSelectedItemBackgroundColor(holder.itemView.getContext()));
        holder.bindNoteData(note); // Common binding for title, content, image
        bindSpecificData(holder, note); // For subclass-specific views

        holder.itemView.setOnLongClickListener(v -> {
            if (!mIsEnable) {
                ActionMode.Callback callback = new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(getActionModeMenuResId(), menu);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        mIsEnable = true;
                        // Select the item that triggered action mode
                        // 'note' and 'holder' are captured from onBindViewHolder's scope
                        if (!mSelectedNotes.contains(note)) {
                            clickItem(holder, note);
                        } else { // If already selected, ensure count is updated for title
                            if (mSizeViewModel != null) {
                                mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
                            }
                        }

                        if (mSizeViewModel != null && mActivity instanceof LifecycleOwner) {
                            mSizeViewModel.getText().observe((LifecycleOwner) mActivity,
                                    s -> {
                                        if (mode != null && s != null) { // Check for null mode and s
                                            mode.setTitle(s + " " + getActionModeTitleSuffix());
                                        }
                                    });
                        }
                        return true;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        return handleActionItemClick(mode, item, new ArrayList<>(mSelectedNotes), mData);
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        mIsEnable = false;
                        mIsSelectAll = false; // This ensures onBindViewHolder renders items as not selected

                        // Keep track of which items were selected to update only them
                        List<Object> previouslySelectedIds = new ArrayList<>();
                        if (mData != null) { // Ensure mData is not null
                            for (Note selectedNote : mSelectedNotes) {
                                // Assuming Note has a getUid() method returning a unique ID (e.g., long, String)
                                previouslySelectedIds.add(selectedNote.getUid());
                            }
                        }

                        mSelectedNotes.clear();
                        if (mSizeViewModel != null) {
                            mSizeViewModel.setText("0"); // Reset count
                        }

                        // Notify changes only for the items that were deselected
                        if (mData != null) {
                            for (int i = 0; i < mData.size(); i++) {
                                Note currentNote = mData.get(i);
                                // Check if this note was in the previouslySelectedIds list
                                if (previouslySelectedIds.contains(currentNote.getUid())) {
                                    notifyItemChanged(i);
                                }
                            }
                        }
                        // If no items were selected, or mData is null, no notifications are sent.
                    }
                };
                ((AppCompatActivity) v.getContext()).startActionMode(callback);
            } else {
                // If action mode is already enabled, a long click might have a different meaning
                // or just toggle selection like a normal click.
                clickItem(holder, note);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (mIsEnable) {
                clickItem(holder, note);
            } else {
                handleRegularItemClick(note, holder.getAdapterPosition());
            }
        });
    }

    protected void clickItem(VH holder, Note note) {
        int defaultColor = getDefaultItemBackgroundColor(holder.itemView.getContext());
        int selectedColor = getSelectedItemBackgroundColor(holder.itemView.getContext());
        if (mSelectedNotes.contains(note)) {
            mSelectedNotes.remove(note);
            holder.updateSelectionVisuals(false, defaultColor, selectedColor);
        } else {
            mSelectedNotes.add(note);
            holder.updateSelectionVisuals(true, defaultColor, selectedColor);
        }
        if (mSizeViewModel != null) {
            mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
        }
    }
    public void setDataList(List<Note> newData) {
        final List<Note> oldNotes = new ArrayList<>(this.mData); // Current data becomes old data
        final List<Note> newNotesList = (newData == null) ? new ArrayList<>() : new ArrayList<>(newData); // Use a mutable copy of new data

        // Calculate the diff
        NoteDiffCallback diffCallback = new NoteDiffCallback(oldNotes, newNotesList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        // Update the internal data list *before* dispatching updates
        this.mData.clear();
        this.mData.addAll(newNotesList);

        // Reconcile selected notes based on the new mData
        if (mIsEnable) {
            List<Note> stillPresentSelectedNotes = new ArrayList<>();
            // Iterate over a copy of mSelectedNotes to avoid concurrent modification issues if needed,
            // though here we are rebuilding it.
            List<Note> currentSelectedCopy = new ArrayList<>(mSelectedNotes);
            mSelectedNotes.clear();

            for (Note previouslySelectedNote : currentSelectedCopy) {
                for (Note newNoteInDataSet : this.mData) { // Check against the new dataset
                    // Assuming Note has a getUid() for unique and reliable comparison
                    if (newNoteInDataSet.getUid() == previouslySelectedNote.getUid()) {
                        stillPresentSelectedNotes.add(newNoteInDataSet); // Add instance from the new list
                        break;
                    }
                }
            }
            mSelectedNotes.addAll(stillPresentSelectedNotes);
            if (mSizeViewModel != null) {
                mSizeViewModel.setText(String.valueOf(mSelectedNotes.size()));
            }
            // Items that were selected but are no longer in the list will be removed from mSelectedNotes.
            // Items that were selected and are still in the list remain selected.
            // The DiffResult will handle notifying changes for items whose content might have changed,
            // and onBindViewHolder will update their selection visuals.
        }

        // Dispatch the calculated diff to the adapter
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public List<Note> getDataList() {
        return mData;
    }

    protected abstract boolean handleActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotesCopy, List<Note> allNotes);

    protected int getSelectedItemBackgroundColor(Context context) {
        TypedValue typedValueSelected = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.cardItemBackgroundSelected, typedValueSelected, true);
        return typedValueSelected.data;
    }

    protected int getDefaultItemBackgroundColor(Context context) {
        TypedValue typedValueDefault = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.cardItemBackgroundDefault, typedValueDefault, true);
        return typedValueDefault.data;
    }
    // Abstract methods to be implemented by subclasses
    protected abstract int getListItemLayoutResId();
    protected abstract VH createViewHolder(View view);
    protected abstract int getActionModeMenuResId();
    protected abstract String getActionModeTitleSuffix();
    protected abstract boolean handleSpecificActionItemClick(ActionMode mode, MenuItem item, List<Note> selectedNotes, List<Note> allNotes);
    protected abstract void handleRegularItemClick(Note note, int position);
    protected abstract void bindSpecificData(VH holder, Note note);

    // --- Inner BaseViewHolder ---
    public static class BaseViewHolder extends RecyclerView.ViewHolder {
        public final CardView cardView;
        public final ImageView imageView;
        public final TextView title;
        public final TextView content;

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv);
            imageView = itemView.findViewById(R.id.img);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
        }

        public void bindNoteData(Note note) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            imageView.setImageDrawable(null); // Reset before loading new image
            if (note.getImgUri() != null) {
                Glide.with(itemView.getContext())
                        .load(note.getImgUri())
                        .error(R.drawable.reload) // Ensure this drawable exists and is accessible
                        .into(imageView);
            } else {
                // Optionally set a placeholder or hide if no image URI
                imageView.setImageDrawable(null); // Or hide: imageView.setVisibility(View.GONE);
            }
        }

        public void updateSelectionVisuals(boolean isSelected, int defaultColor, int selectedColor) {
            cardView.setCardBackgroundColor(isSelected ? selectedColor : defaultColor);
        }
    }
}