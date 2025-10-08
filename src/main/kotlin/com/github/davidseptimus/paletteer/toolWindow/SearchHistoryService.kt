package com.github.davidseptimus.paletteer.toolWindow

import com.intellij.openapi.components.*

/**
 * State object for storing search history.
 */
@State(
    name = "PaletteerSearchHistory",
    storages = [Storage("paletteer-search-history.xml")]
)
@Service(Service.Level.APP)
class SearchHistoryService : PersistentStateComponent<SearchHistoryService.State> {

    data class State(
        var searches: MutableList<String> = mutableListOf()
    )

    private var state = State()

    companion object {
        const val MAX_HISTORY_SIZE = 50

        fun getInstance(): SearchHistoryService = service()
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    /**
     * Add a search query to history.
     * Avoids duplicates and maintains max size.
     */
    fun addSearch(query: String) {
        if (query.isBlank()) return

        // Remove existing instance if present
        state.searches.remove(query)

        // Add to front
        state.searches.add(0, query)

        // Trim to max size
        if (state.searches.size > MAX_HISTORY_SIZE) {
            state.searches = state.searches.take(MAX_HISTORY_SIZE).toMutableList()
        }
    }

    /**
     * Get all search history, most recent first.
     */
    fun getSearchHistory(): List<String> = state.searches.toList()

    /**
     * Clear all search history.
     */
    fun clearHistory() {
        state.searches.clear()
    }
}