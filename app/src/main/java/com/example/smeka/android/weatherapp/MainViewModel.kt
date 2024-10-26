package com.example.smeka.android.weatherapp

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smeka.android.weatherapp.service.WeatherRepository
import com.example.smeka.android.weatherapp.service.dto.CurrentWeather
import com.example.smeka.android.weatherapp.service.dto.FullWeather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    // Current weather based on location
    val currentLocationWeather: Flow<CurrentWeather> = repository.getCurrentWeather()

    // 5-day weather forecast based on location
    val forecast: Flow<List<FullWeather.Daily>> = repository.getFiveDayForecast()

    // StateFlow to hold the result of a city-based weather search
    private val _cityWeather = MutableStateFlow<CurrentWeather?>(null)
    val cityWeather: StateFlow<CurrentWeather?> = _cityWeather.asStateFlow()

    // Function to search weather by city name
    fun searchWeatherByCity(city: String) {
        viewModelScope.launch {
            repository.getCurrentWeatherByCity(city).collect { weather ->
                _cityWeather.value = weather
            }
        }
    }
}