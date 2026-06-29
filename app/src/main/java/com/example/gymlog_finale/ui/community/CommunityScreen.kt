package com.example.gymlog_finale.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog_finale.data.model.FriendStats
import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.ui.community.IncomingRequestUi
import com.example.gymlog_finale.ui.community.components.*

/**
 * Schermata principale Community con tab dinamici in base al ruolo dell'utente.
 * onCreateWorkoutForClient è invocato dal tab Clienti quando il PT clicca "Crea scheda".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBack: () -> Unit = {},
    onCreateWorkoutForClient: (clientUid: String, clientName: String) -> Unit = { _, _ -> },
    viewModel: CommunityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostra messaggi transienti come snackbar e li azzera nello state
    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val tabs = remember(state.isCurrentUserPt) {
        buildList {
            add("Amici")
            if (state.isCurrentUserPt) add("Clienti")
            add("Richieste")
            add("Cerca")
        }
    }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Community", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFF97316),
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .defaultMinSize(minWidth = 90.dp)
                            .selectable(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                role = Role.Tab,
                                interactionSource = interactionSource,
                                indication = null
                            )
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title, 
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) Color.Black else Color.Gray,
                            fontSize = 16.sp
                        ) 
                    }
                }
            }

            when (tabs[selectedTab]) {
                "Amici" -> FriendsTab(
                    friends = state.friends,
                    stats = state.friendStats,
                    onRemove = viewModel::removeFriend
                )
                "Clienti" -> PtClientsTab(
                    clients = state.ptClients,
                    stats = state.friendStats,
                    onRemove = viewModel::removePtClient,
                    onCreateWorkout = onCreateWorkoutForClient
                )
                "Richieste" -> RequestsTab(
                    incoming = state.incomingRequests,
                    outgoing = state.outgoingRequests,
                    onAccept = viewModel::acceptRequest,
                    onReject = viewModel::rejectRequest,
                    onCancel = viewModel::cancelRequest
                )
                "Cerca" -> SearchTab(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    isCurrentUserPt = state.isCurrentUserPt,
                    onSendFriend = viewModel::sendFriendshipRequest,
                    onSendCoaching = viewModel::sendPtCoachingRequest
                )
            }
        }
    }
}
