import UIKit
import GoogleSignIn
import ComposeApp

final class GoogleSignInManager {
    static let shared = GoogleSignInManager()

    private init() {}

    func registerHandlers() {
        guard
            let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
            let serverClientID = Bundle.main.object(forInfoDictionaryKey: "GIDServerClientID") as? String,
            !clientID.isEmpty,
            !serverClientID.isEmpty
        else {
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID,
            serverClientID: serverClientID
        )

        IosGoogleSignInIntegrationKt.registerGoogleSignInHandler { completion in
            self.signIn(completion: completion)
        }
    }

    func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    private func signIn(completion: @escaping (String?) -> Void) {
        guard let presentingViewController = Self.rootViewController() else {
            completion(nil)
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if let error = error as NSError? {
                if error.domain == GIDSignInError.errorDomain,
                   error.code == GIDSignInError.canceled.rawValue {
                    completion(nil)
                    return
                }
                completion(nil)
                return
            }

            guard let idToken = result?.user.idToken?.tokenString, !idToken.isEmpty else {
                completion(nil)
                return
            }
            completion(idToken)
        }
    }

    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}
