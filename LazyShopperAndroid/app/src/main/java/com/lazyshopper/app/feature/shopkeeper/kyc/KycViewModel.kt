package com.lazyshopper.app.feature.shopkeeper.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.KycSubmitInput
import com.lazyshopper.app.core.ui.ActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class ShopkeeperKycUiState(
    val loading: Boolean = true,
    val kycStatus: String? = null,
    val rejectReason: String? = null,
    val aadhaarId: String? = null,
    val panId: String? = null,
    val selfieId: String? = null,
    val shopPhotoId: String? = null,
    val gstNumber: String = "",
    val fssaiNumber: String = "",
    val addressText: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val detectingLocation: Boolean = false,
    val uploadingField: String? = null,
    val submitState: ActionState = ActionState.Idle,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = aadhaarId != null && panId != null && selfieId != null && shopPhotoId != null && lat != null && lng != null
}

@HiltViewModel
class KycViewModel @Inject constructor(
    private val repository: KycRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopkeeperKycUiState())
    val state: StateFlow<ShopkeeperKycUiState> = _state.asStateFlow()

    init { loadStatus() }

    fun loadStatus() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            when (val result = repository.myKyc()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        kycStatus = result.data.kycStatus,
                        rejectReason = result.data.kyc?.reason,
                        gstNumber = result.data.kyc?.gst_number ?: it.gstNumber,
                        fssaiNumber = result.data.kyc?.fssai_number ?: it.fssaiNumber,
                        addressText = result.data.kyc?.address_text ?: it.addressText,
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, errorMessage = result.message) }
            }
        }
    }

    fun setGstNumber(v: String) = _state.update { it.copy(gstNumber = v) }
    fun setFssaiNumber(v: String) = _state.update { it.copy(fssaiNumber = v) }
    fun setAddressText(v: String) = _state.update { it.copy(addressText = v) }
    fun setDetectingLocation(v: Boolean) = _state.update { it.copy(detectingLocation = v) }
    fun setLocation(lat: Double, lng: Double) = _state.update { it.copy(lat = lat, lng = lng, detectingLocation = false) }

    fun uploadDoc(field: String, part: MultipartBody.Part) {
        viewModelScope.launch {
            _state.update { it.copy(uploadingField = field, errorMessage = null) }
            when (val result = repository.uploadDoc(part)) {
                is ApiResult.Success -> _state.update {
                    val id = result.data.image_id
                    val withId = when (field) {
                        "aadhaar" -> it.copy(aadhaarId = id)
                        "pan" -> it.copy(panId = id)
                        "selfie" -> it.copy(selfieId = id)
                        "shop_photo" -> it.copy(shopPhotoId = id)
                        else -> it
                    }
                    withId.copy(uploadingField = null)
                }
                is ApiResult.Failure -> _state.update { it.copy(uploadingField = null, errorMessage = result.message) }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) {
            _state.update { it.copy(submitState = ActionState.Failed("Upload all documents and detect your shop's location")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitState = ActionState.InFlight) }
            val input = KycSubmitInput(
                aadhaar_id = s.aadhaarId,
                pan_id = s.panId,
                selfie_id = s.selfieId,
                shop_photo_id = s.shopPhotoId,
                gst_number = s.gstNumber.ifBlank { null },
                fssai_number = s.fssaiNumber.ifBlank { null },
                lat = s.lat,
                lng = s.lng,
                address_text = s.addressText.ifBlank { null },
            )
            when (val result = repository.submit(input)) {
                is ApiResult.Success -> _state.update { it.copy(submitState = ActionState.Done, kycStatus = "submitted") }
                is ApiResult.Failure -> _state.update { it.copy(submitState = ActionState.Failed(result.message)) }
            }
        }
    }
}
