//
//  ModelOptionsBottomSheetView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

// import SwiftUI
//
// struct ModelOptionsBottomSheetView: View {
//  @State private var dragOffset: CGFloat = .zero
//  @State private var showBottomSheet = false
//
//  var body: some View {
//    ZStack(alignment: .bottom) {
//      Color.black.opacity(Constants.backgroundOpacity)
//        .ignoresSafeArea()
//        .onTapGesture {
//          dismiss()
//        }
//        .transition(.opacity)
//
//      if showBottomSheet {
//        VStack(spacing: .zero) {
//          Text("samarth")
//            .frame(height: 300)
//        }
//        .frame(maxWidth: .infinity, alignment: .bottom)
//        .padding(.horizontal, Constants.vStackHorizontalPadding)
//        .background(Constants.backgroundColor)
//        .offset(y: dragOffset)
//        .gesture(
//          DragGesture()
//            .onChanged { value in
//              dragOffset = max(value.translation.height, .zero)
//            }
//            .onEnded { value in
//              if value.translation.height > Constants.bottomSheetDismissValue {
//                dismiss()
//              } else {
//                withAnimation(.easeInOut(duration: Constants.bottomSheetDismissTime)) {
//                  dragOffset = .zero
//                }
//              }
//            }
//        )
//        .transition(.move(edge: .bottom))
//      }
//    }
//    .onAppear {
//      withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
//        showBottomSheet = true
//      }
//    }
//    .onDisappear {
//      UIView.setAnimationsEnabled(true)
//    }
//  }
//
////  private func dismiss() {
////    withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
////      showBottomSheet = false
////    }
////    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.bottomSheetAppearTime) {
////      onKeepPlayingTapped()
////    }
////  }
////
////  private func learnMore() {
////    withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
////      showBottomSheet = false
////    }
////    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.bottomSheetAppearTime) {
////      onLearnMoreTapped()
////    }
////  }
// }
//
// extension ModelOptionsBottomSheetView {
//  enum Constants {
//    static let backgroundOpacity = 0.8
//    static let backgroundColor = YralColor.grey900.swiftUIColor
//    static let vStackHorizontalPadding = 16.0
//
//    static let handleWidth = 32.0
//    static let handleHeight = 2.0
//    static let handleColor = YralColor.grey500.swiftUIColor
//    static let handleTopPadding = 12.0
//
//    static let headingFont = YralFont.pt18.bold.swiftUIFont
//    static let headingTextColor = YralColor.grey0.swiftUIColor
//    static let headingTopPadding = 28.0
//
//    static let lottieWidht = 250.0
//    static let lottieHeight = 130.0
//
//    static let hStackSpacing = 8.0
//    static let hStackTopPadding = 32.0
//    static let hStackBottomPadding = 16.0
//
//    static let titleFont = YralFont.pt16.medium.swiftUIFont
//    static let titleTextColor = YralColor.green50.swiftUIColor
//    static let imageViewSize = 28.0
//
//    static let subheadingFont = YralFont.pt18.bold.swiftUIFont
//    static let subheadingTopPadding = 2.0
//
//    static let keepPlaying = "Keep Playing"
//    static let keepPlayingFont = YralFont.pt16.semiBold.swiftUIFont
//    static let keepPlayingTextColor = YralColor.grey50.swiftUIColor
//
//    static let learnMore = "Learn More"
//    static let learnMoreFont = YralFont.pt16.semiBold.swiftUIFont
//    static let learnMoreTextColor = YralColor.primary300.swiftUIColor
//
//    static let ctaHeight = 42.0
//    static let ctaCornerRadius = 8.0
//
//    static let buttonGradient = LinearGradient(
//      stops: [
//        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
//        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
//        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
//      ],
//      startPoint: .init(x: 0.94, y: 0.13),
//      endPoint: .init(x: 0.35, y: 0.89)
//    )
//
//    static let bottomSheetDismissValue = 100.0
//    static let bottomSheetDismissTime = 0.1
//    static let bottomSheetAppearTime = 0.3
//
//    static let yralTokenString = "YRAL token!"
//    static let yralTokensString = "YRAL tokens!"
//  }
// }
