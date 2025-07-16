//
//  HeaderView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 16/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

protocol HeaderViewDelegate: AnyObject {
  func didTapAccountButton()
}

class HeaderView: UIView {
  lazy var accountButton: UIButton = {
    let button = HeaderView.getUIButton()
    button.setBackgroundImage(Constants.accountImage, for: .normal)
    button.addTarget(self, action: #selector(didTapAccountButton), for: .touchUpInside)
    return button
  }()

  let coinsView = CoinsView()

  weak var delegate: HeaderViewDelegate?

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addAccountButton()
    addCoinsView()
  }

  private func addAccountButton() {
    addSubview(accountButton)
    NSLayoutConstraint.activate([
      accountButton.leadingAnchor.constraint(equalTo: leadingAnchor),
      accountButton.widthAnchor.constraint(equalToConstant: Constants.accountImageSize),
      accountButton.heightAnchor.constraint(equalToConstant: Constants.accountImageSize),
      accountButton.centerYAnchor.constraint(equalTo: centerYAnchor)
    ])
  }

  private func addCoinsView() {
    addSubview(coinsView)
    NSLayoutConstraint.activate([
      coinsView.trailingAnchor.constraint(equalTo: trailingAnchor),
      coinsView.centerYAnchor.constraint(equalTo: centerYAnchor)
    ])
  }

  @objc private func didTapAccountButton() {
    delegate?.didTapAccountButton()
  }

  func set(coins: UInt64) {
    coinsView.set(coins: coins)
  }

  func updateCoins(by newCoins: Int) {
    coinsView.updateCoins(by: newCoins)
  }

  struct ProfileInfo {
    let imageURL: URL?
    let subtitle: String
    var coins: UInt64
  }
}

extension HeaderView {
  enum Constants {
    static let accountImage = UIImage(named: "account")
    static let accountImageSize = 32.0
  }
}
