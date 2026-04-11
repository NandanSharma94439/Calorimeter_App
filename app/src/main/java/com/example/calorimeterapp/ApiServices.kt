package com.example.calorimeterapp

import retrofit2.Call
import retrofit2.http.*

data class FoodRequest(
    val uid: String,
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String,
    val quantity: Double,
    val unit: String,
)

data class FoodResponse(
    val id: String,
    val uid: String,
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String,
    val quantity: Double,
    val unit: String
)

data class SearchResponse(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String
)

data class BarcodeResponse(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String
)

data class SimpleResponse(
    val message: String? = null,
    val error: String? = null
)

interface ApiService {

    @POST("add-food")
    fun addFood(@Body food: FoodRequest): Call<SimpleResponse>

    @GET("get-foods/{uid}")
    fun getFoods(@Path("uid") uid: String): Call<List<FoodResponse>>

    @GET("search-food")
    fun searchFood(@Query("name") name: String): Call<SearchResponse>

    @GET("barcode/{code}")
    fun getBarcode(@Path("code") code: String): Call<BarcodeResponse>

    @DELETE("delete-food/{id}")
    fun deleteFood(@Path("id") id: String): Call<SimpleResponse>
}