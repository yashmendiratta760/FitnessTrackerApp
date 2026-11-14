package com.yash.fitnesstracker.repository

import android.content.Context
import com.yash.fitnesstracker.api.PlannerApi
import com.yash.fitnesstracker.api.ServerDbApi
import com.yash.fitnesstracker.login_Signup.api.LoginSignupAPI
import com.yash.fitnesstracker.login_Signup.repository.LoginSignupRepositoryImpl
import com.yash.fitnesstracker.config.AuthInterceptor
import com.yash.fitnesstracker.database.ActivityDatabase
import com.yash.fitnesstracker.database.PlannerDatabase
import com.yash.fitnesstracker.database.StepsDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer
{
    val stepsLocalDbRepository: StepsLocalDbRepository
    val loginSignupRepository: LoginSignupRepositoryImpl
    val onlineServerDbRep : OnlineServerDbRep
    val activityHistoryRepo: ActivityHistoryRepo
    val plannerRepository: PlannerRepositoryImpl
}
class DefaultAppContainer(private val context: Context): AppContainer
{
    override val stepsLocalDbRepository: StepsLocalDbRepository by lazy {
        OfflineStepsLocalDbRepository(StepsDatabase.getDatabase(context).stepsDao())
    }

    override val activityHistoryRepo: ActivityHistoryRepo by lazy {
        OfflineActivityHistoryRepo(ActivityDatabase.getDatabase(context).activityDao())
    }

    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
//        .baseUrl("http://10.0.2.2:8080/")
//        .baseUrl("http://192.168.0.104:8080/")
        .baseUrl("https://fitnesstracker-p8kx.onrender.com/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val retrofitservice = retrofit.create(LoginSignupAPI::class.java)

    val retrofitserviceServerDb = retrofit.create(ServerDbApi::class.java)

    val retrofitservicePlanner = retrofit.create(PlannerApi::class.java)

    override val loginSignupRepository: LoginSignupRepositoryImpl by lazy {
        LoginSignupRepositoryImpl(retrofitservice)
    }

    override val onlineServerDbRep: OnlineServerDbRep by lazy {
        OnlineServerDbRep(retrofitserviceServerDb)
    }

    val plannerDatabase by lazy {
        PlannerDatabase.getDatabase(context)
    }

    override val plannerRepository: PlannerRepositoryImpl by lazy {
        PlannerRepositoryImpl(
            api = retrofitservicePlanner,
            dao = plannerDatabase.plannerDao()
        )
    }
}
