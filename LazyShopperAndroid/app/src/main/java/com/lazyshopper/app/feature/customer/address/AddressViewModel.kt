package com.lazyshopper.app.feature.customer.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Address
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressFormState(
    val id: String? = null,
    val label: String = "Home",
    val address: String = "",
    val phone: String = "",
    val isDefault: Boolean = false,
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: AddressRepository,
) : ViewModel() {

    private val _addresses = MutableStateFlow<UiState<List<Address>>>(UiState.Loading)
    val addresses: StateFlow<UiState<List<Address>>> = _addresses.asStateFlow()

    private val _form = MutableStateFlow<AddressFormState?>(null)
    val form: StateFlow<AddressFormState?> = _form.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _addresses.value = UiState.Loading
            when (val r = repository.list()) {
                is ApiResult.Success -> _addresses.value = UiState.Success(r.data)
                is ApiResult.Failure -> _addresses.value = UiState.Error(r.message)
            }
        }
    }

    fun startAdd() {
        _form.value = AddressFormState()
    }

    fun startEdit(address: Address) {
        _form.value = AddressFormState(address.id, address.label, address.address, address.phone, address.is_default)
    }

    fun dismissForm() {
        _form.value = null
        _actionState.value = ActionState.Idle
    }

    fun updateForm(transform: (AddressFormState) -> AddressFormState) {
        _form.value = _form.value?.let(transform)
    }

    fun save() {
        val f = _form.value ?: return
        if (f.address.isBlank() || f.phone.isBlank()) {
            _actionState.value = ActionState.Failed("Address and phone are required")
            return
        }
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            val result = if (f.id != null) {
                repository.update(f.id, f.label, f.address, f.phone, f.isDefault)
            } else {
                repository.add(f.label, f.address, f.phone, f.isDefault)
            }
            when (result) {
                is ApiResult.Success -> {
                    _actionState.value = ActionState.Done
                    _form.value = null
                    load()
                }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(result.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            load()
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch {
            repository.setDefault(id)
            load()
        }
    }
}
