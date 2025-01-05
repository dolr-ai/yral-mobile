//
//  ImageLoaderProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import SDWebImage

protocol ImageLoaderProtocol: AnyObject {
  func loadImage(with url: URL, placeholderImage: UIImage?, on imageView: UIImageView)
}

extension ImageLoaderProtocol {
  func loadImage(
    with url: URL,
    placeholderImage: UIImage? = UIImage(named: "placeholderImg"),
    on imageView: UIImageView
  ) {
    imageView.sd_imageIndicator = SDWebImageActivityIndicator.gray
    let localUrl = url
    imageView.sd_setImage(with: url, placeholderImage: placeholderImage) {image, _, _, url in
      if url?.absoluteString == localUrl.absoluteString {
        imageView.image = image
      }
    }
  }

  func loadImage(with url: URL, on button: UIButton) {
    button.sd_imageIndicator = SDWebImageActivityIndicator.gray
    let localUrl = url
    button.sd_setImage(with: url, for: .normal) {image, _, _, url in
      if url?.absoluteString == localUrl.absoluteString {
        button.setImage(image, for: .normal)
      }
    }
  }
}
