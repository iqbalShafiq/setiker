import AuthenticationServices
import UIKit
import ComposeApp

final class AppleSignInManager: NSObject {
    static let shared = AppleSignInManager()

    private var completion: ((String?) -> Void)?

    private override init() {
        super.init()
    }

    func registerHandlers() {
        IosAppleSignInIntegrationKt.registerAppleSignInHandler { completion in
            self.signIn(completion: completion)
        }
    }

    private func signIn(completion: @escaping (String?) -> Void) {
        self.completion = completion

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    private func finish(with idToken: String?) {
        completion?(idToken)
        completion = nil
    }
}

extension AppleSignInManager: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let idToken = String(data: tokenData, encoding: .utf8),
            !idToken.isEmpty
        else {
            finish(with: nil)
            return
        }
        finish(with: idToken)
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        finish(with: nil)
    }
}

extension AppleSignInManager: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow) ?? UIWindow()
    }
}
