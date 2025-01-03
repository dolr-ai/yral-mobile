//
//  InfoTextView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct InfoTextView: View {
  let text: String

  var body: some View {
    Text(text)
      .font(LoginConstants.subtitleFont)
      .foregroundColor(LoginConstants.primaryTextColor.opacity(0.8))
      .multilineTextAlignment(.center)
      .padding(.horizontal, 16)
  }
}
