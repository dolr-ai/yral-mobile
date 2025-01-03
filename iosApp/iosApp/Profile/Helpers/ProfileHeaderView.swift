//
//  ProfileHeaderView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileHeaderView: View {
  let profileImageName: String
  let userId: String

  var body: some View {
    HStack(spacing: 16) {
      Image(profileImageName)
        .resizable()
        .scaledToFill()
        .frame(width: 60, height: 60)
        .clipShape(Circle())

      Text(userId)
        .font(LoginConstants.subtitleFont)
        .foregroundColor(LoginConstants.primaryTextColor)
        .lineLimit(2) // allow wrapping if it's long
    }
    .padding(.bottom, 24)
  }
}
