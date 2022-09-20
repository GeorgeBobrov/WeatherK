@file:Suppress("UNUSED_PARAMETER")

package com.example.WeatherK

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.view.setPadding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.card_weather_hour.*

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

const val PERMISSION_REQUEST_Location = 0

class ActivityWeather : AppCompatActivity() {
	val baseURLRemote = "http://api.openweathermap.org/data/2.5/"
	val APIkey = "534e27824fc3e9e6b42bd9076d595c84"
	private lateinit var prefs: SharedPreferences
	lateinit var locationManager: LocationManager
	var handler = Handler()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_weather)
		locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

		prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
		selectCity.clearFocus()
		//hides keyboard on start
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//needs for scrolling to work
		textResponse.setMovementMethod(ScrollingMovementMethod())

		val pattern = dateFormatDateTime.toPattern()
		dateFormatDateTime.applyPattern(pattern + " (zzzz)")
	}

	val KtorClient = HttpClient(CIO) {
		install(JsonFeature)
	}

	fun buttonGetWeatherClick(sender: View?) {
		hideKeyboard()

		val city = selectCity.text.toString()
		queryWeather(city)
	}

	fun hideKeyboard() {
		selectCity.clearFocus()

		val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(layoutCity.windowToken, 0)
	}

	fun queryWeather(city: String?, lat: Float? = null, lon: Float? = null) = GlobalScope.async() {

		val statement: HttpStatement = KtorClient.get(baseURLRemote + "weather") {
			if (city != null)
				parameter("q", city)
			else {
				parameter("lat", lat.toString())
				parameter("lon", lon.toString())
			}

			parameter("lang", getResources().getString(R.string.current_locale))
			parameter("appid", APIkey)
		}

		statement.execute { response: HttpResponse ->
			try {
				val weatherCity: ResponseWeatherCity = response.receive()
				runOnUiThread {
					processWeatherCity(weatherCity)
					queryWeatherForecast(weatherCity.coord.lat, weatherCity.coord.lon)
					clearPanelForecast()

					updateDropdown(city, weatherCity.name)
				}
			} catch (cre: ClientRequestException) {
				val stringBody: String = cre.response.receive()
				runOnUiThread {
					textResponse.text = stringBody
					clearPanelForecast()
				}
			}
		}
	}


	private fun updateDropdown(city: String?, cityFromResponse: String?) {
		if (city != null) {
			// Correct city spelling in dropdown
			val correctedCity = if ((cityFromResponse != null) &&
				(cityFromResponse != city) &&
				(cityFromResponse.uppercase() == city.uppercase()))
				cityFromResponse
			else
				city

			for (cur in listCities)
				if ((cur != correctedCity) && (cur.uppercase() == correctedCity.uppercase())) {
					selectCity.setText("", false)
					adapterCities.remove(cur)
					adapterCities.add(correctedCity)
					selectCity.setText(correctedCity, false)
					return
				}

			// Add new city to dropdown
			if (!listCities.contains(correctedCity)) {
				// For correct adding new option need first clear input
				selectCity.setText("", false)
				adapterCities.add(correctedCity)
				selectCity.setText(correctedCity, false)
			}

		// If request was by GPS location, add city name from response
		} else if (cityFromResponse != null) {
			if (!listCities.contains(cityFromResponse))
				adapterCities.add(cityFromResponse)

			selectCity.setText(cityFromResponse, false)
		}
	}


	var g_weatherCity: ResponseWeatherCity? = null

	var dateFormatDateTime = SimpleDateFormat()
	fun processWeatherCity(weatherCity: ResponseWeatherCity) {
		g_weatherCity = weatherCity
		val weatherDescription = localizeWeatherDescription(weatherCity.weather[0].main)
		val time = Date(weatherCity.dt * 1000L)

//		val innerText = "Now, ${dateFormatDateTime.format(time)}, in ${weatherCity.name} is " +
//				"${normalizeTemp(weatherCity.main.temp)} °C, $weatherDescription"

		val innerText = String.format(resources.getString(R.string.weather_now),
			dateFormatDateTime.format(time), weatherCity.name,
			normalizeTemp(weatherCity.main.temp), weatherDescription)

		textResponse.text = innerText
	}

	fun localizeWeatherDescription(weatherDescription: String?): String? {
		val strId = resources.getIdentifier(weatherDescription, "string", packageName)
		if (strId != 0)
			return getString(strId)

		return weatherDescription
	}

	fun queryWeatherForecast(lat: Float, lon: Float) = GlobalScope.async() {

		val statement: HttpStatement = KtorClient.get(baseURLRemote + "onecall") {
			parameter("lat", lat.toString())
			parameter("lon", lon.toString())
			parameter("lang", resources.getString(R.string.current_locale))
			parameter("appid", APIkey)
		}

		statement.execute { response: HttpResponse ->
			try {
				val weatherForecast: ResponseWeatherForecast = response.receive()
				runOnUiThread { processWeatherForecast(weatherForecast, true) }
			} catch (cre: ClientRequestException) {
				val stringBody: String = cre.response.receive()
				runOnUiThread { textResponse.text = stringBody }
			}
		}

	}

	var forecastViews = mutableListOf<View>()

	var dateFormatOnlyDay = SimpleDateFormat("dd", Locale.getDefault())
	var g_weatherForecast: ResponseWeatherForecast? = null

	fun processWeatherForecast(weatherForecast: ResponseWeatherForecast, animate: Boolean = false) {
		g_weatherForecast = weatherForecast
		clearPanelForecast()

		val hourSunrise = Date(g_weatherCity?.sys!!.sunrise * 1000L).getHours()
		val hourSunset = Date(g_weatherCity?.sys!!.sunset * 1000L).getHours()
		var prevDayNum = 0

		setTimeZone(weatherForecast.timezone, true)

		for (hourForecast in weatherForecast.hourly) {
			val date = Date(hourForecast.dt * 1000L)
			val dayNum = dateFormatOnlyDay.format(date).toInt()
			if (dayNum != prevDayNum) {
				prevDayNum = dayNum

				val button = addDateLabel(date, animate)
				forecastViews.add(button)
			}
			val isNight = checkNight(hourSunrise, hourSunset, date.getHours())

			val layout = addHourForecastFragment(hourForecast, isNight, animate)
			forecastViews.add(layout)
		}

		if (animate)
			forecastViews.forEachIndexed { i, view ->
				handler.postDelayed({ view.visibility = View.VISIBLE }, 10L * i)
			}

	}

	fun clearPanelForecast() {
		for (view in forecastViews)
			panelForecastHourly.removeView(view)
		forecastViews.clear()
	}

	val dayColor = 0xFFFFE4C4.toInt() //bisque
	val darkColor = 0xFFB0B0B0.toInt()

	var dateFormatOnlyDate = SimpleDateFormat("dd MMMM", Locale.getDefault())
	fun addDateLabel(date: Date, hide: Boolean = false): Button {
		val button = Button(this)
		val info = "${dateFormatOnlyDate.format(date)}:"
		button.text = info
		button.isAllCaps = false
		button.backgroundTintList = ColorStateList.valueOf(darkColor)

		if (hide)
			button.visibility = View.GONE

		panelForecastHourly.addView(button)

		val px = TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_SP, 25f,
			button.context.resources.displayMetrics)
		button.layoutParams.height = px.toInt()
		button.setPadding(1)

		return button
	}


	var dateFormatOnlyTime = SimpleDateFormat("HH:mm", Locale.getDefault())
	fun addHourForecastFragment(hourForecast: Hourly, isNight: Boolean, hide: Boolean = false): FrameLayout {
		val weatherDescription = localizeWeatherDescription(hourForecast.weather[0].main)
		val time = Date(hourForecast.dt * 1000L)

		val frameLayout = FrameLayout(ContextThemeWrapper(this, R.style.Widget_AppCompat_Button_Small))
		frameLayout.id = View.generateViewId()
		panelForecastHourly.addView(frameLayout)

		if (!isNight)
			frameLayout.backgroundTintList = ColorStateList.valueOf(dayColor)

		if (hide)
			frameLayout.visibility = View.GONE

		val px = TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_SP, 40f,
			frameLayout.context.resources.displayMetrics)
		frameLayout.layoutParams.height = px.toInt()
		frameLayout.setPadding(0)

		val fragment = FragmentWeatherHour()

		supportFragmentManager.beginTransaction()
			.add(frameLayout.id, fragment)
			.commitNow()
		frameLayout.tag = fragment

		fragment.textTime.text = "${dateFormatOnlyTime.format(time)}:"
		fragment.textTemp.text = "${normalizeTemp(hourForecast.temp)} °C,"
		fragment.textWeather.text = weatherDescription

		val imgUrl = "http://openweathermap.org/img/w/${hourForecast.weather[0].icon}.png"
		downloadImageCached(fragment.image, imgUrl)

		return frameLayout
	}


	fun checkNight(hourSunrise: Int, hourSunset: Int, hourTest: Int): Boolean {
		if (hourSunset > hourSunrise)
			return ((hourTest < hourSunrise) || (hourTest > hourSunset))
		else
			return ((hourTest < hourSunrise) && (hourTest > hourSunset))
	}

	fun normalizeTemp(t: Float): Int {
		val T0 = 273.15f
		return (t - T0).roundToInt()
	}

//------------------------------------ TimeZone --------------------------------------

	fun radioTimeZoneClick(sender: View?) {
		if (g_weatherForecast != null)
			setTimeZone(g_weatherForecast!!.timezone, false)
	}

	fun setTimeZone(timeZoneStr: String?, skipProcessWeatherForecast: Boolean) {
		val timeZone = if (radioTimeZoneSelectedCity.isChecked) {
			TimeZone.getTimeZone(timeZoneStr)
		} else
			TimeZone.getDefault()

		dateFormatOnlyDay.timeZone = timeZone
		dateFormatOnlyDate.timeZone = timeZone
		dateFormatOnlyTime.timeZone = timeZone
		dateFormatDateTime.timeZone = timeZone

		if (g_weatherCity != null)
			processWeatherCity(g_weatherCity!!)
		if (g_weatherForecast != null && !skipProcessWeatherForecast)
			processWeatherForecast(g_weatherForecast!!)
	}

//------------------------------------  Saving settings --------------------------------------

	override fun onPause() {
		super.onPause()

		val editor = prefs.edit()
		editor.putString("city", selectCity.text.toString())

		val timeZone = if (radioTimeZoneSelectedCity.isChecked)
			"SelectedCity"
		else
			"Local"
		editor.putString("timeZone", timeZone)

		editor.putStringSet("cities", listCities.toSet())

		editor.putBoolean("autoGetWeather", checkBoxAutoGetWeather.isChecked)

		editor.apply()
	}

	var listCities = mutableListOf<String>()
	lateinit var adapterCities: ArrayAdapterNoFilter

	override fun onResume() {
		super.onResume()

		val city = prefs.getString("city", "")
		selectCity.setText(city)

		val timeZone = prefs.getString("timeZone", "Local")!!

		if (timeZone == "Local")
			radioTimeZoneLocal.isChecked = true
		if (timeZone == "SelectedCity")
			radioTimeZoneSelectedCity.isChecked = true

		val cities = prefs.getStringSet("cities", setOf())!!
		listCities = cities.filterNotNull().toMutableList()

		adapterCities = ArrayAdapterNoFilter(this, R.layout.support_simple_spinner_dropdown_item, listCities)
		selectCity.setAdapter(adapterCities)

		checkBoxAutoGetWeather.isChecked = prefs.getBoolean("autoGetWeather", false)

		if (checkBoxAutoGetWeather.isChecked)
			buttonGetWeatherClick(buttonGetWeather)
	}


	fun buttonDeleteCurCityFromListClick(sender: View?) {
		adapterCities.remove(selectCity.text.toString())
		selectCity.setText("")
	}
//------------------------------- Downloading images for buttons -------------------------------

	fun downloadImageCached(imageView: ImageView, url: String) {
		imageView.tag = url

		if (mapDrawables.containsKey(url)) {
			val drawable = mapDrawables.get(url)!!
			imageView.setImageDrawable(drawable)
		} else {
			if (listRequestedDrawables.contains(url)) return
			listRequestedDrawables.add(url)

			DownloadImageTask(url) { drawable: Drawable? ->
				listRequestedDrawables.remove(url)

				if (drawable != null) {
					mapDrawables.put(url, drawable)

					for (view in forecastViews)
						if (view is FrameLayout) {
							val image = (view.tag as FragmentWeatherHour).image
							if (image.tag == url)
								image.setImageDrawable(drawable)
						}

				}
			}.execute()
		}
	}


//------------------------------- Get weather by coordinates -------------------------------

	fun buttonGetLocationClick(sender: View?) {
		hideKeyboard()

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

			getLocation()
		} else
			ActivityCompat.requestPermissions(this,
				arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
				PERMISSION_REQUEST_Location)
	}


	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode == PERMISSION_REQUEST_Location) {
			if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getLocation()
			} else {
				// Permission request was denied.
			}
		}
	}

	fun getLocation() {
		// Permission was checked in onRequestPermissionsResult
		val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

		if (location != null)
			queryWeather(null, location.latitude.toFloat(), location.longitude.toFloat())

	}

	fun toggleButtonSettingsClick(sender: View?) {
		TransitionManager.beginDelayedTransition(linearLayoutMain)
		if (toggleButtonSettings.isChecked)
			layoutSettings.visibility = View.VISIBLE
		else
			layoutSettings.visibility = View.GONE
	}



}

private var mapDrawables: HashMap<String, Drawable> = HashMap()
private var listRequestedDrawables: MutableList<String> = mutableListOf<String>()
