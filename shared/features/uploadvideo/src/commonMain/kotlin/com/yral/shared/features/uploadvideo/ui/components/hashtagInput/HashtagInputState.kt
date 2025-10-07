package com.yral.shared.features.uploadvideo.ui.components.hashtagInput

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State holder class for managing hashtag input state
 */
class HashtagInputState(
    private val hashtags: List<String>,
    private val onHashtagsChange: (List<String>) -> Unit,
    val shouldFocusInputField: Boolean,
    val updateShouldFocusInputField: (Boolean) -> Unit,
) {
    var inputText by mutableStateOf("")
        private set

    var editingIndex by mutableStateOf<Int?>(null)
        private set

    fun updateInputText(text: String) {
        inputText = text
    }

    fun updateEditingIndex(index: Int?) {
        editingIndex = index
    }

    fun createHashtag(tag: String) {
        if (tag.isNotEmpty()) {
            onHashtagsChange(hashtags + tag)
        }
        inputText = ""
    }

    fun updateHashtagAtIndex(
        index: Int,
        newTag: String,
    ) {
        val newList = hashtags.toMutableList()
        if (index in newList.indices) {
            if (newTag.isNotEmpty()) {
                newList[index] = newTag
            } else {
                newList.removeAt(index)
            }
            onHashtagsChange(newList)
        }
    }

    fun exitEditMode() {
        editingIndex = null
        inputText = ""
        updateShouldFocusInputField(true)
    }

    fun handleHashtagSplit(
        index: Int,
        newValue: String,
    ) {
        val spaceIndex = newValue.indexOf(' ')
        val enterIndex = newValue.indexOf('\n')
        val splitIndex =
            when {
                spaceIndex != -1 && enterIndex != -1 -> minOf(spaceIndex, enterIndex)
                spaceIndex != -1 -> spaceIndex
                enterIndex != -1 -> enterIndex
                else -> -1
            }

        if (splitIndex != -1) {
            val first = newValue.substring(0, splitIndex).trim()
            val second = newValue.substring(splitIndex + 1).trim()
            val newList = hashtags.toMutableList()

            when {
                first.isNotEmpty() -> {
                    newList[index] = first
                    if (second.isNotEmpty()) {
                        newList.add(index + 1, second)
                    }
                }
                second.isNotEmpty() -> {
                    newList[index] = second
                }
                else -> {
                    newList.removeAt(index)
                }
            }

            onHashtagsChange(newList)
            editingIndex = null
            inputText = ""
            updateShouldFocusInputField(true)
        }
    }

    fun handleBackspaceOnEmpty(index: Int) {
        if (index > 0) {
            // Make previous chip editable
            editingIndex = index - 1
            inputText = hashtags[index - 1]
        } else {
            // Remove current chip if no previous exists
            val newList = hashtags.toMutableList()
            if (index in newList.indices) {
                newList.removeAt(index)
                onHashtagsChange(newList)
            }
            exitEditMode()
        }
    }

    fun handleEditChipValueChange(
        newValue: String,
        index: Int,
    ) {
        when {
            // Handle space or enter - split into hashtags
            newValue.contains(' ') || newValue.contains('\n') -> {
                handleHashtagSplit(index, newValue)
            }

            newValue.isEmpty() -> {
                when {
                    inputText.isEmpty() -> handleBackspaceOnEmpty(index)
                    inputText.isNotEmpty() -> {
                        updateHashtagAtIndex(index, "")
                        exitEditMode()
                    }
                }
            }

            // Regular text input
            else -> {
                inputText = newValue
            }
        }
    }

    fun handleInputFieldValueChange(newValue: String) {
        when {
            // Handle backspace on empty input - edit last chip
            inputText.isEmpty() && newValue.isEmpty() && hashtags.isNotEmpty() -> {
                val lastIndex = hashtags.size - 1
                inputText = hashtags[lastIndex]
                editingIndex = lastIndex
            }
            // Handle space or enter - create new hashtag
            newValue.endsWith(' ') || newValue.endsWith('\n') -> {
                createHashtag(newValue.trim())
            }
            // Regular text input
            else -> {
                inputText = newValue
            }
        }
    }

    fun handleEditChipDone(index: Int) {
        updateHashtagAtIndex(index, inputText)
        exitEditMode()
    }

    fun handleInputFieldDone() {
        createHashtag(inputText.trim())
    }
}

@Composable
fun rememberHashtagInputState(
    hashtags: List<String>,
    onHashtagsChange: (List<String>) -> Unit,
): HashtagInputState {
    var shouldFocusInputField by remember { mutableStateOf(false) }
    return remember(hashtags, onHashtagsChange, shouldFocusInputField) {
        HashtagInputState(
            hashtags,
            onHashtagsChange,
            shouldFocusInputField,
        ) {
            shouldFocusInputField = it
        }
    }
}
