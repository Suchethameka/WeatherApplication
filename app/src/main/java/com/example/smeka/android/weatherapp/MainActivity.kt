package com.example.smeka.android.weatherapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smeka.android.weatherapp.service.dto.CurrentWeather
import com.example.smeka.android.weatherapp.service.dto.FullWeather
import com.example.smeka.android.weatherapp.ui.theme.CloudyBlue
import com.example.smeka.android.weatherapp.ui.theme.RainyGrey
import com.example.smeka.android.weatherapp.ui.theme.SunnyGreen
import com.example.smeka.android.weatherapp.ui.theme.WeatherAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

@AndroidEntryPoint
@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()  // Inject the ViewModel here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppTheme {
                val permission = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

                PermissionRequired(
                    permissionState = permission,
                    permissionNotGrantedContent = {
                        LocationPermissionDetails(onContinueClick = permission::launchPermissionRequest)
                    },
                    permissionNotAvailableContent = {
                        LocationPermissionNotAvailable(onContinueClick = permission::launchPermissionRequest)
                    }
                ) {
                    MainScreen(viewModel)  // Pass the ViewModel as a parameter
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {  // Accept MainViewModel as a parameter
    val locationWeather by viewModel.currentLocationWeather.collectAsState(null)
    val cityWeather by viewModel.cityWeather.collectAsState(null)
    val forecast by viewModel.forecast.collectAsState(emptyList())

    // City search text
    var citySearch by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(locationWeather?.backgroundColour() ?: Color.White)
            .padding(16.dp)
    ) {
        // Search bar for city input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            BasicTextField(
                value = citySearch,
                onValueChange = { citySearch = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.small)
                    .padding(16.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.Black)
            )
            IconButton(onClick = {
                if (citySearch.isNotEmpty()) {
                    coroutineScope.launch {
                        viewModel.searchWeatherByCity(citySearch)
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray
                )
            }
        }

        // Display weather summary (either city or location-based)
        (cityWeather ?: locationWeather)?.let { weather ->
            rememberSystemUiController().setStatusBarColor(weather.backgroundColour())
            WeatherSummary(weather = weather)
            TemperatureSummary(weather)
            Divider(color = Color.White)
        }

        // 5-day forecast
        FiveDayForecast(forecast)
    }
}

@Composable
fun WeatherSummary(weather: CurrentWeather) {
    Box {
        Image(
            painter = painterResource(id = weather.background()),
            contentDescription = "Background",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Column(
            Modifier
                .padding(top = 48.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = formatTemperature(weather.main.temp), fontSize = 48.sp, color = Color.White)
            Text(text = weather.weather.first().main, fontSize = 28.sp, color = Color.White)
            Text(text = weather.name, fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun TemperatureSummary(weather: CurrentWeather) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTemperature(weather.main.tempMin),
                fontSize = 18.sp,
                color = Color.White
            )
            Text(text = stringResource(R.string.min_temperature), color = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTemperature(weather.main.temp),
                fontSize = 18.sp,
                color = Color.White
            )
            Text(text = stringResource(R.string.now_temperature), color = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTemperature(weather.main.tempMax),
                fontSize = 18.sp,
                color = Color.White
            )
            Text(text = stringResource(R.string.max_temperature), color = Color.White)
        }
    }
}

@Composable
fun FiveDayForecast(forecast: List<FullWeather.Daily>) {
    LazyColumn {
        items(forecast) { dayForecast ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            ) {
                Text(
                    text = SimpleDateFormat("EEEE").format(Date(dayForecast.dt * 1_000)),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Image(
                    painter = painterResource(dayForecast.forecastIcon()),
                    contentDescription = "Forecast icon",
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
                Text(
                    text = formatTemperature(dayForecast.temp.day),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun formatTemperature(temperature: Double): String {
    return stringResource(R.string.temperature_degrees, temperature.roundToInt())
}

@DrawableRes
private fun CurrentWeather.background(): Int {
    val conditions = weather.first().main
    return when {
        conditions.contains("cloud", ignoreCase = true) -> R.drawable.forest_cloudy
        conditions.contains("rain", ignoreCase = true) -> R.drawable.forest_rainy
        else -> R.drawable.forest_sunny
    }
}

private fun CurrentWeather.backgroundColour(): Color {
    val conditions = weather.first().main
    return when {
        conditions.contains("cloud", ignoreCase = true) -> CloudyBlue
        conditions.contains("rain", ignoreCase = true) -> RainyGrey
        else -> SunnyGreen
    }
}

private fun FullWeather.Daily.forecastIcon(): Int {
    val conditions = weather.first().main
    return when {
        conditions.contains("cloud", ignoreCase = true) -> R.drawable.partlysunny
        conditions.contains("rain", ignoreCase = true) -> R.drawable.rain
        else -> R.drawable.clear
    }
}
