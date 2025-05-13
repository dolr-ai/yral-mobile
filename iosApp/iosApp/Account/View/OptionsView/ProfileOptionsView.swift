//
//  ProfileOptionsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileOptionsView: View {
  @State private var selectedOption: ProfileOptionsView.Options?
  @State private var isShowingLoader = false
  @Binding var showLogoutButton: Bool
  var delegate: ProfileOptionsViewDelegate?

  init(showLogoutButton: Binding<Bool>, delegate: ProfileOptionsViewDelegate? = nil) {
    self._showLogoutButton = showLogoutButton
    self.delegate = delegate
  }

  var body: some View {
    ZStack {
      VStack(spacing: Constants.vStackSpacing) {
        ForEach(
          Constants.options.filter { option in
            guard !showLogoutButton else { return true }
            return option.id != Constants.logoutId
            && option.id != Constants.deleteId
          }
        ) { option in
          Button {
            if option.id == Constants.logoutId {
              delegate?.logout()
            } else if option.id == Constants.deleteId {
              delegate?.delete()
            } else {
              isShowingLoader = true
              selectedOption = option
            }
          } label: {
            HStack(spacing: Constants.hStackSpacing) {
              option.image
                .frame(width: Constants.iconSize, height: Constants.iconSize)
              Text(option.text)
                .font(Constants.font)
                .foregroundColor(Constants.textColor)
              Spacer()
              Image(systemName: "chevron.right")
                .foregroundColor(.white)
            }
            .padding(.horizontal, Constants.hStackPadding)
          }
        }
        .sheet(item: $selectedOption) { option in
          NavigationView {
            ProfileOptionsWebView(url: URL(string: option.redirection) ?? URL(fileURLWithPath: ""))
              .navigationBarTitleDisplayMode(.inline)
              .navigationTitle(option.text)
              .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                  Button(Constants.closeButtonTitle) {
                    isShowingLoader = false
                    selectedOption = nil
                  }
                }
              }
          }
          .interactiveDismissDisabled(true)
        }
      }
      if isShowingLoader {
        LottieLoaderView(animationName: Constants.loaderName)
          .padding()
          .background(Color.clear)
          .cornerRadius(Constants.progressViewCornerRadius)
          .frame(width: Constants.progressViewSize, height: Constants.progressViewSize, alignment: .center)
      }
    }
    .onAppear {
      isShowingLoader = false
    }
  }

  struct Options: Identifiable {
    var id: String { redirection }
    let image: Image
    let text: String
    let redirection: String
  }
}

extension ProfileOptionsView {
  enum Constants {
    static let options = [
      Options(
        image: Image("option_chat"),
        text: "Talk to the Team",
        redirection: "https://t.me/+c-LTX0Cp-ENmMzI1"
      ),
      Options(
        image: Image("option_tnc"),
        text: "Terms of service",
        redirection: "https://yral.com/terms-ios"
      ),
      Options(
        image: Image("option_privacy"),
        text: "Privacy Policy",
        redirection: "https://yral.com/privacy-policy"
      ),
      Options(
        image: Image("option_logout"),
        text: "Log Out",
        redirection: Constants.logoutId
      ),
      Options(
        image: Image("option_delete"),
        text: "Delete Account",
        redirection: Constants.deleteId
      )
    ]

    static let hStackSpacing: CGFloat = 16
    static let hStackPadding: CGFloat = 16
    static let iconSize: CGFloat = 24
    static let font = YralFont.pt16.medium.swiftUIFont
    static let textColor = YralColor.grey0.swiftUIColor
    static let closeButtonTitle = "Close"
    static let progressViewSize = 16.0
    static let progressViewCornerRadius = 8.0
    static let vStackSpacing = 30.0
    static let loaderName = "Yral_Loader"
    static let logoutId = "logoutId"
    static let deleteId = "deleteId"
  }
}

// swiftlint: disable class_delegate_protocol
protocol ProfileOptionsViewDelegate: Any {
  func login()
  func logout()
  func delete()
}
// swiftlint: enable class_delegate_protocol
