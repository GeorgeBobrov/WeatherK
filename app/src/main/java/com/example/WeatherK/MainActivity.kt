package com.example.WeatherK

import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
		val innerText = "Now, ${time.toLocaleString()}, in ${weatherCity.name} is" +
				"\n${normalizeTemp(weatherCity.main.temp)} °C, $weatherDescription"

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

		textResponse.append("\nForecast:")

		for (hourForecast in weatherForecast.hourly) {
			val button = addHourForecast(hourForecast)
		}
	}

	var sdf = SimpleDateFormat("HH:mm")
	fun addHourForecast(hourForecast: Hourly): Button {
		val weatherDescription = hourForecast.weather[0].main
		val time = Date(hourForecast.dt * 1000L)

		val button = Button(this)
		val info = "${sdf.format(time)}: \t ${normalizeTemp(hourForecast.temp)} °C, \t $weatherDescription"
		button.text = info
		button.isAllCaps = false

		PanelForecastHourly.addView(button)
		val imgUrl = "http://openweathermap.org/img/w/${hourForecast.weather[0].icon}.png"
		DownloadImageTask(button).execute(imgUrl);

		return button
	}

	fun normalizeTemp(t: Float): Int {
		val T0 = 273.15f
		return (t - T0).roundToInt()
	}


}

private class DownloadImageTask(val button: Button) :
	AsyncTask<String?, Void?, Drawable?>()
{
	override fun doInBackground(vararg urls: String?): Drawable?
	{
		val url = urls[0]
		return try
		{
			val strm = URL(url).getContent() as InputStream
			Drawable.createFromStream(strm, "src name")
		}
		catch (e: java.lang.Exception)
		{
			Log.e("Error", e.message)
			null
		}
	}

	override fun onPostExecute(result: Drawable?)
	{
		if (result == null) return
		result.setBounds(0, 0,  100,  100)
		button.setCompoundDrawables(null, null, result, null)
		val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f, button.context.getResources().getDisplayMetrics());
		button.layoutParams.height = px.toInt()
	}

}
