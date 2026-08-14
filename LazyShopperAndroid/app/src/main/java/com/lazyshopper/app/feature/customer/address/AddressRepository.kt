package com.lazyshopper.app.feature.customer.address

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.dto.Address
import com.lazyshopper.app.core.data.remote.dto.AddressInput
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(
    private val authApi: AuthApi,
) {
    suspend fun list(): ApiResult<List<Address>> = safeApiCall { authApi.myAddresses() }

    suspend fun add(label: String, address: String, phone: String, isDefault: Boolean): ApiResult<Address> =
        safeApiCall { authApi.addAddress(AddressInput(label, address, phone, isDefault)) }

    suspend fun update(id: String, label: String, address: String, phone: String, isDefault: Boolean): ApiResult<Address> =
        safeApiCall { authApi.updateAddress(id, AddressInput(label, address, phone, isDefault)) }

    suspend fun delete(id: String): ApiResult<MessageResponse> = safeApiCall { authApi.deleteAddress(id) }

    suspend fun setDefault(id: String): ApiResult<MessageResponse> = safeApiCall { authApi.setDefaultAddress(id) }
}
