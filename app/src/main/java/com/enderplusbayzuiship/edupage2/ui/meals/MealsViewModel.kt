package com.enderplusbayzuiship.edupage2.ui.meals

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.Meal
import com.edupage.api.model.MealOrderInfo
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.MealsCache
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface MealsUiState {
    object Loading : MealsUiState
    data class Error(val message: String) : MealsUiState
    data class Success(
        val meals: List<Meal>,
        val date: LocalDate,
        val isRefreshing: Boolean = false,
        val credit: String? = null,
        val mealOrderInfo: List<MealOrderInfo>? = null,
        val boarderId: String? = null,
    ) : MealsUiState
}

@HiltViewModel
class MealsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val cache: MealsCache,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "MealsViewModel"
    }

    private val _uiState = MutableStateFlow<MealsUiState>(MealsUiState.Loading)
    val uiState: StateFlow<MealsUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private var loadJob: Job? = null

    init {
        Log.i(TAG, "init: loading meals for ${_selectedDate.value}")
        loadMeals(_selectedDate.value)
    }

    fun goToDate(date: LocalDate) {
        if (_selectedDate.value == date) return
        _selectedDate.value = date
        loadMeals(date)
    }

    fun goToToday() {
        goToDate(LocalDate.now())
    }

    fun nextDay() {
        goToDate(_selectedDate.value.plusDays(1))
    }

    fun prevDay() {
        goToDate(_selectedDate.value.minusDays(1))
    }

    fun refresh() {
        Log.i(TAG, "refresh: forcing network fetch for ${_selectedDate.value}")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _uiState.value
            if (current is MealsUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                _uiState.value = MealsUiState.Loading
            }
            fetchAndUpdate(_selectedDate.value)
        }
    }

    fun orderMeal(mealTypeIndex: String, choice: String) {
        val state = _uiState.value as? MealsUiState.Success ?: return
        val boarderId = state.boarderId ?: return
        val date = state.date

        viewModelScope.launch {
            try {
                val ok = try {
                    edupage.orderMeal(date, mealTypeIndex, choice, boarderId)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.orderMeal(date, mealTypeIndex, choice, boarderId)
                }
                if (ok) {
                    Log.i(TAG, "ordered $choice for type $mealTypeIndex on $date, refreshing")
                    refresh()
                } else {
                    Log.w(TAG, "order failed for $choice type $mealTypeIndex on $date")
                }
            } catch (e: Exception) {
                Log.e(TAG, "order failed for $choice type $mealTypeIndex on $date: ${e.message}", e)
            }
        }
    }

    fun cancelMeal(mealTypeIndex: String) {
        val state = _uiState.value as? MealsUiState.Success ?: return
        val boarderId = state.boarderId ?: return
        val date = state.date

        viewModelScope.launch {
            try {
                val ok = try {
                    edupage.cancelMeal(date, mealTypeIndex, boarderId)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.cancelMeal(date, mealTypeIndex, boarderId)
                }
                if (ok) {
                    Log.i(TAG, "cancelled type $mealTypeIndex on $date, refreshing")
                    refresh()
                } else {
                    Log.w(TAG, "cancel failed for type $mealTypeIndex on $date")
                }
            } catch (e: Exception) {
                Log.e(TAG, "cancel failed for type $mealTypeIndex on $date: ${e.message}", e)
            }
        }
    }

    fun rateMeal(mealTypeIndex: String, quality: Int, quantity: Int) {
        val state = _uiState.value as? MealsUiState.Success ?: return
        val boarderId = state.boarderId ?: return
        val date = state.date

        viewModelScope.launch {
            try {
                val ok = try {
                    edupage.rateMeal(date, mealTypeIndex, boarderId, quality, quantity)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.rateMeal(date, mealTypeIndex, boarderId, quality, quantity)
                }
                if (ok) {
                    Log.i(TAG, "rated type $mealTypeIndex on $date, refreshing")
                    refresh()
                } else {
                    Log.w(TAG, "rating failed for type $mealTypeIndex on $date")
                }
            } catch (e: Exception) {
                Log.e(TAG, "rating failed for type $mealTypeIndex on $date: ${e.message}", e)
            }
        }
    }

    private fun loadMeals(date: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val cached = cache.load(date.toString())
            if (cached != null && cached.date == date) {
                Log.i(TAG, "cache hit: showing ${cached.meals.size} meals for $date")
                _uiState.value = MealsUiState.Success(
                    meals = cached.meals,
                    date = date,
                    isRefreshing = true,
                    credit = cached.credit,
                    mealOrderInfo = cached.mealOrderInfo,
                    boarderId = cached.boarderId,
                )
            } else {
                Log.i(TAG, "cache miss for $date, fetching from network")
                _uiState.value = MealsUiState.Loading
            }

            fetchAndUpdate(date)
        }
    }

    private suspend fun fetchAndUpdate(date: LocalDate) {
        if (!com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver.isOnline.value) {
            val offline = _uiState.value
            if (offline is MealsUiState.Success) {
                _uiState.value = offline.copy(isRefreshing = false)
            } else {
                _uiState.value = MealsUiState.Error(context.getString(R.string.network_error))
            }
            return
        }
        try {
            val meals = try {
                edupage.getMeals(date)
            } catch (e: NotLoggedInException) {
                Log.w(TAG, "session expired, re-authenticating and retrying once")
                sessionRepository.ensureValidSession()
                edupage.getMeals(date)
            }
            if (meals != null) {
                cache.save(meals)
                Log.i(TAG, "fetch success: ${meals.meals.size} meals for $date")
                _uiState.value = MealsUiState.Success(
                    meals = meals.meals,
                    date = date,
                    isRefreshing = false,
                    credit = meals.credit,
                    mealOrderInfo = meals.mealOrderInfo,
                    boarderId = meals.boarderId,
                )
            } else {
                Log.w(TAG, "fetch returned null for $date")
                val current = _uiState.value
                if (current is MealsUiState.Success) {
                    _uiState.value = current.copy(isRefreshing = false)
                } else {
                    _uiState.value = MealsUiState.Error(
                        context.getString(R.string.meals_error)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetch failed for $date: ${e.message}", e)
            val current = _uiState.value
            if (current is MealsUiState.Success) {
                _uiState.value = current.copy(isRefreshing = false)
            } else {
                _uiState.value = MealsUiState.Error(
                    if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.meals_error)
                )
            }
        }
    }
}

