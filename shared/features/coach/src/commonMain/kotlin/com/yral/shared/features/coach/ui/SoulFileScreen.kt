package com.yral.shared.features.coach.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.EnabledSkill
import com.yral.shared.features.chat.domain.models.EngagementSchedule
import com.yral.shared.features.chat.domain.models.FirstTurnNudge
import com.yral.shared.features.chat.domain.models.InactivityProactive
import com.yral.shared.features.chat.domain.models.SkillCheckins
import com.yral.shared.features.chat.domain.models.SystemPromptPreview
import com.yral.shared.features.chat.domain.models.SystemPromptSection
import com.yral.shared.features.coach.nav.SoulFileComponent
import com.yral.shared.features.coach.viewmodel.SoulFileViewModel
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.coach.generated.resources.Res
import yral_mobile.shared.features.coach.generated.resources.coach_back
import yral_mobile.shared.features.coach.generated.resources.soul_file_archetype_label
import yral_mobile.shared.features.coach.generated.resources.soul_file_as_of_label
import yral_mobile.shared.features.coach.generated.resources.soul_file_empty_subtitle
import yral_mobile.shared.features.coach.generated.resources.soul_file_header_hint
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_empty
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l1
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l2_empty
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l2
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l3_flat
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l3_sections
import yral_mobile.shared.features.coach.generated.resources.soul_file_layer_l4
import yral_mobile.shared.features.coach.generated.resources.soul_file_load_failed
import yral_mobile.shared.features.coach.generated.resources.soul_file_load_retry
import yral_mobile.shared.features.coach.generated.resources.soul_file_loading
import yral_mobile.shared.features.coach.generated.resources.soul_file_overrides_empty
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_cadence_hours
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_enabled_no
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_enabled_yes
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_first_turn_header
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_inactivity_header
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_initial_idle_minutes
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_per_conv_overrides
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_per_user_preferred_times_no
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_per_user_preferred_times_yes
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_section
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_skill_checkins_empty
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_skill_checkins_header
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_source
import yral_mobile.shared.features.coach.generated.resources.soul_file_engagement_threshold_hours
import yral_mobile.shared.features.coach.generated.resources.soul_file_overrides_section
import yral_mobile.shared.features.coach.generated.resources.soul_file_screen_title
import yral_mobile.shared.features.coach.generated.resources.soul_file_skills_empty
import yral_mobile.shared.features.coach.generated.resources.soul_file_skills_section
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun SoulFileScreen(
    component: SoulFileComponent,
    viewModel: SoulFileViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val params = component.openSoulFileParams

    // Always fetch fresh on every resume. Backend ships Cache-Control:
    // no-store and Rishi's flow is: open preview → back to Coach → apply
    // override → back to preview. The resume hook covers both the first
    // composition AND the back-stack pop, so we don't need a separate
    // LaunchedEffect for initial load.
    LifecycleResumeEffect(params.botId) {
        viewModel.openForBot(params.botId)
        onPauseOrDispose {}
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.PrimaryContainer)
                .safeDrawingPadding(),
    ) {
        SoulFileHeader(
            avatarUrl = params.avatarUrl,
            botName = params.botName ?: viewState.preview?.botName,
            onBack = { component.onBack() },
        )

        SoulFileHeaderHint()

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            val preview = viewState.preview
            when {
                viewState.isLoading -> SoulFileStatusBlock(stringResource(Res.string.soul_file_loading))
                viewState.error != null && preview == null -> SoulFileLoadFailure(onRetry = { viewModel.retry() })
                preview == null -> SoulFileStatusBlock(stringResource(Res.string.soul_file_empty_subtitle))
                else -> SoulFilePreviewBody(preview = preview)
            }
        }
    }
}

@Composable
private fun SoulFileHeader(
    avatarUrl: String?,
    botName: String?,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.PrimaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = stringResource(Res.string.coach_back),
            modifier = Modifier.size(28.dp).clickable(onClick = onBack),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.size(36.dp).background(YralColors.Neutral800, CircleShape)) {
            YralAsyncImage(
                imageUrl = avatarUrl.orEmpty(),
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.soul_file_screen_title),
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = botName.orEmpty(),
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SoulFileHeaderHint() {
    Text(
        text = stringResource(Res.string.soul_file_header_hint),
        style = LocalAppTopography.current.smRegular,
        color = YralColors.NeutralTextSecondary,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral900)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SoulFileStatusBlock(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SoulFileLoadFailure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.soul_file_load_failed),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(
                stringResource(Res.string.soul_file_load_retry),
                color = YralColors.Pink300,
                style = LocalAppTopography.current.baseSemiBold,
            )
        }
    }
}

@Composable
private fun SoulFilePreviewBody(preview: SystemPromptPreview) {
    val rows = buildRowList(preview)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "meta") {
            SoulFileMetaCard(
                botName = preview.botName,
                archetype = preview.archetype,
                asOf = preview.asOf,
            )
        }
        items(rows, key = { it.key }) { row -> SoulFileExpandableRow(row) }
    }
}

@Composable
private fun SoulFileMetaCard(
    botName: String?,
    archetype: String?,
    asOf: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral900, RoundedCornerShape(14.dp))
                .padding(14.dp),
    ) {
        Text(
            text = botName.orEmpty(),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.soul_file_archetype_label, archetype.orEmpty()),
            style = LocalAppTopography.current.smRegular,
            color = YralColors.NeutralTextSecondary,
        )
        Text(
            text = stringResource(Res.string.soul_file_as_of_label, asOf),
            style = LocalAppTopography.current.smRegular,
            color = YralColors.NeutralTextSecondary,
        )
    }
}

private data class SoulFileRow(
    val key: String,
    val title: String,
    val body: @Composable () -> Unit,
)

@Composable
private fun buildRowList(preview: SystemPromptPreview): List<SoulFileRow> {
    val rows = mutableListOf<SoulFileRow>()
    val layerEmptyText = stringResource(Res.string.soul_file_layer_empty)

    rows +=
        SoulFileRow(
            key = "l1",
            title = stringResource(Res.string.soul_file_layer_l1),
            body = {
                if (preview.layers.l1GlobalRules.isBlank()) {
                    PlainTextBody(layerEmptyText)
                } else {
                    PlainTextBody(preview.layers.l1GlobalRules)
                }
            },
        )
    val l2EmptyText = stringResource(Res.string.soul_file_layer_l2_empty)
    rows +=
        SoulFileRow(
            key = "l2",
            title = stringResource(Res.string.soul_file_layer_l2),
            body = {
                if (preview.layers.l2ArchetypeBlock.isBlank()) {
                    PlainTextBody(l2EmptyText)
                } else {
                    PlainTextBody(preview.layers.l2ArchetypeBlock)
                }
            },
        )
    when {
        preview.layers.l3PersonalitySections.isNotEmpty() -> {
            rows +=
                SoulFileRow(
                    key = "l3",
                    title = stringResource(Res.string.soul_file_layer_l3_sections),
                    body = { SectionListBody(preview.layers.l3PersonalitySections) },
                )
        }
        !preview.layers.l3FlatFallback.isNullOrBlank() -> {
            rows +=
                SoulFileRow(
                    key = "l3-flat",
                    title = stringResource(Res.string.soul_file_layer_l3_flat),
                    body = { PlainTextBody(preview.layers.l3FlatFallback ?: "") },
                )
        }
        else -> {
            rows +=
                SoulFileRow(
                    key = "l3-empty",
                    title = stringResource(Res.string.soul_file_layer_l3_sections),
                    body = { PlainTextBody(layerEmptyText) },
                )
        }
    }
    rows +=
        SoulFileRow(
            key = "l4",
            title = stringResource(Res.string.soul_file_layer_l4),
            body = {
                if (preview.layers.l4UserSegmentTemplate.isBlank()) {
                    PlainTextBody(layerEmptyText)
                } else {
                    PlainTextBody(preview.layers.l4UserSegmentTemplate)
                }
            },
        )
    val skillsEmptyText = stringResource(Res.string.soul_file_skills_empty)
    rows +=
        SoulFileRow(
            key = "skills",
            title = stringResource(Res.string.soul_file_skills_section),
            body = {
                if (preview.skillsEnabled.isEmpty()) {
                    PlainTextBody(skillsEmptyText)
                } else {
                    SkillsListBody(preview.skillsEnabled)
                }
            },
        )
    val overridesEmptyText = stringResource(Res.string.soul_file_overrides_empty)
    rows +=
        SoulFileRow(
            key = "overrides",
            title = stringResource(Res.string.soul_file_overrides_section),
            body = {
                if (preview.appliedOverrides.isEmpty()) {
                    PlainTextBody(overridesEmptyText)
                } else {
                    OverridesListBody(preview.appliedOverrides)
                }
            },
        )
    // Engagement schedule is a backend-side follow-up to the initial
    // preview endpoint. When the response predates that field the
    // mapper returns null and this row stays hidden. Once the backend
    // ships the addendum the row appears automatically — no mobile flag.
    preview.engagementSchedule?.let { schedule ->
        rows +=
            SoulFileRow(
                key = "engagement",
                title = stringResource(Res.string.soul_file_engagement_section),
                body = { EngagementScheduleBody(schedule) },
            )
    }
    return rows
}

@Composable
private fun SoulFileExpandableRow(row: SoulFileRow) {
    var expanded by rememberSaveable(row.key) { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral900, RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.title,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "−" else "+",
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Pink300,
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            row.body()
        }
    }
}

@Composable
private fun PlainTextBody(text: String) {
    Text(
        text = text,
        style = LocalAppTopography.current.baseRegular,
        color = YralColors.NeutralTextSecondary,
    )
}

@Composable
private fun SectionListBody(sections: List<SystemPromptSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.forEach { section ->
            Column {
                Text(
                    text = section.heading,
                    style = LocalAppTopography.current.smSemiBold,
                    color = YralColors.NeutralTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = section.body,
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SkillsListBody(skills: List<EnabledSkill>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        skills.forEach { skill ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(YralColors.Neutral800, RoundedCornerShape(10.dp))
                        .padding(12.dp),
            ) {
                Text(
                    text = skill.name,
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.NeutralTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = skill.description,
                    style = LocalAppTopography.current.smRegular,
                    color = YralColors.NeutralTextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = skill.promptBlock,
                    style = LocalAppTopography.current.smRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun EngagementScheduleBody(schedule: EngagementSchedule) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EngagementSubRow(
            header = stringResource(Res.string.soul_file_engagement_inactivity_header),
            source = schedule.inactivityProactive?.source,
        ) {
            schedule.inactivityProactive?.let { ip -> InactivityProactiveBody(ip) }
        }
        val sc = schedule.skillCheckins
        EngagementSubRow(
            header = stringResource(Res.string.soul_file_engagement_skill_checkins_header),
            source = sc?.source,
        ) {
            if (sc == null) {
                PlainTextBody(stringResource(Res.string.soul_file_engagement_skill_checkins_empty))
            } else {
                SkillCheckinsBody(sc)
            }
        }
        EngagementSubRow(
            header = stringResource(Res.string.soul_file_engagement_first_turn_header),
            source = schedule.firstTurnNudge?.source,
        ) {
            schedule.firstTurnNudge?.let { ftn -> FirstTurnNudgeBody(ftn) }
        }
    }
}

@Composable
private fun EngagementSubRow(
    header: String,
    source: String?,
    body: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral800, RoundedCornerShape(10.dp))
                .padding(12.dp),
    ) {
        Text(
            text = header,
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        body()
        if (!source.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.soul_file_engagement_source, source),
                style = LocalAppTopography.current.smRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Composable
private fun InactivityProactiveBody(ip: InactivityProactive) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ip.enabledByDefault?.let {
            Text(
                text =
                    stringResource(
                        if (it) Res.string.soul_file_engagement_enabled_yes
                        else Res.string.soul_file_engagement_enabled_no,
                    ),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
        }
        ip.thresholdHours?.let {
            Text(
                text = stringResource(Res.string.soul_file_engagement_threshold_hours, it.toString()),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        ip.perConversationOverrides.takeIf { it.isNotEmpty() }?.let { list ->
            Text(
                text = stringResource(Res.string.soul_file_engagement_per_conv_overrides, list.size),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        ip.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Composable
private fun SkillCheckinsBody(sc: SkillCheckins) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sc.displayName?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
            )
        }
        sc.defaultCadenceHours?.let {
            Text(
                text = stringResource(Res.string.soul_file_engagement_cadence_hours, it.toString()),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        sc.perUserPreferredTimes?.let { honoured ->
            Text(
                text =
                    stringResource(
                        if (honoured) Res.string.soul_file_engagement_per_user_preferred_times_yes
                        else Res.string.soul_file_engagement_per_user_preferred_times_no,
                    ),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        sc.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Composable
private fun FirstTurnNudgeBody(ftn: FirstTurnNudge) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ftn.enabled?.let {
            Text(
                text =
                    stringResource(
                        if (it) Res.string.soul_file_engagement_enabled_yes
                        else Res.string.soul_file_engagement_enabled_no,
                    ),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextPrimary,
            )
        }
        ftn.initialIdleMinutes?.let {
            Text(
                text = stringResource(Res.string.soul_file_engagement_initial_idle_minutes, it.toString()),
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        ftn.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Composable
private fun OverridesListBody(overrides: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        overrides.forEach { (key, value) ->
            Row {
                Text(
                    text = key,
                    style = LocalAppTopography.current.smSemiBold,
                    color = YralColors.NeutralTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = LocalAppTopography.current.smRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
    }
}
