package bob.colbaskin.gidromonitor.di

import bob.colbaskin.gidromonitor.features.analysis.data.AnalysisRepositoryRemoteImpl
import bob.colbaskin.gidromonitor.features.analysis.data.AreaTitleStoreImpl
import bob.colbaskin.gidromonitor.features.analysis.domain.AnalysisRepository
import bob.colbaskin.gidromonitor.features.analysis.domain.AreaTitleStore
import bob.colbaskin.gidromonitor.features.onboarding.data.OnboardingRepositoryImpl
import bob.colbaskin.gidromonitor.features.onboarding.domain.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(impl: AnalysisRepositoryRemoteImpl): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAreaTitleStore(impl: AreaTitleStoreImpl): AreaTitleStore

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
