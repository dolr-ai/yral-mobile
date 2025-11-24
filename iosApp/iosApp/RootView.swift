import SwiftUI
import iosSharedUmbrella

@MainActor
struct RootView: UIViewControllerRepresentable {
    typealias UIViewControllerType = UIViewController
    typealias Context = UIViewControllerRepresentableContext<RootView>
    typealias Coordinator = Void

    func makeCoordinator() -> Coordinator { () }

    let root: RootComponent

    func makeUIViewController(context: Context) -> UIViewControllerType {
        return RootViewControllerKt.rootViewController(rootComponent: root)
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
    }
}
