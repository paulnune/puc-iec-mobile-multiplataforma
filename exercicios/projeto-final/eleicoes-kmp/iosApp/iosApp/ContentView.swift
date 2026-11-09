import SwiftUI
import Shared

/// Hospeda a UI Compose do módulo `shared` (Kotlin: MainViewController.kt).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
