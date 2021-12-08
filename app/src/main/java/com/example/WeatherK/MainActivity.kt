package com.example.WeatherK

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.HttpResponse

import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
	val baseURLRemote = "http://api.openweathermap.org/data/2.5/"
	val APIkey = "534e27824fc3e9e6b42bd9076d595c84"
	val inputCity = "Kiev"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		editCity.clearFocus()
		//hides keyboard on start
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		//needs for scrolling to work
		textResponse.setMovementMethod(ScrollingMovementMethod())
	}

	val KtorClient = HttpClient(CIO){
		install(JsonFeature)
	}

	fun buttonQueryClick(sender: View?) {
		editCity.clearFocus()
		//hide keyboard
		val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(linearLayoutH.windowToken, 0)

		GlobalScope.async() {
			val statement: HttpStatement = KtorClient.get(baseURLRemote + "weather") {
				parameter("q", editCity.text.toString())
				parameter("appid", APIkey)
			}
			statement.execute { response: HttpResponse ->
				try {
					val weatherCity: ResponseWeatherCity = response.receive()
					runOnUiThread { processWeatherCity(weatherCity) }
				} catch (cre: ClientRequestException) {
					val stringBody: String = cre.response.receive()
					runOnUiThread { textResponse.text = stringBody }
				}
			}
		}

	}

	fun processWeatherCity(weatherCity: ResponseWeatherCity) {
		val weatherDescription = weatherCity.weather[0].main
		val time = Date(weatherCity.dt * 1000L)
		val innerText = "Now, ${time.toLocaleString()} in ${weatherCity.name} " +
				"is ${normalizeTemp(weatherCity.main.temp)} °C, $weatherDescription"

		textResponse.text = innerText
		//            textResponse.append("\n Humidity " + weatherCity.main.humidity);
		queryWeatherForecast(weatherCity)
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

	fun processWeatherForecast(weatherForecast: ResponseWeatherForecast) {
//            Date time = new Date(weatherCity.dt * 1000l);

		textResponse.append("\nHourly:")

		for (hourForecast in weatherForecast.hourly) {
			val innerText = addHourForecast(hourForecast)
			textResponse.append("\n $innerText")
		}
	}

	var sdf = SimpleDateFormat("HH:mm")
	fun addHourForecast(hourForecast: Hourly): String {
		val weatherDescription = hourForecast.weather[0].main
		val time = Date(hourForecast.dt * 1000L)
		return "${sdf.format(time)}: Temperature ${normalizeTemp(hourForecast.temp)} °C, $weatherDescription"
	}

	fun normalizeTemp(t: Float): Float {
		val T0 = 273.15f
		return Math.round(t - T0).toFloat()
	}


}
