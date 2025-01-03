//
//  FollowUsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct FollowUsView: View {
  var body: some View {
    VStack(spacing: 12) {
      Text("Follow us on")
        .font(LoginConstants.subtitleFont)
        .foregroundColor(LoginConstants.primaryTextColor.opacity(0.7))

      HStack(spacing: 24) {
        Button {
          // Telegram action
        } label: {
          Image(LoginConstants.telegramIconName)
            .resizable()
            .scaledToFit()
            .frame(width: 36, height: 36)
        }

        Button {
          // Discord action
        } label: {
          Image(LoginConstants.discordIconName)
            .resizable()
            .scaledToFit()
            .frame(width: 36, height: 36)
        }

        Button {
          // Twitter action
        } label: {
          Image(LoginConstants.twitterIconName)
            .resizable()
            .scaledToFit()
            .frame(width: 36, height: 36)
        }
      }
    }
  }
}
