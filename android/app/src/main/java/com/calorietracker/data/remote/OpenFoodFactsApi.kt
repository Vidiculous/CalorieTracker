package com.calorietracker.data.remote

import com.calorietracker.data.remote.dto.OFFProductResponse
import com.calorietracker.data.remote.dto.OFFSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): OFFProductResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process"
    ): OFFSearchResponse
}
