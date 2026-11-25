package edu.ap.tobadtoostay.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** The pickup code (K9), the same on the detail screen and in the orders. */
@Composable
fun PickupCode(
    code: String,
    modifier: Modifier = Modifier,
    // on a coloured card pass the matching "on" colour, e.g. onPrimaryContainer
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = code,
        style = MaterialTheme.typography.displayMedium,
        color = color,
        // TalkBack reads the code letter by letter instead of as one word
        modifier = modifier.semantics { contentDescription = code.toList().joinToString(" ") }
    )
}
