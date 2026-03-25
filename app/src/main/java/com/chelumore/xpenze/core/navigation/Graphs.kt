package com.chelumore.xpenze.core.navigation

import kotlinx.serialization.Serializable

/**
 * Top-level nested navigation graphs.
 *
 * We keep these separate from [Screen] so "leaf" destinations remain clean and type-safe.
 */
@Serializable
data object AuthGraph

@Serializable
data object OnboardingGraph

@Serializable
data object MainGraph


