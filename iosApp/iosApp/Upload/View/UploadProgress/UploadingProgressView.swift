//
//  UploadingProgressView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit

struct UploadProgressView: View {
  @State private var progressValue: Double = 0.20
  let videoURL: URL

  var body: some View {
    VStack(spacing: Constants.vStackSpacing) {
      Text(Constants.uploadingMessage)
        .foregroundColor(Constants.uploadingMessageColor)
        .font(Constants.uploadingMessageFont)
        .multilineTextAlignment(.leading)
        .padding(Constants.verticalPadding)

      VStack(alignment: .leading, spacing: Constants.progressVStackSpacing) {
        ProgressView(value: progressValue, total: CGFloat.one)
          .progressViewStyle(GradientProgressViewStyle(
          gradient: Constants.progressBarGradient,
           cornerRadius: Constants.progressBarCornerRadius,
            height: Constants.progressBarHeight
            )
            )
          .frame(height: Constants.progressBarHeight)
        Text("\(Constants.uploadingText) \(Int(progressValue * 100))%")
          .foregroundColor(Constants.uploadingTextColor)
          .font(Constants.uploadingTextFont)
      }
    }
    .frame(maxWidth: .infinity)
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

extension UploadProgressView {
  enum Constants {
    static let vStackSpacing = 16.0
    static let verticalPadding = EdgeInsets(top: 22.0, leading: .zero, bottom: .zero, trailing: .zero)
    static let progressVStackSpacing = 10.0
    // swiftlint: disable line_length
    static let uploadingMessage = "Uploading may take a moment. Feel free to explore more videos on the home page while you wait!"
    // swiftlint: enable line_length
    static let uploadingMessageColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let uploadingMessageFont = Font.custom("Kumbh Sans", size: 12)
    static let uploadingText = "Uploading…"
    static let uploadingTextColor =  Color(red: 0.64, green: 0.64, blue: 0.64)
    static let uploadingTextFont =  Font.custom("Kumbh Sans", size: 12)

    static let progressBarGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 0.93, green: 0.33, blue: 0.65), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 1.00)
      ],
      startPoint: .leading,
      endPoint: .trailing
    )
    static let progressBarHeight = 10.0
    static let progressBarCornerRadius = 5.0
  }
}
