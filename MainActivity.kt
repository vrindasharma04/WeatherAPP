package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var searchInput: AutoCompleteTextView
    private lateinit var address: TextView
    private lateinit var temperatureText: TextView
    private lateinit var status: TextView
    private lateinit var updateTime: TextView
    private lateinit var windSpeed: TextView
    private lateinit var humidityText: TextView
    private lateinit var precautionsText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var forecastContainer: LinearLayout

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val weatherApiKey = "7cd56384527c0a5600574d92385a4e09"
    private val geoapifyApiKey = "70866b29132e4d13aa1d9ffb848ca45e"
    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.searchInput)
        address = findViewById(R.id.address)
        temperatureText = findViewById(R.id.temperatureText)
        status = findViewById(R.id.status)
        updateTime = findViewById(R.id.updateTime)
        windSpeed = findViewById(R.id.windSpeed)
        humidityText = findViewById(R.id.humidityText)
        precautionsText = findViewById(R.id.precautionsText)
        weatherIcon = findViewById(R.id.weatherIcon)
        mainLayout = findViewById(R.id.mainLayout)
        forecastContainer = findViewById(R.id.forecastLayout)

        searchInput.threshold = 1

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                query?.let { if (it.length > 2) fetchSuggestions(it.toString()) }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchInput.setOnItemClickListener { _, _, position, _ ->
            val selectedOption = searchInput.adapter.getItem(position).toString()
            if (selectedOption.startsWith("📍")) {
                checkLocationPermissionAndFetch()
            } else {
                fetchWeather(selectedOption)
            }
        }


        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val city = searchInput.text.toString().trim()
                if (city.isNotEmpty()) fetchWeather(city)
                true
            } else false
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissionAndFetch()
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            fetchWeather("Delhi")
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        val city = addresses?.firstOrNull()?.locality ?: "Delhi"
                        fetchWeather(city)
                    } ?: run {
                        fetchWeather("Delhi")
                    }
                }
                .addOnFailureListener {
                    fetchWeather("Delhi")
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            fetchWeather("Delhi")
        }
    }

    private fun fetchSuggestions(query: String) {
        if (query.isEmpty()) {
            // Get current location
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request permissions if not granted
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                return
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val reverseUrl = "https://api.geoapify.com/v1/geocode/reverse?lat=$lat&lon=$lon&apiKey=$geoapifyApiKey"

                    val reverseRequest = Request.Builder().url(reverseUrl).build()
                    client.newCall(reverseRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("Location", "Reverse geocoding error: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val jsonString = response.body?.string() ?: return
                            try {
                                val json = JSONObject(jsonString)
                                val features = json.getJSONArray("features")
                                if (features.length() > 0) {
                                    val props = features.getJSONObject(0).getJSONObject("properties")
                                    val city = props.optString("formatted", "")
                                    runOnUiThread {
                                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, listOf(city))
                                        searchInput.setAdapter(adapter)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("Location", "Parsing error: ${e.message}")
                            }
                        }
                    })
                } else {
                    Log.e("Location", "Location is null")
                }
            }

            return
        }

        // Normal autocomplete flow
        val url = "https://api.geoapify.com/v1/geocode/autocomplete?text=$query&limit=5&apiKey=$geoapifyApiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Autocomplete", "Suggestion error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string() ?: return
                val suggestions = mutableListOf<String>()
                try {
                    val json = JSONObject(jsonString)
                    val results = json.getJSONArray("features")
                    for (i in 0 until results.length()) {
                        val props = results.getJSONObject(i).getJSONObject("properties")
                        val city = props.optString("formatted", "")
                        if (city.isNotEmpty()) suggestions.add(city)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, suggestions)
                        searchInput.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("Autocomplete", "Parsing error: ${e.message}")
                }
            }
        })
    }

    private fun fetchWeather(city: String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$weatherApiKey"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Weather", "Fetch error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: return
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData)
                        val weatherArray = json.getJSONArray("weather")
                        val weather = weatherArray.getJSONObject(0)
                        val main = json.getJSONObject("main")
                        val wind = json.getJSONObject("wind")

                        val cityName = json.getString("name")
                        val temp = main.getDouble("temp")
                        val weatherDesc = weather.getString("main")
                        val iconCode = weather.getString("icon")
                        val humidity = main.getInt("humidity")
                        val windSpeedVal = wind.getDouble("speed")
                        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                        address.text = cityName
                        temperatureText.text = "$temp°C"
                        status.text = weatherDesc
                        updateTime.text = "Updated: $time"
                        humidityText.text = "Humidity: $humidity%"
                        windSpeed.text = "Wind: $windSpeedVal m/s"
                        setPrecautions(weatherDesc)

                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                        Glide.with(this@MainActivity).load(iconUrl).into(weatherIcon)

                        // 🌙 Check if it's night
                        val timezoneOffset = json.getLong("timezone") // in seconds
                        val currentUtc = System.currentTimeMillis() / 1000L // current UTC time in seconds
                        val localTime = currentUtc + timezoneOffset

                        val localHour = (localTime % 86400) / 3600 // extract hour (0-23)

                        val isNight = localHour < 6 || localHour >= 18


                        // 🎨 Pick background based on temperature and time
                        val bgResId = when {
                            temp >= 35 -> if (isNight) R.drawable.night else R.drawable.bg_hot
                            temp in 25.0..34.9 -> if (isNight) R.drawable.pl_night else R.drawable.bg_warm
                            temp in 15.0..24.9 -> if (isNight) R.drawable.pl_night else R.drawable.bg_cool
                            temp in 5.0..14.9 -> if (isNight) R.drawable.cool_night else R.drawable.bg_cold
                            else -> if (isNight) R.drawable.cold_night else R.drawable.bg_freezing
                        }
                        mainLayout.setBackgroundResource(bgResId)

                        fetchForecast(city)

                    } catch (e: Exception) {
                        Log.e("Weather", "Parsing error: ${e.message}")
                    }
                }
            }
        })
    }


    private fun setPrecautions(condition: String) {
        val advice = when (condition.lowercase(Locale.ROOT)) {
            "clear" -> "Wear sunglasses and stay hydrated."
            "clouds" -> "It might get gloomy, carry a light jacket."
            "rain" -> "Carry an umbrella and wear waterproof shoes."
            "snow" -> "Bundle up, roads may be slippery!"
            "thunderstorm" -> "Stay indoors if possible!"
            "mist", "fog" -> "Drive carefully, low visibility."
            else -> "Have a nice day!"
        }
        precautionsText.text = "Precaution: $advice"
    }

    private fun fetchForecast(city: String) {
        val url = "https://api.openweathermap.org/data/2.5/forecast?q=$city&units=metric&cnt=40&appid=$weatherApiKey"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Forecast", "Fetch error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: return)
                val list = json.getJSONArray("list")
                val forecasts = mutableMapOf<String, JSONObject>()

                for (i in 0 until list.length()) {
                    val forecast = list.getJSONObject(i)
                    val dt_txt = forecast.getString("dt_txt")
                    if (dt_txt.contains("12:00:00")) {
                        val date = dt_txt.split(" ")[0]
                        forecasts[date] = forecast
                    }
                }

                runOnUiThread {
                    forecastContainer.removeAllViews()
                    val inflater = layoutInflater

                    for ((date, forecast) in forecasts) {
                        val view =
                            inflater.inflate(R.layout.forecast_item, forecastContainer, false)
                        val temp = forecast.getJSONObject("main").getDouble("temp")
                        val weather = forecast.getJSONArray("weather").getJSONObject(0)
                        val icon = weather.getString("icon")
                        val desc = weather.getString("main")

                        view.findViewById<TextView>(R.id.forecastDate).text = date
                        view.findViewById<TextView>(R.id.forecastTemp).text = "$temp°C"
                        view.findViewById<TextView>(R.id.forecastStatus).text = desc

                        val iconUrl = "https://openweathermap.org/img/wn/$icon@2x.png"
                        Glide.with(this@MainActivity).load(iconUrl)
                            .into(view.findViewById(R.id.forecastIcon))

                        forecastContainer.addView(view)
                    }
                }
            }
        })
    }
}
