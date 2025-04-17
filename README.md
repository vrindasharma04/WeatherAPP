# 🌦️ Weather Forecast App (Android)

An Android application that shows the **5-day weather forecast** for a selected city using **Weather API** and **OkHttp** library for network calls.

## 📱 Features

- 📍 Shows current location weather *(optional based on permission)*
- 📆 Displays 5-day weather forecast
- 🌐 Fetches data from a weather API using **OkHttp**
- 📊 Beautiful UI built with **ConstraintLayout**
- 🔄 Real-time data parsing using JSON

## 🔧 Technologies Used

| Purpose               | Technology         |
|-----------------------|--------------------|
| Language              | Kotlin             |
| Layout                | ConstraintLayout   |
| Network Requests      | OkHttp             |
| JSON Parsing          | JSONObject / Gson  |
| UI Components         | RecyclerView, CardView |
| Location (optional)   | FusedLocationProviderClient (Google Play Services) |

## 📦 API Used

This app uses **[OpenWeatherMap API](https://openweathermap.org/api)** to get weather forecast data.

You can sign up and get a free API key to use with this app.

---

## 🧪 How It Works

1. User selects or app detects current city
2. App sends request using **OkHttp** to weather API
3. Receives a JSON response with weather data for the next 5 days
4. Parses the data and displays it on screen

---

## 🔒 Permissions

- `INTERNET` - To make API calls
- `ACCESS_FINE_LOCATION` *(optional)* - To fetch current location (if needed)

---

## 📸 Screenshots

| Home Screen | Weather Forecast |
|-------------|------------------|
| ![Home](screenshots/home.png) | ![Forecast](screenshots/forecast.png) |

---

## 💡 Future Enhancements

- Add dark mode support
- Use Retrofit instead of OkHttp for better scalability
- Allow city search feature
- Add icons and animations for weather conditions

---

## 👩‍💻 Developed By

**Your Name**  
BSc CS Student  
Email: your.email@example.com  
GitHub: [@yourusername](https://github.com/yourusername)

