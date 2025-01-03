//
//  PoweredByView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct PoweredByView: View {
  var body: some View {
    HStack {
      Image(LoginConstants.icpLogoName)
        .resizable()
        .scaledToFit()
        .frame(height: 40)

      VStack(alignment: .leading, spacing: 4) {
        Text("Powered by")
          .font(LoginConstants.subtitleFont)
          .foregroundColor(LoginConstants.primaryTextColor.opacity(0.7))

        Text("INTERNET COMPUTER")
          .font(LoginConstants.titleFont)
          .foregroundColor(LoginConstants.primaryTextColor)
      }
    }
    .padding()
    .background(Color.gray.opacity(0.2))
    .cornerRadius(8)
  }
}
