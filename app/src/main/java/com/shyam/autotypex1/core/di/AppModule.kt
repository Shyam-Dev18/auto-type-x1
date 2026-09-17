package com.shyam.autotypex1.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for general application-level bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
