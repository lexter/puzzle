import SwiftUI
import UIKit
import ComposeApp

/// The only SwiftUI ↔ Compose bridge: hosts the shared Compose UI full screen.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
