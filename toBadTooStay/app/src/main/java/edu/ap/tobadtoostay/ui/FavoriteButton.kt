package edu.ap.tobadtoostay.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import edu.ap.tobadtoostay.R

/**
 * Heart to mark a store as favorite (K12): filled when it is one.
 * [onFavoriteChange] gets the new state: true = add, false = remove.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    storeName: String,
    onFavoriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = onFavoriteChange,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            // with the store name, so TalkBack users know which heart this is
            contentDescription = stringResource(
                if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
                storeName
            ),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
