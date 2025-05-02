//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct AccountView: View {
  @StateObject var viewModel: AccountViewModel
  @State var profileInfo: AccountInfo?
  @State private var isLoadingFirstTime = true
  @State private var showLoginButton: Bool = false
  @State private var showSignupSheet: Bool = false
  @State private var showSignupFailureSheet: Bool = false
  @State private var isSigningUp = false
  @State private var isLoggingOut = false
  @EnvironmentObject var session: SessionManager

  init(viewModel: AccountViewModel) {
    _viewModel = StateObject(wrappedValue: viewModel)
  }

  var body: some View {
    ZStack {
      ScrollView {
        VStack(spacing: Constants.vStackSpacing) {
          switch viewModel.state {
          case .successfullyFetched(let info):
            UserInfoView(
              accountInfo: .constant(info),
              shouldApplySpacing: true,
              showLoginButton: $showLoginButton,
              delegate: self
            )
          default:
            UserInfoView(
              accountInfo: .constant(
                AccountInfo(
                  imageURL: URL(fileURLWithPath: ""),
                  canisterID: ""
                )
              )
              , shouldApplySpacing: true,
              showLoginButton: $showLoginButton,
              delegate: self
            )
          }
          ProfileOptionsView(showLogoutButton: $showLoginButton.inverted, delegate: self)
          ShareOptionsView()
          //        ICPBrandingView()
          Spacer().frame(height: Constants.bottomSpacing)
        }
        .padding([.top], Constants.vStackPadding)
      }
      .onReceive(session.$state) { state in
        switch state {
        case .loggedOut,
            .ephemeralAuthentication,
            .permanentAuthentication:
          self.showLoginButton = !state.isLoggedIn
          Task {
            await viewModel.fetchProfileInfo()
          }
        default: break
        }
      }
      .onReceive(viewModel.$event) { event in
        guard let event = event else { return }
        switch event {
        case .socialSignInSuccess:
          isSigningUp = false
          showSignupSheet = false
        case .socialSignInFailure:
          isSigningUp = false
          showSignupSheet = false
          showSignupFailureSheet = true
        case .logoutSuccess, .logoutFailure:
          isLoggingOut = false
        }
        viewModel.event = nil
      }
      if isLoggingOut {
        ZStack {
          Color.black.opacity(Constants.loadingStateOpacity)
            .ignoresSafeArea()

          LottieLoaderView(animationName: Constants.lottieName)
            .frame(width: Constants.loaderSize, height: Constants.loaderSize)
        }
      }
    }
    .task {
      guard isLoadingFirstTime else { return }
      isLoadingFirstTime = false
      await viewModel.fetchProfileInfo()
    }
    .fullScreenCover(isPresented: $showSignupSheet) {
      ZStack(alignment: .center) {
        SignupSheet(
          onComplete: { showSignupSheet = false },
          isSigningUp: $isSigningUp,
          delegate: self
        )
        .background( ClearBackgroundView() )
      }
    }
    .fullScreenCover(isPresented: $showSignupFailureSheet) {
      SignupFailureSheet(onComplete: {
        showSignupSheet = false
        showSignupFailureSheet = false
      })
      .background( ClearBackgroundView() )
    }
  }
}

extension AccountView: UserInfoViewProtocol {
  func loginPressed() {
    UIView.setAnimationsEnabled(false)
    showSignupSheet = true
  }
}

extension AccountView: SignupSheetProtocol {
  func signupwithGoogle() {
    isSigningUp = true
    Task {
      await viewModel.socialSignIn(request: .google)
    }
  }

  func signupwithApple() {
    isSigningUp = true
    Task {
      await viewModel.socialSignIn(request: .apple)
    }
  }
}

extension AccountView: ProfileOptionsViewDelegate {
  func login() {

  }

  func logout() {
    isLoggingOut = true
    Task {
      await viewModel.logout()
    }
  }
}

extension AccountView {
  enum Constants {
    static let vStackSpacing = 30.0
    static let vStackPadding = 30.0
    static let bottomSpacing = 40.0
    static let loadingStateOpacity = 0.4
    static let loaderSize = 24.0
    static let lottieName = "Yral_Loader"
  }
}
