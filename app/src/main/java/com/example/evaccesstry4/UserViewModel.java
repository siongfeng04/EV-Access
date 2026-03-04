package com.example.evaccesstry4;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UserViewModel extends ViewModel {

    private final MutableLiveData<String> role = new MutableLiveData<>("driver");

    // Getter for fragments to observe
    public LiveData<String> getRole() {
        return role;
    }

    // Setter for fragments to update the role
    public void setRole(String newRole) {
        role.setValue(newRole);
    }
}