package org.coffeetrain.onxproject

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.forecast_day_row_view.view.date_label
import kotlinx.android.synthetic.main.forecast_day_row_view.view.forecast_icon
import kotlinx.android.synthetic.main.forecast_day_row_view.view.pop_label
import kotlinx.android.synthetic.main.forecast_day_row_view.view.weather_label
import java.text.DateFormat

class ForecastDayRowView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
  fun set(item: CityForecastDay) {
    picasso.load(item.iconUrl)
        .into(forecast_icon)
    date_label.text = DateFormat.getDateInstance(DateFormat.SHORT).format(item.date)
    weather_label.text = item.conditions
    val floatPop = item.pop.toFloat()
    val formattedPop = String.format("%.0f%%", floatPop)
    pop_label.text = "$formattedPop chance of rain"
  }

}
