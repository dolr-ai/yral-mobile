import UIKit
import SwiftUI

class LottieLoaderContainerView: UIView {
  private var hostingController: UIHostingController<LottieLoaderView>?

  init(animationName: String) {
    super.init(frame: .zero)
    let loaderView = LottieLoaderView(animationName: animationName, resetProgess: false)
    let hostingController = UIHostingController(rootView: loaderView)
    hostingController.view.backgroundColor = .clear
    self.hostingController = hostingController

    hostingController.view.translatesAutoresizingMaskIntoConstraints = false
    addSubview(hostingController.view)
    NSLayoutConstraint.activate([
      hostingController.view.topAnchor.constraint(equalTo: topAnchor),
      hostingController.view.bottomAnchor.constraint(equalTo: bottomAnchor),
      hostingController.view.leadingAnchor.constraint(equalTo: leadingAnchor),
      hostingController.view.trailingAnchor.constraint(equalTo: trailingAnchor)
    ])
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func startAnimating(in parentView: UIView, frame: CGRect? = nil) {
    if self.superview == nil {
      if let frame = frame {
        self.frame = frame
      } else {
        self.frame = parentView.bounds
      }
      self.autoresizingMask = [.flexibleWidth, .flexibleHeight]
      parentView.addSubview(self)
    }
    self.isHidden = false
  }

  func stopAnimating() {
    self.isHidden = true
  }
}
