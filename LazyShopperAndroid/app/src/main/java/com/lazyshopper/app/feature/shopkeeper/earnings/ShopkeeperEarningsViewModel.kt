package com.lazyshopper.app.feature.shopkeeper.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.BankDetails
import com.lazyshopper.app.core.data.remote.dto.BankDetailsInput
import com.lazyshopper.app.core.data.remote.dto.ShopkeeperEarningsResponse
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopkeeperEarningsUiState(
    val screen: UiState<ShopkeeperEarningsResponse> = UiState.Loading,
    val holder: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val vpa: String = "",
    val bankSaveState: ActionState = ActionState.Idle,
)

@HiltViewModel
class ShopkeeperEarningsViewModel @Inject constructor(
    private val repository: ShopkeeperEarningsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopkeeperEarningsUiState())
    val state: StateFlow<ShopkeeperEarningsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(screen = UiState.Loading) }

            when (val result = repository.earnings()) {
                is ApiResult.Success -> _state.update { it.copy(screen = UiState.Success(result.data)) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(screen = UiState.Error(result.message)) }
                    return@launch
                }
            }

            val bank = (repository.bank() as? ApiResult.Success)?.data ?: BankDetails()
            _state.update {
                it.copy(
                    holder = bank.holder.orEmpty(),
                    accountNumber = bank.account_number.orEmpty(),
                    ifsc = bank.ifsc.orEmpty(),
                    vpa = bank.vpa.orEmpty(),
                )
            }
        }
    }

    fun setHolder(v: String) = _state.update { it.copy(holder = v) }
    fun setAccountNumber(v: String) = _state.update { it.copy(accountNumber = v) }
    fun setIfsc(v: String) = _state.update { it.copy(ifsc = v) }
    fun setVpa(v: String) = _state.update { it.copy(vpa = v) }

    fun saveBank() {
        viewModelScope.launch {
            _state.update { it.copy(bankSaveState = ActionState.InFlight) }
            val s = _state.value
            val input = BankDetailsInput(
                holder = s.holder.ifBlank { null },
                account_number = s.accountNumber.ifBlank { null },
                ifsc = s.ifsc.ifBlank { null },
                vpa = s.vpa.ifBlank { null },
            )
            when (val result = repository.updateBank(input)) {
                is ApiResult.Success -> _state.update { it.copy(bankSaveState = ActionState.Done) }
                is ApiResult.Failure -> _state.update { it.copy(bankSaveState = ActionState.Failed(result.message)) }
            }
        }
    }
}
