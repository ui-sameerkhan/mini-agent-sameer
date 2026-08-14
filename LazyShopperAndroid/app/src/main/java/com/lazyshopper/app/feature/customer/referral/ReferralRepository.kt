package com.lazyshopper.app.feature.customer.referral

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ReferralApi
import com.lazyshopper.app.core.data.remote.dto.MyReferralResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferralRepository @Inject constructor(
    private val referralApi: ReferralApi,
) {
    suspend fun myReferral(): ApiResult<MyReferralResponse> = safeApiCall { referralApi.myReferral() }
}
