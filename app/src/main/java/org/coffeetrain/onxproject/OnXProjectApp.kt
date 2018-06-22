package org.coffeetrain.onxproject

import android.app.Application
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Database
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.Insert
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import com.facebook.stetho.Stetho
import com.squareup.moshi.Moshi
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import rx.Observable
import rx.schedulers.Schedulers
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.Date

// In a real app this would likely be injected via Dagger/etc.
lateinit var weather: Weather
lateinit var picasso: Picasso


fun buildWeather(app: OnXProjectApp): Weather {
  val moshi = Moshi.Builder()
      .build()

  val loggingInterceptor = HttpLoggingInterceptor()
  loggingInterceptor.level = BODY
  val client = OkHttpClient.Builder()
      .addInterceptor(loggingInterceptor)
      .build()
  val retrofit = Retrofit.Builder()
      .baseUrl("http://api.wunderground.com/api/a8ed47370bda6da6/")
      .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .client(client)
      .build()
  val weatherUnderground = retrofit.create(WeatherUndergroundAPI::class.java)

  val database = Room.databaseBuilder(app, OnXDatabase::class.java, "onx-database")
      .build()

  return Weather(app, weatherUnderground, database.forecastDao())
}

class OnXProjectApp : Application() {
  override fun onCreate() {
    super.onCreate()
    weather = buildWeather(this)
    picasso = Picasso.Builder(this)
        .build()
    Picasso.setSingletonInstance(picasso)
    Timber.plant(DebugTree())
    Stetho.initializeWithDefaults(this)

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
      Timber.e(e, "Uncaught exception")
    }
  }
}

interface WeatherUndergroundAPI {
  @GET("forecast10day/q/{state}/{city}.json")
  fun getTenDayForecast(
      @Path("state") state: String,
      @Path("city") city: String): Observable<ForecastResponse>
}

// Weather Underground API stuff

data class ForecastResponse(val forecast: Forecast)

data class Forecast(val simpleforecast: SimpleForecast)

data class SimpleForecast(val forecastday: List<ForecastDay>)

data class ForecastDay(val date: DateWrapper, val conditions: String, val icon_url: String,
    val pop: Int)

data class DateWrapper(val epoch: Long)

// Database stuff

@Entity
data class CityForecast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "city") val city: String)

@Entity(foreignKeys = [ForeignKey(entity = CityForecast::class,
    parentColumns = ["id"],
    childColumns = ["cityId"],
    onDelete = CASCADE)])
data class CityForecastDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cityId") val cityId: Long,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "conditions") val conditions: String,
    @ColumnInfo(name = "icon_url") val iconUrl: String,
    @ColumnInfo(name = "pop") val pop: Int)

@Dao
interface ForecastDao {
  @Query("SELECT * FROM CityForecast WHERE city = :city AND state = :state")
  fun findForecastForCity(city: String, state: String): CityForecast?

  @Query("SELECT * FROM CityForecastDay WHERE cityId = :cityId")
  fun loadForecastDaysForCity(cityId: Long): List<CityForecastDay>

  @Insert
  fun insertCity(forecast: CityForecast): Long

  @Insert
  fun insertForecastDays(forecastDay: List<CityForecastDay>)
}

@Database(entities = [CityForecast::class, CityForecastDay::class], version = 1)
@TypeConverters(DateTypeConverter::class)
abstract class OnXDatabase : RoomDatabase() {
  abstract fun forecastDao(): ForecastDao
}

class DateTypeConverter {
  @TypeConverter
  fun toDate(value: Long?): Date? {
    return if (value == null) null else Date(value)
  }

  @TypeConverter
  fun toLong(value: Date?): Long? {
    return value?.time
  }
}
