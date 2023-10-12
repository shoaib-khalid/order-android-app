package com.ekedai.merchant.ui.voucher;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchViewModel extends ViewModel {
    private final MutableLiveData<String> searchTerm = new MutableLiveData<>("");
    public void setSearchTerm(String inputText) {
        searchTerm.setValue(inputText);
    }
    public LiveData<String> getSearchTerm() {
        return searchTerm;
    }

}
