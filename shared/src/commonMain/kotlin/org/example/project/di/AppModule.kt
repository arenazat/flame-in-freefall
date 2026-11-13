package org.example.project.di

import org.example.project.data.network.SupabaseProvider
import org.example.project.data.repository.ExperimentRepositoryImpl
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.domain.usecase.GetExperimentsUseCase
import org.example.project.domain.usecase.GetRiskRankingsUseCase
import org.example.project.domain.usecase.QueryExperimentsUseCase
import org.example.project.utils.FlameConfig
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

import io.ktor.client.HttpClient
import org.example.project.data.network.RagRepositoryImpl
import org.example.project.domain.repository.RagRepository
import org.example.project.domain.usecase.AskRagQuestionUseCase
import org.example.project.domain.usecase.GetFilterOptionsUseCase
import org.example.project.domain.usecase.GetSimilarExperimentsUseCase
import org.example.project.ui.screens.AboutViewModel
import org.example.project.ui.screens.ExperimentListViewModel
import org.example.project.ui.screens.RiskRankingViewModel
import org.example.project.ui.screens.TimelineViewModel
import org.example.project.ui.components.AskPanelViewModel

import org.example.project.data.network.ReportRepositoryImpl
import org.example.project.domain.repository.ReportRepository
import org.example.project.domain.usecase.GenerateReportUseCase

import org.example.project.data.repository.PsiRepositoryImpl
import org.example.project.domain.repository.PsiRepository
import org.example.project.domain.usecase.GetEarthApplicationsUseCase
import org.example.project.domain.usecase.GetFireIncidentsUseCase
import org.example.project.domain.usecase.GetHabitatProfilesUseCase
import org.example.project.domain.usecase.GetPsiFilterOptionsUseCase
import org.example.project.domain.usecase.GetPsiInvestigationUseCase
import org.example.project.domain.usecase.GetPsiInvestigationsUseCase
import org.example.project.domain.usecase.GetPsiPublicationsUseCase
import org.example.project.domain.usecase.GetPsiTestConditionsUseCase
import org.example.project.domain.usecase.RankForHabitatUseCase
import org.example.project.ui.psi.HabitatViewModel
import org.example.project.ui.psi.HistoryEarthViewModel
import org.example.project.ui.psi.PsiCatalogViewModel
import org.example.project.ui.psi.PsiCompareViewModel
import org.example.project.ui.psi.PsiDetailViewModel

val networkModule = module {
    single<FlameConfig> { FlameConfig.default() }
    single<HttpClient> { HttpClient() }
    single<SupabaseProvider> { SupabaseProvider(get()) }
}

val repositoryModule = module {
    single<ExperimentRepository> { ExperimentRepositoryImpl(get()) }
    single<RagRepository> { RagRepositoryImpl(get(), get()) }
    single<ReportRepository> { ReportRepositoryImpl(get(), get()) }
    single<PsiRepository> { PsiRepositoryImpl(get()) }
}

val useCaseModule = module {
    factory { GetExperimentsUseCase(get()) }
    factory { GetRiskRankingsUseCase(get()) }
    factory { QueryExperimentsUseCase(get()) }
    factory { GetFilterOptionsUseCase(get()) }
    factory { GetSimilarExperimentsUseCase(get()) }
    factory { AskRagQuestionUseCase(get()) }
    factory { GenerateReportUseCase(get()) }
    factory { GetPsiInvestigationsUseCase(get()) }
    factory { GetPsiInvestigationUseCase(get()) }
    factory { GetPsiTestConditionsUseCase(get()) }
    factory { GetPsiPublicationsUseCase(get()) }
    factory { GetPsiFilterOptionsUseCase(get()) }
    factory { GetHabitatProfilesUseCase(get()) }
    factory { RankForHabitatUseCase(get()) }
    factory { GetFireIncidentsUseCase(get()) }
    factory { GetEarthApplicationsUseCase(get()) }
}

val viewModelModule = module {
    factory { ExperimentListViewModel(get(), get(), get()) }
    factory { RiskRankingViewModel(get()) }
    factory { TimelineViewModel(get()) }
    factory { AboutViewModel(get()) }
    single { AskPanelViewModel(get()) }
    factory { PsiCatalogViewModel(get(), get()) }
    factory { PsiDetailViewModel(get(), get(), get()) }
    factory { PsiCompareViewModel(get()) }
    factory { HabitatViewModel(get(), get(), get()) }
    factory { HistoryEarthViewModel(get(), get(), get()) }
}

val appModule: List<Module> = listOf(
    networkModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)

fun initKoin(appDeclaration: KoinAppDeclaration? = null) {
    startKoin {
        appDeclaration?.invoke(this)
        modules(appModule)
    }
}
