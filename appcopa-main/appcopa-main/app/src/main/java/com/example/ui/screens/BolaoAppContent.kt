package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClubEntity
import com.example.data.MatchEntity
import com.example.data.MainRepository
import com.example.data.TransactionEntity
import com.example.data.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun BolaoAppContent(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUserState.collectAsState()
    val matches by viewModel.matchesState.collectAsState()
    val clubs by viewModel.clubsState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val standings by viewModel.standingsState.collectAsState()

    // Dialog state controllers
    var showDepositDialog by remember { mutableStateOf(false) }
    var showCreateClubDialog by remember { mutableStateOf(false) }
    var targetClubToJoin by remember { mutableStateOf<ClubEntity?>(null) }
    var privateClubPasswordInput by remember { mutableStateOf("") }

    // Dynamic snackbar/toast
    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage?.let {
            // Dismiss automatically
            kotlinx.coroutines.delay(3000)
            viewModel.dismissSnackbar()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .drawBehind {
                // Subtle pitch center circle representation to match stadium-bg aesthetic
                val centerOffset = Offset(size.width / 2, size.height * 0.1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(LightMint.copy(alpha = 0.08f), Color.Transparent),
                        center = centerOffset,
                        radius = size.width * 0.8f
                    ),
                    center = centerOffset,
                    radius = size.width * 0.8f
                )
            }
    ) {
        Scaffold(
            topBar = {
                if (viewModel.isLoggedIn) {
                    BolaoTopAppBar(currentUser = currentUser, onProfileClick = {
                        viewModel.currentScreen = "perfil"
                    })
                }
            },
            bottomBar = {
                if (viewModel.isLoggedIn) {
                    BolaoBottomNavBar(
                        currentScreen = viewModel.currentScreen,
                        onScreenSelected = { viewModel.currentScreen = it }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (viewModel.currentScreen) {
                    "login" -> LoginRegisterContainerScreen(viewModel)
                    "hub" -> DashboardHubScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onCreateClubClick = { showCreateClubDialog = true }
                    )
                    "simulador" -> SimulatorScreen(
                        viewModel = viewModel,
                        matches = matches,
                        standings = standings
                    )
                    "clubes" -> ClubsDiscoverScreen(
                        viewModel = viewModel,
                        clubs = clubs,
                        onJoinClick = { club ->
                            if (club.isPrivate) {
                                targetClubToJoin = club
                                privateClubPasswordInput = ""
                            } else {
                                viewModel.joinClub(club)
                            }
                        },
                        onCreateClubClick = { showCreateClubDialog = true }
                    )
                    "carteira" -> WalletDashboardScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        transactions = transactions,
                        onAddFundsClick = { showDepositDialog = true }
                    )
                    "perfil" -> PlayerProfileScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onSubmitLogout = { viewModel.logout() }
                    )
                }
            }
        }

        // Floating dynamic Stripe deposit popup
        if (showDepositDialog) {
            SimulatedStripeDepositDialog(
                onDismiss = { showDepositDialog = false },
                onDeposit = { amount ->
                    viewModel.addFundsViaStripe(amount)
                    showDepositDialog = false
                }
            )
        }

        // Create Club full configuration bottom portal
        if (showCreateClubDialog) {
            CreateClubBottomPortal(
                viewModel = viewModel,
                onDismiss = { showCreateClubDialog = false },
                onCreate = {
                    viewModel.createClub()
                    showCreateClubDialog = false
                }
            )
        }

        // Private Club join security auth dialog
        targetClubToJoin?.let { club ->
            JoinPrivateClubDialog(
                clubName = club.name,
                entryFee = club.entryFee,
                onDismiss = { targetClubToJoin = null },
                onConfirm = { password ->
                    if (password == club.accessPassword) {
                        viewModel.joinClub(club)
                        targetClubToJoin = null
                    } else {
                        viewModel.snackbarMessage = "Senha incorreta para participar."
                    }
                }
            )
        }

        // Floating Toast/Snackbar notification system
        viewModel.snackbarMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 84.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ElevatedGrey),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Alert",
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightAshText
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissSnackbar() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MutedAshText
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MAIN NAV COMPONENTS (TOP & BOTTOM)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BolaoTopAppBar(currentUser: UserEntity?, onProfileClick: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = SlateGrey,
            titleContentColor = GoldAccent
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚽ WORLD CUP 2026",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp,
                    color = GoldAccent
                )
            }
        },
        actions = {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .testTag("avatar_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MediumFieldGreen)
                        .border(1.5.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser?.name?.take(2)?.uppercase() ?: "RK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }
        }
    )
}

@Composable
fun BolaoBottomNavBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = SlateGrey,
        tonalElevation = 8.dp
    ) {
        val navItems = listOf(
            Triple("hub", Icons.Default.Home, "Início"),
            Triple("simulador", Icons.Default.Settings, "Simulador"),
            Triple("clubes", Icons.Default.Share, "Clubes"),
            Triple("carteira", Icons.Default.Menu, "Carteira")
        )

        navItems.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentScreen == route,
                onClick = { onScreenSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (currentScreen == route) GoldAccent else MutedAshText
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (currentScreen == route) FontWeight.Bold else FontWeight.Medium,
                        color = if (currentScreen == route) GoldAccent else MutedAshText
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MediumFieldGreen
                )
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: LOGIN & CADASTRO
// -------------------------------------------------------------

@Composable
fun LoginRegisterContainerScreen(viewModel: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // World Cup Icon representation
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(MediumFieldGreen, CircleShape)
                .border(2.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⚽", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isRegisterMode) "JOIN THE PITCH" else "WORLD CUP BOLÃO 2026",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldAccent,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isRegisterMode) "Crie sua conta para palpitar e gerenciar bolões" else "Monte sua torcida, jogue contra amigos e vença!",
            fontSize = 14.sp,
            color = MutedAshText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRegisterMode) {
                    // Registration inputs
                    OutlinedTextField(
                        value = viewModel.regName,
                        onValueChange = { viewModel.regName = it },
                        label = { Text("Nome Completo") },
                        placeholder = { Text("Ex: Rogerio Kaique") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightMint,
                            unfocusedBorderColor = MutedAshText
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.regEmail,
                        onValueChange = { viewModel.regEmail = it },
                        label = { Text("Endereço de E-mail") },
                        placeholder = { Text("rogerrokaique48@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightMint,
                            unfocusedBorderColor = MutedAshText
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.regUsername,
                        onValueChange = { viewModel.regUsername = it },
                        label = { Text("Nome de Usuário") },
                        placeholder = { Text("roger_wc26") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightMint,
                            unfocusedBorderColor = MutedAshText
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = viewModel.regTermsAgreed,
                            onCheckedChange = { viewModel.regTermsAgreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                        )
                        Text(
                            text = "Eu concordo com os Termos e Condições do aplicativo Bolão Copa 2026.",
                            fontSize = 11.sp,
                            color = LightAshText,
                            lineHeight = 14.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.signUp() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("signup_submit")
                    ) {
                        Text("Cadastrar e Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                } else {
                    // Login inputs
                    OutlinedTextField(
                        value = viewModel.loginEmail,
                        onValueChange = { viewModel.loginEmail = it },
                        label = { Text("Endereço de E-mail") },
                        placeholder = { Text("rogerrokaique48@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightMint,
                            unfocusedBorderColor = MutedAshText
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.loginPassword,
                        onValueChange = { viewModel.loginPassword = it },
                        label = { Text("Senha") },
                        placeholder = { Text("••••••••") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightMint,
                            unfocusedBorderColor = MutedAshText
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.login() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit")
                    ) {
                        Text("Acessar Conta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MutedAshText.copy(alpha = 0.2f))
            Text(
                "OU CONTINUE COM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedAshText,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MutedAshText.copy(alpha = 0.2f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Button(
                onClick = {
                    viewModel.loginEmail = "rogerrokaique48@gmail.com"
                    viewModel.login()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                border = BorderStroke(1.dp, MutedAshText.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Google", color = LightAshText, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    viewModel.loginEmail = "rogerrokaique48@gmail.com"
                    viewModel.login()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                border = BorderStroke(1.dp, MutedAshText.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Apple", color = LightAshText, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = { isRegisterMode = !isRegisterMode }
        ) {
            Text(
                text = if (isRegisterMode) "Já tem conta? Faça Login" else "Não tem conta? Cadastre-se",
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: DASHBOARD / HUB
// -------------------------------------------------------------

@Composable
fun DashboardHubScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    onCreateClubClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Headline Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MediumFieldGreen),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bem-vindo ao Campo!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentUser?.name ?: "Rogerio Kaique",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LightAshText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seu saldo: R$ ${String.format("%.2f", currentUser?.balance ?: 1450.0)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightMint
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(SlateGrey, CircleShape)
                            .border(2.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏟️", fontSize = 28.sp)
                    }
                }
            }
        }

        // Action Quick Access Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.currentScreen = "simulador" },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Simulador", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                    }
                }

                Button(
                    onClick = { viewModel.currentScreen = "clubes" },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ver Clubes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                    }
                }

                Button(
                    onClick = onCreateClubClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("➕", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Criar Clube", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ÁREA DO JOGADOR - MEUS BOLOÕES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 1.sp
                )
            }
        }

        // Bet Item active card list
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MediumFieldGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👥", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bolão da Firma", fontWeight = FontWeight.Bold, color = LightAshText, fontSize = 15.sp)
                            Text("Progresso: 8/12 palpites", fontSize = 12.sp, color = MutedAshText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SEU RANK", fontSize = 9.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            Text("2º 📈", fontSize = 16.sp, fontWeight = FontWeight.Black, color = LightMint)
                        }
                    }

                    HorizontalDivider(color = MutedAshText.copy(alpha = 0.1f))

                    // Item 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MediumFieldGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌐", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Liga Global VIP", fontWeight = FontWeight.Bold, color = LightAshText, fontSize = 15.sp)
                            Text("Progresso: 12/12 palpites", fontSize = 12.sp, color = MutedAshText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SEU RANK", fontSize = 9.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            Text("154º 📉", fontSize = 16.sp, fontWeight = FontWeight.Black, color = ErrorRed)
                        }
                    }
                }
            }
        }

        // Promotion banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(MediumFieldGreen, SlateGrey)
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RECOMPENSA DE BOLOÃO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Participe de novas ligas com prêmios em dinheiro real coletor via Stripe!",
                            fontSize = 13.sp,
                            color = LightAshText,
                            lineHeight = 16.sp
                        )
                    }
                    Text("💡", fontSize = 28.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: SIMULADOR DE COPA
// -------------------------------------------------------------

@Composable
fun SimulatorScreen(
    viewModel: MainViewModel,
    matches: List<MatchEntity>,
    standings: List<MainRepository.TeamStats>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Intro banner representation
        Text(
            text = "Simulador de Resultados",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LightAshText
        )
        Text(
            text = "Projete os cenários da fase de grupos e veja como suas previsões moldam as oitavas de final. O caminho para a glória começa aqui.",
            fontSize = 14.sp,
            color = MutedAshText,
            lineHeight = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PREVISÕES DO GRUPO A • RODADA 3",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = GoldAccent,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .background(MediumFieldGreen, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("ATIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            }
        }

        // Matches scoring simulator block
        matches.filter { it.groupName == "A" }.take(2).forEach { match ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                border = BorderStroke(1.dp, MutedAshText.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${match.date} • ${match.time} • ${match.stadium}",
                        fontSize = 11.sp,
                        color = MutedAshText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Team A Representation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = match.flagA,
                                fontSize = 36.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = match.teamA,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LightAshText
                            )
                        }

                        // Score Input selectors (A)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.updatePrediction(match.id, true, true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Add", tint = GoldAccent)
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 56.dp)
                                    .background(DarkSlateBg, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (match.predictedScoreA ?: 0).toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                            IconButton(onClick = { viewModel.updatePrediction(match.id, true, false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Remove", tint = GoldAccent)
                            }
                        }

                        Text("X", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MutedAshText)

                        // Score Input selectors (B)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.updatePrediction(match.id, false, true) }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Add", tint = GoldAccent)
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp, 56.dp)
                                    .background(DarkSlateBg, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (match.predictedScoreB ?: 0).toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightAshText
                                )
                            }
                            IconButton(onClick = { viewModel.updatePrediction(match.id, false, false) }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Remove", tint = GoldAccent)
                            }
                        }

                        // Team B Representation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = match.flagB,
                                fontSize = 36.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = match.teamB,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LightAshText
                            )
                        }
                    }
                }
            }
        }

        // Live standings table computed reactively using Flow in Repository
        Text(
            "CLASSIFICAÇÃO EM TEMPO REAL (GRUPO A)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = GoldAccent,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Standings table header
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Pos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedAshText, modifier = Modifier.width(32.dp))
                    Text("Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedAshText, modifier = Modifier.weight(1f))
                    Text("P", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedAshText, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text("SG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedAshText, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text("GM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedAshText, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(8.dp))

                standings.forEachIndexed { idx, team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}º",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (idx < 2) LightMint else MutedAshText,
                            modifier = Modifier.width(32.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(team.flag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(team.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                        }
                        Text(team.points.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text(team.goalDiff.toString(), fontSize = 13.sp, color = LightAshText, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text(team.goalsScored.toString(), fontSize = 13.sp, color = LightAshText, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Simulated Round of 16 match projection based reactively on Group standings
        Text(
            "CHAVEAMENTO SIMULADO (OITAVAS)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = GoldAccent,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        val firstPlace = standings.firstOrNull()?.name ?: "BRA"
        val firstFlag = standings.firstOrNull()?.flag ?: "🇧🇷"

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MediumFieldGreen),
            border = BorderStroke(1.5.dp, GoldAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(GoldAccent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("PROJEÇÃO OITAVAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(firstFlag, fontSize = 48.sp)
                        Text(firstPlace, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LightAshText)
                        Text("1º Grupo A", fontSize = 11.sp, color = GoldAccent)
                    }

                    Text("VS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = GoldAccent)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇵🇹", fontSize = 48.sp)
                        Text("POR", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LightAshText)
                        Text("2º Grupo B", fontSize = 11.sp, color = MutedAshText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LightAshText.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "28 JUN • MARACANÃ • RIO DE JANEIRO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightMint,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: DISCOVER CLUBS / ADVERTISEMENTS
// -------------------------------------------------------------

@Composable
fun ClubsDiscoverScreen(
    viewModel: MainViewModel,
    clubs: List<ClubEntity>,
    onJoinClick: (ClubEntity) -> Unit,
    onCreateClubClick: () -> Unit
) {
    var filterIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Descobrir Clubes",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightAshText
                )
                Text(
                    text = "Encontre a liga perfeita e dispute os maiores prêmios.",
                    fontSize = 13.sp,
                    color = MutedAshText
                )
            }
            IconButton(
                onClick = onCreateClubClick,
                modifier = Modifier
                    .background(MediumFieldGreen, CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Add, "New Club", tint = GoldAccent)
            }
        }

        // Horizontal Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Todos os Clubes", "Maiores Prêmios", "Entrada Grátis")
            filters.forEachIndexed { index, text ->
                FilterChip(
                    selected = filterIndex == index,
                    onClick = { filterIndex = index },
                    label = { Text(text, color = if (filterIndex == index) Color.Black else LightAshText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent,
                        containerColor = SlateGrey
                    ),
                    border = null
                )
            }
        }

        // Clubs Feed layout
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val filteredClubs = when (filterIndex) {
                1 -> clubs.sortedByDescending { it.estimatedPrizePool }
                2 -> clubs.filter { it.entryFee == 0.0 }
                else -> clubs
            }

            items(filteredClubs) { club ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MediumFieldGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (club.isPrivate) "🔒" else "🏆", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = club.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightAshText
                                )
                                Text(
                                    text = "${if (club.isPrivate) "Privado" else "Público"} • ${club.memberCount} participantes",
                                    fontSize = 11.sp,
                                    color = MutedAshText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = club.description,
                            fontSize = 13.sp,
                            color = LightAshText,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // financial layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PRÊMIO TOTAL ESTIMADO", fontSize = 9.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                                Text("R$ ${String.format("%.2f", club.estimatedPrizePool)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TAXA DE ENTRADA", fontSize = 9.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (club.entryFee == 0.0) "GRÁTIS" else "R$ ${String.format("%.2f", club.entryFee)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (club.entryFee == 0.0) LightMint else LightAshText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onJoinClick(club) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (club.isPrivate) "Pedir para Participar" else "Entrar Agora", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (club.isPrivate) Icons.Default.Lock else Icons.Default.ArrowForward,
                                    contentDescription = "Action",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5: WALLET / CARTEIRA & TRANSACTION LOGS
// -------------------------------------------------------------

@Composable
fun WalletDashboardScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    transactions: List<TransactionEntity>,
    onAddFundsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Minha Carteira",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LightAshText
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MediumFieldGreen),
            border = BorderStroke(1.5.dp, GoldAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SALDO DISPONÍVEL (STRIPE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "R$ ${String.format("%.2f", currentUser?.balance ?: 1450.0)}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = LightAshText
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddFundsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.Add, "Deposit")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar Saldo", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.snackbarMessage = "Saques processados via Stripe caem em até 24 horas!" },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Withdraw")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Efetuar Saque", fontWeight = FontWeight.Bold, color = LightAshText)
                    }
                }
            }
        }

        Text(
            text = "HISTÓRICO DE TRANSAÇÕES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma transação efetuada.", color = MutedAshText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { trans ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (trans.amount >= 0) MediumFieldGreen else DarkSlateBg,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (trans.amount >= 0) Icons.Default.Add else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Status",
                                    tint = if (trans.amount >= 0) LightMint else ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(trans.description, fontWeight = FontWeight.Bold, color = LightAshText, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(trans.date, fontSize = 11.sp, color = MutedAshText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        trans.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (trans.status == "PAGO") GoldAccent else LightMint
                                    )
                                }
                            }

                            Text(
                                text = "${if (trans.amount >= 0) "+" else ""} R$ ${String.format("%.2f", trans.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (trans.amount >= 0) LightMint else ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 6: PLAYER PROFILE / CONFIGS
// -------------------------------------------------------------

@Composable
fun PlayerProfileScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity?,
    onSubmitLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MediumFieldGreen, CircleShape)
                .border(2.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🦁", fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentUser?.name ?: "Rogerio Kaique",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = LightAshText
        )
        Text(
            text = "@${currentUser?.username ?: "roger_wc26"}",
            fontSize = 14.sp,
            color = MutedAshText
        )

        Row(
            modifier = Modifier
                .background(MediumFieldGreen, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("★ Global Rank #42", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        }

        // Progress level block
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("NÍVEL ATUAL", fontSize = 9.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                        Text(currentUser?.levelName ?: "Veteran Player", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                    }
                    Text("Próximo: PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { 0.845f },
                    color = GoldAccent,
                    trackColor = DarkSlateBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "8.450 / 10.000 XP",
                    fontSize = 11.sp,
                    color = MutedAshText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Stats grid Row Bento style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PONTOS", fontSize = 11.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                    Text((currentUser?.points ?: 1450).toString(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                    Text("+120 esta semana", fontSize = 10.sp, color = LightMint)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PLACAR EXATO", fontSize = 11.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                    Text((currentUser?.accurateScoresCount ?: 45).toString(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = LightAshText)
                    Text("Partidas acertadas", fontSize = 10.sp, color = MutedAshText)
                }
            }
        }

        // Win Rate Block
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGrey),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TAXA DE VITÓRIA", fontSize = 10.sp, color = MutedAshText, fontWeight = FontWeight.Bold)
                    Text("${currentUser?.winRate ?: 68}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = LightMint)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MediumFieldGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📈", fontSize = 22.sp)
                }
            }
        }

        // Achievements Portal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONQUISTAS", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "🥇 Especialista" to "5 placares corretos",
                "⏱️ Águia" to "Sempre palpita cedo",
                "🏆 Campeão" to "Venceu liga privada"
            ).forEach { (badge, info) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateGrey),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(130.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(badge.split(" ").first(), fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(badge.split(" ").last(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightAshText)
                        Text(info, fontSize = 10.sp, color = MutedAshText, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmitLogout,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSlateBg),
            border = BorderStroke(1.dp, ErrorRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Text("Sair da Conta", color = ErrorRed, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// DIAOLOG PORTAL COMPONENTS (STRIPE & CREATE CLUB)
// -------------------------------------------------------------

@Composable
fun SimulatedStripeDepositDialog(
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Depositar via Stripe Integration", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecione o valor para depositar em sua carteira com o gateway seguro da Stripe:",
                    color = LightAshText,
                    fontSize = 13.sp
                )

                listOf(50.0, 100.0, 500.0).forEach { value ->
                    Button(
                        onClick = { onDeposit(value) },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💳 Stripe Checkout", color = LightAshText, fontSize = 13.sp)
                            Text("R$ ${String.format("%.2f", value)}", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ErrorRed)
            }
        },
        containerColor = SlateGrey
    )
}

@Composable
fun CreateClubBottomPortal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🏆 CONFIGURAR NOVO CLUBE", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.newClubName,
                    onValueChange = { viewModel.newClubName = it },
                    label = { Text("Nome do Clube") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.newClubDesc,
                    onValueChange = { viewModel.newClubDesc = it },
                    label = { Text("Descrição do Clube") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Privacidade do Clube:", color = LightAshText, fontSize = 13.sp)
                    Row {
                        FilterChip(
                            selected = viewModel.newClubIsPrivate,
                            onClick = { viewModel.newClubIsPrivate = true },
                            label = { Text("🔒 Privado") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = !viewModel.newClubIsPrivate,
                            onClick = { viewModel.newClubIsPrivate = false },
                            label = { Text("🌐 Público") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent)
                        )
                    }
                }

                if (viewModel.newClubIsPrivate) {
                    OutlinedTextField(
                        value = viewModel.newClubPassword,
                        onValueChange = { viewModel.newClubPassword = it },
                        label = { Text("Senha do Clube") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = viewModel.newClubEntryFee,
                    onValueChange = { viewModel.newClubEntryFee = it },
                    label = { Text("Taxa de Entrada (R$)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Divisão de Premiação (Membro Dono define em %):",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.newClub1stPlacePct,
                        onValueChange = { viewModel.newClub1stPlacePct = it },
                        label = { Text("1º Place") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.newClub2ndPlacePct,
                        onValueChange = { viewModel.newClub2ndPlacePct = it },
                        label = { Text("2º Place") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.newClub3rdPlacePct,
                        onValueChange = { viewModel.newClub3rdPlacePct = it },
                        label = { Text("3º Place") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("CRIAR E PAGAR CONEXÃO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ErrorRed)
            }
        },
        containerColor = SlateGrey
    )
}

@Composable
fun JoinPrivateClubDialog(
    clubName: String,
    entryFee: Double,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🔒 LIGA PRIVADA: $clubName", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Este bolão necessita pagamento de R$ ${String.format("%.2f", entryFee)} via Stripe e a senha de convite configurada pelo idealizador do clube.",
                    color = LightAshText,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Senha do Convite") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LightMint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passwordInput) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
            ) {
                Text("Acessar e Efetuar Stripe Pay", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ErrorRed)
            }
        },
        containerColor = SlateGrey
    )
}
