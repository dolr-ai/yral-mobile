package com.yral.shared.features.tournament.di

import com.yral.shared.features.tournament.viewmodel.TournamentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val tournamentModule =
    module {
        viewModelOf(::TournamentViewModel)
    }
