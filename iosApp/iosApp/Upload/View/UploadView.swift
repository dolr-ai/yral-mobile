//
//  UploadView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UploadView: View {
  @State private var caption: String = ""
  @State private var hashtags = [String]()

  init() {
    // swiftlint:disable unavailable_condition
    if #available(iOS 16.0, *) {
      // do nothing, we can use scrollContentBackground
    } else {
      UITextView.appearance().backgroundColor = .clear
    }
    // swiftlint:enable unavailable_condition
  }

  var body: some View {
    ScrollView {
      VStack(spacing: Constants.verticalSpacing) {
        SelectFileView()
        CaptionsView(caption: $caption)
        HashtagView(hashtags: $hashtags)
        Button { }
        label: {
          Text(Constants.uploadButtonTitle)
            .foregroundColor(Constants.uploadButtonTextColor)
            .font(Constants.uploadButtonFont)
            .frame(maxWidth: .infinity, minHeight: Constants.uploadButtonHeight)
            .background(Constants.uploadButtonGradient)
            .cornerRadius(Constants.uploadButtonRadius)
        }
      }
      .padding([.horizontal], Constants.horizontalPadding)
    }
  }
}

extension UploadView {
  enum Constants {
    static let navigationTitle = "Upload Video"
    static let verticalSpacing = 20.0
    static let horizontalPadding = 16.0
    static let uploadButtonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let uploadButtonTitle = "Upload"
    static let uploadButtonTextColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let uploadButtonFont = Font.custom("Kumbh Sans", size: 16)
      .weight(.bold)
    static let uploadButtonHeight = 45.0
    static let uploadButtonRadius = 8.0
  }
}

struct UploadVideoView_Previews: PreviewProvider {
  static var previews: some View {
    UploadView()
  }
}
