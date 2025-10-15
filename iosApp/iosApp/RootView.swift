import SwiftUI
import iosSharedUmbrella

struct RootView: UIViewControllerRepresentable {
    let root: RootComponent

    func makeUIViewController(context: Context) -> UIViewController {
        return RootViewControllerKt.rootViewController(rootComponent: root)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
