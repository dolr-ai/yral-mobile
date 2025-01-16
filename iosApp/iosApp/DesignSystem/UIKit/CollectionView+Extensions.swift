//
//  CollectionView+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

extension UICollectionView {
  func register<T: UICollectionViewCell>(_: T.Type) where T: ReusableView {
    register(T.self, forCellWithReuseIdentifier: T.defaultReuseIdentifier)
  }

  func register<T: UICollectionViewCell>(_: T.Type) where T: ReusableView, T: NibLoadableView {
    let bundle = Bundle(for: T.self)
    let nib = UINib(nibName: T.nibName, bundle: bundle)

    register(nib, forCellWithReuseIdentifier: T.defaultReuseIdentifier)
  }

  func register<T: UICollectionReusableView>(
    _: T.Type,
    forSupplementaryViewOfKind elementKind: String
  ) where T: ReusableView {
    register(T.self, forSupplementaryViewOfKind: elementKind, withReuseIdentifier: T.defaultReuseIdentifier)
  }

  func register<T: UICollectionReusableView>(
    _: T.Type,
    forSupplementaryViewOfKind elementKind: String
  ) where T: ReusableView, T: NibLoadableView {
    let bundle = Bundle(for: T.self)
    let nib = UINib(nibName: T.nibName, bundle: bundle)

    register(nib, forSupplementaryViewOfKind: elementKind, withReuseIdentifier: T.defaultReuseIdentifier)
  }

  func dequeueReusableCell<T: UICollectionViewCell>(
    _: T.Type,
    for indexPath: IndexPath
  ) -> T where T: ReusableView {
    guard let cell = dequeueReusableCell(withReuseIdentifier: T.defaultReuseIdentifier, for: indexPath) as? T else {
      fatalError("Could not dequeue cell with identifier '\(T.defaultReuseIdentifier)'")
    }

    return cell
  }

  func dequeueReusableSupplementaryView<T: UICollectionReusableView>(
    _: T.Type,
    ofKind elementKind: String,
    for indexPath: IndexPath
  ) -> T where T: ReusableView {
    guard let supplementaryView = dequeueReusableSupplementaryView(
      ofKind: elementKind,
      withReuseIdentifier: T.defaultReuseIdentifier,
      for: indexPath
    ) as? T else {
      // swiftlint:disable:next line_length
      fatalError("Could not dequeue supplementary view of kind '\(elementKind)' with identifier '\(T.defaultReuseIdentifier)'")
    }

    return supplementaryView
  }
}
