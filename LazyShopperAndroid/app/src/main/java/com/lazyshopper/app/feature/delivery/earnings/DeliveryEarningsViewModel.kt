package com.lazyshopper.app.feature.delivery.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.BankDetails
import com.lazyshopper.app.core.data.remote.dto.BankDetailsInput
import com.lazyshopper.app.core.data.remote.dto.DeliveryEarningsResponse
import com.lazyshopper.app.core.data.remote.dto.DeliveryPayoutBatch
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.delivery.dashboard.DeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarningsScreenData(
    val earnings: DeliveryEarningsResponse,
    val payouts: List<DeliveryPayoutBatch>,
)

data class DeliveryEarningsUiState(
    val screen: UiState<EarningsScreenData> = UiState.Loading,
    val holder: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val vpa: String = "",
    val bankSaveState: ActionState = ActionState.Idle,
)

@HiltViewModel
class DeliveryEarningsViewModel @Inject constructor(
    private val repository: DeliveryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryEarningsUiState())
    val state: StateFlow<DeliveryEarningsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(screen = UiState.Loading) }

            val earningsResult = repository.earnings()
            if (earningsResult is ApiResult.Failure) {
                _state.update { it.copy(screen = UiState.Error(earningsResult.message)) }
                return@launch
            }
            val earnings = (earningsResult as ApiResult.Success).data

            val payouts = (repository.payoutBatches() as? ApiResult.Success)?.data ?: emptyList()
            val bank = (repository.bank() as? ApiResult.Success)?.data ?: BankDetails()

            _state.update {
                it.copy(
                    screen = UiState.Success(EarningsScreenData(earnings, payouts)),
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
