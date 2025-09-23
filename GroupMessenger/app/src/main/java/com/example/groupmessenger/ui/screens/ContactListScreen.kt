package com.example.groupmessenger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.groupmessenger.data.model.Contact
import com.example.groupmessenger.ui.components.ContactItem
import com.example.groupmessenger.util.PermissionUtil
import com.example.groupmessenger.viewmodel.ContactsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    onNavigateToGroups: () -> Unit,
    onNavigateToMessage: (List<Contact>) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val contactsPermission = rememberPermissionState(PermissionUtil.PERMISSION_READ_CONTACTS)
    
    LaunchedEffect(contactsPermission.hasPermission) {
        if (contactsPermission.hasPermission) {
            viewModel.loadContacts()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("연락처") },
                actions = {
                    IconButton(onClick = onNavigateToGroups) {
                        Icon(Icons.Default.Add, contentDescription = "그룹 관리")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedContacts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onNavigateToMessage(selectedContacts) }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "메시지 보내기")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!contactsPermission.hasPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "연락처 접근 권한이 필요합니다",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { contactsPermission.launchPermissionRequest() }
                        ) {
                            Text("권한 허용")
                        }
                    }
                }
            } else {
                if (selectedContacts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedContacts.size}명 선택됨")
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("선택 해제")
                            }
                        }
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(contacts) { contact ->
                            ContactItem(
                                contact = contact,
                                onContactClick = { viewModel.toggleContactSelection(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

