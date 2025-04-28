//
//  SignupSheet.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SignupSheet: View {
  let onComplete: () -> Void
  @State private var showCard = false
  @State private var dragOffset: CGFloat = .zero
  var delegate: SignupSheetProtocol?

  var body: some View {
    GeometryReader { geo in
      ZStack(alignment: .top) {
        Color.black.opacity(Constants.dimmedBackgroundOpacity)
          .ignoresSafeArea()
          .onTapGesture { dismiss() }
          .transition(.opacity)

        if showCard {
          ZStack(alignment: .top) {
            Constants.cardBackgroundColor
              .cornerRadius(Constants.cardCornerRadius)
            SignupView(delegate: self)
              .padding(.top, Constants.topPadding)
          }
          .frame(maxWidth: .infinity, alignment: .bottom)
          .frame(height: geo.size.height * Constants.screenRatio, alignment: .bottom)
          .frame(maxHeight: .infinity, alignment: .bottom)
          .padding(.bottom, -geo.safeAreaInsets.bottom)
          .shadow(radius: Constants.cardShadowRadius)
          .offset(y: dragOffset)
          .gesture(
            DragGesture()
              .onChanged { value in
                dragOffset = max(value.translation.height, 0)
              }
              .onEnded { value in
                if value.translation.height > 100 {
                  dismiss()
                } else {
                  withAnimation(.easeInOut(duration: CGFloat.pointOne)) {
                    dragOffset = .zero
                  }
                }
              }
          )
          .transition(.move(edge: .bottom))
        }
      }
      .onAppear { withAnimation(Constants.appearAnimation) { showCard = true } }
      .onDisappear { UIView.setAnimationsEnabled(true) }
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showCard = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      onComplete()
    }
  }
}

extension SignupSheet: SignupViewProtocol {
  func signupwithGoogle() {
    delegate?.signupwithGoogle()
  }

  func signupwithApple() {
    delegate?.signupwithApple()
  }
}

extension SignupSheet {
  enum Constants {
    static let screenRatio = 0.9
    static let bottomPadding: CGFloat = 16
    static let topPadding = 45.0

    static let cardCornerRadius: CGFloat = 20
    static let cardShadowRadius: CGFloat = 8

    static let illustrationSize: CGFloat = 152
    static let cardBackgroundColor = YralColor.grey900.swiftUIColor
    static let dimmedBackgroundOpacity = 0.8
    static let appearAnimation = Animation.easeInOut(duration: CGFloat.animationPeriod)
  }
}

protocol SignupSheetProtocol: Any {
  func signupwithGoogle()
  func signupwithApple()
}
