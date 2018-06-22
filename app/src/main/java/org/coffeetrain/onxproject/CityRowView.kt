package org.coffeetrain.onxproject

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.city_row_view.view.close_button
import kotlinx.android.synthetic.main.city_row_view.view.title

class CityRowView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
  fun set(item: CityForecast) {
    title.text = "${item.city}, ${item.state}"
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    close_button.setOnClickListener {
      weather.dismissForecast()
    }
  }
}
