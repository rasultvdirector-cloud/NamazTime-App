package com.muslimtime.app.ui

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AppLocation
import com.muslimtime.app.data.ManualCityOption
import com.muslimtime.app.data.ManualCountryOption

internal fun locationSummaryText(context: Context, city: String, country: String, source: String): String =
    context.getString(
        R.string.settings_location_summary_template,
        city,
        country,
        prayerSourceOptions(context).firstOrNull { it.first == source }?.second
            ?: context.getString(R.string.settings_prayer_source_auto),
    )

internal fun applyLocationModeBadge(
    context: Context,
    badge: TextView,
    isAuto: Boolean,
) {
    val textRes = if (isAuto) R.string.settings_location_mode_auto_badge else R.string.settings_location_mode_manual_badge
    badge.text = context.getString(textRes)
    badge.setBackgroundResource(
        if (isAuto) R.drawable.bg_widget_status_active else R.drawable.bg_widget_status_neutral,
    )
    badge.setTextColor(
        ContextCompat.getColor(
            context,
            if (isAuto) R.color.white else R.color.text_primary,
        ),
    )
}

internal fun applyLocationDialogModeUi(
    context: Context,
    citySpinner: Spinner?,
    countrySpinner: Spinner?,
    useDetectedButton: Button?,
    helpText: TextView?,
    isAuto: Boolean,
) {
    citySpinner?.apply {
        isEnabled = !isAuto
        alpha = if (isAuto) 0.55f else 1f
    }
    countrySpinner?.apply {
        isEnabled = !isAuto
        alpha = if (isAuto) 0.55f else 1f
    }
    useDetectedButton?.visibility = if (isAuto) android.view.View.VISIBLE else android.view.View.GONE
    helpText?.text = context.getString(R.string.settings_auto_location_help)
}

internal fun bindCountrySpinner(
    context: Context,
    spinner: Spinner?,
    countryOptions: List<ManualCountryOption>,
) {
    val labels = countryOptions.map { it.label }
    spinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, labels)
}

internal fun bindCitySpinner(
    context: Context,
    spinner: Spinner?,
    cityOptions: List<ManualCityOption>,
    preselectedCity: ManualCityOption? = null,
): ManualCityOption? {
    val labels = cityOptions.map { it.label }
    spinner?.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, labels)
    val cityIndex = cityOptions.indexOfFirst { it.apiValue == preselectedCity?.apiValue }.takeIf { it >= 0 } ?: 0
    spinner?.setSelection(cityIndex, false)
    return cityOptions.getOrNull(cityIndex)
}

internal fun selectedManualLocation(
    countryOptions: List<ManualCountryOption>,
    cityOptions: List<ManualCityOption>,
    countrySpinner: Spinner?,
    citySpinner: Spinner?,
    fallbackLocation: AppLocation,
): Pair<ManualCountryOption, ManualCityOption>? {
    val country = countryOptions.getOrNull(countrySpinner?.selectedItemPosition ?: -1)
        ?: com.muslimtime.app.data.ManualLocationCatalog.findCountry(fallbackLocation.country)
    val city = cityOptions.getOrNull(citySpinner?.selectedItemPosition ?: -1)
        ?: country?.cities?.firstOrNull()
    return if (country != null && city != null) country to city else null
}

internal fun updateManualLocationPreview(
    countryOptions: List<ManualCountryOption>,
    cityOptions: List<ManualCityOption>,
    countrySpinner: Spinner?,
    citySpinner: Spinner?,
    updateSummary: (city: String, country: String) -> Unit,
) {
    val country = countryOptions.getOrNull(countrySpinner?.selectedItemPosition ?: -1) ?: return
    val city = cityOptions.getOrNull(citySpinner?.selectedItemPosition ?: -1) ?: return
    updateSummary(city.label, country.label)
}
