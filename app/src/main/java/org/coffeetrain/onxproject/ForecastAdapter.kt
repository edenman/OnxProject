package org.coffeetrain.onxproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import org.coffeetrain.onxproject.R.layout
import org.coffeetrain.onxproject.Weather.ForecastWrapper

class ForecastAdapter(context: Context) : BaseAdapter() {
  private val layoutInflater = LayoutInflater.from(context)
  private var wrapper: ForecastWrapper? = null

  fun set(wrapper: ForecastWrapper) {
    this.wrapper = wrapper
    notifyDataSetChanged()
  }

  override fun getCount(): Int {
    val wrapper = wrapper ?: return 0
    return wrapper.days.size + 1
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItem(position: Int): Any? {
    val wrapper = wrapper ?: return null
    if (position == 0) {
      return wrapper.city
    }
    return wrapper.days[position - 1]
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) 0 else 1
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val item = getItem(position)
    if (item is CityForecast) {
      val cityRow = convertView as? CityRowView
          ?: layoutInflater.inflate(
              layout.city_row_view, parent, false) as CityRowView
      cityRow.set(item)
      return cityRow
    } else if (item is CityForecastDay) {
      val dayRow = convertView as? ForecastDayRowView
          ?: layoutInflater.inflate(layout.forecast_day_row_view, parent, false) as ForecastDayRowView
      dayRow.set(item)
      return dayRow
    } else {
      throw IllegalStateException("Got invalid item $item")
    }
  }
}
