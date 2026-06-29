package com.example.gymlog_finale.ui.community

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

/**
 * Richiesta in entrata arricchita con il profilo del mittente.
 */
data class IncomingRequestUi(
    val request: FriendRequest,
    val sender: User
)

/**
 * Richiesta in uscita arricchita con il profilo del destinatario.
 */
data class OutgoingRequestUi(
    val request: FriendRequest,
    val receiver: User
)

/**
 * Stato globale della schermata Community.
 */
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
    /**
     * Indica se l'utente corrente è un personal trainer.
     */
    val isCurrentUserPt: Boolean
        get() = currentUser?.isPersonalTrainer == true
}

/**
 * ViewModel della schermata Community con supporto richieste PT.
 */
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

    /**
     * Carica il profilo dell'utente autenticato.
     */
    /**
     * Carica il profilo dell'utente autenticato.
     */
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

    /**
     * Avvia i listener realtime di amicizie, richieste, clienti PT e blocchi.
     */
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

    /**
     * Carica tutti gli utenti iniziali, li salva in cache e li mostra nella ricerca.
     */
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

    /**
     * Ricarica da remoto tutti gli utenti aggiornando la cache locale.
     */
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

    /**
     * Filtra in memoria la cache locale su username, nome e cognome, escludendo l'utente corrente.
     */
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

    /**
     * Carica le statistiche di un set di utenti senza bloccare la lista principale.
     */
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

    /**
     * Osserva la query di ricerca con debounce e fallback a refresh remoto.
     */
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

    /**
     * Aggiorna il testo della query di ricerca.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Invia una richiesta amicizia standard.
     */
    fun sendFriendshipRequest(receiverUid: String) = runOnVm {
        friendshipRepository.sendFriendRequest(
            receiverId = receiverUid,
            requestType = FriendRequestType.FRIENDSHIP.name
        ).reportAs("Richiesta amicizia inviata")
    }

    /**
     * Invia una richiesta per essere seguito da un PT.
     */
    fun sendPtCoachingRequest(receiverUid: String) = runOnVm {
        friendshipRepository.sendFriendRequest(
            receiverId = receiverUid,
            requestType = FriendRequestType.PT_COACHING.name
        ).reportAs("Richiesta coaching inviata")
    }

    /**
     * Accetta una richiesta ricevuta.
     */
    fun acceptRequest(requestId: String) = runOnVm {
        friendshipRepository.acceptFriendRequest(requestId).reportAs("Richiesta accettata")
    }

    /**
     * Rifiuta una richiesta ricevuta.
     */
    fun rejectRequest(requestId: String) = runOnVm {
        friendshipRepository.rejectFriendRequest(requestId).reportAs("Richiesta rifiutata")
    }

    /**
     * Annulla una richiesta inviata.
     */
    fun cancelRequest(requestId: String) = runOnVm {
        friendshipRepository.cancelFriendRequest(requestId).reportAs("Richiesta annullata")
    }

    /**
     * Rimuove un amico.
     */
    fun removeFriend(friendUid: String) = runOnVm {
        friendshipRepository.removeFriend(friendUid).reportAs("Amicizia rimossa")
    }

    /**
     * Rimuove un cliente del PT corrente.
     */
    fun removePtClient(clientUid: String) = runOnVm {
        friendshipRepository.removePtClient(clientUid).reportAs("Cliente rimosso")
    }

    /**
     * Pulisce i messaggi transienti mostrati in snackbar.
     */
    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }

    /**
     * Esegue un'azione nel ViewModel gestendo lo stato loading globale.
     */
    private inline fun runOnVm(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            block()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Converte un Result in messaggio di successo o errore per la UI.
     */
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