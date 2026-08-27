package io.abbafather.feature.myprayers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.abbafather.domain.model.PersonalPrayer
import io.abbafather.domain.repository.PersonalPrayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * The written prayers. The only state this screen owns is which card, if any, is asking whether the
 * reader means to delete it — and that lives in [SavedStateHandle], so a rotation taken mid-question
 * comes back to the question rather than quietly dropping it.
 */
@HiltViewModel
class MyPrayersViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val personalPrayerRepository: PersonalPrayerRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<MyPrayersUiState> = combine(
        personalPrayerRepository.observePersonalPrayers(),
        savedStateHandle.getStateFlow(PendingDeleteKey, NothingPendingDeletion),
    ) { personalPrayers, pendingDeleteId ->
        MyPrayersUiState(
            prayers = personalPrayers.map { personalPrayer ->
                personalPrayer.toCard(isAwaitingDeleteConfirmation = personalPrayer.id == pendingDeleteId)
            },
            isLoaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyPrayersUiState(),
    )

    fun onAction(action: MyPrayersAction) {
        when (action) {
            is MyPrayersAction.AskToDelete -> savedStateHandle[PendingDeleteKey] = action.personalPrayerId
            MyPrayersAction.CancelDelete -> savedStateHandle[PendingDeleteKey] = NothingPendingDeletion
            is MyPrayersAction.ConfirmDelete -> viewModelScope.launch {
                personalPrayerRepository.deletePersonalPrayer(
                    action.personalPrayerId,
                    deletedAt = clock.millis(),
                )
                savedStateHandle[PendingDeleteKey] = NothingPendingDeletion
            }
            // Both are the navigator's alone; opening a written prayer records nothing, because it
            // is the reader's own and its date is the date they last changed it.
            is MyPrayersAction.OpenPrayer, MyPrayersAction.WriteNewPrayer -> Unit
        }
    }

    private companion object {
        const val PendingDeleteKey = "myPrayersPendingDeleteId"
        const val NothingPendingDeletion = ""
    }
}

private fun PersonalPrayer.toCard(isAwaitingDeleteConfirmation: Boolean) = MyPrayerCardUiState(
    personalPrayerId = id,
    title = title,
    excerpt = excerpt,
    lastTouchedAt = updatedAt,
    isAwaitingDeleteConfirmation = isAwaitingDeleteConfirmation,
)
