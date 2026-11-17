package com.noor.app.ui.prayer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.prefs.AppSettings
import com.noor.app.data.prefs.DeviceLocation
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.UnavailableState
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.prayerIcon
import com.noor.app.ui.prayerNameRes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.core.calendar.HijriMonths
import com.noor.core.location.Cities
import com.noor.core.prayer.CalculationMethod
import com.noor.core.prayer.Prayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class LocateStatus { IDLE, LOCATING, SUCCESS, FAILED }

class PrayerViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _locate = MutableStateFlow(LocateStatus.IDLE)
    val locate: StateFlow<LocateStatus> = _locate.asStateFlow()

    val prayer get() = container.prayer
    fun hijri(adjust: Int) = container.hijri(adjust)

    fun useDeviceLocation() {
        viewModelScope.launch {
            _locate.value = LocateStatus.LOCATING
            val loc = container.location.currentLocation()
            if (loc == null) {
                _locate.value = LocateStatus.FAILED
            } else {
                container.settings.setDeviceLocation(DeviceLocation(loc.latitude, loc.longitude, ZoneId.systemDefault().id))
                _locate.value = LocateStatus.SUCCESS
            }
        }
    }

    fun selectCity(id: String) {
        viewModelScope.launch { container.settings.selectCity(id) }
    }

    fun resetLocate() {
        _locate.value = LocateStatus.IDLE
    }

    fun locationPermissionDenied() {
        _locate.value = LocateStatus.FAILED
    }
}

@Composable
fun rememberLocationRequest(vm: PrayerViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.useDeviceLocation() else vm.locationPermissionDenied()
    }
    return { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
}

@Composable
fun PrayerTimesScreen(nav: NoorNavigator) {
    val vm = noorViewModel { PrayerViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val locate by vm.locate.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    val requestLocation = rememberLocationRequest(vm)
    var dayOffset by rememberSaveable { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_prayer), onBack = nav::back)
        val s = settings ?: return@Column
        val location = vm.prayer.location(s)
        if (location == null) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UnavailableState(
                    icon = Icons.Outlined.LocationOn,
                    title = stringResource(R.string.prayer_no_location_title),
                    message = stringResource(R.string.prayer_no_location_msg),
                    actionLabel = stringResource(R.string.prayer_choose_city),
                    onAction = { nav.open(Routes.CITY) },
                )
                LocateButton(locate, requestLocation)
            }
            return@Column
        }

        val date = LocalDate.now(location.zone).plusDays(dayOffset)
        val day = vm.prayer.day(s, date)
        val snapshot = vm.prayer.snapshot(s)
        val hijri = vm.hijri(s.hijriAdjust).toHijri(date)

        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                HeroCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, tint = palette.heroAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(location.name(arabic), style = MaterialTheme.typography.titleMedium, color = palette.onHero, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            R.string.prayer_method_label,
                            stringResource(if (s.calculationMethod == CalculationMethod.JAFARI) R.string.method_jafari else R.string.method_tehran),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onHero.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { nav.open(Routes.CITY) }) { Text(stringResource(R.string.prayer_change_city)) }
                        LocateButton(locate, requestLocation, compact = true)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { dayOffset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.action_previous_day)) }
                    AnimatedContent(
                        targetState = date,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.weight(1f),
                        label = "date",
                    ) { d ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Formatters.gregorianLong(d, arabic), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${Formatters.number(hijri.day, arabic)} ${HijriMonths.name(hijri.month, arabic)} ${Formatters.number(hijri.year, arabic)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accent,
                            )
                        }
                    }
                    IconButton(onClick = { dayOffset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.action_next_day)) }
                }
                if (dayOffset != 0L) {
                    TextButton(onClick = { dayOffset = 0 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_today)) }
                }
            }
            val times = day?.times.orEmpty()
            items(Prayer.displayOrder) { p ->
                val time = times[p]
                val next = snapshot?.next
                val isNext = next != null && next.prayer == p && next.time.toLocalDate() == date
                val bg by animateColorAsState(if (isNext) palette.accent.copy(alpha = 0.14f) else Color.Transparent, label = "bg")
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(
                        Modifier
                            .background(bg)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(prayerIcon(p), null, tint = if (isNext) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(prayerNameRes(p)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (p.isPrayer) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (p.isPrayer) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isNext) {
                                Text(stringResource(R.string.home_next_prayer), style = MaterialTheme.typography.labelSmall, color = palette.accent)
                            }
                        }
                        Text(
                            time?.let { Formatters.time(it, arabic) } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNext) palette.accent else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.prayer_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LocateButton(status: LocateStatus, onRequest: () -> Unit, compact: Boolean = false) {
    Column {
        OutlinedButton(onClick = onRequest, enabled = status != LocateStatus.LOCATING) {
            if (status == LocateStatus.LOCATING) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.prayer_use_location))
        }
        if (status == LocateStatus.FAILED && !compact) {
            Text(stringResource(R.string.prayer_location_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun CityPickerScreen(nav: NoorNavigator) {
    val vm = noorViewModel { PrayerViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val locate by vm.locate.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val requestLocation = rememberLocationRequest(vm)

    // Return automatically once a device location has been obtained.
    LaunchedEffect(locate) {
        if (locate == LocateStatus.SUCCESS) {
            vm.resetLocate()
            nav.back()
        }
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.city_title), onBack = nav::back)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            placeholder = { Text(stringResource(R.string.city_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear)) }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = palette.accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        )
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth(), onClick = requestLocation, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.MyLocation, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.prayer_use_location), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(
                                    when (locate) {
                                        LocateStatus.LOCATING -> R.string.city_locating
                                        LocateStatus.FAILED -> R.string.prayer_location_failed
                                        LocateStatus.IDLE, LocateStatus.SUCCESS -> if (settings?.deviceLocation != null) R.string.city_using_device else R.string.city_location_optional
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (locate == LocateStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (locate == LocateStatus.LOCATING) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else if (settings?.deviceLocation != null) Icon(Icons.Outlined.Check, null, tint = palette.accent)
                    }
                }
            }
            val cities = Cities.all.filter { it.matches(query) }
            items(cities, key = { it.id }) { city ->
                val selected = settings?.cityId == city.id
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    onClick = {
                        vm.selectCity(city.id)
                        nav.back()
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(city.name(arabic), style = MaterialTheme.typography.titleSmall, color = if (selected) palette.accent else MaterialTheme.colorScheme.onSurface)
                            Text(city.country(arabic), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) Icon(Icons.Outlined.Check, null, tint = palette.accent)
                    }
                }
            }
        }
    }
}
