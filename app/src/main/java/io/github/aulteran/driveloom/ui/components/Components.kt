// Shared building blocks of the Loom HUD look: the ambient backdrop, glass
// panels, gradient actions, monospaced readouts and list rows.
package io.github.aulteran.driveloom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.ui.Load
import io.github.aulteran.driveloom.ui.Loader
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

val PanelShape = RoundedCornerShape(16.dp)
val ControlShape = RoundedCornerShape(10.dp)
val ChipShape = RoundedCornerShape(8.dp)

/** Two aurora glows over a fading engineering grid. Static: nothing here animates. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Loom.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .drawBehind {
                val reach = maxOf(size.width, size.height)
                drawRect(Brush.radialGradient(listOf(c.glow1, Color.Transparent), center = Offset(size.width * 0.08f, -size.height * 0.05f), radius = reach * 0.75f))
                drawRect(Brush.radialGradient(listOf(c.glow2, Color.Transparent), center = Offset(size.width * 1.05f, size.height * 1.05f), radius = reach * 0.7f))
                val step = 44.dp.toPx()
                val fade = size.height * 0.6f
                val vertical = Brush.verticalGradient(listOf(c.grid, Color.Transparent), endY = fade)
                var x = step
                while (x < size.width) {
                    drawLine(vertical, Offset(x, 0f), Offset(x, fade), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < fade) {
                    drawLine(c.grid.copy(alpha = c.grid.alpha * (1f - y / fade)), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            },
        content = content,
    )
}

/** A glass panel with a gradient hairline on its top edge. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    title: String? = null,
    danger: Boolean = false,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Loom.colors
    val edge = if (danger) Brush.horizontalGradient(listOf(c.bad, c.bad.copy(alpha = 0f))) else Brush.horizontalGradient(listOf(c.accent, c.accent2, Color.Transparent))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(c.surface)
            .border(1.dp, c.line, PanelShape)
            .drawBehind { drawRect(edge, size = Size(size.width, 1.5.dp.toPx()), alpha = 0.7f) }
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) PanelTitle(title, danger)
        content()
    }
}

/** A panel heading with the gradient marker bar. */
@Composable
fun PanelTitle(text: String, danger: Boolean = false) {
    val c = Loom.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (danger) Brush.verticalGradient(listOf(c.bad, c.bad)) else Brush.verticalGradient(listOf(c.accent, c.accent2))),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = Loom.type.h2, color = c.text, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: Color = Loom.colors.muted) {
    Text(text.uppercase(), style = Loom.type.label, color = color, modifier = modifier)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(text, style = if (small) Loom.type.small else Loom.type.body, color = Loom.colors.muted, modifier = modifier)
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Loom.type.body, color = Loom.colors.text, modifier = modifier)
}

/** The primary action: the signal gradient. Use at most one per screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
) {
    val c = Loom.colors
    val active = enabled && !busy
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ControlShape)
            .background(c.gradient)
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), color = c.accentText, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Loom.type.button, color = c.accentText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class ButtonTone { Neutral, Danger, Accent }

/** A quiet glass button for secondary actions. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
    tone: ButtonTone = ButtonTone.Neutral,
) {
    val c = Loom.colors
    val ink = when (tone) {
        ButtonTone.Neutral -> c.text
        ButtonTone.Danger -> c.bad
        ButtonTone.Accent -> c.accentInk
    }
    val active = enabled && !busy
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ControlShape)
            .background(c.surface2)
            .border(1.dp, if (tone == ButtonTone.Danger) c.bad.copy(alpha = 0.5f) else c.lineStrong, ControlShape)
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), color = ink, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Loom.type.button, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A bare text action. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Loom.colors.accentInk) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Loom.type.button, color = if (enabled) color else Loom.colors.muted)
    }
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Loom.colors.text) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun Badge(text: String, accent: Boolean = false, warn: Boolean = false) {
    val c = Loom.colors
    val ink = when {
        warn -> c.warn
        accent -> c.accentInk
        else -> c.muted
    }
    Text(
        text.uppercase(),
        style = Loom.type.label,
        color = ink,
        modifier = Modifier
            .clip(ChipShape)
            .background(if (accent) c.accentSoft else c.surface2)
            .border(1.dp, ink.copy(alpha = 0.35f), ChipShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, error: Boolean = false) {
    val c = Loom.colors
    val edge = if (error) c.bad else c.accent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(if (error) c.bad.copy(alpha = 0.10f) else c.accentSoft)
            .border(1.dp, edge.copy(alpha = 0.35f), ControlShape)
            .drawBehind { drawRect(edge, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Text(text, style = Loom.type.small.copy(lineHeight = Loom.type.body.lineHeight), color = c.text)
    }
}

/** Shows [message] as an alert when it is not null. */
@Composable
fun ErrorNotice(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Notice(message, modifier, error = true)
}

@Composable
fun LoomField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    password: Boolean = false,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    maxLength: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = Loom.colors
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { text -> onValueChange(if (maxLength != null) text.take(maxLength) else text) },
            label = { Text(label) },
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            isError = error != null,
            textStyle = if (mono) Loom.type.readout.copy(color = c.text) else Loom.type.body.copy(color = c.text),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = !password && keyboardType == KeyboardType.Text && capitalization != KeyboardCapitalization.None,
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = if (onDone != null) ImeAction.Done else imeAction,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            trailingIcon = trailing,
            shape = ControlShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.field,
                unfocusedContainerColor = c.field,
                disabledContainerColor = c.field,
                errorContainerColor = c.field,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.lineStrong,
                disabledBorderColor = c.line,
                focusedLabelColor = c.accentInk,
                unfocusedLabelColor = c.muted,
                disabledLabelColor = c.muted,
                disabledTextColor = c.muted,
                cursorColor = c.accent,
                errorBorderColor = c.bad,
                errorLabelColor = c.bad,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        val below = error ?: hint
        if (below != null) {
            Text(
                below,
                style = Loom.type.small,
                color = if (error != null) c.bad else c.muted,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

/** A drop-down choice styled like a field. [options] are value to label pairs. */
@Composable
fun LoomSelect(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
) {
    val c = Loom.colors
    var open by remember { mutableStateOf(false) }
    val shown = options.firstOrNull { it.first == selected }?.second ?: selected
    Column(modifier) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(ControlShape)
                    .background(c.field)
                    .border(1.dp, c.lineStrong, ControlShape)
                    .clickable(enabled = enabled, role = Role.DropdownList) { open = true }
                    .semantics { contentDescription = "$label: $shown" }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = Loom.type.small.copy(fontSize = Loom.type.label.fontSize), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shown, style = Loom.type.body, color = if (enabled) c.text else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(LoomIcons.ChevronDown, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                for ((value, text) in options) {
                    DropdownMenuItem(
                        text = { Text(text, color = if (value == selected) c.accentInk else c.text) },
                        onClick = {
                            open = false
                            if (value != selected) onSelect(value)
                        },
                    )
                }
            }
        }
        if (hint != null) Text(hint, style = Loom.type.small, color = c.muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** A segmented control; the active segment is a raised solid chip. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Loom.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(c.surface2)
            .border(1.dp, c.line, ControlShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((value, text) in options) {
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(ChipShape)
                    .background(if (active) c.surfaceSolid else Color.Transparent)
                    .then(if (active) Modifier.border(1.dp, c.accent.copy(alpha = 0.45f), ChipShape) else Modifier)
                    .clickable(role = Role.Tab) { if (!active) onSelect(value) }
                    .semantics { stateDescription = if (active) "Selected" else "Not selected" }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, style = Loom.type.button.copy(fontSize = Loom.type.small.fontSize), color = if (active) c.accentInk else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, enabled: Boolean = true) {
    val c = Loom.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(ControlShape)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Loom.type.body, color = c.text)
            if (subtitle != null) Text(subtitle, style = Loom.type.small, color = c.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.accentText,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.surface2,
                uncheckedBorderColor = c.lineStrong,
            ),
        )
    }
}

/** A statistic: a mono micro-label above the value. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, note: String? = null, unit: String? = null) {
    val c = Loom.colors
    Column(
        modifier = modifier
            .clip(ControlShape)
            .background(c.surface2)
            .border(1.dp, c.line, ControlShape)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MicroLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Loom.type.readoutLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (unit != null) {
                Spacer(Modifier.width(6.dp))
                Text(unit, style = Loom.type.small, color = c.muted, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (note != null) Text(note, style = Loom.type.small, color = c.muted)
    }
}

/** A navigation row: icon, title, optional detail and a chevron. */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    detail: String? = null,
    subtitle: String? = null,
    danger: Boolean = false,
) {
    val c = Loom.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(36.dp).clip(ControlShape).background(if (danger) c.bad.copy(alpha = 0.12f) else c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (danger) c.bad else c.accentInk, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Loom.type.title, color = if (danger) c.bad else c.text)
            if (subtitle != null) Text(subtitle, style = Loom.type.small, color = c.muted)
        }
        if (detail != null) {
            Text(detail, style = Loom.type.mono, color = c.muted, maxLines = 1, modifier = Modifier.padding(start = 8.dp).widthIn(max = 150.dp), overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        Icon(LoomIcons.Chevron, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
    }
}

/** A group of rows inside one glass panel, separated by hairlines. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Loom.colors
    Column(
        modifier = modifier.fillMaxWidth().clip(PanelShape).background(c.surface).border(1.dp, c.line, PanelShape),
        content = content,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Loom.colors.line))
}

/** The loading indicator: a thin scanning bar. */
@Composable
fun ScanBar(modifier: Modifier = Modifier) {
    val c = Loom.colors
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(2.dp).semantics { contentDescription = "Loading" },
        color = c.accent,
        trackColor = c.line,
    )
}

@Composable
fun EmptyState(title: String, body: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = Loom.type.title, color = Loom.colors.text, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = Loom.type.small, color = Loom.colors.muted, textAlign = TextAlign.Center)
    }
}

/** Renders a [Loader]: scanning bar, error with retry, or the content. */
@Composable
fun <T> LoadView(loader: Loader<T>, content: @Composable (T) -> Unit) {
    when (val s = loader.state) {
        is Load.Loading -> ScanBar(Modifier.padding(vertical = 24.dp))
        is Load.Failed -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Notice(s.message, error = true)
            SecondaryButton("Try again", onClick = { loader.reload() })
        }
        is Load.Ready -> content(s.value)
    }
}

/** The frame of a pushed screen: back button, title, optional action, scrolling content. */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    eyebrow: String? = null,
    action: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Loom.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) IconAction(LoomIcons.Back, "Back", onBack)
            Spacer(Modifier.weight(1f))
            if (action != null) action()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (eyebrow != null) MicroLabel(eyebrow, color = c.accentInk)
                Text(title, style = Loom.type.h1, color = c.text, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    val c = Loom.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Loom.type.h2) },
        text = { Text(message, style = Loom.type.body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (danger) c.bad else c.accentInk, style = Loom.type.button) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted, style = Loom.type.button) } },
        containerColor = c.surfaceSolid,
        titleContentColor = c.text,
        textContentColor = c.muted,
        shape = PanelShape,
    )
}

/** Something secret shown once: a key, a code or a link, in mono with a copy action. */
@Composable
fun SecretBox(text: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    val c = Loom.colors
    Row(
        modifier = modifier.fillMaxWidth().clip(ControlShape).background(c.field).border(BorderStroke(1.dp, c.lineStrong), ControlShape).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Loom.type.mono, color = c.accentInk, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        LinkButton("Copy", onClick = onCopy)
    }
}
