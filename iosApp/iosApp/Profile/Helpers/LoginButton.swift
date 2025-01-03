//
//  LoginButton.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct LoginButton: View {
  let title: String

  var body: some View {
    // swiftlint: disable multiple_closures_with_trailing_closure
    Button(action: {}) {
      Text(title)
        .font(LoginConstants.buttonFont)
        .foregroundColor(.white)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(LoginConstants.buttonColor)
        .cornerRadius(8)
    }
    // swiftlint: enable multiple_closures_with_trailing_closure
  }
}
