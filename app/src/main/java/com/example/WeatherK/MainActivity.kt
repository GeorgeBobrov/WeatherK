package com.example.WeatherK

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.activity_main.*
import com.example.WeatherK.R
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

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

	private val client = OkHttpClient()

	fun buttonQueryClick(sender: View?) {
		editCity.clearFocus()
		//hide keyboard
		val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(linearLayoutH.windowToken, 0)

		val urlBuilder = (baseURLRemote + "weather").toHttpUrl().newBuilder()
		urlBuilder.addQueryParameter("q", editCity.text.toString())
		urlBuilder.addQueryParameter("appid", APIkey)
		val url = urlBuilder.build().toString()

		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				call.cancel()
			}

			override fun onResponse(call: Call, response: Response) {
				val myResponse = response.body!!.string()
				if (response.code == 200)
					runOnUiThread { processWeatherCity(myResponse) }
				else
					runOnUiThread { textResponse.text = myResponse }
			}
		})
	}

	fun processWeatherCity(weather: String?) {
		val gson = Gson()
		try {
			val weatherCity = gson.fromJson(weather, ResponseWeatherCity::class.java)
			val weatherDescription = weatherCity.weather[0].main
			val time = Date(weatherCity.dt * 1000L)
			val innerText = "Now, ${time.toLocaleString()} in ${weatherCity.name} " +
				"is ${normalizeTemp(weatherCity.main.temp)} °C, $weatherDescription"

			textResponse.text = innerText
			//            textResponse.append("\n Humidity " + weatherCity.main.humidity);
			queryWeatherForecast(weatherCity)
		} catch (e: JsonSyntaxException) {
			Log.d("JsonSyntaxException", e.localizedMessage)
		}
	}

	fun queryWeatherForecast(a_weatherCity: ResponseWeatherCity) {
		val urlBuilder = (baseURLRemote + "onecall").toHttpUrl().newBuilder()
		urlBuilder.addQueryParameter("lat", a_weatherCity.coord.lat.toString())
		urlBuilder.addQueryParameter("lon", a_weatherCity.coord.lon.toString())
		urlBuilder.addQueryParameter("appid", APIkey)
		val url = urlBuilder.build().toString()

		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				call.cancel()
			}

			@Throws(IOException::class)
			override fun onResponse(call: Call, response: Response) {
				val myResponse = response.body!!.string()
				if (response.code == 200)
					runOnUiThread { processWeatherForecast(myResponse) }
				else
					runOnUiThread { textResponse.text = myResponse }
			}
		})
	}

	fun processWeatherForecast(weather: String?) {
		val gson = Gson()
		try {
			val weatherForecast = gson.fromJson(weather, ResponseWeatherForecast::class.java)

//            Date time = new Date(weatherCity.dt * 1000l);

			textResponse.append("\nHourly:")

			for (hourForecast in weatherForecast.hourly) {
				val innerText = addHourForecast(hourForecast)
				textResponse.append("\n $innerText")
			}
		} catch (e: JsonSyntaxException) {
			Log.d("JsonSyntaxException", e.localizedMessage)
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
