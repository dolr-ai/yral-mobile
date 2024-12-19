//
//  NibLoadableVIew.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

protocol NibLoadableView: AnyObject {
  static var nibName: String { get }
  static func instantiate() -> Self
}

extension NibLoadableView where Self: UIView {
  static var nibName: String {
    return String(describing: self)
  }

  static func instantiate() -> Self {
    return UINib(nibName: nibName, bundle: Bundle(for: Self.self))
      .instantiate(withOwner: nil, options: nil)[0] as! Self // swiftlint:disable:this force_cast
  }
}
