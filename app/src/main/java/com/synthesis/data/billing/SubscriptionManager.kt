package com.synthesis.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class SubscriptionPlan(val id: String, val title: String, val priceFormatted: String, val period: String) {
    NONE("none", "Free Trial", "0.00 €", "60 minute"),
    MONTHLY(PlayBillingService.SKU_MONTHLY, "Abonament Lunar", "9.99 €", "pe lună"),
    YEARLY(PlayBillingService.SKU_YEARLY, "Abonament Anual (Recomandat)", "75.00 €", "pe an (-37% reducere)")
}

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val playBillingService: PlayBillingService
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("synthesis_billing_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val TRIAL_DURATION_MS = 60 * 60 * 1000L // Exact 1 hour (60 minutes)
        private const val KEY_FIRST_LAUNCH = "key_first_launch_time"
        private const val KEY_ACCUMULATED_USAGE = "key_accumulated_usage_ms"
        private const val KEY_LAST_TICK = "key_last_tick_time"
        private const val KEY_IS_PRO = "key_is_pro_subscriber"
        private const val KEY_ACTIVE_PLAN = "key_active_plan_id"
        private const val KEY_ORDER_ID = "key_active_order_id"
    }

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _activePlan = MutableStateFlow(
        SubscriptionPlan.values().find { it.id == prefs.getString(KEY_ACTIVE_PLAN, SubscriptionPlan.NONE.id) }
            ?: SubscriptionPlan.NONE
    )
    val activePlan: StateFlow<SubscriptionPlan> = _activePlan.asStateFlow()

    private val _remainingTrialSeconds = MutableStateFlow(calculateRemainingSeconds())
    val remainingTrialSeconds: StateFlow<Long> = _remainingTrialSeconds.asStateFlow()

    private val _isLocked = MutableStateFlow(checkIfLocked())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        ensureFirstLaunchInitialized()
        tickUsage()
        observeExternalPurchases()
    }

    private fun observeExternalPurchases() {
        scope.launch {
            playBillingService.activePurchase.collect { purchase ->
                if (purchase != null) {
                    val matchingPlan = SubscriptionPlan.values().find { it.id == purchase.productId }
                        ?: SubscriptionPlan.YEARLY
                    activatePro(matchingPlan, purchase.orderId)
                }
            }
        }
    }

    private fun ensureFirstLaunchInitialized() {
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_FIRST_LAUNCH, now)
                .putLong(KEY_LAST_TICK, now)
                .putLong(KEY_ACCUMULATED_USAGE, 0L)
                .apply()
        }
    }

    fun tickUsage() {
        if (_isPro.value) {
            _isLocked.value = false
            return
        }

        val now = System.currentTimeMillis()
        val lastTick = prefs.getLong(KEY_LAST_TICK, now)
        val delta = (now - lastTick).coerceAtLeast(0L).coerceAtMost(300_000L) // Cap runaway sleep deltas
        val currentAccumulated = prefs.getLong(KEY_ACCUMULATED_USAGE, 0L) + delta

        prefs.edit()
            .putLong(KEY_ACCUMULATED_USAGE, currentAccumulated)
            .putLong(KEY_LAST_TICK, now)
            .apply()

        val remaining = calculateRemainingSeconds()
        _remainingTrialSeconds.value = remaining
        _isLocked.value = remaining <= 0L
    }

    private fun calculateRemainingSeconds(): Long {
        if (prefs.getBoolean(KEY_IS_PRO, false)) return Long.MAX_VALUE
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
        val accumulated = prefs.getLong(KEY_ACCUMULATED_USAGE, 0L)
        val elapsedWallClock = (System.currentTimeMillis() - firstLaunch).coerceAtLeast(0L)

        // Strict limit: whichever comes first (60 minutes active usage OR 60 minutes wall clock)
        val effectiveUsed = maxOf(accumulated, elapsedWallClock)
        val remainingMs = (TRIAL_DURATION_MS - effectiveUsed).coerceAtLeast(0L)
        return remainingMs / 1000L
    }

    private fun checkIfLocked(): Boolean {
        if (_isPro.value) return false
        return calculateRemainingSeconds() <= 0L
    }

    fun purchaseSubscription(
        plan: SubscriptionPlan,
        activity: Activity? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ): Boolean {
        var success = false

        val handleResult: (PurchaseVerificationResult) -> Unit = { result ->
            when (result) {
                is PurchaseVerificationResult.Success -> {
                    activatePro(plan, result.orderId)
                    success = true
                    onComplete?.invoke(true, "Abonament activat cu succes! Comanda: ${result.orderId}")
                }
                is PurchaseVerificationResult.Pending -> {
                    onComplete?.invoke(false, "Tranzacție în așteptare.")
                }
                is PurchaseVerificationResult.Failed -> {
                    onComplete?.invoke(false, result.error)
                }
            }
        }

        if (activity != null) {
            playBillingService.launchBillingFlow(activity, plan.id, handleResult)
        } else {
            playBillingService.launchPurchaseFlow(plan.id, handleResult)
        }

        return success
    }

    private fun activatePro(plan: SubscriptionPlan, orderId: String) {
        prefs.edit()
            .putBoolean(KEY_IS_PRO, true)
            .putString(KEY_ACTIVE_PLAN, plan.id)
            .putString(KEY_ORDER_ID, orderId)
            .apply()
        _isPro.value = true
        _activePlan.value = plan
        _isLocked.value = false
        _remainingTrialSeconds.value = Long.MAX_VALUE
    }

    fun restorePurchases(onComplete: ((Boolean, String?) -> Unit)? = null): Boolean {
        // First check local preferences
        val isSubscribedLocally = prefs.getBoolean(KEY_IS_PRO, false)
        if (isSubscribedLocally) {
            _isPro.value = true
            _isLocked.value = false
            onComplete?.invoke(true, "Abonament restaurat din cache local.")
            return true
        }

        // Query official Google Play Billing Store
        playBillingService.queryExistingPurchases { activePurchases ->
            if (activePurchases.isNotEmpty()) {
                val first = activePurchases.first()
                val plan = SubscriptionPlan.values().find { it.id in first.products } ?: SubscriptionPlan.YEARLY
                activatePro(plan, first.orderId ?: "GPA.RESTORED")
                onComplete?.invoke(true, "Abonament Google Play restaurat cu succes!")
            } else {
                onComplete?.invoke(false, "Nu s-au găsit achiziții active pe acest cont Google Play.")
            }
        }
        return false
    }

    // Diagnostic & Demo Controls for testing
    fun forceExpireTrialForTesting() {
        prefs.edit()
            .putBoolean(KEY_IS_PRO, false)
            .putLong(KEY_ACCUMULATED_USAGE, TRIAL_DURATION_MS + 1000L)
            .putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis() - TRIAL_DURATION_MS - 5000L)
            .apply()
        _isPro.value = false
        _remainingTrialSeconds.value = 0L
        _isLocked.value = true
    }

    fun resetTrialForTesting() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_IS_PRO, false)
            .putLong(KEY_FIRST_LAUNCH, now)
            .putLong(KEY_LAST_TICK, now)
            .putLong(KEY_ACCUMULATED_USAGE, 0L)
            .putString(KEY_ACTIVE_PLAN, SubscriptionPlan.NONE.id)
            .apply()
        _isPro.value = false
        _activePlan.value = SubscriptionPlan.NONE
        _remainingTrialSeconds.value = TRIAL_DURATION_MS / 1000L
        _isLocked.value = false
    }
}
