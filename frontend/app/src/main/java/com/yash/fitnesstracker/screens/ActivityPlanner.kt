package com.yash.fitnesstracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yash.fitnesstracker.api.ExerciseWorkout
import com.yash.fitnesstracker.api.PlannedExercise
import com.yash.fitnesstracker.api.WorkoutSession
import com.yash.fitnesstracker.api.WorkoutSet
import com.yash.fitnesstracker.screens.components.CustomisedTopBar
import com.yash.fitnesstracker.utils.Bg
import com.yash.fitnesstracker.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private val plannerDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun todayDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun newId(): String = UUID.randomUUID().toString()

private fun displayDate(date: String): String = try {
    SimpleDateFormat("dd MMM yyyy", Locale.US)
        .format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!)
} catch (_: Exception) {
    date
}

private enum class ExerciseMetric {
    WEIGHT, REPS, VOLUME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPlannerScreen(
    navController: NavHostController,
    isDarkTheme: Boolean,
    viewModel: PlannerViewModel = viewModel(factory = PlannerViewModel.Factory)
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val uiState by viewModel.uiState.collectAsState()
    val selectedDay = uiState.selectedDay
    val plans = uiState.plans
    val history = uiState.history
    val isSyncing = uiState.isSyncing
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage
    val isBackendHealthy = uiState.isBackendHealthy

    var showAddDialog by remember { mutableStateOf(false) }
    var showPlanEditor by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var newSets by remember { mutableStateOf("3") }
    var newReps by remember { mutableStateOf("10") }
    var newWeight by remember { mutableStateOf("0") }
    var editingExerciseId by remember { mutableStateOf<String?>(null) }

    var analyticsExerciseId by remember {
        mutableStateOf("bench_press")
    }
    var selectedMetric by remember {
        mutableStateOf(ExerciseMetric.WEIGHT)
    }

    val dayPlan: List<PlannedExercise> = plans[selectedDay] ?: emptyList()

    val sessionForToday: WorkoutSession? = history.firstOrNull {
        it.date == todayDate() && it.day == selectedDay
    }

    val activeSession: WorkoutSession? = uiState.currentSession?.takeIf {
        it.date == todayDate() && it.day == selectedDay
    } ?: sessionForToday

    fun handleAddOrEditExercise() {
        val name = newExerciseName.trim()
        if (name.isBlank()) return

        val sets = (newSets.toIntOrNull() ?: 3).coerceIn(1, 10)
        val reps = (newReps.toIntOrNull() ?: 10).coerceIn(1, 100)
        val weight = (newWeight.toFloatOrNull() ?: 0f).coerceAtLeast(0f)

        val exercise = PlannedExercise(
            id = editingExerciseId ?: newId(),
            name = name,
            targetSets = sets,
            targetReps = reps,
            targetWeight = weight
        )

        viewModel.addOrUpdateExercise(selectedDay, exercise)

        newExerciseName = ""
        newSets = "3"
        newReps = "10"
        newWeight = "0"
        editingExerciseId = null
        showAddDialog = false
    }

    Scaffold(
        topBar = {
            CustomisedTopBar(
                title = "Activity Planner",
                navController = navController
            )
        }
    ) { padding ->
        Bg()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading / Syncing Indicator
            if (isSyncing || isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error banner
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { viewModel.syncWithBackend() }) {
                                Text("Retry")
                            }
                            IconButton(onClick = { viewModel.dismissError() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            // Success banner
            if (successMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                successMessage,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissSuccess() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            // Header and Days selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YOUR WEEK",
                            fontSize = 12.sp,
                            color = muted,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Backend status indicator
                            if (isBackendHealthy == true) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Cloud Connected",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Synced",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline Mode",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Offline (Room DB)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.syncWithBackend() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plannerDays.forEach { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { viewModel.selectDay(day) },
                                label = { Text(day) }
                            )
                        }
                    }

                    PlannerTile(isDarkTheme = isDarkTheme) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$selectedDay Workout",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                "${dayPlan.size} exercises planned",
                                color = muted
                            )

                            Button(
                                onClick = { viewModel.startOrResumeWorkout() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (activeSession == null) {
                                        "Start Workout"
                                    } else {
                                        "Resume Workout"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Plan Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Workout Plan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(
                            onClick = { showPlanEditor = !showPlanEditor }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit plan"
                            )
                        }

                        IconButton(onClick = {
                            newExerciseName = ""
                            newSets = "3"
                            newReps = "10"
                            newWeight = "0"
                            editingExerciseId = null
                            showAddDialog = true
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add exercise",
                                tint = primary
                            )
                        }
                    }
                }
            }

            if (dayPlan.isEmpty()) {
                item {
                    PlannerEmptyCard(
                        message = "No exercises planned for $selectedDay.",
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            items(dayPlan, key = { it.id }) { exercise ->
                PlannerTile(isDarkTheme = isDarkTheme) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    exercise.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${exercise.targetSets} sets × ${exercise.targetReps} reps",
                                    color = muted,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Target: ${formatNumber(exercise.targetWeight)} kg",
                                    color = muted,
                                    fontSize = 13.sp
                                )
                            }

                            if (showPlanEditor) {
                                IconButton(onClick = {
                                    editingExerciseId = exercise.id
                                    newExerciseName = exercise.name
                                    newSets = exercise.targetSets.toString()
                                    newReps = exercise.targetReps.toString()
                                    newWeight = exercise.targetWeight.toString()
                                    showAddDialog = true
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit exercise"
                                    )
                                }

                                IconButton(onClick = {
                                    viewModel.deleteExercise(selectedDay, exercise.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete exercise",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today's Actual Workout Section
            if (activeSession != null) {
                item {
                    Text(
                        "Today's Actual Workout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val completedSets = activeSession.exercises
                        .flatMap { it.sets }
                        .count { it.isCompleted }

                    val totalSets = activeSession.exercises.sumOf {
                        it.sets.size
                    }

                    PlannerTile(isDarkTheme = isDarkTheme) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${activeSession.exercises.count { it.isCompleted }} / ${activeSession.exercises.size} exercises completed",
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = {
                                    if (totalSets == 0) 0f
                                    else completedSets.toFloat() / totalSets
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "$completedSets of $totalSets sets completed",
                                color = muted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                items(
                    activeSession.exercises,
                    key = { "${activeSession.id}_${it.exerciseId}" }
                ) { workout ->
                    PlannerTile(isDarkTheme = isDarkTheme) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        workout.exerciseName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        "${workout.sets.count { it.isCompleted }} / ${workout.sets.size} sets",
                                        color = muted,
                                        fontSize = 13.sp
                                    )
                                }

                                Checkbox(
                                    checked = workout.isCompleted,
                                    onCheckedChange = { checked ->
                                        val latest = activeSession
                                        val updatedExercises = latest.exercises.map { ex ->
                                            if (ex.exerciseId == workout.exerciseId) {
                                                ex.copy(
                                                    isCompleted = checked,
                                                    sets = ex.sets.map { set ->
                                                        set.copy(isCompleted = checked)
                                                    }
                                                )
                                            } else ex
                                        }
                                        viewModel.updateSessionInProgress(
                                            latest.copy(exercises = updatedExercises)
                                        )
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Set",
                                    modifier = Modifier.weight(0.45f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Reps",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Weight (kg)",
                                    modifier = Modifier.weight(1.25f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Done",
                                    modifier = Modifier.weight(0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            workout.sets.forEachIndexed { index, set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${set.setNumber}",
                                        modifier = Modifier.weight(0.45f)
                                    )

                                    var repsText by remember(
                                        activeSession.id,
                                        workout.exerciseId,
                                        set.setNumber,
                                        set.actualReps
                                    ) {
                                        mutableStateOf(set.actualReps.toString())
                                    }

                                    var weightText by remember(
                                        activeSession.id,
                                        workout.exerciseId,
                                        set.setNumber,
                                        set.actualWeight
                                    ) {
                                        mutableStateOf(formatNumber(set.actualWeight))
                                    }

                                    OutlinedTextField(
                                        value = repsText,
                                        onValueChange = { value ->
                                            repsText = value.filter { it.isDigit() }
                                            val reps = repsText.toIntOrNull() ?: 0

                                            val latest = activeSession
                                            val updatedExercises = latest.exercises.map { ex ->
                                                if (ex.exerciseId != workout.exerciseId) ex
                                                else ex.copy(
                                                    sets = ex.sets.mapIndexed { i, old ->
                                                        if (i == index) old.copy(actualReps = reps) else old
                                                    }
                                                )
                                            }
                                            viewModel.updateSessionInProgress(
                                                latest.copy(exercises = updatedExercises)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 13.sp
                                        )
                                    )

                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { value ->
                                            weightText = value.filter {
                                                it.isDigit() || it == '.'
                                            }
                                            val weight = weightText.toFloatOrNull() ?: 0f

                                            val latest = activeSession
                                            val updatedExercises = latest.exercises.map { ex ->
                                                if (ex.exerciseId != workout.exerciseId) ex
                                                else ex.copy(
                                                    sets = ex.sets.mapIndexed { i, old ->
                                                        if (i == index) old.copy(actualWeight = weight) else old
                                                    }
                                                )
                                            }
                                            viewModel.updateSessionInProgress(
                                                latest.copy(exercises = updatedExercises)
                                            )
                                        },
                                        modifier = Modifier.weight(1.25f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 13.sp
                                        )
                                    )

                                    Checkbox(
                                        checked = set.isCompleted,
                                        onCheckedChange = { checked ->
                                            val latest = activeSession
                                            val updatedExercises = latest.exercises.map { ex ->
                                                if (ex.exerciseId != workout.exerciseId) ex
                                                else {
                                                    val newSetList = ex.sets.mapIndexed { i, old ->
                                                        if (i == index) old.copy(isCompleted = checked) else old
                                                    }
                                                    ex.copy(
                                                        sets = newSetList,
                                                        isCompleted = newSetList.all { it.isCompleted }
                                                    )
                                                }
                                            }
                                            viewModel.updateSessionInProgress(
                                                latest.copy(exercises = updatedExercises)
                                            )
                                        }
                                    )
                                }
                            }

                            val volume = workout.sets
                                .filter { it.isCompleted }
                                .fold(0f) { total, set ->
                                    total + set.actualWeight * set.actualReps
                                }

                            Text(
                                "Completed volume: ${formatNumber(volume)} kg",
                                color = muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.saveWorkoutSession(activeSession)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSyncing) "Saving..." else "Save Workout")
                    }
                }
            }

            // Progress Analytics Section
            item {
                Text(
                    "Progress Analytics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val exerciseOptions = history
                    .flatMap { it.exercises }
                    .distinctBy { it.exerciseId }

                if (exerciseOptions.isEmpty()) {
                    PlannerEmptyCard(
                        message = "Complete a workout to see your progress here.",
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    val selectedExercise = exerciseOptions.firstOrNull {
                        it.exerciseId == analyticsExerciseId
                    } ?: exerciseOptions.first()

                    LaunchedEffect(selectedExercise.exerciseId) {
                        analyticsExerciseId = selectedExercise.exerciseId
                    }

                    PlannerTile(isDarkTheme = isDarkTheme) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Exercise progress",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            var exerciseMenuExpanded by remember {
                                mutableStateOf(false)
                            }

                            ExposedDropdownMenuBox(
                                expanded = exerciseMenuExpanded,
                                onExpandedChange = {
                                    exerciseMenuExpanded = !exerciseMenuExpanded
                                }
                            ) {
                                OutlinedTextField(
                                    value = selectedExercise.exerciseName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Exercise") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = exerciseMenuExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(
                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                        )
                                )

                                ExposedDropdownMenu(
                                    expanded = exerciseMenuExpanded,
                                    onDismissRequest = {
                                        exerciseMenuExpanded = false
                                    }
                                ) {
                                    exerciseOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.exerciseName) },
                                            onClick = {
                                                analyticsExerciseId = option.exerciseId
                                                exerciseMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ExerciseMetric.values().forEachIndexed { index, metric ->
                                    SegmentedButton(
                                        selected = selectedMetric == metric,
                                        onClick = { selectedMetric = metric },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ExerciseMetric.values().size
                                        )
                                    ) {
                                        Text(
                                            when (metric) {
                                                ExerciseMetric.WEIGHT -> "Weight"
                                                ExerciseMetric.REPS -> "Reps"
                                                ExerciseMetric.VOLUME -> "Volume"
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            val points: List<ChartPoint> = history
                                .sortedBy { it.date }
                                .mapNotNull { session ->
                                    val exercise = session.exercises.firstOrNull {
                                        it.exerciseId == selectedExercise.exerciseId
                                    } ?: return@mapNotNull null

                                    val completed = exercise.sets.filter {
                                        it.isCompleted
                                    }

                                    if (completed.isEmpty()) return@mapNotNull null

                                    val value: Float = when (selectedMetric) {
                                        ExerciseMetric.WEIGHT ->
                                            completed.maxOf {
                                                it.actualWeight
                                            }

                                        ExerciseMetric.REPS ->
                                            completed.sumOf {
                                                it.actualReps
                                            }.toFloat()

                                        ExerciseMetric.VOLUME ->
                                            completed.fold(0f) { total, set ->
                                                total + (set.actualWeight * set.actualReps)
                                            }
                                    }

                                    ChartPoint(
                                        label = displayDate(session.date),
                                        value = value
                                    )
                                }
                                .takeLast(4)

                            if (points.isEmpty()) {
                                Text(
                                    "Mark at least one set complete to show chart data.",
                                    color = muted
                                )
                            } else {
                                Text(
                                    when (selectedMetric) {
                                        ExerciseMetric.WEIGHT ->
                                            "Maximum completed set weight (kg)"
                                        ExerciseMetric.REPS ->
                                            "Total completed reps"
                                        ExerciseMetric.VOLUME ->
                                            "Completed volume (kg)"
                                    },
                                    color = muted,
                                    fontSize = 12.sp
                                )

                                SimpleLineChart(
                                    points = points,
                                    lineColor = primary
                                )

                                points.forEach { point ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            point.label,
                                            color = muted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            formatNumber(point.value),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Workout History Section
            item {
                Text(
                    "Workout History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (history.isEmpty()) {
                item {
                    PlannerEmptyCard(
                        message = "Your completed workout sessions will appear here.",
                        isDarkTheme = isDarkTheme
                    )
                }
            } else {
                items(
                    history.sortedByDescending { it.date },
                    key = { it.id }
                ) { session ->
                    PlannerTile(isDarkTheme = isDarkTheme) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${session.day} • ${displayDate(session.date)}",
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { viewModel.deleteWorkoutSession(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete workout session",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                "${session.exercises.count { ex ->
                                    ex.sets.any { it.isCompleted }
                                }} exercises with completed sets",
                                color = muted,
                                fontSize = 13.sp
                            )
                            session.exercises.forEach { exercise ->
                                val completedSets = exercise.sets.filter {
                                    it.isCompleted
                                }
                                if (completedSets.isNotEmpty()) {
                                    val volume = completedSets.fold(0f) { total, set ->
                                        total + set.actualWeight * set.actualReps
                                    }
                                    Text(
                                        "${exercise.exerciseName}: " +
                                                "${completedSets.size} sets • " +
                                                "${formatNumber(volume)} kg volume",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    if (editingExerciseId == null) {
                        "Add Exercise"
                    } else {
                        "Edit Exercise"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        label = { Text("Exercise name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSets,
                        onValueChange = {
                            newSets = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Number of sets") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newReps,
                        onValueChange = {
                            newReps = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Target reps per set") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newWeight,
                        onValueChange = {
                            newWeight = it.filter { char ->
                                char.isDigit() || char == '.'
                            }
                        },
                        label = { Text("Target weight (kg)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleAddOrEditExercise() },
                    enabled = newExerciseName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlannerTile(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = if (isDarkTheme) Color(0xFF4B5859) else Color(0xFFAEBDBF)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.6f)),
        content = content
    )
}

private data class ChartPoint(
    val label: String,
    val value: Float
)

@Composable
private fun PlannerEmptyCard(message: String, isDarkTheme: Boolean) {
    PlannerTile(isDarkTheme = isDarkTheme) {
        Text(
            message,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(value: Float): String =
    if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

@Composable
private fun SimpleLineChart(
    points: List<ChartPoint>,
    lineColor: Color
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val left = 12.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 12.dp.toPx()
            val chartHeight = bottom - top
            val chartWidth = right - left

            repeat(4) { index ->
                val y = top + chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.isEmpty()) return@Canvas

            val maxValue = max(1f, points.maxOf { it.value })
            val minValue = points.minOf { it.value }.coerceAtMost(maxValue)
            val range = (maxValue - minValue).coerceAtLeast(1f)

            val positions = points.mapIndexed { index, point ->
                val x = if (points.size == 1) {
                    left + chartWidth / 2f
                } else {
                    left + chartWidth * index / (points.size - 1).toFloat()
                }

                val y = bottom -
                        ((point.value - minValue) / range) * chartHeight

                Offset(x, y)
            }

            for (index in 0 until positions.size - 1) {
                drawLine(
                    color = lineColor,
                    start = positions[index],
                    end = positions[index + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            positions.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
            }
        }

        Text(
            "Last ${points.size} workout${if (points.size == 1) "" else "s"}",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 11.sp,
            color = textColor
        )
    }
}
