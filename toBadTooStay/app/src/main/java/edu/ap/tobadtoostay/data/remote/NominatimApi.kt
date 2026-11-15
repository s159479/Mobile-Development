package edu.ap.tobadtoostay.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int = 18,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1
    ): NominatimResponse

    companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org/"

        // Nominatim requires a User-Agent that identifies the app (no fake browser name)
        private const val USER_AGENT = "ToBadTooStay/1.0 (Android school project, AP Hogeschool)"

        val instance: NominatimApi by lazy {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                    chain.proceed(request)
                }
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimApi::class.java)
        }
    }
}

data class NominatimResponse(
    @SerializedName("display_name") val displayName: String?,
    val address: NominatimAddress?
)

data class NominatimAddress(
    val road: String?,
    @SerializedName("house_number") val houseNumber: String?,
    val postcode: String?,
    val city: String?,
    val town: String?,
    val village: String?,
    val municipality: String?
) {
    /** Depending on the place, Nominatim puts the municipality in city, town or village. */
    val cityName: String
        get() = city ?: town ?: village ?: municipality ?: ""
}
