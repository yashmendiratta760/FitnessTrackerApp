
package com.yash.fitnesstracker.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yash.fitnesstracker.MainApplication
import com.yash.fitnesstracker.database.ActivityDTO
import com.yash.fitnesstracker.database.ActivityHistoryEntities
import com.yash.fitnesstracker.database.StepsDTO
import com.yash.fitnesstracker.database.StepsEntities
import com.yash.fitnesstracker.repository.ActivityHistoryRepo
import com.yash.fitnesstracker.repository.OnlineServerDbRep
import com.yash.fitnesstracker.repository.ServerDbRepo
import com.yash.fitnesstracker.repository.StepRepository
import com.yash.fitnesstracker.repository.StepsLocalDbRepository
import com.yash.fitnesstracker.service.AutoWorkManager
import com.yash.fitnesstracker.service.DataStoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
class AppViewModel(
    private val stepsLocalDbRepository: StepsLocalDbRepository,
    private val onlineServerDbRep: ServerDbRepo,
    private val activityHistoryRepo: ActivityHistoryRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1
    )
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private fun emitError(message: String) {
        _errorEvent.tryEmit(message)
    }

    init {
        viewModelScope.launch {
            try {
                val formattedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val existingEntity = stepsLocalDbRepository.getStepsByDate(formattedDate)
                if (existingEntity != null && existingEntity.steps > 0) {
                    _uiState.value = _uiState.value.copy(
                        steps = existingEntity.steps
                    )
                    if (StepRepository.stepCount.value < existingEntity.steps) {
                        StepRepository.updateSteps(existingEntity.steps)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error loading initial steps from Room", e)
            }
        }

        viewModelScope.launch {
            try {
                StepRepository.stepCount.collectLatest { steps ->
                    if (steps > 0 || _uiState.value.steps == 0) {
                        _uiState.value = _uiState.value.copy(
                            steps = steps
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error collecting step count", e)
                emitError("Unable to read steps")
            }
        }
    }

    fun getAllStepsFromLocalDb() {
        viewModelScope.launch {
            try {
                val allStepData = stepsLocalDbRepository.getAllData()
                val steps = mutableListOf<StepsDTO>()

                allStepData?.forEach { data ->
                    try {
                        val formattedDate =
                            data.date.format(DateTimeFormatter.ISO_DATE)

                        steps.add(
                            StepsDTO(
                                data.steps.toString(),
                                formattedDate
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "Error formatting local step data", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    stepsData = steps
                )
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error getting steps from local DB", e)
                emitError("Unable to load steps")
            }
        }
    }

    fun getAllSteps() {
        Log.d("ServerFetchSteps", "Start")

        viewModelScope.launch {
            try {
                val response = onlineServerDbRep.getAllData()

                if (response.isSuccessful) {
                    val stepsData = response.body() ?: emptyList()

                    Log.d("getAllSteps", "Fetched: ${stepsData.size} entries")

                    stepsData.forEach { serverData ->
                        try {
                            val localStep =
                                stepsLocalDbRepository.getStepsByDate(serverData.date)

                            if (localStep == null) {
                                val formattedDate =
                                    serverData.date.format(DateTimeFormatter.ISO_DATE)

                                insertOrUpdateInRoom(
                                    serverData.steps.toInt(),
                                    formattedDate
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("getAllSteps", "Error processing server step", e)
                        }
                    }
                } else {
                    Log.e(
                        "getAllSteps",
                        "API failed: ${response.code()} ${response.message()}"
                    )
                    emitError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("getAllSteps", "Server request failed", e)
                emitError("Unable to connect to server")
            }
        }
    }

    fun syncStepsOncePerDay() {
        viewModelScope.launch {
            try {
                val lastSyncDate = DataStoreManager.getLastSyncDate()

                if (!DataStoreManager.isToday(lastSyncDate)) {
                    getAllSteps()

                    val today =
                        LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                    DataStoreManager.setLastSyncDate(today)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error syncing steps", e)
                emitError("Step sync failed")
            }
        }
    }

    suspend fun insertOrUpdateInRoom(
        steps: Int,
        formattedDate: String
    ) {
        try {
            val existingEntity =
                stepsLocalDbRepository.getStepsByDate(formattedDate)

            if (existingEntity != null) {
                stepsLocalDbRepository.update(
                    existingEntity.copy(steps = steps)
                )
            } else {
                stepsLocalDbRepository.insert(
                    StepsEntities(
                        steps = steps,
                        date = formattedDate
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Error inserting/updating Room", e)
            emitError("Unable to save steps")
        }
    }

    /**
     * Schedule background uploads when a network connection is available.
     * WorkManager may execute later than 15 minutes.
     *
     * AutoWorkManager must upload pending local data to the backend.
     */
    fun scheduleStepUpload(context: android.content.Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest =
                PeriodicWorkRequestBuilder<AutoWorkManager>(
                    15,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    "stepUpload",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        } catch (e: Exception) {
            Log.e("AppViewModel", "Error scheduling step upload", e)
            emitError("Unable to schedule step upload")
        }
    }

    fun getActivityHistory() {
        viewModelScope.launch {
            try {
                val activity = activityHistoryRepo.getData()

                _uiState.value = _uiState.value.copy(
                    activityHistory = activity
                )
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error getting activity history", e)
                emitError("Unable to load activity history")
            }
        }
    }

    fun addActivity(activityDTO: ActivityDTO) {
        viewModelScope.launch {
            try {
                val entity = ActivityHistoryEntities(
                    name = activityDTO.name,
                    time = activityDTO.time,
                    cal = activityDTO.cal
                )

                activityHistoryRepo.insert(entity)
                delay(500L)
                getActivityHistory()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error adding activity", e)
                emitError("Unable to add activity")
            }
        }
    }

    fun uploadImage(
        image: MultipartBody.Part,
        userName: RequestBody
    ) {
        viewModelScope.launch {
            try {
                val response = onlineServerDbRep.uploadAndGetImage(
                    image,
                    userName
                )

                if (response.isSuccessful) {
                    val imageUrl = response.body()?.imageUrl

                    _uiState.value = _uiState.value.copy(
                        imageUrl = imageUrl
                    )

                    Log.d("UPLOAD", "Image uploaded: $imageUrl")
                } else {
                    Log.e("UPLOAD", "Upload failed: ${response.code()}")
                    emitError("Image upload failed")
                }
            } catch (e: Exception) {
                Log.e("UPLOAD", "Upload exception", e)
                emitError("Unable to upload image")
            }
        }
    }

    fun getImageUrl() {
        viewModelScope.launch {
            try {
                val response = onlineServerDbRep.getImageUrl()

                if (response.code() == 200) {
                    val imageUrl = response.body()

                    _uiState.value = _uiState.value.copy(
                        imageUrl = imageUrl
                    )

                    Log.d("imageUrl", imageUrl.toString())
                } else {
                    Log.d("imageUrl", "Not found")
                    emitError("Image not found")
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error getting image URL", e)
                emitError("Unable to load image")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MainApplication

                AppViewModel(
                    application.container.stepsLocalDbRepository,
                    application.container.onlineServerDbRep,
                    application.container.activityHistoryRepo
                )
            }
        }
    }
}
