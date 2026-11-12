package com.lexterlabra.puzzle

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point called from Swift (`MainViewControllerKt.MainViewController()`), so the name is fixed. */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
