package com.lazyshopper.app.feature.admin.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminReferralsResponse
import com.lazyshopper.app.core.data.remote.dto.ReferralSettings
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminFinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _settings = MutableStateFlow<UiState<ReferralSettings>>(UiState.Loading)
    val settings: StateFlow<UiState<ReferralSettings>> = _settings.asStateFlow()
    private val _referrals = MutableStateFlow<UiState<AdminReferralsResponse>>(UiState.Loading)
    val referrals: StateFlow<UiState<AdminReferralsResponse>> = _referrals.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            _settings.value = UiState.Loading
            when (val r = repo.referralSettings()) {
                is ApiResult.Success -> _settings.value = UiState.Success(r.data)
                is ApiResult.Failure -> _settings.value = UiState.Error(r.message)
            }
        }
    }

    fun loadReferrals() {
        viewModelScope.launch {
            _referrals.value = UiState.Loading
            when (val r = repo.referrals(_statusFilter.value)) {
                is ApiResult.Success -> _referrals.value = UiState.Success(r.data)
                is ApiResult.Failure -> _referrals.value = UiState.Error(r.message)
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
        loadReferrals()
    }

    fun saveSettings(
        enabled: Boolean,
        discountAmount: Double,
        rewardAmount: Double,
        minOrderValue: Double,
        maxDiscount: Double,
        rewardLimitPerUser: Int,
    ) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.updateReferralSettings(enabled, discountAmount, rewardAmount, minOrderValue, maxDiscount, rewardLimitPerUser)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; loadSettings() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun approve(id: String) = viewModelScope.launch {
        when (val r = repo.approveReferral(id)) {
            is ApiResult.Success -> loadReferrals()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun reject(id: String) = viewModelScope.launch {
        when (val r = repo.rejectReferral(id)) {
            is ApiResult.Success -> loadReferrals()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

@Composable
fun AdminReferralScreen(onMenuClick: () -> Unit, viewModel: ReferralViewModel = hiltViewModel()) {
    val settingsState by viewModel.settings.collectAsState()
    val referralsState by viewModel.referrals.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadSettings(); viewModel.loadReferrals() }
    var tab by remember { mutableIntStateOf(0) }

    AdminScreenScaffold(title = "Referrals", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Referrals") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Settings") })
            }
            when (tab) {
                0 -> {
                    val s = referralsState
                    EmptyOrList(
                        loading = s is UiState.Loading,
                        error = (s as? UiState.Error)?.message,
                        isEmpty = (s as? UiState.Success)?.data?.referrals.isNullOrEmpty() && s is UiState.Success,
                        emptyMessage = "No referrals yet",
                        onRetry = viewModel::loadReferrals,
                    ) {
                        val d = (s as UiState.Success).data
                        Column {
                            StatTileRow(
                                buildList {
                                    add("Total rewarded" to d.total_rewarded.money())
                                    d.counts.forEach { (k, v) -> add(k to v.toString()) }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            FilterChipsRow(
                                options = d.counts.keys.toList(),
                                selected = statusFilter,
                                onSelect = { viewModel.setStatusFilter(it) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            AdminLazyList {
                                items(d.referrals, key = { it.id }) { r ->
                                    CompactRow(
                                        title = r.referrer_name ?: r.referrer_id,
                                        subtitle = "referred ${r.referred_name ?: r.referred_id ?: "-"} · code ${r.code ?: "-"}",
                                        meta = "discount ${r.discount_amount.money()} · reward ${r.reward_amount.money()} · ${r.created_at ?: "-"}",
                                        badge = { StatusChip(r.status) },
                                        trailing = {
                                            if (r.status == "order_placed" || r.flags.isNotEmpty()) {
                                                Row {
                                                    TextButton(onClick = { viewModel.approve(r.id) }, enabled = actionState !is ActionState.InFlight) { Text("Approve") }
                                                    TextButton(onClick = { viewModel.reject(r.id) }, enabled = actionState !is ActionState.InFlight) { Text("Reject") }
                                                }
                                            }
                                        },
                                    ) {
                                        if (r.reason != null) Text("Reason: ${r.reason}", style = MaterialTheme.typography.labelSmall)
                                        if (r.flags.isNotEmpty()) Text("Flags: ${r.flags.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val s = settingsState
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::loadSettings) {
                        ReferralSettingsForm((s as UiState.Success).data, actionState is ActionState.InFlight, onSave = viewModel::saveSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralSettingsForm(
    settings: ReferralSettings,
    saving: Boolean,
    onSave: (Boolean, Double, Double, Double, Double, Int) -> Unit,
) {
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var discountAmount by remember(settings) { mutableStateOf(settings.discount_amount.toString()) }
    var rewardAmount by remember(settings) { mutableStateOf(settings.reward_amount.toString()) }
    var minOrderValue by remember(settings) { mutableStateOf(settings.min_order_value.toString()) }
    var maxDiscount by remember(settings) { mutableStateOf(settings.max_discount.toString()) }
    var rewardLimit by remember(settings) { mutableStateOf(settings.reward_limit_per_user.toString()) }

    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        LabeledSwitchRow("Referral program enabled", enabled, { enabled = it })
        Spacer(Modifier.height(8.dp))
        LsTextField(discountAmount, { discountAmount = it }, "Referred user discount (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(rewardAmount, { rewardAmount = it }, "Referrer reward (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(minOrderValue, { minOrderValue = it }, "Minimum order value (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(maxDiscount, { maxDiscount = it }, "Maximum discount (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(rewardLimit, { rewardLimit = it }, "Reward limit per user (0 = unlimited)", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        LsPrimaryButton(
            text = "Save referral settings",
            loading = saving,
            onClick = {
                onSave(
                    enabled,
                    discountAmount.toDoubleOrNull() ?: 0.0,
                    rewardAmount.toDoubleOrNull() ?: 0.0,
                    minOrderValue.toDoubleOrNull() ?: 0.0,
                    maxDiscount.toDoubleOrNull() ?: 0.0,
                    rewardLimit.toIntOrNull() ?: 0,
                )
            },
        )
    }
}
