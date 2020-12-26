package com.example.wallnotes;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SizeViewModel extends ViewModel {
    private final MutableLiveData<String> mutableLiveData = new MutableLiveData<>();

    public MutableLiveData<String> getText() {
        return mutableLiveData;
    }
    public void setText(String text) {
        mutableLiveData.setValue(text);
    }
}
