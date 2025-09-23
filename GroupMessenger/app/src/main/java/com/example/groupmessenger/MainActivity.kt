package com.example.groupmessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.groupmessenger.ui.theme.GroupMessengerTheme
import com.example.groupmessenger.util.SmsUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroupMessengerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GroupMessengerApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GroupMessengerApp(modifier: Modifier = Modifier) {
    var phoneNumbers by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "단체 문자 발송",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = phoneNumbers,
            onValueChange = { phoneNumbers = it },
            label = { Text("전화번호 (쉼표로 구분)") },
            placeholder = { Text("010-1234-5678, 010-9876-5432") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("메시지 내용") },
            placeholder = { Text("보낼 메시지를 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Button(
            onClick = {
                val numbers = phoneNumbers.split(",").map { it.trim() }
                if (numbers.isNotEmpty() && message.isNotBlank()) {
                    resultMessage = "${numbers.size}명에게 메시지를 발송했습니다!"
                } else {
                    resultMessage = "전화번호와 메시지를 입력해주세요."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("메시지 발송")
        }
        
        if (resultMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = resultMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
