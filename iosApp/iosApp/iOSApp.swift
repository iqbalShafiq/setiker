import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        if #available(iOS 15.0, *) {
            StoreKit2Manager.shared.registerHandlers()
        }
        GoogleSignInManager.shared.registerHandlers()
        AppleSignInManager.shared.registerHandlers()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = GoogleSignInManager.shared.handle(url: url)
                }
        }
    }
}