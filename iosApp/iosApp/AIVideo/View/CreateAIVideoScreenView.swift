//
//  CreateAIVideoScreenView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct CreateAIVideoScreenView: View {
  @EnvironmentObject var session: SessionManager
  @ObservedObject var viewModel: CreateAIVideoViewModel

  let onDismiss: () -> Void
  var isButtonEnabled: Bool {
    let trimmed = promptText.trimmingCharacters(in: .whitespacesAndNewlines)
    return !creditsUsed && !trimmed.isEmpty
  }

  var isErrorPresented: Binding<Bool> {
    Binding(
      get: { errorMessage != nil },
      set: { newValue in
        if newValue == false {
          errorMessage = nil
        }
      }
    )
  }

  @State private var showLoader = true
  @State private var promptText = ""
  @State private var creditsUsed = false
  @State private var showProviderBottomSheet = false
  @State private var showSignupSheet = false
  @State private var showSignupFailureSheet = false
  @State private var isUserLoggedIn = false
  @State private var loadingProvider: SocialProvider?
  @State private var selectedProvider: AIVideoProviderResponse?
  @State private var errorMessage: String?

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      if let providers = viewModel.providers {
        HStack(alignment: .center, spacing: Constants.navHstackSpacing) {
          Image(Constants.backImage)
            .resizable()
            .frame(width: Constants.backImageSize, height: Constants.backImageSize)
            .onTapGesture {
              onDismiss()
            }

          Text(Constants.screenTitle)
            .font(Constants.screenTitleFont)
            .foregroundColor(Constants.screenTitleColor)
        }
        .padding(.bottom, Constants.navHstackBottom)
        .padding(.leading, -Constants.navHstackLeading)

        if let selectedProvider = selectedProvider {
          buildSelectedModelView(with: selectedProvider)
            .frame(maxWidth: .infinity)
        }

        PromptView(prompt: $promptText)
          .padding(.top, Constants.promptTop)

        Button {
          if isUserLoggedIn {
            if let provider = selectedProvider, !promptText.isEmpty {
              Task {
                await viewModel.generateVideo(for: promptText, withProvider: provider)
              }
            }
          } else {
            showSignupSheet = true
          }
        } label: {
          Text(Constants.generateButtonTitle)
            .foregroundColor(Constants.generateButtonTextColor)
            .font(Constants.generateButtonFont)
            .frame(maxWidth: .infinity)
            .frame(height: Constants.generateButtonHeight)
            .background(
              isButtonEnabled
              ? Constants.generateButtonEnabledGradient
              : Constants.generateButtonDisabledGradient
            )
            .cornerRadius(Constants.generateButtonCornerRadius)
        }
        .disabled(!isButtonEnabled)
        .padding(.top, Constants.generateButtonTop)
      }
    }
    .navigationBarHidden(true)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .padding(.horizontal, Constants.vstackHorizontal)
    .overlay(alignment: .center, content: {
      if showLoader {
        LottieLoaderView(animationName: Constants.loader)
          .frame(width: Constants.loaderSize, height: Constants.loaderSize)
      }
    })
    .fullScreenCover(isPresented: $showProviderBottomSheet) {
      if let providers = viewModel.providers {
        ProviderOptionsBottomSheetView(
          providers: providers) { newSelectedProvider in
            viewModel.updateSelectedProvider(newSelectedProvider)
          } onDismiss: {
            showProviderBottomSheet = false
          }
          .background(ClearBackgroundView())
      }
    }
    .fullScreenCover(isPresented: $showSignupSheet) {
      ZStack(alignment: .center) {
        SignupSheet(
          onComplete: { showSignupSheet = false },
          loadingProvider: $loadingProvider,
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
    .fullScreenCover(isPresented: isErrorPresented) {
      CreateAIVideoErrorBottomSheet(text: errorMessage ?? "") {
        errorMessage = nil
      }
      .background( ClearBackgroundView() )
    }
    .onReceive(session.phasePublisher, perform: { phase in
      if case .permanent = phase {
        isUserLoggedIn = true
      }
    })
    .onReceive(viewModel.$state, perform: { state in
      switch state {
      case .loading:
        showLoader = true
      case .success:
        showLoader = false
      default:
        showLoader = false
      }
    })
    .onReceive(viewModel.$event, perform: { event in
      switch event {
      case .updateSelectedProvider(let provider):
        selectedProvider = provider
      case .socialSignInSuccess:
        loadingProvider = nil
        showSignupSheet = false
      case .socialSignInFailure:
        loadingProvider = nil
        showSignupSheet = false
        showSignupFailureSheet = true
      case .generateVideoSuccess(let response):
        break
      case .generateVideoFailure(let errMessage):
        errorMessage = errMessage
      default:
        break
      }

      DispatchQueue.main.async {
        viewModel.event = nil
      }
    })
    .onAppear {
      showLoader = true
      Task {
        await viewModel.getAIVideoProviders()
      }
    }
  }
}

extension CreateAIVideoScreenView {
  @ViewBuilder
  func buildSelectedModelView(with provider: AIVideoProviderResponse) -> some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      Text(Constants.modelTitle)
        .font(Constants.modelFont)
        .foregroundColor(Constants.modelColor)

      HStack {
        URLImage(url: URL(string: provider.iconURL))
          .id(provider.id)
          .frame(width: Constants.modelImageSize, height: Constants.modelImageSize)
          .padding(.horizontal, Constants.modelNameLeading)

        VStack(alignment: .leading) {
          Text(provider.name)
            .font(Constants.modelNameFont)
            .foregroundColor(Constants.modelNameColor)

          Text(provider.description)
            .font(Constants.modelDescriptionFont)
            .foregroundColor(Constants.modelDescriptionColor)
            .lineLimit(.one)
        }

        Spacer(minLength: Constants.chevronLeading)

        Image(Constants.chevronDown)
          .resizable()
          .frame(width: Constants.chevronSize, height: Constants.chevronSize)
          .padding(.trailing, Constants.chevronTrailing)
      }
      .frame(maxWidth: .infinity)
      .frame(height: Constants.modelHstackHeight)
      .background(Constants.modelHstackBackground)
      .cornerRadius(Constants.modelHstackCornerRadius)
      .overlay(
        RoundedRectangle(cornerRadius: Constants.modelHstackCornerRadius)
          .stroke(Constants.modelHstackBorderColor, lineWidth: .one)
      )
      .onTapGesture {
        showProviderBottomSheet = true
      }

      Text(Constants.creditsUsed)
        .font(Constants.creditsUsedFont)
        .foregroundColor(creditsUsed ? Constants.creditsUsedRedColor : Constants.creditsUsedGreenColor)
    }
  }
}

extension CreateAIVideoScreenView: SignupSheetProtocol {
  func signupwithGoogle() {
    loadingProvider = .google
    Task {
      await viewModel.socialSignIn(request: .google)
    }
  }

  func signupwithApple() {
    loadingProvider = .apple
    Task {
      await viewModel.socialSignIn(request: .apple)
    }
  }
}

extension CreateAIVideoScreenView {
  enum Constants {
    static let loader = "Yral_Loader"
    static let loaderSize = 24.0

    static let vstackSpacing = 8.0
    static let vstackHorizontal = 16.0

    static let navHstackSpacing = 12.0
    static let navHstackBottom = 24.0
    static let navHstackLeading = 4.0
    static let backImage = "chevron-left"
    static let backImageSize = 24.0
    static let screenTitle = "Create AI Video"
    static let screenTitleFont = YralFont.pt20.bold.swiftUIFont
    static let screenTitleColor = YralColor.grey0.swiftUIColor

    static let modelTitle = "Model"
    static let modelFont = YralFont.pt14.medium.swiftUIFont
    static let modelColor = YralColor.grey300.swiftUIColor
    static let modelHstackHeight = 58.0
    static let modelHstackBackground = YralColor.grey900.swiftUIColor
    static let modelHstackCornerRadius = 8.0
    static let modelHstackBorderColor = YralColor.grey700.swiftUIColor
    static let modelImageLeading = 12.0
    static let modelImageSize = 30.0
    static let modelNameLeading = 10.0
    static let modelNameFont = YralFont.pt14.regular.swiftUIFont
    static let modelNameColor = YralColor.grey50.swiftUIColor
    static let modelDescriptionFont = YralFont.pt12.swiftUIFont
    static let modelDescriptionColor = YralColor.grey400.swiftUIColor
    static let chevronDown = "chevron_down"
    static let chevronLeading = 28.0
    static let chevronSize = 24.0
    static let chevronTrailing = 12.0
    static let creditsUsed = "0 of 1 credits used"
    static let creditsUsedFont = YralFont.pt14.semiBold.swiftUIFont
    static let creditsUsedRedColor = YralColor.red300.swiftUIColor
    static let creditsUsedGreenColor = YralColor.green300.swiftUIColor

    static let promptTop = 12.0
    static let promptHeight = 150.0

    static let generateButtonTitle = "Generate video"
    static let generateButtonTextColor = YralColor.grey50.swiftUIColor
    static let generateButtonFont = YralFont.pt16.bold.swiftUIFont
    static let generateButtonHeight = 45.0
    static let generateButtonCornerRadius = 8.0
    static let generateButtonTop = 16.0

    static let generateButtonEnabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let generateButtonDisabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 0.87, green: 0.6, blue: 0.75), location: 0.00),
        Gradient.Stop(color: Color(red: 0.77, green: 0.36, blue: 0.58), location: 0.33),
        Gradient.Stop(color: Color(red: 0.51, green: 0.33, blue: 0.43), location: 1.00)
      ],
      startPoint: UnitPoint(x: 1, y: 0.51),
      endPoint: UnitPoint(x: 0.03, y: 1)
    )

  }
}
