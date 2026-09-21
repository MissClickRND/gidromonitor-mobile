package bob.colbaskin.gidromonitor.features.analysis.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import bob.colbaskin.gidromonitor.features.analysis.domain.AreaTitleStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.areaTitlesDataStore by preferencesDataStore(name = "area_titles")

class AreaTitleStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AreaTitleStore {
    override suspend fun save(areaId: String, title: String) {
        context.areaTitlesDataStore.edit { preferences ->
            preferences[stringPreferencesKey(areaId)] = title
        }
    }

    override suspend fun get(areaId: String): String? = context.areaTitlesDataStore.data.first()[stringPreferencesKey(areaId)]
}
