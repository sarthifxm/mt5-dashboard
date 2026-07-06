package com.jetro.mt5dashboard.api

import retrofit2.http.GET
import retrofit2.http.Query

interface MT5ApiService {

    @GET("api/account")
    suspend fun getAccount(): AccountResponse

    @GET("api/open-trades")
    suspend fun getOpenTrades(): OpenTradesResponse

    @GET("api/history")
    suspend fun getHistory(): HistoryResponse

    @GET("api/advanced-analytics")
    suspend fun getAdvancedAnalytics(): AdvancedAnalyticsResponse

    @GET("api/advisory")
    suspend fun getAdvisory(): AdvisoryResponse

    @GET("api/candles")
    suspend fun getCandles(
        @Query("symbol") symbol: String = "EURUSD",
        @Query("timeframe") timeframe: String = "M5",
        @Query("count") count: Int = 100
    ): CandlesResponse

    @GET("api/symbols")
    suspend fun getSymbols(): SymbolsResponse
}
