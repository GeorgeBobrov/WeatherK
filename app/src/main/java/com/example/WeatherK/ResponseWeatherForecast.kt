package com.example.WeatherK

class ResponseWeatherForecast
{
	var lat: Float = 0f
	var lon: Float = 0f
	var timezone: String? = null
	var timezone_offset: Int = 0
	lateinit var current: Current
	lateinit var minutely: Array<Minutely>
	lateinit var hourly: Array<Hourly>
	lateinit var daily: Array<Daily>
}

class Current
{
	var dt: Int = 0
	var sunrise: Int = 0
	var sunset: Int = 0
	var temp: Float = 0f
	var feels_like: Float = 0f
	var pressure: Float = 0f
	var humidity: Float = 0f
	var dew_point: Float = 0f
	var uvi: Float = 0f
	var clouds: Float = 0f
	var visibility: Float = 0f
	var wind_speed: Float = 0f
	var wind_deg: Float = 0f
	lateinit var weather: Array<Weather>
}

class Daily
{
	var dt: Int = 0
	var sunrise: Int = 0
	var sunset: Int = 0
	var temp: Temp? = null
	var feels_like: Feels_like? = null
	var pressure: Float = 0f
	var humidity: Float = 0f
	var dew_point: Float = 0f
	var wind_speed: Float = 0f
	var wind_deg: Int = 0
	lateinit var weather: Array<Weather>
	var clouds: Int = 0
	var pop: Float = 0f
	var snow: Float = 0f
	var uvi: Float = 0f
	var rain: Float = 0f
}

class Feels_like
{
	var day: Float = 0f
	var night: Float = 0f
	var eve: Float = 0f
	var morn: Float = 0f
}

class Hourly
{
	var dt: Int = 0
	var temp: Float = 0f
	var feels_like: Float = 0f
	var pressure: Float = 0f
	var humidity: Float = 0f
	var dew_point: Float = 0f
	var uvi: Float = 0f
	var clouds: Float = 0f
	var visibility: Int = 0
	var wind_speed: Float = 0f
	var wind_deg: Float = 0f
	lateinit var weather: Array<Weather>
	var pop: Float = 0f
}

class Minutely
{
	var dt: Int = 0
	var precipitation: Float = 0f
}

class Temp
{
	var day: Float = 0f
	var min: Float = 0f
	var max: Float = 0f
	var night: Float = 0f
	var eve: Float = 0f
	var morn: Float = 0f
}