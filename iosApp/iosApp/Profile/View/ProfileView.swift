//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileView: View {
  @State var showAccountInfo = false
  @State var profileInfo: AccountInfo?
  var uploadVideoPressed: (() -> Void) = {}
  let viewModel: ProfileViewModel

  init(viewModel: ProfileViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    ScrollView {
      VStack(spacing: Constants.vStackSpacing) {
        Text(Constants.navigationTitle)
          .font(Constants.navigationTitleFont)
          .foregroundColor(Constants.navigationTitleTextColor)
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding(Constants.navigationTitlePadding)

        if showAccountInfo {
          UserInfoView(accountInfo: $profileInfo, shouldApplySpacing: false)
        }
        Spacer(minLength: Constants.minimumTopSpacing)
        ProfileEmptyStateView {
          uploadVideoPressed()
        }
        Spacer(minLength: Constants.minimumBottomSpacing)
      }
      .padding(.horizontal, Constants.horizontalPadding)
    }
    .onReceive(viewModel.$event) { event in
      switch event {
      case .fetchedAccountInfo(let info):
        showAccountInfo = true
        profileInfo = info
      default:
        break
      }
    }
    .task {
      await viewModel.fetchProfileInfo()
    }
  }

  func onUploadAction(_ action: @escaping () -> Void) -> ProfileView {
    var copy = self
    copy.uploadVideoPressed = action
    return copy
  }
}

extension ProfileView {
  enum Constants {
    static let navigationTitle = "My Profile"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let navigationTitlePadding = EdgeInsets(
      top: 20.0,
      leading: 0.0,
      bottom: 16.0,
      trailing: 0.0
    )

    static let vStackSpacing: CGFloat = 20.0
    static let horizontalPadding: CGFloat = 16.0
    static let minimumTopSpacing: CGFloat = 16.0
    static let minimumBottomSpacing: CGFloat = 16.0
  }
}

#Preview {
  ProfileView(
    viewModel: ProfileViewModel(
      accountUseCase: AccountUseCase(
        accountRepository: AccountRepository(
          httpService: HTTPService(),
          authClient:
            DefaultAuthClient(
              networkService: HTTPService(),
              crashReporter: FirebaseCrashlyticsReporter()
            )
        ),
        crashReporter: FirebaseCrashlyticsReporter()
      )
    )
  )
}
