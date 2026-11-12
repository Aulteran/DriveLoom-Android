package io.github.aulteran.driveloom.ui.components

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/** Lets a password manager fill this field. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(vararg types: AutofillType, onFill: (String) -> Unit): Modifier = composed {
    val autofill = LocalAutofill.current
    val tree = LocalAutofillTree.current
    val node = remember { AutofillNode(autofillTypes = types.toList(), onFill = onFill) }
    tree += node
    this
        .onGloballyPositioned { node.boundingBox = it.boundsInWindow() }
        .onFocusChanged { focus ->
            autofill?.run {
                if (focus.isFocused && node.boundingBox != null) requestAutofillForNode(node) else cancelAutofillForNode(node)
            }
        }
}
