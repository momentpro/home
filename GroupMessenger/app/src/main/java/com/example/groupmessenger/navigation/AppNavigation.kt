package com.example.groupmessenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.groupmessenger.data.model.Contact
import com.example.groupmessenger.ui.screens.ContactListScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "contacts"
    ) {
        composable("contacts") {
            ContactListScreen(
                onNavigateToGroups = {
                    // 그룹 화면으로 네비게이션 (추후 구현)
                },
                onNavigateToMessage = { contacts ->
                    // 메시지 화면으로 네비게이션 (추후 구현)
                }
            )
        }
        
        // 다른 화면들은 추후 추가
    }
}









