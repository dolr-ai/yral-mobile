//
//  MenuRowView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct MenuRowView: View {
  let iconName: String
  let title: String
  let action: () -> Void

  var body: some View {
    Button(action: action) {
      HStack {
        Image(iconName)
          .resizable()
          .scaledToFit()
          .frame(width: 24, height: 24)

        Text(title)
          .font(LoginConstants.titleFont)
          .foregroundColor(LoginConstants.primaryTextColor)

        Spacer()

        Image(systemName: "chevron.right")
          .foregroundColor(LoginConstants.primaryTextColor)
      }
      .padding(.vertical, 8)
    }
  }
}
