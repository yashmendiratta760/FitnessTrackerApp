package com.yash.fitnesstracker

import android.app.Application
import com.yash.fitnesstracker.repository.AppContainer
import com.yash.fitnesstracker.repository.DefaultAppContainer
import com.yash.fitnesstracker.repository.StepRepository
import com.yash.fitnesstracker.service.DataStoreManager

class MainApplication : Application()
{
    lateinit var container: AppContainer
    override fun onCreate()
    {
        super.onCreate()
        DataStoreManager.init(this)
        container = DefaultAppContainer(this)
        StepRepository.initialize(applicationContext)
    }
}
