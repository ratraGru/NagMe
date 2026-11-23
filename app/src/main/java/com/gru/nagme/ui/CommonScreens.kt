package com.gru.nagme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.gru.nagme.NagMeApp
import com.gru.nagme.R
import com.gru.nagme.overlay.CharacterView

/** Empty state lucu (Bagian 10.4). Karakter asli menyusul di hari berikutnya. */
@Composable
fun EmptyState(messageRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BobbingFace(140)
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** Karakter kecil (tanpa balon) yang bernapas pelan; animasinya ada di CharacterView. */
@Composable
fun BobbingFace(sizeDp: Int = 140, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val style = remember { (ctx.applicationContext as NagMeApp).container.settings.characterStyle }
    AndroidView(
        factory = { c ->
            CharacterView(c).apply {
                compact = true
                this.style = style
            }
        },
        modifier = modifier.size(sizeDp.dp)
    )
}

@Composable
fun PlaceholderScreen(titleRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
