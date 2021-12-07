package com.example.WeatherK

class ResponseWeatherCity
{
	lateinit var coord: Coord
	lateinit var weather: Array<Weather>
	var base: String? = null
	lateinit var main: Main
	var visibility: Int = 0
	lateinit var wind: Wind
	lateinit var rain: Rain
	lateinit var clouds: Clouds
	var dt: Int = 0
	lateinit var sys: Sys
	var timezone: Int = 0
	var id: Int = 0
	var name: String? = null
	var cod: Int = 0
} //-----------------------------------com.example.Clouds.java-----------------------------------

class Clouds
{
	var all: Int = 0
} //-----------------------------------com.example.Coord.java-----------------------------------

class Coord
{
	var lon: Float = 0f
	var lat: Float = 0f
} //-----------------------------------com.example.Main.java-----------------------------------

class Main
{
	var temp: Float = 0f
	var feels_like: Float = 0f
	var temp_min: Float = 0f
	var temp_max: Float = 0f
	var pressure: Float = 0f
	var humidity: Float = 0f
} //-----------------------------------com.example.Rain.java-----------------------------------

class Rain
{
	var _1h: Float = 0f
} //-----------------------------------com.example.Sys.java-----------------------------------

class Sys
{
	var type: Int = 0
	var id: Int = 0
	var country: String? = null
	var sunrise: Int = 0
	var sunset: Int = 0
} //-----------------------------------com.example.Weather.java-----------------------------------

class Weather
{
	var id: Int = 0
	var main: String? = null
	var description: String? = null
	var icon: String? = null
} //-----------------------------------com.example.Wind.java-----------------------------------

class Wind
{
	var speed: Float = 0f
	var deg: Float = 0f
}