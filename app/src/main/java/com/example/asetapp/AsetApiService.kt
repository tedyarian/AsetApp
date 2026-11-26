package com.example.asetapp

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AssetApiService {
    @GET("api/v1/asset/form-options")
    suspend fun getFormOptions(): Response<FormOptionsResponse>

    @GET("api/v1/asset/recent")
    suspend fun getRecentAssets(
        @Query("room_id") roomId: Int? = null
    ): Response<RecentAssetsResponse>

    @Multipart
    @POST("api/v1/asset/inventory")
    suspend fun storeAsset(
        @Part("department_id") departmentId: RequestBody,
        @Part("location_id") locationId: RequestBody,
        @Part("room_id") roomId: RequestBody,
        @Part("name") name: RequestBody,
        @Part("code") code: RequestBody,
        @Part("brand") brand: RequestBody,
        @Part("status") status: RequestBody,
        @Part("quantity") quantity: RequestBody,
        @Part("unit") unit: RequestBody,
        @Part("price") price: RequestBody? = null,
        @Part("acquisition_year") acquisitionYear: RequestBody,
        @Part("sumber_dana") sumberDana: RequestBody,
        @Part("pic") pic: RequestBody? = null,
        @Part("notes") notes: RequestBody? = null,
        @Part image: MultipartBody.Part? = null
    ): Response<StoreAssetResponse>
}

object ApiClient {
    private const val BASE_URL = "http://simbara.my.id/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson by lazy {
        com.google.gson.GsonBuilder()
            .setLenient()
            .create()
    }

    val service: AssetApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AssetApiService::class.java)
    }
}

data class OptionItem(
    val id: Int? = null,
    val name: String? = null,
    val kondisi: String? = null,
    val nama_sumber_dana: String? = null,
    val code: String? = null
) {
    fun displayLabel(): String {
        return name ?: kondisi ?: nama_sumber_dana ?: code ?: ""
    }
}

data class FormOptionsResponse(
    val departments: List<OptionItem> = emptyList(),
    val locations: List<OptionItem> = emptyList(),
    val rooms: List<OptionItem> = emptyList(),
    val status_barang: List<OptionItem> = emptyList(),
    val sumber_dana: List<OptionItem> = emptyList(),
    val bmn_items: List<OptionItem> = emptyList()
)

data class AssetItem(
    val id: Int? = null,
    val name: String = "",
    val code: String = "",
    val brand: String? = null,
    val status: String = "",
    val quantity: Int = 1,
    val unit: String = "Pcs"
)

data class RecentAssetsResponse(
    val data: List<AssetItem> = emptyList()
)

data class StoreAssetResponse(
    val success: Boolean = false,
    val message: String? = null
)