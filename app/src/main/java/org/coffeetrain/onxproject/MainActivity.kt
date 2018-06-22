package org.coffeetrain.onxproject

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.forecast_list
import kotlinx.android.synthetic.main.activity_main.weather_map
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

class MainActivity : AppCompatActivity() {
  private val subscriptions = CompositeSubscription()
  private lateinit var adapter: ForecastAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    weather_map.onCreate(savedInstanceState)
    Timber.d("getMapAsync")
    weather_map.getMapAsync { map ->
      Timber.d("getMapAsync got a map and stuff")
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.692994, -111.034739), 14f))
      map.setOnMapClickListener { location ->
        weather.onMapTapped(location)
      }
    }
    adapter = ForecastAdapter(this)
    forecast_list.adapter = adapter
  }

  override fun onResume() {
    super.onResume()
    weather_map.onResume()
    subscriptions.add(weather.observeForecastToDisplay()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { toDisplay ->
          if (toDisplay == null) {
            if (forecast_list.measuredHeight == 0) {
              forecast_list.onMeasured {
                Timber.d("Hiding list, setting to ${forecast_list.measuredHeight}")
                forecast_list.translationY = forecast_list.measuredHeight.toFloat()
              }
            } else {
              Timber.d("Hiding list, animating to ${forecast_list.measuredHeight}")
              forecast_list.animate().translationY(forecast_list.measuredHeight.toFloat())
            }
          } else {
            Timber.d("Animating to 0, populating adapter")
            forecast_list.animate().translationY(0f)
            adapter.set(toDisplay)
          }
        })
  }

  override fun onStart() {
    super.onStart()
    weather_map.onStart()
  }

  override fun onPause() {
    super.onPause()
    weather_map.onPause()
  }

  override fun onStop() {
    super.onStop()
    weather_map.onStop()
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    super.onSaveInstanceState(outState)
    weather_map.onSaveInstanceState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    weather_map.onDestroy()
  }

  override fun onLowMemory() {
    super.onLowMemory()
    weather_map.onLowMemory()
  }
}

inline fun View.onMeasured(crossinline function: () -> Unit) {
  if (measuredHeight != 0 && measuredWidth != 0) {
    function()
    return
  }
  check(viewTreeObserver.isAlive, { "Tried to attach a viewTreeObserver but the observer is dead" })
  viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
    override fun onGlobalLayout() {
      if (measuredHeight != 0 && measuredWidth != 0) {
        viewTreeObserver.removeOnGlobalLayoutListener(this)
        function()
      }
    }
  })
}
