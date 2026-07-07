package com.example.gymlog_finale.ui.community

// ViewModel della schermata Community: aggrega dati da FriendshipRepository e UserRepository.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog_finale.data.firebase.FirebaseFriendshipSource
import com.example.gymlog_finale.data.firebase.FirebaseUserSource
import com.example.gymlog_finale.data.model.FriendRequest
import com.example.gymlog_finale.data.model.FriendRequestType
import com.example.gymlog_finale.data.model.User
import com.example.gymlog_finale.data.repository.FriendshipRepository
import com.example.gymlog_finale.data.repository.UserRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class IncomingRequestUi: aggregato immutabile di dati.
data class IncomingRequestUi(
    val request: FriendRequest,
    val sender: User
)

// Data class OutgoingRequestUi: aggregato immutabile di dati.
data class OutgoingRequestUi(
    val request: FriendRequest,
    val receiver: User
)

// Data class CommunityUiState: aggregato immutabile di dati.
data class CommunityUiState(
    val currentUser: User? = null,
    val friends: List<User> = emptyList(),
    val ptClients: List<User> = emptyList(),
    val incomingRequests: List<IncomingRequestUi> = emptyList(),
    val outgoingRequests: List<OutgoingRequestUi> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val friendStats: Map<String, com.example.gymlog_finale.data.model.FriendStats> = emptyMap(),
    val successMessage: String? = null

) {

    val isCurrentUserPt: Boolean
        get() = currentUser?.isPersonalTrainer == true
}

// Classe CommunityViewModel: unità principale definita in questo file.
@OptIn(FlowPreview::class)
class CommunityViewModel(
    private val friendshipRepository: FriendshipRepository = FirebaseFriendshipSource(),
    private val userRepository: UserRepository = FirebaseUserSource()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var allUsersCache: List<User> = emptyList()
    private var hasLoadedUsers = false

    init {
        loadCurrentUser()
        observeAll()
        observeSearchQuery()
        loadInitialUsers()
    }

    // Carica i dati necessari per la schermata o il caso d'uso.
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val result = userRepository.fetchCurrentUser()

            if (result == null) {
                _uiState.update {
                    it.copy(errorMessage = "Nessuna sessione attiva")
                }
                return@launch
            }

            result.fold(
                onSuccess = { user ->
                    _uiState.update { currentState ->
                        currentState.copy(currentUser = user)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Errore nel caricamento profilo"
                        )
                    }
                }
            )
        }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    private fun observeAll() {
        viewModelScope.launch {
            friendshipRepository.observeFriendships().collect {
                refreshFriendsUsers()
            }
        }

        viewModelScope.launch {
            friendshipRepository.observeIncomingRequests().collect { requests ->
                val enriched = requests.mapNotNull { request ->
                    val user = userRepository.getUser(request.senderId).getOrNull()
                    if (user != null) IncomingRequestUi(request, user) else null
                }

                _uiState.update { it.copy(incomingRequests = enriched) }
            }
        }

        viewModelScope.launch {
            friendshipRepository.observeOutgoingRequests().collect { requests ->
                val enriched = requests.mapNotNull { request ->
                    val user = userRepository.getUser(request.receiverId).getOrNull()
                    if (user != null) OutgoingRequestUi(request, user) else null
                }
                _uiState.update { it.copy(outgoingRequests = enriched) }
            }
        }

        viewModelScope.launch {
            friendshipRepository.observePtClients().collect {
                refreshPtClients()
            }
        }
    }

    // Carica i dati necessari per la schermata o il caso d'uso.
    private fun loadInitialUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            userRepository.getAllUsersForCommunity().fold(
                onSuccess = { users ->
                    allUsersCache = users
                    hasLoadedUsers = true
                    _uiState.update {
                        it.copy(
                            searchResults = if (_searchQuery.value.trim().isEmpty()) emptyList() else filterCachedUsers(_searchQuery.value.trim()),
                            isSearching = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = error.message ?: "Errore nel caricamento utenti"
                        )
                    }
                }
            )
        }
    }

    // Funzione di supporto interna alla classe.
    private suspend fun reloadUsersFromRemote(): List<User>? {
        return userRepository.getAllUsersForCommunity().fold(
            onSuccess = { users ->
                allUsersCache = users
                hasLoadedUsers = true
                users
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Errore nell'aggiornamento utenti")
                }
                null
            }
        )
    }

    // Funzione di supporto interna alla classe.
    private fun filterCachedUsers(query: String): List<User> {
        val normalized = query.trim().lowercase()
        val currentUid = _uiState.value.currentUser?.uid

        val baseList = allUsersCache.filter { it.uid != currentUid }

        if (normalized.isBlank()) return baseList

        return baseList.filter { user ->
            user.username.lowercase().contains(normalized) ||
                    user.nome.lowercase().contains(normalized) ||
                    user.cognome.lowercase().contains(normalized)
        }
    }

    // Carica i dati necessari per la schermata o il caso d'uso.
    private fun loadStatsFor(users: List<User>) {
        viewModelScope.launch {
            val map = _uiState.value.friendStats.toMutableMap()
            users.forEach { u ->
                if (!map.containsKey(u.uid)) {
                    map[u.uid] = friendshipRepository.fetchUserStats(u.uid)
                }
            }
            _uiState.update { it.copy(friendStats = map) }
        }
    }

    // Forza il ricaricamento dei dati dalla sorgente.
    private fun refreshFriendsUsers() {
        viewModelScope.launch {
            friendshipRepository.fetchFriendsAsUsers().fold(
                onSuccess = { users ->
                    _uiState.update { it.copy(friends = users) }
                    loadStatsFor(users)
                },
                onFailure = { }
            )
        }
    }

    // Forza il ricaricamento dei dati dalla sorgente.
    private fun refreshPtClients() {
        viewModelScope.launch {
            friendshipRepository.fetchPtClientsAsUsers().fold(
                onSuccess = { users ->
                    _uiState.update { it.copy(ptClients = users) }
                    loadStatsFor(users)
                },
                onFailure = { }
            )
        }
    }

    // Espone un flusso reattivo che emette gli aggiornamenti dalla sorgente dati.
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val normalized = query.trim()

                    if (!hasLoadedUsers) {
                        loadInitialUsers()
                        return@collect
                    }

                    if (normalized.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                searchResults = emptyList(),
                                isSearching = false
                            )
                        }
                        return@collect
                    }

                    _uiState.update { it.copy(isSearching = true) }

                    val localResults = filterCachedUsers(normalized)
                    if (localResults.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                searchResults = localResults,
                                isSearching = false
                            )
                        }
                        return@collect
                    }

                    val refreshedUsers = reloadUsersFromRemote()
                    val refreshedResults = if (refreshedUsers != null) {
                        filterCachedUsers(normalized)
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            searchResults = refreshedResults,
                            isSearching = false
                        )
                    }
                }
        }
    }

    // Handler UI: aggiorna nello stato il campo modificato dall'utente.
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    // Invia la richiesta o il messaggio indicati.
    fun sendFriendshipRequest(receiverUid: String) = runOnVm {
        friendshipRepository.sendFriendRequest(
            receiverId = receiverUid,
            requestType = FriendRequestType.FRIENDSHIP.name
        ).reportAs("Richiesta amicizia inviata")
    }

    // Invia la richiesta o il messaggio indicati.
    fun sendPtCoachingRequest(receiverUid: String) = runOnVm {
        friendshipRepository.sendFriendRequest(
            receiverId = receiverUid,
            requestType = FriendRequestType.PT_COACHING.name
        ).reportAs("Richiesta coaching inviata")
    }

    // Accetta la richiesta ricevuta e aggiorna il relativo stato.
    fun acceptRequest(requestId: String) = runOnVm {
        friendshipRepository.acceptFriendRequest(requestId).reportAs("Richiesta accettata")
    }

    // Rifiuta la richiesta ricevuta.
    fun rejectRequest(requestId: String) = runOnVm {
        friendshipRepository.rejectFriendRequest(requestId).reportAs("Richiesta rifiutata")
    }

    // Annulla l'operazione in corso o la richiesta pendente.
    fun cancelRequest(requestId: String) = runOnVm {
        friendshipRepository.cancelFriendRequest(requestId).reportAs("Richiesta annullata")
    }

    // Elimina la relazione o l'elemento indicato.
    fun removeFriend(friendUid: String) = runOnVm {
        friendshipRepository.removeFriend(friendUid).reportAs("Amicizia rimossa")
    }

    // Elimina la relazione o l'elemento indicato.
    fun removePtClient(clientUid: String) = runOnVm {
        friendshipRepository.removePtClient(clientUid).reportAs("Cliente rimosso")
    }

    // Ripulisce lo stato o la collezione indicati.
    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }

    // Funzione di supporto interna alla classe.
    private inline fun runOnVm(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            block()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // Funzione di supporto interna alla classe.
    private fun Result<Unit>.reportAs(success: String) {
        fold(
            onSuccess = {
                _uiState.update { it.copy(successMessage = success) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Errore")
                }
            }
        )
    }
}