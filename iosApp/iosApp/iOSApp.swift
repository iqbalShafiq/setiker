import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        if #available(iOS 15.0, *) {
            StoreKit2Manager.shared.registerHandlers()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}