package com.hourlyvoiceclock.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hourlyvoiceclock.data.AndroidPackageInfoProvider

class HomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                application = application,
                packageInfoProvider = AndroidPackageInfoProvider(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
