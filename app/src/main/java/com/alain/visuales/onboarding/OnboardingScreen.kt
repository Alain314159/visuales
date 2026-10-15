package com.alain.visuales.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.sync.listado.worker.ListadoSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    fun setCompleted(context: Context) {
        viewModelScope.launch {
            settings.setOnboardingCompleted(true)
            ListadoSyncWorker.runNow(context)
        }
    }
}

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Outlined.Movie,
                title = "Bienvenido a Visuales",
                message = "Descarga películas, series, documentales y cursos del catálogo de la UCLV de forma fácil y rápida."
            ),
            OnboardingPage(
                icon = Icons.Outlined.CloudSync,
                title = "Sincronizar catálogo",
                message = "Vamos a descargar el índice completo del catálogo (≈1.5 MB). Así podrás navegar y buscar sin conexión."
            ),
            OnboardingPage(
                icon = Icons.Outlined.Folder,
                title = "Carpeta de descargas",
                message = "Por defecto, las descargas se guardan en /Downloads/Visuales. Puedes cambiarlo luego en Ajustes."
            ),
            OnboardingPage(
                icon = Icons.Outlined.Notifications,
                title = "Notificaciones",
                message = "Te avisaremos cuando una descarga termine o falle, y cuando haya novedades en el catálogo."
            ),
            OnboardingPage(
                icon = Icons.Outlined.PlayCircle,
                title = "¡Todo listo!",
                message = "Ya puedes empezar a explorar el catálogo. Disfruta Visuales."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    OutlinedButton(onClick = {
                        viewModel.setCompleted(context)
                        onCompleted()
                    }) {
                        Text("Saltar")
                    }
                }
            }

            // Middle: pager
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val data = pages[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = data.icon,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // Bottom: page indicators + button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { i ->
                        val isActive = i == pagerState.currentPage
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(if (isActive) 10.dp else 8.dp)
                        ) {}
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            viewModel.setCompleted(context)
                            onCompleted()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (pagerState.currentPage < pages.lastIndex) "Siguiente" else "Comenzar"
                    )
                }
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val message: String
)
