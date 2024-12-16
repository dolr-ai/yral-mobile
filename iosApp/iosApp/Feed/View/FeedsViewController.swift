//
//  FeedsViewController.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import UIKit
import Combine

class FeedsViewController: UIViewController {
  typealias DataSource = UICollectionViewDiffableDataSource<Int, FeedResult>
  typealias Snapshot = NSDiffableDataSourceSnapshot<Int, FeedResult>

  private var viewModel: FeedsViewModel
  private var cancellables: Set<AnyCancellable> = []
  private var feedsCV: UICollectionView = {
    let collectionView = getUICollectionView()
    collectionView.showsVerticalScrollIndicator = false
    collectionView.showsHorizontalScrollIndicator = false
    return collectionView
  }()

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
    Task { @MainActor in
      await viewModel.fetchFeeds(request: FeedRequest(filteredPosts: [], numResults: Constants.initialNumResults))
    }
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
    }.store(in: &cancellables)
  }

  func setupUI() {}
}

extension FeedsViewController {
  enum Constants {
    static let initialNumResults = 5
  }
}
