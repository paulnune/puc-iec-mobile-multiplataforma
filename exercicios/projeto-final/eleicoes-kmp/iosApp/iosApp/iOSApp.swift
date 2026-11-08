import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // O Compose aplica as próprias insets (entalhe, barra inferior): sem isto elas contam em dobro.
                .ignoresSafeArea()
        }
    }
}
