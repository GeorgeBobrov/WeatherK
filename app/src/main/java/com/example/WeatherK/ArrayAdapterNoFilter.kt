package com.example.WeatherK

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.NonNull

class ArrayAdapterNoFilter : ArrayAdapter<Any?> {
	constructor(context: Context, resource: Int) : super(context, resource) {}
	constructor(context: Context, resource: Int, textViewResourceId: Int) :
		super(context, resource, textViewResourceId) {}

	constructor(context: Context, resource: Int, objects: Array<Any?>) :
		super(context, resource, objects) {}

	constructor(context: Context,resource: Int,textViewResourceId: Int, objects: Array<Any?>) :
	    super(context, resource, textViewResourceId, objects) {}

	constructor(context: Context, resource: Int, objects: List<*>) :
		super(context, resource, objects) {}

	constructor(context: Context, resource: Int, textViewResourceId: Int, objects: List<*>) :
		super(context, resource, textViewResourceId, objects) {}

	override fun getFilter(): Filter {
		return object : Filter() {
			override fun performFiltering(constraint: CharSequence?): FilterResults? {
				return null
			}

			override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
		}
	}
}