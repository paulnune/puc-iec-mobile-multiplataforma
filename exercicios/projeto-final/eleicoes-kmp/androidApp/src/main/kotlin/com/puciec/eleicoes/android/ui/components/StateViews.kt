package com.puciec.eleicoes.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.puciec.eleicoes.android.ui.state.userMessage
import com.puciec.eleicoes.android.ui.theme.Spacing
import com.puciec.eleicoes.domain.model.AppError

/** Pincel animado do shimmer. Crie um por tela e passe aos blocos. */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing)),
        label = "shimmer-offset",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + 600f, 0f),
    )
}

@Composable
fun ShimmerBlock(brush: Brush, modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    Box(modifier.clip(shape).background(brush))
}

/** Lista "fantasma" enquanto carrega, no lugar do spinner solto (dica do enunciado). */
@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, rows: Int = 6) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.m)
            .semantics { contentDescription = "Carregando" },
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBlock(brush, Modifier.size(56.dp), shape = CircleShape)
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    ShimmerBlock(brush, Modifier.fillMaxWidth(0.7f).height(16.dp))
                    ShimmerBlock(brush, Modifier.fillMaxWidth(0.45f).height(12.dp))
                }
                Spacer(Modifier.width(Spacing.m))
                ShimmerBlock(brush, Modifier.width(72.dp).height(16.dp))
            }
        }
    }
}

/** Estado vazio (e base do estado de erro): ícone, título, explicação e uma ação opcional. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Filled.Info,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.m))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(Spacing.s))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(Spacing.l))
            action()
        }
    }
}

/** Erro com mensagem clara e botão de tentar de novo (nunca a exceção crua). */
@Composable
fun ErrorState(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = "Não deu para carregar",
        message = error.userMessage(),
        icon = Icons.Filled.Warning,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text("Tentar de novo") } },
    )
}

/** Mostrada quando o app foi buildado sem ELEICOES_API_KEY. */
@Composable
fun MissingKeyScreen() {
    Surface(Modifier.fillMaxSize()) {
        EmptyState(
            title = "Falta a chave da API",
            message = "Crie uma chave grátis em eleicoes.dev/dashboard, coloque ELEICOES_API_KEY=<sua chave> " +
                "no arquivo .env da raiz do repositório e builde de novo.",
            icon = Icons.Filled.Lock,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
