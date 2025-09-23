package com.example.groupmessenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.groupmessenger.data.model.Contact
import com.example.groupmessenger.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {
    
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()
    
    private val _selectedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContacts: StateFlow<List<Contact>> = _selectedContacts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val contactList = contactRepository.getAllContacts()
                _contacts.value = contactList
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleContactSelection(contact: Contact) {
        val currentSelected = _selectedContacts.value.toMutableList()
        if (currentSelected.contains(contact)) {
            currentSelected.remove(contact)
        } else {
            currentSelected.add(contact)
        }
        _selectedContacts.value = currentSelected
        
        // Update contacts list with selection state
        _contacts.value = _contacts.value.map { 
            if (it.id == contact.id) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
    }
    
    fun clearSelection() {
        _selectedContacts.value = emptyList()
        _contacts.value = _contacts.value.map { it.copy(isSelected = false) }
    }
}

