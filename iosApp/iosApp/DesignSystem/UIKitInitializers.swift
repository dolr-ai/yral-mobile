//
//  UIKitInitializers.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

extension NSObject {
  class func getUIView() -> UIView {
    let view = UIView()
    view.translatesAutoresizingMaskIntoConstraints = false
    return view
  }

  class func getUIImageView() -> UIImageView {
    let imageView = UIImageView()
    imageView.translatesAutoresizingMaskIntoConstraints = false
    return imageView
  }

  class func getUILabel() -> UILabel {
    let label = UILabel()
    label.translatesAutoresizingMaskIntoConstraints = false
    return label
  }

  class func getUIButton() -> UIButton {
    let button = UIButton()
    button.translatesAutoresizingMaskIntoConstraints = false
    return button
  }

  class func getUIStackView() -> UIStackView {
    let stackView = UIStackView()
    stackView.translatesAutoresizingMaskIntoConstraints = false
    return stackView
  }

  class func getUITableView() -> UITableView {
    let tableView = UITableView()
    tableView.translatesAutoresizingMaskIntoConstraints = false
    return tableView
  }

  class func getUICollectionView() -> UICollectionView {
    let collectionView = UICollectionView(frame: .zero, collectionViewLayout: UICollectionViewLayout())
    collectionView.translatesAutoresizingMaskIntoConstraints = false
    return collectionView
  }

  class func getUIScrollView() -> UIScrollView {
    let scrollView = UIScrollView(frame: .zero)
    scrollView.translatesAutoresizingMaskIntoConstraints = false
    return scrollView
  }

  class func getUIPageControl() -> UIPageControl {
    let pageControl = UIPageControl(frame: .zero)
    pageControl.translatesAutoresizingMaskIntoConstraints = false
    return pageControl
  }

  class func getUITextField() -> UITextField {
    let textfield = UITextField()
    textfield.translatesAutoresizingMaskIntoConstraints = false
    return textfield
  }

  class func getUITextView() -> UITextView {
    let textView = UITextView()
    textView.translatesAutoresizingMaskIntoConstraints = false
    return textView
  }

  class func getUISearchBar() -> UISearchBar {
    let searchBar = UISearchBar()
    searchBar.translatesAutoresizingMaskIntoConstraints = false
    return searchBar
  }
}
