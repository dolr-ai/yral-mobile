package com.yral.shared.features.tournament.di

import com.yral.shared.features.tournament.data.ITournamentRemoteDataSource
import com.yral.shared.features.tournament.data.TournamentRemoteDataSource
import com.yral.shared.features.tournament.data.TournamentRepository
import com.yral.shared.features.tournament.domain.CastTournamentVoteUseCase
import com.yral.shared.features.tournament.domain.GetMyTournamentsUseCase
import com.yral.shared.features.tournament.domain.GetTournamentLeaderboardUseCase
import com.yral.shared.features.tournament.domain.GetTournamentStatusUseCase
import com.yral.shared.features.tournament.domain.GetTournamentsUseCase
import com.yral.shared.features.tournament.domain.ITournamentRepository
import com.yral.shared.features.tournament.domain.RegisterForTournamentUseCase
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import com.yral.shared.features.tournament.viewmodel.TournamentLeaderboardViewModel
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val tournamentModule =
    module {
        // Data Source
        factoryOf(::TournamentRemoteDataSource) { bind<ITournamentRemoteDataSource>() }

        // Repository
        factoryOf(::TournamentRepository) { bind<ITournamentRepository>() }

        // Use Cases
        factoryOf(::GetTournamentsUseCase)
        factoryOf(::GetTournamentStatusUseCase)
        factoryOf(::RegisterForTournamentUseCase)
        factoryOf(::GetMyTournamentsUseCase)
        factoryOf(::CastTournamentVoteUseCase)
        factoryOf(::GetTournamentLeaderboardUseCase)

        // ViewModels
        viewModelOf(::TournamentViewModel)
        viewModelOf(::TournamentLeaderboardViewModel)
        viewModelOf(::TournamentGameViewModel)
    }
