//
//  CreateAIVideoScreenView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct CreateAIVideoScreenView: View {
  let onDismiss: () -> Void
  var isButtonEnabled: Bool {
    !creditsUsed && !promptText.isEmpty
  }

  @State private var promptText = ""
  @State private var creditsUsed = false
  @State private var showModelBottomSheet = false
  @State private var selectedModel = ModelOption(
    id: "veo3",
    name: "Veo 3",
    description: "Google's advanced video generation model",
    isActive: true,
    iconURL: "https://yral.com/img/ai-models/veo3.svg"
  )

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
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

      buildSelectedModelView()

      PromptView(prompt: $promptText)
        .padding(.top, Constants.promptTop)

      Button {

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
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    .padding(.horizontal, Constants.vstackHorizontal)
    .fullScreenCover(isPresented: $showModelBottomSheet) {}
  }
}

extension CreateAIVideoScreenView {
  @ViewBuilder
  func buildSelectedModelView() -> some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      Text(Constants.modelTitle)
        .font(Constants.modelFont)
        .foregroundColor(Constants.modelColor)

      HStack {
        Spacer(minLength: Constants.modelNameLeading)

        URLImage(url: URL(string: selectedModel.iconURL))
          .frame(width: Constants.modelImageSize, height: Constants.modelImageSize)

        Spacer(minLength: Constants.modelNameLeading)

        VStack(alignment: .leading) {
          Text(selectedModel.name)
            .font(Constants.modelNameFont)
            .foregroundColor(Constants.modelNameColor)

          Text(selectedModel.description)
            .font(Constants.modelDescriptionFont)
            .foregroundColor(Constants.modelDescriptionColor)
        }

        Spacer(minLength: Constants.chevronLeading)

        Image(Constants.chevronDown)
          .resizable()
          .frame(width: Constants.chevronSize, height: Constants.chevronSize)

        Spacer(minLength: Constants.chevronTrailing)
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
        showModelBottomSheet = true
      }

      Text(Constants.creditsUsed)
        .font(Constants.creditsUsedFont)
        .foregroundColor(creditsUsed ? Constants.creditsUsedRedColor : Constants.creditsUsedGreenColor)
    }
  }
}

extension CreateAIVideoScreenView {
  enum Constants {
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

#Preview {
  CreateAIVideoScreenView {}
}
