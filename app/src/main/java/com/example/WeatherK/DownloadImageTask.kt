package com.example.WeatherK

import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Log
import java.io.InputStream
import java.net.URL


class DownloadImageTask(val url: String, val onFinish: (drawable: Drawable?) -> Unit) :
	AsyncTask<String?, Void?, Drawable?>() {

	override fun doInBackground(vararg params: String?): Drawable?	{
		return try {
			val strm = URL(url).getContent() as InputStream
			Drawable.createFromStream(strm, "src name")
		}
		catch (e: java.lang.Exception) {
			Log.e("Error", e.message ?: "Null")
			null
		}
	}

	override fun onPostExecute(drawable: Drawable?)	{
		onFinish(drawable);
	}
}