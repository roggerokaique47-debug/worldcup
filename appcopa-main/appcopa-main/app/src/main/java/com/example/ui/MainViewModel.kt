package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ClubEntity
import com.example.data.MatchEntity
import com.example.data.MainRepository
import com.example.data.TransactionEntity
import com.example.data.UserEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MainRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MainRepository(database)
    }

    val currentUserState: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val clubsState: StateFlow<List<ClubEntity>> = repository.allClubsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchesState: StateFlow<List<MatchEntity>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsState: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standingsState: StateFlow<List<MainRepository.TeamStats>> = repository.groupAStandingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and UI state
    var currentScreen by mutableStateOf("login")
    var isLoggedIn by mutableStateOf(false)

    // Form states for login/registration
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var regName by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regUsername by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regTermsAgreed by mutableStateOf(false)

    // Form states for creating a Club
    var newClubName by mutableStateOf("")
    var newClubDesc by mutableStateOf("")
    var newClubIsPrivate by mutableStateOf(true)
    var newClubPassword by mutableStateOf("GOL2026")
    var newClubEntryFee by mutableStateOf("10.0")
    var newClub1stPlacePct by mutableStateOf("70")
    var newClub2ndPlacePct by mutableStateOf("20")
    var newClub3rdPlacePct by mutableStateOf("10")

    // General user feedback messages
    var snackbarMessage by mutableStateOf<String?>(null)

    fun login() {
        val email = loginEmail.trim()
        if (email.isEmpty()) {
            showToast("Preencha o e-mail de acesso")
            return
        }
        viewModelScope.launch {
            // Find existing user or seed, then log in
            val user = repository.currentUserFlow.stateIn(viewModelScope).value
            if (user == null) {
                repository.registerUser("Rogerio Kaique", email, "roggero_wc26")
            }
            isLoggedIn = true
            currentScreen = "hub"
            showToast("Seja bem-vindo, ${user?.name ?: "Rogerio"}!")
        }
    }

    fun signUp() {
        if (regName.isBlank() || regEmail.isBlank() || regUsername.isBlank()) {
            showToast("Preencha todos os campos obrigatórios")
            return
        }
        if (!regTermsAgreed) {
            showToast("Você precisa aceitar os Termos e Condições")
            return
        }
        viewModelScope.launch {
            repository.registerUser(regName.trim(), regEmail.trim(), regUsername.trim())
            isLoggedIn = true
            currentScreen = "hub"
            showToast("Cadastro realizado com sucesso!")
        }
    }

    fun logout() {
        isLoggedIn = false
        currentScreen = "login"
        regName = ""
        regEmail = ""
        regUsername = ""
        loginEmail = ""
        showToast("Você desconectou da sua conta")
    }

    // Match predictions
    fun updatePrediction(matchId: Int, isTeamA: Boolean, increment: Boolean) {
        viewModelScope.launch {
            val matches = matchesState.value
            val match = matches.find { it.id == matchId } ?: return@launch

            var currentVal = if (isTeamA) match.predictedScoreA else match.predictedScoreB
            if (currentVal == null) {
                currentVal = 0
            }

            val newVal = if (increment) currentVal + 1 else maxOf(0, currentVal - 1)

            if (isTeamA) {
                repository.savePrediction(matchId, newVal, match.predictedScoreB)
            } else {
                repository.savePrediction(matchId, match.predictedScoreA, newVal)
            }
        }
    }

    // SQLite interaction for creating a Club & simulated Stripe checkout
    fun createClub() {
        if (newClubName.isBlank()) {
            showToast("O nome do clube é obrigatório")
            return
        }
        val pct1 = newClub1stPlacePct.toIntOrNull() ?: 70
        val pct2 = newClub2ndPlacePct.toIntOrNull() ?: 20
        val pct3 = newClub3rdPlacePct.toIntOrNull() ?: 10

        if (pct1 + pct2 + pct3 != 100) {
            showToast("A soma das porcentagens de premiação deve ser 100%")
            return
        }

        val fee = newClubEntryFee.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val user = currentUserState.value
            if (user != null && fee > user.balance) {
                showToast("Saldo insuficiente (Stripe necessita R$ $fee)")
                return@launch
            }

            val success = repository.createClubAndPay(
                name = newClubName.trim(),
                description = newClubDesc.trim(),
                isPrivate = newClubIsPrivate,
                passwordCustom = if (newClubIsPrivate) newClubPassword else "",
                entryFee = fee,
                prizesFirst = pct1,
                prizesSecond = pct2,
                prizesThird = pct3
            )

            if (success) {
                showToast("Clube criado e taxas pagas via Stripe!")
                currentScreen = "clubes"
                // reset form
                newClubName = ""
                newClubDesc = ""
                newClubIsPrivate = true
                newClubEntryFee = "10.0"
            } else {
                showToast("Erro ao criar clube.")
            }
        }
    }

    // SQLite interaction to Join a Club with simulated Stripe
    fun joinClub(club: ClubEntity) {
        viewModelScope.launch {
            val user = currentUserState.value ?: return@launch
            if (user.balance < club.entryFee) {
                showToast("Saldo insuficiente para pagar entrada de R$ ${club.entryFee}")
                return@launch
            }

            val success = repository.joinClubAndPay(club.id)
            if (success) {
                showToast("Sua entrada no clube ${club.name} foi processada via Stripe!")
            } else {
                showToast("Erro ao participar do clube.")
            }
        }
    }

    // Stripe deposit simulation
    fun addFundsViaStripe(amount: Double) {
        viewModelScope.launch {
            val success = repository.addFundsSimulated(amount)
            if (success) {
                showToast("Depósito de R$ $amount adicionado via Stripe com sucesso!")
            } else {
                showToast("Falha ao depositar fundos.")
            }
        }
    }

    private fun showToast(msg: String) {
        snackbarMessage = msg
    }

    fun dismissSnackbar() {
        snackbarMessage = null
    }
}
