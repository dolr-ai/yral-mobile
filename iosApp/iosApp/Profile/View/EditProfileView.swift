//
//  EditProfileView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 14/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct EditProfileView: View {
  @Environment(\.appDIContainer) private var appDIContainer
  @EnvironmentObject var eventBus: EventBus

  @Binding private var accountInfo: AccountInfo
  @Binding private var showEditProfile: Bool

  @State private var showConfirmationSheet = false
  @State private var username: String
  @State private var errorMessage: String = ""

  @FocusState private var isFocused: Bool

  init(accountInfo: Binding<AccountInfo>, showEditProfile: Binding<Bool>) {
    self._accountInfo = accountInfo
    self._showEditProfile = showEditProfile
    self.username = accountInfo.wrappedValue.username
  }

  var body: some View {
    VStack(spacing: .zero) {
      HStack {
        Image(Constants.backImage)
          .resizable()
          .frame(width: Constants.backImageSize, height: Constants.backImageSize)
          .onTapGesture {
            showEditProfile = false
          }

        Spacer()

        if isFocused {
          Text("Done")
            .font(YralFont.pt16.bold.swiftUIFont)
            .foregroundColor(YralColor.primary300.swiftUIColor)
            .onTapGesture {
              if isUsernameValid(username) {
                isFocused = false
                saveUsername(username)
              }
            }
        }
      }
      .overlay(
        Text("Edit Profile")
          .font(YralFont.pt20.bold.swiftUIFont)
          .foregroundColor(YralColor.grey0.swiftUIColor)
      )
      .padding(.top, 16)
      .padding(.bottom, 36)

      URLImage(url: accountInfo.imageURL)
        .frame(width: 114, height: 114)
        .clipShape(Circle())

      VStack(alignment: .leading, spacing: .zero) {
        Text("Username")
          .font(YralFont.pt14.medium.swiftUIFont)
          .foregroundColor(YralColor.grey600.swiftUIColor)
          .padding(.bottom, 8)

        HStack(alignment: .center, spacing: .zero) {
          Text("@")
            .font(YralFont.pt14.medium.swiftUIFont)
            .foregroundColor(
              isFocused ? YralColor.grey600.swiftUIColor : YralColor.grey50.swiftUIColor
            )

          TextField(" Type user name", text: $username)
            .font(YralFont.pt14.medium.swiftUIFont)
            .foregroundColor(YralColor.grey50.swiftUIColor)
            .focused($isFocused)
            .onChange(of: username) { newValue in
              validateUsername(newValue)
            }

          Spacer(minLength: 16)

          Image(isFocused ? "upload_player_close" : "copy")
            .resizable()
            .frame(width: 20, height: 20)
            .onTapGesture {
              if isFocused {
                username = ""
              } else {
                UIPasteboard.general.string = username
                HapticGenerator.performFeedback(.impact(weight: .heavy))
              }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(
          RoundedRectangle(cornerRadius: 8)
            .fill(YralColor.grey900.swiftUIColor)
        )
        .overlay(
          RoundedRectangle(cornerRadius: 8)
            .stroke(
              isFocused ? YralColor.primary300.swiftUIColor : YralColor.grey700.swiftUIColor,
              lineWidth: 1
            )
        )

        if isFocused {
          Text(
            errorMessage.isEmpty ?
            "Try creating a unique username. Use 3–15 alphabets or numbers. No spaces or special characters allowed." :
              errorMessage
          )
          .font(YralFont.pt14.regular.swiftUIFont)
          .foregroundColor(
            errorMessage.isEmpty ?
            YralColor.grey500.swiftUIColor :
              YralColor.red300.swiftUIColor
          )
          .padding(.top, 16)
        } else {
          Text("Unique ID")
            .font(YralFont.pt14.medium.swiftUIFont)
            .foregroundColor(YralColor.grey600.swiftUIColor)
            .padding(.top, 24)
            .padding(.bottom, 8)

          HStack(alignment: .top, spacing: .zero) {
            Text(accountInfo.canisterID)
              .font(YralFont.pt14.medium.swiftUIFont)
              .foregroundColor(YralColor.grey500.swiftUIColor)

            Spacer(minLength: 16)

            Image("copy")
              .resizable()
              .frame(width: 20, height: 20)
              .onTapGesture {
                UIPasteboard.general.string = accountInfo.canisterID
                HapticGenerator.performFeedback(.impact(weight: .heavy))
              }
          }
          .frame(maxWidth: .infinity)
          .padding(.vertical, 8)
          .padding(.horizontal, 10)
          .background(
            RoundedRectangle(cornerRadius: 8)
              .fill(YralColor.grey900.swiftUIColor)
          )
          .overlay(
            RoundedRectangle(cornerRadius: 8)
              .stroke(YralColor.grey700.swiftUIColor, lineWidth: 1)
          )
        }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(.top, 24)
    }
    .frame(maxHeight: .infinity, alignment: .top)
    .padding(.horizontal, 16)
    .background(
      Color.clear
        .contentShape(Rectangle())
        .onTapGesture {
          UIApplication.shared.endEditing()
          username = accountInfo.username
        }
    )
    .overlay(alignment: .center, content: {
      if showConfirmationSheet {
        Color.black.opacity(0.8)
          .ignoresSafeArea()
          .transition(.opacity)
      }
    })
    .fullScreenCover(isPresented: $showConfirmationSheet) {
      ConfirmUsernameBottomSheet(newUsername: username) {
        username = accountInfo.username
        showConfirmationSheet = false
      } onChangeName: {
        if let authClient = appDIContainer?.authClient,
           let canisterID = authClient.canisterPrincipalString,
           let identity = authClient.identityData {
          identity.withUnsafeBytes { ptr in
            if ptr.count > 0 {
              Task {
                do {
                  _ = try await set_user_metadata(
                    RustVec(bytes: ptr),
                    canisterID.intoRustString(),
                    username.intoRustString()
                  )
                  await MainActor.run {
                    authClient.updateUsername(username)
                    eventBus.updatedUsername.send(username)
                    accountInfo = AccountInfo(
                      imageURL: accountInfo.imageURL,
                      canisterID: accountInfo.canisterID,
                      username: username
                    )
                  }
                } catch {
                  await MainActor.run {
                    isFocused = true
                    if error.localizedDescription.contains("DuplicateUsername") {
                      errorMessage = "This username is already taken."
                    } else {
                      errorMessage = "There was an issue updating your username. Please try again after sometime."
                    }
                  }
                }

                showConfirmationSheet = false
              }
            }
          }
        }
      }
      .background( ClearBackgroundView() )
    }
  }

  func validateUsername(_ input: String) {
    guard input.count >= 3 && input.count <= 15 else {
      errorMessage = "Username must be between 3 and 15 characters."
      return
    }

    let allowed = CharacterSet.alphanumerics
    if input.rangeOfCharacter(from: allowed.inverted) != nil {
      errorMessage = "Username can only contain letters and numbers."
      return
    }

    errorMessage = ""
  }

  func isUsernameValid(_ input: String) -> Bool {
    input.count >= 3 &&
    input.count <= 15 &&
    input.rangeOfCharacter(from: CharacterSet.alphanumerics.inverted) == nil
  }

  func saveUsername(_ input: String) {
    if input != accountInfo.username {
      showConfirmationSheet = true
    }
  }
}

extension EditProfileView {
  enum Constants {
    static let backImage = "chevron-left"
    static let backImageSize = 24.0
  }
}
