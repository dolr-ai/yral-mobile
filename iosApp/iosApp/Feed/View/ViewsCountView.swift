//
//  ViewsCountView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 06/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

final class ViewsCountView: UIView {

  private let iconView: UIImageView = {
    let imageView = UIImageView(image: UIImage(named: "video_views"))
    imageView.contentMode = .scaleAspectFit
    imageView.translatesAutoresizingMaskIntoConstraints = false
    return imageView
  }()

  private let countLabel: UILabel = {
    let label = UILabel()
    label.textColor = YralColor.grey50.uiColor
    label.font = YralFont.pt12.semiBold.uiFont
    label.textAlignment = .center
    label.text = ""
    label.translatesAutoresizingMaskIntoConstraints = false
    return label
  }()

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    addSubview(iconView)
    addSubview(countLabel)

    NSLayoutConstraint.activate([
      iconView.centerXAnchor.constraint(equalTo: centerXAnchor),
      iconView.topAnchor.constraint(equalTo: topAnchor),
      iconView.widthAnchor.constraint(equalToConstant: 36),
      iconView.heightAnchor.constraint(equalToConstant: 36),

      countLabel.topAnchor.constraint(equalTo: iconView.bottomAnchor),
      countLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
      countLabel.bottomAnchor.constraint(equalTo: bottomAnchor)
    ])
  }

  func setCount(_ value: Int64) {
    countLabel.text = "\(value.formattedWithSuffix)"
  }
}
