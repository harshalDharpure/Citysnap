package com.prod.singles_date.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prod.singles_date.R
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.ui.components.CityPicker
import com.prod.singles_date.ui.components.LocalityPicker

private enum class OnboardingStep { City, Locality }

@Composable
fun CitySelectScreen(
    isBusy: Boolean,
    onContinue: (cityId: String, localityId: String) -> Unit,
) {
    var step by remember { mutableIntStateOf(OnboardingStep.City.ordinal) }
    var selectedCity by remember { mutableStateOf(AppCity.BANGALORE) }
    var selectedLocality by remember { mutableStateOf("") }

    val localities = AppLocality.localitiesForCity(selectedCity)
    val requiresLocality = localities.isNotEmpty()
    val appName = stringResource(R.string.app_name)
    val enterLabel = stringResource(R.string.onboarding_enter)

    LaunchedEffect(selectedCity) {
        selectedLocality = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = appName,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (OnboardingStep.entries[step]) {
                OnboardingStep.City -> {
                    Text(
                        text = "Where are you?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_city_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CityPicker(
                        selectedCity = selectedCity,
                        onCitySelected = { selectedCity = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OnboardingStep.Locality -> {
                    Text(
                        text = "Pick your area",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your feed will feel more local.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    LocalityPicker(
                        cityId = selectedCity,
                        selectedLocality = selectedLocality,
                        onLocalitySelected = { selectedLocality = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val buttonLabel = when (OnboardingStep.entries[step]) {
                OnboardingStep.City -> if (requiresLocality) "Continue" else enterLabel
                OnboardingStep.Locality -> enterLabel
            }

            Button(
                onClick = {
                    when (OnboardingStep.entries[step]) {
                        OnboardingStep.City -> {
                            if (requiresLocality) {
                                step = OnboardingStep.Locality.ordinal
                            } else {
                                onContinue(selectedCity, selectedLocality)
                            }
                        }
                        OnboardingStep.Locality -> {
                            onContinue(selectedCity, selectedLocality)
                        }
                    }
                },
                enabled = when (OnboardingStep.entries[step]) {
                    OnboardingStep.City -> AppCity.isValid(selectedCity) && !isBusy
                    OnboardingStep.Locality -> selectedLocality.isNotBlank() && !isBusy
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(if (isBusy) "Saving…" else buttonLabel)
            }

            if (step > OnboardingStep.City.ordinal) {
                TextButton(onClick = { step -= 1 }) {
                    Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
