package com.hourlyvoiceclock.di

/**
 * Interface that the Application class implements so that ViewModels
 * and other Android-framework-created objects can access shared dependencies
 * without constructing them inline.
 */
interface DependenciesProvider {
    val dependencies: AppDependencies
}
