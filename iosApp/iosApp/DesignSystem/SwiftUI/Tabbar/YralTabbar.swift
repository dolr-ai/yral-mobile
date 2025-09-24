//
//  YralTabbar.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct YralTabbar: View {
  @Binding var selectedTab: Tab
  let icons: [(selected: String, unselected: String)]

  var body: some View {
    GeometryReader { geo in
      let bottomInset = geo.safeAreaInsets.bottom
      let effectivePadding: CGFloat = Constants.horizontalPadding
      let contentWidth = geo.size.width - (effectivePadding * .two)
      let tabWidth = contentWidth / CGFloat(icons.count)
      let baseX = CGFloat(selectedTab.intValue) * tabWidth + (tabWidth - Constants.indicatorWidth) / .two

      ZStack(alignment: .bottomLeading) {
        RoundedRectangle(cornerRadius: Constants.corner)
          .fill(Constants.bgColor.opacity(Constants.barOpacity))
          .overlay(
            RoundedRectangle(cornerRadius: Constants.corner)
              .stroke(Constants.borderColor, lineWidth: .half)
          )
          .frame(height: Constants.height + bottomInset)

        HStack(spacing: .zero) {
          ForEach(Int.zero..<icons.count, id: \.self) { idx in
            let isSelected = idx == selectedTab.intValue
            let pair = icons[idx]
            Button {
              withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
                selectedTab = Self.tab(for: idx)
              }
              UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
              Image(ImageResource(name: isSelected ? pair.selected : pair.unselected, bundle: .main))
                .renderingMode(.original)
                .frame(maxWidth: .infinity, minHeight: Constants.height, maxHeight: Constants.height)
                .contentShape(Rectangle())
            }
          }
        }
        .padding(.horizontal, effectivePadding)
        .padding(.bottom, bottomInset)
        .overlay(alignment: .topLeading) {
          Rectangle()
            .fill(Constants.indicatorColor)
            .frame(width: Constants.indicatorWidth, height: Constants.indicatorHeight)
            .cornerRadius(Constants.indicatorHeight / .two)
            .offset(x: baseX + effectivePadding, y: .zero)
            .animation(.easeInOut(duration: CGFloat.animationPeriod), value: selectedTab)
        }
      }
      .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
    }
    .frame(height: Constants.height)
    .background(Color.clear)
  }

  private static func tab(for idx: Int) -> Tab {
    switch idx {
    case .zero: return .home
    case .one: return .leaderboard
    case .two: return .upload
    case .three: return .wallet
    default: return .profile
    }
  }
}

extension YralTabbar {
  enum Constants {
    static let height: CGFloat = 68
    static let corner: CGFloat = 0
    static let horizontalPadding: CGFloat = 2.0
    static let indicatorWidth: CGFloat = 30
    static let indicatorHeight: CGFloat = 2
    static let barOpacity: CGFloat = 1
    static let bgColor: Color = .black
    static let indicatorColor: Color = YralColor.primary300.swiftUIColor
    static let borderColor = Color.white.opacity(0.08)
  }
}
