//
//  FeedsViewController.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import UIKit

class FeedsViewController: UIViewController {
  private var viewModel: FeedsViewModel

  init(viewModel: FeedsViewModel) {
    self.viewModel = viewModel
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    bindViewModel(viewModel: viewModel)
  }

  func bindViewModel(viewModel: FeedsViewModel) {
    viewModel.$state.receive(on: RunLoop.main).sink { [weak self] state in
      switch state {
      case .initalized:
        break
      case .loading:
        break
      case .successfullyFetched(let feeds):
        print(feeds)
      case .failure(let error):
        print(error)
      }
    }
  }
}
