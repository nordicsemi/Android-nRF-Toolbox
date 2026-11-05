package no.nordicsemi.android.ui.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.SectionTitle as CommonSectionTitle

/**
 * A section title with an icon and an optional [menu] on the right.
 *
 * Unlike the common one, the [menu] does not make the title taller. The buttons are 40-48 dp
 * high, and would make the row higher than the 24 dp icon, adding space above and below the
 * title compared to the sections without a menu. Here the menu takes the height of the icon,
 * and the button is centered on it, overlapping the padding of the section.
 */
@Composable
fun SectionTitle(
    painter: Painter,
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    menu: @Composable (RowScope.() -> Unit)? = null,
) {
    CommonSectionTitle(
        painter = painter,
        title = title,
        modifier = modifier,
        tint = tint,
        menu = menu?.let {
            {
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
                    verticalAlignment = Alignment.CenterVertically,
                    content = it,
                )
            }
        },
    )
}

/**
 * @see SectionTitle
 */
@Composable
fun SectionTitle(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    menu: @Composable (RowScope.() -> Unit)? = null,
) {
    SectionTitle(
        painter = rememberVectorPainter(image = icon),
        title = title,
        modifier = modifier,
        tint = tint,
        menu = menu,
    )
}
