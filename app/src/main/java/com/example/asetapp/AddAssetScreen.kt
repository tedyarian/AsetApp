package com.example.asetapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private object AppPalette {
    val ScreenBg = Color(0xFFF1F5F9)
    val CardBg = Color(0xFFFFFFFF)
    val PrimaryBlue = Color(0xFF1E40AF)
    val AccentBlue = Color(0xFF2563EB)
    val SoftBlueBg = Color(0xFFEFF6FF)
    val SoftBlueBorder = Color(0xFFBFDBFE)

    val TealAccent = Color(0xFF0F766E)
    val SoftTealBg = Color(0xFFF0FDFA)
    val SoftTealBorder = Color(0xFF99F6E4)

    val AmberAccent = Color(0xFFB45309)
    val SoftAmberBg = Color(0xFFFFFBEB)
    val SoftAmberBorder = Color(0xFFFDE68A)

    val SuccessBg = Color(0xFFDCFCE7)
    val SuccessBorder = Color(0xFF4ADE80)
    val SuccessText = Color(0xFF14532D)

    val ErrorBg = Color(0xFFFEE2E2)
    val ErrorBorder = Color(0xFFF87171)
    val ErrorText = Color(0xFF7F1D1D)

    val TextDark = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)
    val BorderLight = Color(0xFFE2E8F0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    var options by remember { mutableStateOf(FormOptionsResponse()) }
    var recentAssets by remember { mutableStateOf<List<AssetItem>>(emptyList()) }

    // 1. Lokasi & Penempatan
    var selectedDeptId by remember { mutableStateOf<Int?>(null) }
    var selectedLocId by remember { mutableStateOf<Int?>(null) }
    var selectedRoomId by remember { mutableStateOf<Int?>(null) }

    // 2. Detail Spesifik Barang & Kondisi
    var searchBmn by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Pcs") }
    var price by remember { mutableStateOf("") }
    var acquisitionYear by remember { mutableStateOf(currentYear.toString()) }
    var sumberDana by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var compressedSizeKb by remember { mutableStateOf<Int?>(null) }

    var isLoadingOptions by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadFormOptions() {
        isLoadingOptions = true
        try {
            val res = ApiClient.service.getFormOptions()
            if (res.isSuccessful) {
                val body = res.body()
                if (body != null) {
                    options = body
                    feedbackMessage =
                        "Terhubung: ${body.departments.size} dept, ${body.locations.size} gedung, ${body.rooms.size} ruang, ${body.bmn_items.size} BMN"
                } else {
                    feedbackMessage = "Respons body kosong dari server."
                }
            } else {
                feedbackMessage = "Gagal memuat opsi (HTTP ${res.code()})."
            }
        } catch (e: Exception) {
            feedbackMessage = "Koneksi gagal: ${e.localizedMessage}"
        } finally {
            isLoadingOptions = false
        }
    }

    suspend fun loadRecent(roomId: Int?) {
        runCatching {
            val res = ApiClient.service.getRecentAssets(roomId)
            if (res.isSuccessful) {
                recentAssets = res.body()?.data.orEmpty()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFormOptions()
        loadRecent(null)
    }

    LaunchedEffect(selectedRoomId) {
        loadRecent(selectedRoomId)
    }

    var filteredBmn by remember { mutableStateOf<List<OptionItem>>(emptyList()) }

    LaunchedEffect(searchBmn, selectedName, options.bmn_items) {
        val kw = searchBmn.trim().lowercase()
        if (kw.length < 2 || kw == selectedName.trim().lowercase()) {
            filteredBmn = emptyList()
            return@LaunchedEffect
        }

        kotlinx.coroutines.delay(150)

        val matches = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val all = options.bmn_items
            val starts = all.filter {
                it.name?.trim()?.lowercase()?.startsWith(kw) == true ||
                        it.code?.trim()?.lowercase()?.startsWith(kw) == true
            }
            val contains = all.filter { item ->
                val alreadyInStarts = starts.any { it === item }
                !alreadyInStarts && (
                        item.name?.lowercase()?.contains(kw) == true ||
                                item.code?.lowercase()?.contains(kw) == true
                        )
            }
            (starts + contains).take(20)
        }

        filteredBmn = matches
    }

    fun decodeSampledBitmap(uri: Uri, maxSide: Int = 1024): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var inSampleSize = 1
        while (srcWidth / inSampleSize > maxSide || srcHeight / inSampleSize > maxSide) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }

    fun compressImageBytes(uri: Uri, maxBytes: Int = 100 * 1024): ByteArray? {
        val rawBitmap = decodeSampledBitmap(uri, maxSide = 1024) ?: return null

        val maxDim = maxOf(rawBitmap.width, rawBitmap.height)
        val ratio = minOf(1f, 1024f / maxDim)
        val targetW = (rawBitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (rawBitmap.height * ratio).toInt().coerceAtLeast(1)

        var currentBitmap: Bitmap = if (ratio < 1f) {
            Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
        } else {
            rawBitmap
        }

        var quality = 80
        var compressedBytes: ByteArray

        do {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            compressedBytes = stream.toByteArray()
            quality -= 10
        } while (compressedBytes.size > maxBytes && quality >= 15)

        while (compressedBytes.size > maxBytes && maxOf(currentBitmap.width, currentBitmap.height) > 480) {
            val nextW = (currentBitmap.width * 0.75f).toInt().coerceAtLeast(240)
            val nextH = (currentBitmap.height * 0.75f).toInt().coerceAtLeast(240)
            currentBitmap = Bitmap.createScaledBitmap(currentBitmap, nextW, nextH, true)
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            compressedBytes = stream.toByteArray()
        }

        return compressedBytes
    }

    fun uriToMultipart(uri: Uri, maxBytes: Int = 100 * 1024): MultipartBody.Part? {
        val compressedBytes = compressImageBytes(uri, maxBytes) ?: return null
        compressedSizeKb = (compressedBytes.size / 1024).coerceAtLeast(1)
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        tempFile.writeBytes(compressedBytes)
        val reqFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", tempFile.name, reqFile)
    }

    fun updateSelectedPhoto(uri: Uri?) {
        imageUri = uri
        compressedSizeKb = uri?.let { compressImageBytes(it)?.size?.div(1024)?.coerceAtLeast(1) }
    }

    fun createCameraImageUri(): Uri? {
        return try {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "asset_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            feedbackMessage = "Gagal menyiapkan aset: ${e.localizedMessage}"
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            updateSelectedPhoto(cameraUri)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            updateSelectedPhoto(uri)
        }
    }

    fun submitAsset() {
        val deptId = selectedDeptId
        val locId = selectedLocId
        val roomId = selectedRoomId
        val finalName = if (selectedName.isNotBlank()) selectedName else searchBmn.trim()

        if (deptId == null || locId == null || roomId == null || finalName.isBlank() || selectedCode.isBlank()) {
            feedbackMessage = "Lengkapi Departemen, Gedung, Ruangan, Nama Barang BMN, dan Kode BMN."
            return
        }

        scope.launch {
            isSaving = true
            feedbackMessage = null
            try {
                fun textPart(v: String) = v.toRequestBody("text/plain".toMediaTypeOrNull())
                val imagePart = imageUri?.let { uriToMultipart(it) }

                val response = ApiClient.service.storeAsset(
                    departmentId = textPart(deptId.toString()),
                    locationId = textPart(locId.toString()),
                    roomId = textPart(roomId.toString()),
                    name = textPart(finalName),
                    code = textPart(selectedCode.trim()),
                    brand = textPart(brand.trim()),
                    status = textPart(status.trim()),
                    quantity = textPart((quantity.toIntOrNull() ?: 1).toString()),
                    unit = textPart(unit.trim().ifEmpty { "Pcs" }),
                    price = price.takeIf { it.isNotBlank() }?.let { textPart(it.trim()) },
                    acquisitionYear = textPart(acquisitionYear.trim().ifEmpty { currentYear.toString() }),
                    sumberDana = textPart(sumberDana.trim()),
                    pic = pic.takeIf { it.isNotBlank() }?.let { textPart(it.trim()) },
                    notes = notes.takeIf { it.isNotBlank() }?.let { textPart(it.trim()) },
                    image = imagePart
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    feedbackMessage = response.body()?.message ?: "Aset berhasil disimpan ke database."
                    searchBmn = ""
                    selectedName = ""
                    selectedCode = ""
                    brand = ""
                    quantity = "1"
                    price = ""
                    notes = ""
                    imageUri = null
                    cameraUri = null
                    compressedSizeKb = null
                    loadRecent(roomId)
                } else {
                    feedbackMessage = "Gagal menyimpan aset (Kode: ${response.code()})."
                }
            } catch (e: Exception) {
                feedbackMessage = "Error koneksi: ${e.localizedMessage}"
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = AppPalette.ScreenBg,
        topBar = {
            Surface(
                color = AppPalette.PrimaryBlue,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Input Aset BMN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sinkronisasi Real-Time ke Database",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "API v1",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)
        ) {
            feedbackMessage?.let { msg ->
                item {
                    val isPositive = msg.contains("berhasil", ignoreCase = true) ||
                            msg.contains("tersimpan", ignoreCase = true) ||
                            msg.startsWith("Terhubung:", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPositive) AppPalette.SuccessBg else AppPalette.ErrorBg,
                        border = BorderStroke(
                            1.dp,
                            if (isPositive) AppPalette.SuccessBorder else AppPalette.ErrorBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isPositive) AppPalette.SuccessText else AppPalette.ErrorText
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isPositive) AppPalette.SuccessText else AppPalette.ErrorText
                            )
                        }
                    }
                }
            }

            // BAGIAN 1: LOKASI & PENEMPATAN
            item {
                SectionCard(
                    badge = "1",
                    badgeColor = AppPalette.AccentBlue,
                    badgeBg = AppPalette.SoftBlueBg,
                    title = "Lokasi & Penempatan",
                    subtitle = "Tetap tersimpan untuk mempermudah input massal"
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                feedbackMessage = "Memuat data dari server..."
                                loadFormOptions()
                                loadRecent(selectedRoomId)
                            }
                        },
                        enabled = !isLoadingOptions,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppPalette.SoftBlueBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppPalette.SoftBlueBg,
                            contentColor = AppPalette.PrimaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isLoadingOptions) "Memuat Pilihan..." else "Muat Ulang Pilihan dari Server",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    PickerDialogField(
                        label = "Departemen / Divisi",
                        accentColor = AppPalette.AccentBlue,
                        items = options.departments,
                        selectedText = options.departments.find { it.id == selectedDeptId }?.name.orEmpty(),
                        onSelect = { selectedDeptId = it.id }
                    )
                    PickerDialogField(
                        label = "Lokasi Gedung",
                        accentColor = AppPalette.AccentBlue,
                        items = options.locations,
                        selectedText = options.locations.find { it.id == selectedLocId }?.name.orEmpty(),
                        onSelect = { selectedLocId = it.id }
                    )
                    PickerDialogField(
                        label = "Ruangan",
                        accentColor = AppPalette.AccentBlue,
                        items = options.rooms,
                        selectedText = options.rooms.find { it.id == selectedRoomId }?.name.orEmpty(),
                        onSelect = { selectedRoomId = it.id }
                    )
                }
            }

            // BAGIAN 2: DETAIL SPESIFIK BARANG
            item {
                SectionCard(
                    badge = "2",
                    badgeColor = AppPalette.TealAccent,
                    badgeBg = AppPalette.SoftTealBg,
                    title = "Detail Spesifik Barang & Kondisi",
                    subtitle = "Cari referensi BMN untuk mengisi kode otomatis"
                ) {
                    OutlinedTextField(
                        value = searchBmn,
                        onValueChange = {
                            searchBmn = it
                            selectedCode = ""
                        },
                        label = { Text("Cari Nama Barang BMN") },
                        placeholder = { Text("Contoh: Laptop, Pasir, Meja...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = AppPalette.TealAccent
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredBmn.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppPalette.SoftTealBg,
                            border = BorderStroke(1.dp, AppPalette.SoftTealBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                filteredBmn.forEach { bmn ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val chosenName = bmn.name.orEmpty();
                                                selectedName = bmn.name.orEmpty()
                                                searchBmn = bmn.name.orEmpty()
                                                selectedCode = bmn.code.orEmpty()
                                                filteredBmn = emptyList()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = bmn.name.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppPalette.TextDark
                                        )
                                        Text(
                                            text = "Kode: ${bmn.code.orEmpty()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = AppPalette.TealAccent
                                        )
                                    }
                                    HorizontalDivider(color = AppPalette.SoftTealBorder)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedCode,
                        onValueChange = { selectedCode = it },
                        label = { Text("Kode BMN / Barcode ID") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Tipe / Merk") },
                        placeholder = { Text("Contoh: Lenovo Thinkpad") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PickerDialogField(
                        label = "Status Fisik Aset",
                        accentColor = AppPalette.TealAccent,
                        items = options.status_barang,
                        selectedText = status,
                        onSelect = { status = it.kondisi.orEmpty() }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Jumlah") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Satuan") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Harga Aset (Rp)") },
                        placeholder = { Text("Contoh: 5000000") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = acquisitionYear,
                        onValueChange = { acquisitionYear = it },
                        label = { Text("Tahun Perolehan") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PickerDialogField(
                        label = "Sumber Dana",
                        accentColor = AppPalette.TealAccent,
                        items = options.sumber_dana,
                        selectedText = sumberDana,
                        onSelect = { sumberDana = it.nama_sumber_dana.orEmpty() }
                    )

                    OutlinedTextField(
                        value = pic,
                        onValueChange = { pic = it },
                        label = { Text("Penanggung Jawab (PIC)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val uri = createCameraImageUri()
                                if (uri != null) {
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppPalette.TealAccent),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppPalette.SoftTealBg,
                                contentColor = AppPalette.TealAccent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kamera HP", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppPalette.BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = AppPalette.TextDark
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Galeri", fontWeight = FontWeight.Medium)
                        }
                    }

                    if (imageUri != null) {
                        val sizeText = compressedSizeKb?.let { " (~$it KB)" } ?: ""
                        Text(
                            text = "Foto siap diunggah$sizeText ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppPalette.TealAccent
                        )
                    }

                    Button(
                        onClick = { submitAsset() },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPalette.AccentBlue,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AppPalette.PrimaryBlue, AppPalette.AccentBlue)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Text(
                            text = if (isSaving) "Menyimpan ke Database..." else "+ Simpan & Tambah Aset Lagi",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BAGIAN 3: ASET TERBARU
            item {
                SectionCard(
                    badge = "3",
                    badgeColor = AppPalette.AmberAccent,
                    badgeBg = AppPalette.SoftAmberBg,
                    title = "Aset Terbaru di Ruangan Ini",
                    subtitle = "${recentAssets.size} data terakhir tersimpan"
                ) {
                    if (recentAssets.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppPalette.SoftAmberBg,
                            border = BorderStroke(1.dp, AppPalette.SoftAmberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada aset terbaru yang dimuat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.AmberAccent,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        recentAssets.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppPalette.SoftAmberBg.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, AppPalette.SoftAmberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppPalette.TextDark
                                    )
                                    Text(
                                        text = "Kode: ${item.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppPalette.AmberAccent
                                    )
                                    Text(
                                        text = "${item.brand ?: "-"} • ${item.quantity} ${item.unit} • ${item.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppPalette.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    badge: String,
    badgeColor: Color,
    badgeBg: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppPalette.CardBg,
        border = BorderStroke(1.dp, AppPalette.BorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.TextDark
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.TextMuted
                    )
                }
            }
            HorizontalDivider(color = AppPalette.BorderLight)
            content()
        }
    }
}

@Composable
private fun PickerDialogField(
    label: String,
    accentColor: Color,
    items: List<OptionItem>,
    selectedText: String,
    onSelect: (OptionItem) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text("Pilih $label") },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = accentColor
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = AppPalette.TextDark,
                disabledBorderColor = if (selectedText.isNotEmpty()) accentColor else AppPalette.BorderLight,
                disabledLabelColor = AppPalette.TextMuted,
                disabledPlaceholderColor = AppPalette.TextMuted,
                disabledTrailingIconColor = accentColor,
                disabledContainerColor = if (selectedText.isNotEmpty()) accentColor.copy(alpha = 0.05f) else Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = AppPalette.CardBg,
            title = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            },
            text = {
                if (items.isEmpty()) {
                    Text(
                        text = "Data pilihan kosong.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.TextMuted
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(items) { option ->
                            val text = option.displayLabel()
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = text,
                                        fontWeight = FontWeight.Medium,
                                        color = AppPalette.TextDark
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onSelect(option)
                                    showDialog = false
                                }
                            )
                            HorizontalDivider(color = AppPalette.BorderLight)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Tutup", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}