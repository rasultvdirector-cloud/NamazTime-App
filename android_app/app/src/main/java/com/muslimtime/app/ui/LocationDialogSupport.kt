package com.muslimtime.app.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AppLocation
import com.muslimtime.app.data.ManualCountryOption
import com.muslimtime.app.data.ManualLocationCatalog

internal object LocationDialogSupport {
    internal data class Bindings(
        val summaryView: TextView,
        val modeBadgeView: TextView,
        val citySpinner: Spinner,
        val countrySpinner: Spinner,
        val useDetectedButton: Button,
        val autoLocationSwitch: SwitchCompat,
        val autoLocationHelpText: TextView,
    )

    internal fun collectBindings(view: View): Bindings = Bindings(
        summaryView = view.findViewById(R.id.location_dialog_summary),
        modeBadgeView = view.findViewById(R.id.location_dialog_mode_badge),
        citySpinner = view.findViewById(R.id.settings_city_input),
        countrySpinner = view.findViewById(R.id.settings_country_input),
        useDetectedButton = view.findViewById(R.id.use_detected_button),
        autoLocationSwitch = view.findViewById(R.id.switch_auto_location),
        autoLocationHelpText = view.findViewById(R.id.auto_location_help_text),
    )

    internal fun bindInitialState(
        context: Context,
        bindings: Bindings,
        savedLocation: AppLocation,
        isAutoLocation: Boolean,
        countryOptions: List<ManualCountryOption>,
        bindCountrySpinner: (AppLocation) -> Unit,
        updateDialogLocationSummary: (String, String) -> Unit,
        updateLocationModeBadge: (Boolean) -> Unit,
        updateLocationDialogModeUi: (Boolean) -> Unit,
    ) {
        bindCountrySpinner(savedLocation)
        bindings.useDetectedButton.text = context.getString(R.string.settings_use_current_location)
        bindings.autoLocationSwitch.isChecked = isAutoLocation
        updateDialogLocationSummary(savedLocation.city, savedLocation.country)
        updateLocationModeBadge(isAutoLocation)
        updateLocationDialogModeUi(isAutoLocation)
    }

    internal fun attachManualSelectionListeners(
        bindings: Bindings,
        countryOptions: List<ManualCountryOption>,
        cityOptionsProvider: () -> List<com.muslimtime.app.data.ManualCityOption>,
        suggestedLocation: AppLocation,
        isSuppressed: () -> Boolean,
        setSuppressed: (Boolean) -> Unit,
        bindCitySpinner: (ManualCountryOption, com.muslimtime.app.data.ManualCityOption?) -> Unit,
        updateDialogLocationSummary: (String, String) -> Unit,
    ) {
        bindings.countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val country = countryOptions.getOrNull(position) ?: return
                if (isSuppressed()) return
                val currentCity = cityOptionsProvider()
                    .getOrNull(bindings.citySpinner.selectedItemPosition)
                    ?.takeIf { selected ->
                        country.cities.any { it.apiValue == selected.apiValue }
                    }
                setSuppressed(true)
                bindCitySpinner(country, currentCity ?: country.cities.firstOrNull())
                updateDialogLocationSummary(
                    (currentCity ?: country.cities.firstOrNull())?.label ?: suggestedLocation.city,
                    country.label,
                )
                setSuppressed(false)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        bindings.citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSuppressed()) return
                if (!bindings.autoLocationSwitch.isChecked) {
                    updateManualLocationPreview(
                        countryOptions = countryOptions,
                        cityOptions = cityOptionsProvider(),
                        countrySpinner = bindings.countrySpinner,
                        citySpinner = bindings.citySpinner,
                    ) { city, country ->
                        updateDialogLocationSummary(city, country)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    internal fun manualSelectionOrFallback(
        countryOptions: List<ManualCountryOption>,
        cityOptions: List<com.muslimtime.app.data.ManualCityOption>,
        bindings: Bindings,
        fallbackLocation: AppLocation,
    ): Pair<ManualCountryOption, com.muslimtime.app.data.ManualCityOption>? {
        return selectedManualLocation(
            countryOptions = countryOptions,
            cityOptions = cityOptions,
            countrySpinner = bindings.countrySpinner,
            citySpinner = bindings.citySpinner,
            fallbackLocation = fallbackLocation,
        )
    }

    internal fun clearFragmentDialogRefs(clear: () -> Unit) = clear()
}
