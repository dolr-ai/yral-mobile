package com.yral.android.ui.screens.uploadVideo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.uploadVideo.UploadVideoScreenConstants.UPLOAD_BOX_ASPECT_RATIO
import com.yral.android.ui.widgets.YralButton
import com.yral.android.ui.widgets.YralButtonState
import com.yral.android.ui.widgets.YralGradientButton
import com.yral.android.ui.widgets.video.YralLocalVideoPlayer

private const val TOTAL_ITEMS = 5

@Composable
fun UploadVideoScreen(modifier: Modifier = Modifier) {
    val videoFilePath by remember { mutableStateOf("storage/emulated/0/Movies/default.mp4") }
    val listState = rememberLazyListState()
    val keyboardHeight by keyboardHeightAsState()
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0) {
            listState.animateScrollToItem(TOTAL_ITEMS - 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.imePadding(),
    ) {
        // Update TOTAL_ITEMS if adding any more items
        item { Header() }
        item { UploadVideo("") }
        item { Spacer(Modifier.height(20.dp)) }
        item { VideoDetailsAndSubmit() }
        item { Submit() }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.upload_video),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun UploadVideo(videoFilePath: String) {
    Row(Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier =
                Modifier
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).fillMaxWidth()
                    .aspectRatio(UPLOAD_BOX_ASPECT_RATIO)
                    .padding(8.dp)
                    .background(
                        color = YralColors.Neutral950,
                        shape = RoundedCornerShape(size = 8.dp),
                    ),
        ) {
            if (videoFilePath.isNotEmpty()) {
                VideoView(videoFilePath)
            } else {
                SelectVideoView()
            }
        }
    }
}

@Composable
private fun SelectVideoView(maxSeconds: Int = 60) {
    Column(
        modifier =
            Modifier.padding(start = 18.dp, end = 18.dp, top = 83.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.upload_video_to_share_with_world),
            style = LocalAppTopography.current.mdMedium,
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = stringResource(R.string.video_file_max_seconds, maxSeconds),
            style = LocalAppTopography.current.regMedium,
            color = YralColors.NeutralTextSecondary,
        )
        Spacer(Modifier.height(0.dp))
        YralButton(
            modifier = Modifier.wrapContentSize(),
            text = stringResource(R.string.select_file),
            borderWidth = 1.dp,
            borderColor = YralColors.Pink300,
            backgroundColor = YralColors.Neutral900,
            textStyle =
                TextStyle(
                    color = YralColors.Pink300,
                ),
        ) { }
    }
}

@Composable
private fun VideoView(videoFilePath: String) {
    Box(Modifier.fillMaxSize()) {
        YralLocalVideoPlayer(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center),
            localFilePath = videoFilePath,
            autoPlay = true,
            onError = { error ->
                Logger.d("Video error: $error")
            },
        )
        Image(
            painter = painterResource(id = R.drawable.cross),
            contentDescription = "image description",
            contentScale = ContentScale.None,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun VideoDetailsAndSubmit() {
    var text by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf(emptyList<String>()) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.caption),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            CaptionInput(text) { text = it }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.add_hashtag),
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.Neutral300,
            )
            HashtagInput(hashtags) { hashtags = it }
        }
    }
}

@Composable
fun Submit() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        YralGradientButton(
            text = stringResource(R.string.upload),
            buttonState = YralButtonState.Disabled,
        ) {
        }
    }
}

@Composable
private fun CaptionInput(
    text: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp)),
        value = text,
        onValueChange = onValueChange,
        colors =
            TextFieldDefaults.colors().copy(
                focusedTextColor = YralColors.Neutral300,
                unfocusedTextColor = YralColors.Neutral300,
                disabledTextColor = YralColors.Neutral600,
                focusedContainerColor = YralColors.Neutral800,
                unfocusedContainerColor = YralColors.Neutral800,
                disabledContainerColor = YralColors.Neutral800,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        textStyle = LocalAppTopography.current.baseRegular,
        placeholder = {
            Text(
                text = stringResource(R.string.enter_caption_here),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral600,
            )
        },
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun HashtagInput(
    hashtags: List<String>,
    onHashtagsChange: (List<String>) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var previousInputText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var shouldFocusInputField by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(YralColors.Neutral800)
                .padding(vertical = 12.dp)
                .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hashtags.isNotEmpty()) {
            Row(modifier = Modifier.padding(start = 6.dp)) {
                hashtags.forEachIndexed { index, tag ->
                    if (editingIndex == index) {
                        HashtagEditChip(
                            value = inputText,
                            onValueChange = { newValue ->
                                // If space is entered, split into two chips
                                val spaceIndex = newValue.indexOf(' ')
                                if (spaceIndex != -1) {
                                    val first = newValue.substring(0, spaceIndex).trim()
                                    val second = newValue.substring(spaceIndex + 1).trim()
                                    val newList = hashtags.toMutableList()
                                    if (first.isNotEmpty()) {
                                        newList[index] = first
                                        if (second.isNotEmpty()) {
                                            newList.add(index + 1, second)
                                        }
                                    } else if (second.isNotEmpty()) {
                                        newList[index] = second
                                    } else {
                                        newList.removeAt(index)
                                    }
                                    onHashtagsChange(newList)
                                    editingIndex = null
                                    inputText = ""
                                    shouldFocusInputField = true
                                } else if (newValue.isEmpty() && previousInputText.isEmpty()) {
                                    // Backspace on empty: make previous chip editable if exists
                                    if (index > 0) {
                                        editingIndex = index - 1
                                        inputText = hashtags[index - 1]
                                    } else {
                                        // If no previous, just remove
                                        val newList = hashtags.toMutableList()
                                        if (index in newList.indices) {
                                            newList.removeAt(index)
                                            onHashtagsChange(newList)
                                        }
                                        editingIndex = null
                                        inputText = ""
                                        shouldFocusInputField = true
                                    }
                                } else if (newValue.isEmpty()) {
                                    // If input is empty, remove the hashtag and exit edit mode
                                    val newList = hashtags.toMutableList()
                                    if (index in newList.indices) {
                                        newList.removeAt(index)
                                        onHashtagsChange(newList)
                                    }
                                    editingIndex = null
                                    inputText = ""
                                    shouldFocusInputField = true
                                } else {
                                    inputText = newValue
                                }
                                previousInputText = newValue
                            },
                        )
                    } else {
                        HashtagChip(
                            tag = tag,
                            onClick = {
                                editingIndex = index
                                inputText = tag
                            },
                        )
                    }
                }
            }
        }
        if (editingIndex == null) {
            val focusRequester = remember { FocusRequester() }
            if (shouldFocusInputField) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                // Reset the flag after requesting focus
                shouldFocusInputField = false
            }
            HashtagInputField(
                value = inputText,
                onValueChange = { newValue ->
                    // Backspace on empty: make last chip editable
                    if (previousInputText.isEmpty() && newValue.isEmpty() && hashtags.isNotEmpty()) {
                        val lastIndex = hashtags.size - 1
                        inputText = hashtags[lastIndex]
                        editingIndex = lastIndex
                        // Do NOT remove the chip here!
                    } else if (newValue.endsWith(" ")) {
                        val tag = newValue.trim()
                        if (tag.isNotEmpty()) {
                            onHashtagsChange(hashtags + tag)
                        }
                        inputText = ""
                    } else {
                        inputText = newValue
                    }
                    previousInputText = newValue
                },
                placeholder =
                    if (hashtags.isEmpty()) {
                        stringResource(R.string.hit_enter_to_add_hashtags)
                    } else {
                        stringResource(R.string.add_hashtags)
                    },
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
private fun HashtagChip(
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(YralColors.Neutral700)
                .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            Text(
                text = "#$tag",
                style = LocalAppTopography.current.regRegular,
                color = Color.White,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ChipInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    showHash: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }

    // Keep textFieldValue in sync with value from parent
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue =
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length), // always at end
                )
        }
    }

    Box(
        modifier =
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(16.dp)),
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
            },
            singleLine = true,
            textStyle = LocalAppTopography.current.regRegular.copy(color = YralColors.Neutral300),
            modifier =
                Modifier
                    .wrapContentSize()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        // When focused and not empty, move cursor to end
                        if (focusState.isFocused && textFieldValue.text.isNotEmpty()) {
                            textFieldValue =
                                textFieldValue.copy(
                                    selection = TextRange(textFieldValue.text.length),
                                )
                        }
                    }.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    if (showHash) {
                        Text(
                            text = "#",
                            style = LocalAppTopography.current.regRegular,
                            color = Color.White,
                        )
                    }
                    if (textFieldValue.text.isEmpty() && !isFocused) {
                        Text(
                            text = placeholder,
                            style = LocalAppTopography.current.regRegular,
                            color = YralColors.Neutral600,
                        )
                    } else {
                        innerTextField()
                    }
                }
            },
            cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        )
    }
}

@Composable
private fun HashtagEditChip(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    ChipInputField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(R.string.add_hashtags),
        showHash = true,
        focusRequester = focusRequester,
    )
}

@Composable
private fun HashtagInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester? = null,
) {
    ChipInputField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        showHash = false,
        focusRequester = focusRequester,
    )
}

@Composable
fun keyboardHeightAsState(): State<Int> {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val heightPx = ime.getBottom(density)
    return rememberUpdatedState(heightPx)
}

object UploadVideoScreenConstants {
    const val UPLOAD_BOX_ASPECT_RATIO = 1.18f
}
