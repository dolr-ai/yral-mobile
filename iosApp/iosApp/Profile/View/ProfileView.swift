//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileView: View {
  @ObservedObject var viewModel: ProfileViewModel
  @State var profileInfo: ProfileInfo?

  init(viewModel: ProfileViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    ScrollView {
      VStack(spacing: Constants.vStackSpacing) {
        switch viewModel.state {
        case .successfullyFetched(let info):
          UserInfoView(profileInfo: .constant(info))
        default:
          UserInfoView(profileInfo: .constant(ProfileInfo(imageURL: URL(fileURLWithPath: ""), canisterID: "")))
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
  ProfileView(
    viewModel: ProfileViewModel(
      useCase: ProfileUseCase(
        profileRepository: ProfileRepository(httpService: HTTPService(),
                                             authClient:
                                              DefaultAuthClient(
                                                networkService: HTTPService()
                                              )
                                            )
      )
    )
  )
}

extension ProfileView {
  enum Constants {
    static let vStackSpacing = 30.0
    static let vStackPadding = 30.0
    static let bottomSpacing = 40.0
  }
}
