//
//  CreateAIVideoScreenView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

// swiftlint: disable file_length
struct CreateAIVideoHost: View {
  private let onDismiss: () -> Void
  @StateObject private var viewModel: CreateAIVideoViewModel

  init(createAIVideoDI: CreateAIVideoDIContainer, onDismiss: @escaping () -> Void) {
    self.onDismiss = onDismiss
    _viewModel = StateObject(wrappedValue: createAIVideoDI.makeAIVideoViewModel())
  }

  var body: some View {
    CreateAIVideoScreenView(viewModel: viewModel, onDismiss: onDismiss)
  }
}

// swiftlint: disable type_body_length
struct CreateAIVideoScreenView: View {
  @EnvironmentObject var session: SessionManager
  @EnvironmentObject var eventBus: EventBus
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

  var isCompletionViewPresented: Binding<Bool> {
    Binding(
      get: { videoURL != nil },
      set: { newValue in
        if newValue == false {
          videoURL = nil
        }
      }
    )
  }

  private let timer = Timer.publish(every: Constants.loadingMessageTime, on: .main, in: .common).autoconnect()

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

  @State private var generatingVideo = false
  @State private var generatingVideoTextCurrentIndex: Int = .zero
  @State private var showDisableBackBottomSheet = false
  @State private var videoURL: URL?

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      HStack(alignment: .center, spacing: Constants.navHstackSpacing) {
        Image(Constants.backImage)
          .resizable()
          .frame(width: Constants.backImageSize, height: Constants.backImageSize)
          .onTapGesture {
            if generatingVideo {
              showDisableBackBottomSheet = true
            } else {
              onDismiss()
            }
          }

        Text(Constants.screenTitle)
          .font(Constants.screenTitleFont)
          .foregroundColor(Constants.screenTitleColor)
      }
      .padding(.bottom, Constants.navHstackBottom)
      .padding(.leading, -Constants.navHstackLeading)
      .padding(.top, Constants.navHstackTop)

      if let provider = selectedProvider, generatingVideo {
        Text(promptText)
          .font(Constants.genPromptFont)
          .foregroundColor(Constants.genPromptColor)
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding(.vertical, Constants.genPromptVertical)
          .padding(.horizontal, Constants.genPromptHorizontal)
          .background(Constants.genPromptBackground)
          .cornerRadius(Constants.genPromptCornerRadius)
          .overlay(
            RoundedRectangle(cornerRadius: Constants.genPromptCornerRadius)
              .stroke(Constants.genPromptBorderColor, lineWidth: .one)
          )

        HStack(spacing: Constants.genHstackSpacing) {
          URLImage(url: URL(string: provider.iconURL))
            .id(provider.id)
            .frame(width: Constants.genImageSize, height: Constants.genImageSize)

          Text(provider.name)
            .font(Constants.genNameFont)
            .foregroundColor(Constants.genNameColor)
        }

        RoundedRectangle(cornerRadius: Constants.genViewCornerRadius)
          .fill(Constants.genViewBackground)
          .frame(maxWidth: .infinity, maxHeight: .infinity)
          .padding(.bottom, Constants.genViewBottom)
          .overlay(alignment: .center) {
            VStack(spacing: Constants.genViewVstackSpacing) {
              LottieLoaderView(animationName: Constants.loader)
                .frame(width: Constants.loaderSize, height: Constants.loaderSize)

              Text(Constants.loadingMessages[generatingVideoTextCurrentIndex])
                .font(Constants.genViewTextFont)
                .foregroundColor(Constants.genViewTextColor)
            }
            .offset(y: -Constants.genViewBottom/2)
          }
          .onReceive(timer) { _ in
            withAnimation(.easeInOut) {
              generatingVideoTextCurrentIndex = (
                generatingVideoTextCurrentIndex + .one
              ) % Constants.loadingMessages.count
            }
          }
      } else {
        if let providers = viewModel.providers {
          if let selectedProvider = selectedProvider {
            buildSelectedModelView(with: selectedProvider)
              .frame(maxWidth: .infinity)
          }

          PromptView(prompt: $promptText)
            .padding(.top, Constants.promptTop)

          Button {
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: CreateAIVideoClickedData(model: selectedProvider?.name ?? "")
            )
            if isUserLoggedIn {
              if let provider = selectedProvider, !promptText.isEmpty {
                Task {
                  await viewModel.generateVideo(for: promptText, withProvider: provider)
                }
              }
            } else {
              AnalyticsModuleKt.getAnalyticsManager().trackEvent(
                event: AuthScreenViewedEventData(pageName: .videoCreation)
              )
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
    }
    .navigationBarHidden(true)
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .background(
      Color.clear
        .contentShape(Rectangle())
        .hideKeyboardOnTap()
    )
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
        if generatingVideo {
          onDismiss()
        }
      }
      .background( ClearBackgroundView() )
    }
    .fullScreenCover(isPresented: $showDisableBackBottomSheet) {
      DisableBackBottomSheet {
        showDisableBackBottomSheet = false
        viewModel.stopPolling()
        onDismiss()
      } onStayHere: {
        showDisableBackBottomSheet = false
      }
      .background( ClearBackgroundView() )
    }
    .fullScreenCover(isPresented: isCompletionViewPresented) {
      if let url = videoURL {
        AIVideoCompletedView(
          videoURL: url,
          videoAspectRatio: selectedProvider?.defaultAspectRatio ?? ""
        ) {
          videoURL = nil
          generatingVideo = false
          onDismiss()
          eventBus.finishUploadingVideo.send(())
        }
      }
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
      case .socialSignInSuccess(let creditsAvailable):
        loadingProvider = nil
        showSignupSheet = false
        creditsUsed = !creditsAvailable
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: VideoCreationPageViewedEventData(
            type: .aiVideo,
            creditsFetched: true,
            creditsAvailable: creditsAvailable ? 1 : 0
          )
        )
      case .socialSignInFailure:
        if let authJourney = loadingProvider?.authJourney() {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: AuthFailedEventData(authJourney: authJourney)
          )
        }
        loadingProvider = nil
        showSignupSheet = false
        showSignupFailureSheet = true
      case .generateVideoSuccess:
        generatingVideo = true
        viewModel.startPolling()
      case .generateVideoFailure(let errMessage):
        errorMessage = errMessage
      case .generateVideoStatusFailure(let errMessage):
        errorMessage = errMessage
      case .uploadAIVideoSuccess(let videoURLString):
        videoURL = URL(string: videoURLString)
      case .uploadAIVideoFailure(let errMessage):
        errorMessage = errMessage
      default:
        break
      }
    })
    .onAppear {
      if !generatingVideo {
        showLoader = true
        Task {
          await viewModel.getAIVideoProviders()
          if isUserLoggedIn {
            let availableCredits = await viewModel.creditsAvailable()
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: VideoCreationPageViewedEventData(
                type: .aiVideo,
                creditsFetched: true,
                creditsAvailable: availableCredits ? 1 : 0
              )
            )
            await MainActor.run {
              creditsUsed = !availableCredits
            }
          } else {
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: VideoCreationPageViewedEventData(
                type: .aiVideo,
                creditsFetched: false,
                creditsAvailable: 1
              )
            )
            await MainActor.run {
              creditsUsed = false
            }
          }
        }
      }
    }
    .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
      if generatingVideo {
        viewModel.stopPolling(removeKey: false)
      }
    }
    .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
      if generatingVideo {
        viewModel.startPolling()
      }
    }
  }
}
// swiftlint: enable type_body_length

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

      Text(creditsUsed ? Constants.creditsUsed : Constants.creditsNotUsed)
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
    static let navHstackTop = 20.0
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
    static let creditsNotUsed = "0 of 1 credits used"
    static let creditsUsed = "1 of 1 credits used"
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

    static let loadingMessages = [
      "Generating Video...",
      "This may take few minutes"
    ]
    static let loadingMessageTime = 5.0

    static let genPromptFont = YralFont.pt14.regular.swiftUIFont
    static let genPromptColor = YralColor.grey50.swiftUIColor
    static let genPromptBackground = YralColor.grey900.swiftUIColor
    static let genPromptBorderColor = YralColor.grey700.swiftUIColor
    static let genPromptVertical = 10.0
    static let genPromptHorizontal = 12.0
    static let genPromptCornerRadius = 8.0

    static let genHstackSpacing = 12.0
    static let genImageSize = 20.0
    static let genNameFont = YralFont.pt12.regular.swiftUIFont
    static let genNameColor = YralColor.grey50.swiftUIColor

    static let genViewBackground = YralColor.grey900.swiftUIColor
    static let genViewCornerRadius = 8.0
    static let genViewBottom = 80.0
    static let genViewVstackSpacing = 12.0
    static let genViewTextFont = YralFont.pt14.regular.swiftUIFont
    static let genViewTextColor = YralColor.grey50.swiftUIColor
  }
}

// swiftlint: enable file_length
