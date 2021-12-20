package com.example.WeatherK

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
	val baseURLRemote = "http://api.openweathermap.org/data/2.5/"
	val APIkey = "534e27824fc3e9e6b42bd9076d595c84"
	private lateinit var prefs: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
		selectCity.clearFocus()
		//hides keyboard on start
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//needs for scrolling to work
		textResponse.setMovementMethod(ScrollingMovementMethod())

		val pattern = dateFormatDateTime.toPattern()
		dateFormatDateTime.applyPattern(pattern + " (zzzz)")
	}

	val KtorClient = HttpClient(CIO){
		install(JsonFeature)
	}

	fun buttonQueryClick(sender: View?) {
		selectCity.clearFocus()
		//hide keyboard
		val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(linearLayoutH.windowToken, 0)

		GlobalScope.async() {
			val city = selectCity.text.toString()
			val statement: HttpStatement = KtorClient.get(baseURLRemote + "weather") {
				parameter("q", city)
				parameter("appid", APIkey)
			}
			statement.execute { response: HttpResponse ->
				try {
					val weatherCity: ResponseWeatherCity = response.receive()
					runOnUiThread {
						processWeatherCity(weatherCity)
						queryWeatherForecast(weatherCity)
						clearPanelForecast()
						if (!listCities.contains(city))
							adapterCities.add(city)
					}
				} catch (cre: ClientRequestException) {
					val stringBody: String = cre.response.receive()
					runOnUiThread { textResponse.text = stringBody }
				}
			}
		}

	}

	var g_weatherCity: ResponseWeatherCity? = null

	var dateFormatDateTime = SimpleDateFormat()
	fun processWeatherCity(weatherCity: ResponseWeatherCity) {
		g_weatherCity = weatherCity
		val weatherDescription = weatherCity.weather[0].main
		val time = Date(weatherCity.dt * 1000L)

		val innerText = "Now, ${dateFormatDateTime.format(time)}, in ${weatherCity.name} is " +
				"${normalizeTemp(weatherCity.main.temp)} °C, $weatherDescription"

		textResponse.text = innerText
	}

	fun queryWeatherForecast(a_weatherCity: ResponseWeatherCity) {

		GlobalScope.async() {
			val statement: HttpStatement = KtorClient.get(baseURLRemote + "onecall") {
				parameter("lat", a_weatherCity.coord.lat.toString())
				parameter("lon", a_weatherCity.coord.lon.toString())
				parameter("appid", APIkey)
			}

			statement.execute { response: HttpResponse ->
				try {
					val weatherForecast: ResponseWeatherForecast = response.receive()
					runOnUiThread { processWeatherForecast(weatherForecast) }
				} catch (cre: ClientRequestException) {
					val stringBody: String = cre.response.receive()
					runOnUiThread { textResponse.text = stringBody }
				}
			}
		}

	}

	var buttons = mutableListOf<Button>()
	var dateFormatOnlyDay = SimpleDateFormat("dd", Locale.getDefault())
	var g_weatherForecast: ResponseWeatherForecast? = null

	fun processWeatherForecast(weatherForecast: ResponseWeatherForecast) {
		g_weatherForecast = weatherForecast
		clearPanelForecast()

		val hourSunrise = Date(g_weatherCity?.sys!!.sunrise * 1000L).getHours()
		val hourSunset = Date(g_weatherCity?.sys!!.sunset * 1000L).getHours()
		var prevDayNum = 0

		setupTimeZone(weatherForecast.timezone)

		for (hourForecast in weatherForecast.hourly) {
			val date = Date(hourForecast.dt * 1000L)
			val dayNum = dateFormatOnlyDay.format(date).toInt()
			if (dayNum != prevDayNum) {
				prevDayNum = dayNum

				val button = addDateLabel(date)
				buttons.add(button)
			}
			val isNight = checkNight(hourSunrise, hourSunset, date.getHours())

			val button = addHourForecast(hourForecast, isNight)
			buttons.add(button)
		}
	}

	fun clearPanelForecast() {
		for (button in buttons)
			panelForecastHourly.removeView(button)
		buttons.clear()
	}

	val dayColor = 0xFFFFE4C4.toInt() //bisque
	val darkColor = 0xFFB0B0B0.toInt()

	var dateFormatOnlyDate = SimpleDateFormat("dd MMMM", Locale.getDefault())
	fun addDateLabel(date: Date): Button {
		val button = Button(this)
		val info = "${dateFormatOnlyDate.format(date)}:"
		button.text = info
		button.isAllCaps = false
		button.backgroundTintList = ColorStateList.valueOf(darkColor)

		panelForecastHourly.addView(button)
		val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25f,
			button.context.resources.displayMetrics);
		button.layoutParams.height = px.toInt()
		button.setPadding(1)

		return button
	}

	var dateFormatOnlyTime = SimpleDateFormat("HH:mm", Locale.getDefault())
	fun addHourForecast(hourForecast: Hourly, isNight: Boolean): Button {
		val weatherDescription = hourForecast.weather[0].main
		val time = Date(hourForecast.dt * 1000L)

		val button = Button(this)
		val info = "${dateFormatOnlyTime.format(time)}: \t ${normalizeTemp(hourForecast.temp)} °C, \t $weatherDescription"
		button.text = info
		button.isAllCaps = false
		if (!isNight)
			button.backgroundTintList = ColorStateList.valueOf(dayColor)

		panelForecastHourly.addView(button)
		val imgUrl = "http://openweathermap.org/img/w/${hourForecast.weather[0].icon}.png"
		button.tag = imgUrl

		downloadImageCached(button, imgUrl)

		return button
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
		if (g_weatherCity == null) return
		if (g_weatherForecast == null) return

		processWeatherForecast(g_weatherForecast!!) //setupTimeZone here
		processWeatherCity(g_weatherCity!!) //show date with new TimeZone setted up
	}

	fun setupTimeZone(timeZoneStr: String?) {
		val timeZone = if (radioTimeZoneSelectedCity.isChecked) {
			TimeZone.getTimeZone(timeZoneStr)
		} else
			TimeZone.getDefault()

		dateFormatOnlyDay.timeZone = timeZone
		dateFormatOnlyDate.timeZone = timeZone
		dateFormatOnlyTime.timeZone = timeZone
		dateFormatDateTime.timeZone = timeZone
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

		editor.apply()
	}

	var listCities = mutableListOf<String>()
	lateinit var adapterCities: ArrayAdapter<String>

	override fun onResume() {
		super.onResume()

		if (prefs.contains("city")) {
			val city = prefs.getString("city", "")!!
			selectCity.setText(city)
		}

		val timeZone = prefs.getString("timeZone", "Local")!!

		if (timeZone == "Local")
			radioTimeZoneLocal.isChecked = true
		if (timeZone == "SelectedCity")
			radioTimeZoneSelectedCity.isChecked = true

		val cities = prefs.getStringSet("cities", setOf())!!
		listCities = cities.toMutableList()

		adapterCities = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, listCities)
		selectCity.setAdapter(adapterCities)
	}

//------------------------------- Downloading images for buttons -------------------------------

	fun downloadImageCached(button: Button, url: String) {

		if (mapDrawables.containsKey(url)) {
			val drawable = mapDrawables.get(url)!!
			addImageToButton(button, drawable)
		} else {
			if (listRequestedDrawables.contains(url)) return
			listRequestedDrawables.add(url)

			DownloadImageTask(url) { drawable: Drawable? ->
				listRequestedDrawables.remove(url)

				if (drawable != null) {
					mapDrawables.put(url, drawable);

					for (ibutton in buttons)
						if (ibutton.tag == url)
							addImageToButton(ibutton, drawable)
				}
			}.execute()
		}
	}

	fun addImageToButton(button: Button, drawable: Drawable) {
		drawable.setBounds(0, 0, 100, 100)
		button.setCompoundDrawables(null, null, drawable, null)
		val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f,
			button.context.resources.displayMetrics);
		button.layoutParams.height = px.toInt()
	}

}

private var mapDrawables: HashMap<String, Drawable> = HashMap()
private var listRequestedDrawables: MutableList<String> = mutableListOf<String>()
