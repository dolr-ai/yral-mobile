//
//  WalletOverlayView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Lottie

enum WalletPhase: Equatable {
  case loading
  case success(Int64)
  case failure
  case none
}

struct WalletOverlayView: View {
  @Binding var phase: WalletPhase
  @Binding var outcome: WalletPhase
  @State private var loopKey = UUID()

  let onAllFinished: () -> Void

  var body: some View {
    Group {
      switch phase {
      case .loading:
        loadingClip
          .id(loopKey)
      case .success:
        LottieView(
          name: Constants.rechargeSuccessLottie,
          loopMode: .playOnce,
          animationSpeed: .one,
          resetProgress: false
        ) {
          end(after: CGFloat.animationPeriod)
        }
      case .failure:
        LottieView(
          name: Constants.rechargeFailureLottie,
          loopMode: .playOnce,
          animationSpeed: .one,
          resetProgress: false
        ) {
          end(after: CGFloat.three)
        }
      case .none:
        EmptyView()
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }

  private var loadingClip: some View {
    LottieView(
      name: Constants.rechargeLoadingLottie,
      loopMode: .playOnce,
      animationSpeed: CGFloat.one,
      resetProgress: false
    ) {
      if outcome == .none {
        loopKey = UUID()
      } else {
        phase = outcome
        outcome = .none
      }
    }
  }

  private func end(after delay: CGFloat) {
    DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
      withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
        phase = .none
        outcome = .none
      }
      onAllFinished()
    }
  }
}

extension WalletOverlayView {
  enum Constants {
    static let opacity = 0.8
    static let rechargeLoadingLottie = "sats_claim_loading"
    static let rechargeSuccessLottie = "sats_claim_success"
    static let rechargeFailureLottie = "sats_claim_failure"
  }
}
