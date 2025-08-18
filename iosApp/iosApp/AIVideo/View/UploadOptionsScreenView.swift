//
//  UploadOptionsScreenView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 12/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UploadOptionsScreenView: View {
  @State private var navigateToAIVideoScreen = false
  @State private var navigateToUploadVideoScreen = false

  var body: some View {
    NavigationView {
      VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
        Text(Constants.screenTitle)
          .font(Constants.screenTitleFont)
          .foregroundColor(Constants.screenTitleColor)
          .padding(.bottom, Constants.screenTitleBottom)

        UploadOptionView(option: Constants.uploadOptionAI) {
          navigateToAIVideoScreen = true
        }
        .background(
          NavigationLink("", destination: makeCreateAIVideoDIContainer().makeCreateAIVideoSreenView {
            navigateToAIVideoScreen = false
          }, isActive: $navigateToAIVideoScreen)
            .hidden()
        )

        UploadOptionView(option: Constants.uploadOptionDevice) {
          navigateToUploadVideoScreen = true
        }
        .background(
          NavigationLink("", destination: makeCreateAIVideoDIContainer().makeCreateAIVideoSreenView {
            navigateToUploadVideoScreen = false
          }, isActive: $navigateToUploadVideoScreen)
            .hidden()
        )

        Spacer(minLength: .zero)
      }
      .padding(.vertical, Constants.vstackVertical)
      .padding(.horizontal, Constants.vstackHorizontal)
      .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
  }
}

extension UploadOptionsScreenView {
  private func makeCreateAIVideoDIContainer() -> CreateAIVideoDIContainer {
    return CreateAIVideoDIContainer(
      dependencies: CreateAIVideoDIContainer.Dependencies(
        httpService: HTTPService(baseURLString: AppConfiguration().offchainBaseURLString),
        crashReporter: CompositeCrashReporter(reporters: [FirebaseCrashlyticsReporter()])
      )
    )
  }
}

extension UploadOptionsScreenView {
  enum Constants {
    static let vstackSpacing = 20.0
    static let vstackVertical = 8.0
    static let vstackHorizontal = 16.0

    static let screenTitleBottom = 8.0
    static let screenTitle = "Add Video"
    static let screenTitleFont = YralFont.pt20.bold.swiftUIFont
    static let screenTitleColor = YralColor.grey50.swiftUIColor

    static let uploadOptionAI = UploadOption(
      id: "ai",
      imageName: "upload_option_ai",
      title: "Create AI Video",
      subtitle: "Generate a video by giving a prompt to AI.",
      ctaText: "Create with AI"
    )

    static let uploadOptionDevice = UploadOption(
      id: "device",
      imageName: "upload_option_device",
      title: "Upload a Video",
      subtitle: "Add a video from your device.",
      ctaText: "Upload Video"
    )
  }
}
