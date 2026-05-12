@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)

package com.nandan.calorimeterapp

import com.google.android.gms.ads.MobileAds
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.nandan.calorimeterapp.ui.theme.CalorimeterAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import java.util.concurrent.Executors
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import android.app.Activity
import com.google.android.gms.ads.interstitial.InterstitialAd
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private var mInterstitialAd by mutableStateOf<InterstitialAd?>(null)
    fun loadInterstitial() {
        InterstitialAd.load(
            this,
            "ca-app-pub-2842829612476029/1293112009",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mInterstitialAd = ad
                    Log.d("ADS", "Interstitial Loaded")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    Log.d("ADS", "Failed: ${adError.message}")
                }
            }
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        loadInterstitial()
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        setContent {
            CalorimeterAppTheme {
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                var userId by remember { mutableStateOf(auth.currentUser?.uid ?: "") }
                var showOnboarding by remember { mutableStateOf(false) }
                
                var userWeight by remember { mutableStateOf("70") }
                var userGoal by remember { mutableStateOf("Maintain Weight") }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!isLoggedIn) {
                        AuthScreen(auth) { uid, isNewUser ->
                            userId = uid
                            showOnboarding = isNewUser
                            isLoggedIn = true
                        }
                    } else if (showOnboarding) {
                        OnboardingScreen(
                            onComplete = { weight, goal ->
                                userWeight = weight
                                userGoal = goal
                                showOnboarding = false
                            }
                        )
                    } else {
                        MainContainer(
                            uid = userId,
                            currentWeight = userWeight,
                            currentGoal = userGoal,
                            onUpdateSettings = { w, g ->
                                userWeight = w
                                userGoal = g
                            },
                            onLogout = {
                                auth.signOut()
                                isLoggedIn = false
                            },
                            mInterstitialAd = mInterstitialAd
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: (String, String) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("Maintain Weight") }
    var expanded by remember { mutableStateOf(false) }
    val goals = listOf("Lose Weight", "Maintain Weight", "Bulk")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Calorimeter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Let's personalize your plan and calculate your BMI", color = Color.Gray, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = weight, onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = height, onValueChange = { height = it },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = goal,
                onValueChange = {},
                readOnly = true,
                label = { Text("Goal") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                goals.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            goal = g
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        val w = weight.toDoubleOrNull() ?: 0.0
        val h = height.toDoubleOrNull() ?: 0.0
        if (w > 0 && h > 0) {
            val bmi = w / ((h / 100.0) * (h / 100.0))
            val category = when {
                bmi < 18.5 -> "Underweight"
                bmi < 25.0 -> "Healthy Weight"
                bmi < 30.0 -> "Overweight"
                else -> "Obese"
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your BMI: ${String.format(Locale.US, "%.1f", bmi)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        Button(
            onClick = { onComplete(weight, goal) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = weight.isNotBlank() && height.isNotBlank()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, onSuccess: (String, Boolean) -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = if (isLogin) "Welcome Back" else "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                val task = if (isLogin) auth.signInWithEmailAndPassword(email, password)
                else auth.createUserWithEmailAndPassword(email, password)
                task.addOnCompleteListener { result ->
                    isLoading = false
                    if (result.isSuccessful) onSuccess(auth.currentUser?.uid ?: "", !isLogin)
                    else Toast.makeText(context, "Error: ${result.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text(if (isLogin) "Sign In" else "Create Account")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(" OR ", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { Toast.makeText(context, "Google Sign-in coming soon", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "New here? Create an account" else "Already have an account? Sign In")
        }
    }
}

@Composable
fun MainContainer(
    uid: String, 
    currentWeight: String, 
    currentGoal: String,
    onUpdateSettings: (String, String) -> Unit,
    onLogout: () -> Unit,
    mInterstitialAd: InterstitialAd?
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val calorieTarget = when(currentGoal) {
        "Lose Weight" -> (currentWeight.toDoubleOrNull() ?: 70.0) * 22
        "Bulk" -> (currentWeight.toDoubleOrNull() ?: 70.0) * 35
        else -> (currentWeight.toDoubleOrNull() ?: 70.0) * 28
    }.toInt()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Food")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calorimeter", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        bottomBar = {
            AndroidView(
                factory = { context ->
                    val adView = AdView(context)
                    adView.setAdSize(AdSize.BANNER)
                    adView.adUnitId = "ca-app-pub-2842829612476029/4573881460"
                    adView.loadAd(AdRequest.Builder().build())
                    adView
                }
            )
        }

    ) { padding ->
        FoodListScreen(uid, padding, refreshTrigger, calorieTarget) { refreshTrigger++ }

        if (showAddDialog) {
            AddFoodDialog(
                uid = uid,
                onDismiss = { showAddDialog = false },
                onSuccess = {
                    showAddDialog = false
                    refreshTrigger++

                    if (mInterstitialAd != null) {
                        mInterstitialAd?.show(context as Activity)

                        (context as Activity).let {
                            if (it is MainActivity) {
                                it.loadInterstitial()
                            }
                        }

                    } else {
                        Log.d("ADS", "Ad not ready")
                    }
                }
            )
        }

        if (showSettings) {
            SettingsDialog(
                currentWeight = currentWeight, 
                currentGoal = currentGoal,
                onDismiss = { showSettings = false },
                onSave = { w, g -> 
                    onUpdateSettings(w, g)
                    showSettings = false 
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(currentWeight: String, currentGoal: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var weight by remember { mutableStateOf(currentWeight) }
    var goal by remember { mutableStateOf(currentGoal) }
    val goals = listOf("Lose Weight", "Maintain Weight", "Bulk")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(weight, goal) }) { Text("Save") } },
        title = { Text("Goal Settings") },
        text = {
            Column {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = goal,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Goal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        goals.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = { 
                                    goal = g
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FoodListScreen(uid: String, padding: PaddingValues, refreshTrigger: Int, target: Int, onUpdate: () -> Unit) {
    var foodList by remember { mutableStateOf(listOf<FoodResponse>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        ApiClient.apiService.getFoods(uid).enqueue(object : Callback<List<FoodResponse>> {
            override fun onResponse(call: Call<List<FoodResponse>>, response: Response<List<FoodResponse>>) {
                if (response.isSuccessful) foodList = response.body() ?: emptyList()
                isLoading = false
            }
            override fun onFailure(call: Call<List<FoodResponse>>, t: Throwable) { isLoading = false }
        })
    }

    val totalCals = foodList.sumOf { it.calories }
    val totalProt = foodList.sumOf { it.protein }
    val totalCarbs = foodList.sumOf { it.carbs }
    val totalFat = foodList.sumOf { it.fat }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
        item {
            DashboardHeader(totalCals, target, totalProt, totalCarbs, totalFat)
            Spacer(modifier = Modifier.height(24.dp))
            InsightBanner(totalProt, totalCals.toDouble(), target)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Today's Diary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (isLoading) {
            item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (foodList.isEmpty()) {
            item { Text("No meals logged. Tap + to start!", modifier = Modifier.padding(16.dp), color = Color.Gray) }
        } else {
            items(foodList) { item ->
                FoodCardWithDelete(item) {
                    ApiClient.apiService.deleteFood(item.id).enqueue(object : Callback<SimpleResponse> {
                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) { if (response.isSuccessful) onUpdate() }
                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {}
                    })
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(consumed: Int, target: Int, p: Double, c: Double, f: Double) {
    val remaining = (target - consumed).coerceAtLeast(0)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Budget", style = MaterialTheme.typography.labelLarge)
                    Text("$target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Consumed", style = MaterialTheme.typography.labelLarge)
                    Text("$consumed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.size(100.dp), strokeWidth = 10.dp, trackColor = Color.White.copy(alpha = 0.2f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$remaining", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("left", fontSize = 10.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroStat("Protein", p, (target * 0.25 / 4).toInt())
                MacroStat("Carbs", c, (target * 0.50 / 4).toInt())
                MacroStat("Fat", f, (target * 0.25 / 9).toInt())
            }
        }
    }
}

@Composable
fun MacroStat(label: String, current: Double, goal: Int) {
    Column(modifier = Modifier.width(90.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(progress = { (current.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
        Text("${current.toInt()}g / ${goal}g", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun InsightBanner(p: Double, cal: Double, target: Int) {
    val message = when {
        cal > target -> "Goal exceeded! Focus on fiber and hydration."
        p < 40 -> "Protein is low. Consider some chicken or tofu."
        cal > target * 0.8 -> "Nearly at your limit. Great discipline!"
        else -> "You're on track! Good balance so far."
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TipsAndUpdates, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun FoodCardWithDelete(item: FoodResponse, onDelete: () -> Unit) {
     Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.height(90.dp).fillMaxWidth()) {
            AsyncImage(model = item.imageUrl.ifEmpty { "https://via.placeholder.com/150?text=Food" }, contentDescription = null, modifier = Modifier.width(90.dp).fillMaxHeight(), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.foodName, fontWeight = FontWeight.Bold, maxLines = 1)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.5f), modifier = Modifier.size(18.dp)) }
                }
                Text("${item.quantity} ${item.unit} • ${item.calories} kcal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("P: ${item.protein.toInt()}g", fontSize = 10.sp)
                    Text("C: ${item.carbs.toInt()}g", fontSize = 10.sp)
                    Text("F: ${item.fat.toInt()}g", fontSize = 10.sp)
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun AddFoodDialog(
    uid: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var foodName by remember { mutableStateOf("") }
    var caloriesInput by remember { mutableStateOf("0") }
    var pInput by remember { mutableStateOf("0.0") }
    var cInput by remember { mutableStateOf("0.0") }
    var fInput by remember { mutableStateOf("0.0") }
    
    var baseCals by remember { mutableDoubleStateOf(0.0) }
    var baseP by remember { mutableDoubleStateOf(0.0) }
    var baseC by remember { mutableDoubleStateOf(0.0) }
    var baseF by remember { mutableDoubleStateOf(0.0) }
    
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("serving") }
    var imageUrl by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(qty, unit, baseCals, baseP, baseC, baseF) {
        if (isLoaded) {
            val q = qty.toDoubleOrNull() ?: 1.0
            val mult = if (unit == "grams") q / 100.0 else q
            caloriesInput = (baseCals * mult).toInt().toString()
            pInput = String.format(Locale.US, "%.1f", baseP * mult)
            cInput = String.format(Locale.US, "%.1f", baseC * mult)
            fInput = String.format(Locale.US, "%.1f", baseF * mult)
        }
    }

    fun handleSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        isLoaded = false
        ApiClient.apiService.searchFood(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                isSearching = false
                if (response.isSuccessful) {
                    response.body()?.let { d ->
                        baseCals = d.calories
                        baseP = d.protein
                        baseC = d.carbs
                        baseF = d.fat
                        imageUrl = d.imageUrl
                        isLoaded = true
                    }
                }
            }
            override fun onFailure(call: Call<SearchResponse>, t: Throwable) { isSearching = false }
        })
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onDismiss = { showScanner = false },
            onCodeScanned = { code ->
                showScanner = false
                isSearching = true
                ApiClient.apiService.getBarcode(code).enqueue(object : Callback<BarcodeResponse> {
                    override fun onResponse(call: Call<BarcodeResponse>, response: Response<BarcodeResponse>) {
                        isSearching = false
                        if (response.isSuccessful) {
                            response.body()?.let { d ->
                                foodName = d.name
                                baseCals = d.calories
                                baseP = d.protein
                                baseC = d.carbs
                                baseF = d.fat
                                imageUrl = d.imageUrl
                                isLoaded = true
                            }
                        }
                    }
                    override fun onFailure(call: Call<BarcodeResponse>, t: Throwable) { isSearching = false }
                })
            }
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Log Food", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showScanner = true }) { Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = foodName, onValueChange = { foodName = it }, label = { Text("Search Food") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else IconButton(onClick = { handleSearch(foodName) }) { Icon(Icons.Default.Search, null) }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { handleSearch(foodName) })
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty") }, modifier = Modifier.weight(0.4f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    var exp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = exp,
                        onExpandedChange = { exp = it },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                            listOf("serving", "pieces", "grams", "kgs", "cups").forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; exp = false }) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = caloriesInput, onValueChange = { caloriesInput = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                if (imageUrl.isNotEmpty()) { AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.height(120.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val q = qty.toDoubleOrNull() ?: 1.0
                        val req = FoodRequest(
                            uid = uid, 
                            foodName = foodName, 
                            calories = caloriesInput.toIntOrNull() ?: 0, 
                            protein = pInput.toDoubleOrNull() ?: 0.0, 
                            carbs = cInput.toDoubleOrNull() ?: 0.0, 
                            fat = fInput.toDoubleOrNull() ?: 0.0, 
                            imageUrl = imageUrl, 
                            quantity = q, 
                            unit = unit
                        )
                        ApiClient.apiService.addFood(req).enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Added!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            }
                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                Toast.makeText(context, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isSearching && foodName.isNotBlank()
                ) { Text("Add to Diary") }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_ALL_FORMATS
                )
                .build()
        )
    }

    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        hasPerm = it
    }

    LaunchedEffect(Unit) {
        if (!hasPerm) launcher.launch(Manifest.permission.CAMERA)
    }

    var scanned by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            if (hasPerm) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { proxy ->
                                val mediaImage = proxy.image

                                if (mediaImage != null && !scanned) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        proxy.imageInfo.rotationDegrees
                                    )

                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val value = barcode.rawValue
                                                if (!value.isNullOrEmpty()) {
                                                    scanned = true
                                                    onCodeScanned(value)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            // optional log
                                        }
                                        .addOnCompleteListener {
                                            proxy.close()
                                        }
                                } else {
                                    proxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }

            Text(
                "Align Barcode",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(64.dp)
            )
        }
    }
}

@ExperimentalGetImage
@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(proxy: ImageProxy, scanner: com.google.mlkit.vision.barcode.BarcodeScanner, onCodeScanned: (String) -> Unit) {
    val mediaImage = proxy.image ?: run { proxy.close(); return }
    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let { onCodeScanned(it) } }
        .addOnCompleteListener { proxy.close() }
}
