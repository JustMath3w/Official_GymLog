package com.example.gymlog_finale.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymlog_finale.data.firebase.FirebaseAuthSource
import com.example.gymlog_finale.ui.auth.login.LoginScreen
import com.example.gymlog_finale.ui.auth.login.LoginViewModel
import com.example.gymlog_finale.ui.auth.register.GoogleOnboardingScreen
import com.example.gymlog_finale.ui.auth.register.RegisterStep1Screen
import com.example.gymlog_finale.ui.auth.register.RegisterStep2Screen
import com.example.gymlog_finale.ui.auth.register.RegisterViewModel
import com.example.gymlog_finale.ui.home.HomeScreen
import com.example.gymlog_finale.ui.diet.DietScreen
import com.example.gymlog_finale.ui.progress.ProgressScreen
import com.example.gymlog_finale.ui.community.CommunityScreen
import com.example.gymlog_finale.ui.workout.PtCreateWorkoutScreen
import com.example.gymlog_finale.ui.workout.WorkoutScreen
import com.example.gymlog_finale.ui.profile.ProfileScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val LOGIN = "login"
    const val REGISTER_STEP1 = "register_step1"
    const val REGISTER_STEP2 = "register_step2"
    const val GOOGLE_ONBOARDING = "google_onboarding"
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val DIET = "diet"
    const val DIET_HISTORY = "diet_history"
    const val COMMUNITY = "community"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val PT_CREATE_WORKOUT = "pt_create_workout/{clientUid}/{clientName}"

    /**
     * Builder helper per costruire la route concreta con encoding URL del nome cliente.
     */
    fun ptCreateWorkout(clientUid: String, clientName: String): String {
        val safeName = clientName.ifBlank { "Cliente" }
        val encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8.toString())
        return "pt_create_workout/$clientUid/$encoded"
    }
}

/**
 * Grafo di navigazione principale.
 * RegisterViewModel è condiviso tra Step1, Step2 e GoogleOnboarding
 * per mantenere i dati inseriti attraverso i vari step.
 * startDestination dipende dalla sessione Firebase attiva.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {

    val authSource = FirebaseAuthSource()
    val startDestination = if (authSource.isUserLoggedIn()) Routes.HOME else Routes.LOGIN

    val registerViewModel = remember { RegisterViewModel() }
    val loginViewModel = remember { LoginViewModel() }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER_STEP1) },
                onNavigateToGoogleOnboarding = {
                    navController.navigate(Routes.GOOGLE_ONBOARDING) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                loginViewModel = loginViewModel,
                registerViewModel = registerViewModel
            )
        }

        composable(Routes.REGISTER_STEP1) {
            RegisterStep1Screen(
                onNextStep = { navController.navigate(Routes.REGISTER_STEP2) },
                onNavigateToLogin = { navController.popBackStack() },
                viewModel = registerViewModel
            )
        }

        composable(Routes.REGISTER_STEP2) {
            RegisterStep2Screen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
                viewModel = registerViewModel
            )
        }

        composable(Routes.GOOGLE_ONBOARDING) {
            GoogleOnboardingScreen(
                onOnboardingSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.GOOGLE_ONBOARDING) { inclusive = true }
                    }
                },
                viewModel = registerViewModel
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToWorkout = { navController.navigate(Routes.WORKOUT) },
                onNavigateToDiet = { navController.navigate(Routes.DIET) },
                onNavigateToCommunity = { navController.navigate(Routes.COMMUNITY) },
                onNavigateToProgress = { navController.navigate(Routes.PROGRESS) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.WORKOUT) {
            WorkoutScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.DIET) {
            DietScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Routes.DIET_HISTORY) }
            )
        }

        composable(Routes.DIET_HISTORY) {
            com.example.gymlog_finale.ui.diet.DietHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.COMMUNITY) {
            CommunityScreen(
                onBack = { navController.popBackStack() },
                onCreateWorkoutForClient = { uid, name ->
                    navController.navigate(Routes.ptCreateWorkout(uid, name))
                }
            )
        }

        composable(
            route = Routes.PT_CREATE_WORKOUT,
            arguments = listOf(
                navArgument("clientUid") { type = NavType.StringType },
                navArgument("clientName") { type = NavType.StringType }
            )
        ) { entry ->
            val clientUid = entry.arguments?.getString("clientUid").orEmpty()
            val raw = entry.arguments?.getString("clientName").orEmpty()
            val clientName = URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
            PtCreateWorkoutScreen(
                clientUid = clientUid,
                clientName = clientName,
                onDone = { navController.popBackStack() }
            )
        }

        composable(Routes.PROGRESS) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}