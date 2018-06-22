package org.coffeetrain.onxproject

import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.google.android.gms.maps.model.LatLng
import com.jakewharton.rxrelay.BehaviorRelay
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.Date

class Weather(val app: OnXProjectApp,
    val weatherUnderground: WeatherUndergroundAPI,
    val dao: ForecastDao) {
  private val displayForecastRelay = BehaviorRelay.create(null as ForecastWrapper?)

  fun observeForecastToDisplay(): Observable<ForecastWrapper?> = displayForecastRelay

  fun dismissForecast() {
    displayForecastRelay.call(null)
  }

  fun onMapTapped(location: LatLng) {
    toast("Geocoding ${location.latitude},${location.longitude}")
    val doLookup = doLookup@{
      return@doLookup Geocoder(app).getFromLocation(location.latitude, location.longitude, 1).firstOrNull()
    }
    Observable.fromCallable(doLookup)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { result ->
          toast("Geocoded result ${result?.locality}")
          if (result != null) {
            gotGeocodedResult(result)
          }
        }
  }

  private fun gotGeocodedResult(result: Address) {
    val doLookup = loadFromDb@{
      val city =
          dao.findForecastForCity(city = result.locality, state = result.adminArea)
      if (city != null) {
        val days = dao.loadForecastDaysForCity(city.id)
        return@loadFromDb ForecastWrapper(city, days)
      }
      return@loadFromDb null
    }
    Observable.fromCallable(doLookup)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wrapper ->
          if (wrapper == null) {
            fetchForecast(result)
          } else {
            displayForecastRelay.call(wrapper)
            toast("City is already in the database")
          }
        }
  }

  private fun fetchForecast(result: Address) {
    val state = result.adminArea
    val city = result.locality
    weatherUnderground.getTenDayForecast(state, city)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { response ->
          insertResponse(city, state, response)
        }
  }

  private fun insertResponse(city: String, state: String, response: ForecastResponse) {
    toast("Got weather underground forecast, inserting city and ${response.forecast.simpleforecast.forecastday.size} days")
    val doInsert = doInsert@{
      val cityForecast = CityForecast(state = state, city = city)
      val insertedCityId = dao.insertCity(cityForecast)
      val forecastDays = response.forecast.simpleforecast.forecastday.map { forecastDay ->
        CityForecastDay(
            cityId = insertedCityId,
            date = Date(forecastDay.date.epoch * 1000),
            conditions = forecastDay.conditions,
            iconUrl = forecastDay.icon_url,
            pop = forecastDay.pop)
      }
      dao.insertForecastDays(forecastDays)
      return@doInsert ForecastWrapper(cityForecast, forecastDays)
    }
    Observable.fromCallable(doInsert)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { wrapper ->
          displayForecastRelay.call(wrapper)
        }
  }

  private fun toast(message: String) {
    Timber.d("ERICZ $message")
//    Toast.makeText(app, message, LENGTH_SHORT).show()
  }

  data class ForecastWrapper(val city: CityForecast, val days: List<CityForecastDay>)
}
