//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct AccountView: View {
  @ObservedObject var viewModel: AccountViewModel
  @State var profileInfo: AccountInfo?

  init(viewModel: AccountViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    ScrollView {
      VStack(spacing: Constants.vStackSpacing) {
        switch viewModel.state {
        case .successfullyFetched(let info):
          UserInfoView(accountInfo: .constant(info))
        default:
          UserInfoView(accountInfo: .constant(AccountInfo(imageURL: URL(fileURLWithPath: ""), canisterID: "")))
        }
        ProfileOptionsView()
        ShareOptionsView()
        ICPBrandingView()
        Spacer().frame(height: Constants.bottomSpacing)
      }
      .padding([.top], Constants.vStackPadding)
    }
    .task {
      await viewModel.fetchProfileInfo()
    }
  }
}

#Preview {
  AccountView(
    viewModel: AccountViewModel(
      useCase: AccountUseCase(
        profileRepository: AccountRepository(httpService: HTTPService(),
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

extension AccountView {
  enum Constants {
    static let vStackSpacing = 30.0
    static let vStackPadding = 30.0
    static let bottomSpacing = 40.0
  }
}
