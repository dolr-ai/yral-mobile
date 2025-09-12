//
//  DraggableView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct DraggableView<Content: View, StickyContent: View>: View {
  @Binding var isExpanded: Bool

  @State private var sheetY: CGFloat = .zero
  @State private var dragStartY: CGFloat = .zero
  @State private var isDraggingSheet: Bool = false
  @State private var scrollOffset: CGFloat = .zero
  @State private var scrollProxy: ScrollViewProxy?

  @ViewBuilder let stickyContent: () -> StickyContent
  @ViewBuilder let content: () -> Content

  let topInset: CGFloat
  let peekHeight: CGFloat
  let background: Color

  private let expandCollapseAnim = Animation.easeOut(duration: 0.25)
  private let layoutAnim = Animation.easeOut(duration: 0.2)
  private let collapseThreshold: CGFloat = 60
  private let topAnchorID = "SCROLL_TOP"
  private let topEpsilon: CGFloat = 1.0

  init(
    isExpanded: Binding<Bool>,
    topInset: CGFloat,
    peekHeight: CGFloat,
    background: Color,
    @ViewBuilder stickyContent: @escaping () -> StickyContent,
    @ViewBuilder content: @escaping () -> Content
  ) {
    self._isExpanded = isExpanded
    self.topInset = topInset
    self.peekHeight = peekHeight
    self.background = background
    self.stickyContent = stickyContent
    self.content = content
  }

  private func collapsedY(_ totalHeight: CGFloat) -> CGFloat {
    max(topInset, totalHeight - peekHeight)
  }

  private func scrollToTop(_ proxy: ScrollViewProxy?) {
    guard let proxy else { return }
    withAnimation(nil) {
      proxy.scrollTo(topAnchorID, anchor: .top)
    }
  }

  var body: some View {
    GeometryReader { geo in
      let totalHeight = geo.size.height
      let availableWidth = geo.size.width
      let collapsed = collapsedY(totalHeight)
      let clamp: (CGFloat) -> CGFloat = { yOffset in max(topInset, min(yOffset, collapsed)) }

      let bodyContent =
      ScrollViewReader { proxy in
        VStack(spacing: 0) {
          stickyContent()

          ScrollView(showsIndicators: false) {
            Color.clear
              .frame(height: 1)
              .id(topAnchorID)

            TrackScrollOffset()

            VStack(spacing: .zero) {
              content()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
          }
          .coordinateSpace(name: "SCROLL")
          .onPreferenceChange(ScrollOffsetKey.self) { scrollOffset = $0 }
          .scrollLock(!isExpanded)
          .allowsHitTesting(!(isExpanded && isDraggingSheet))
        }
        .frame(maxWidth: .infinity, alignment: .top)
        .onAppear { scrollProxy = proxy }
        .onChange(of: isExpanded) { expanded in
          if !expanded {
            scrollToTop(proxy)
            DispatchQueue.main.async { scrollToTop(proxy) }
          }
        }
      }

      let drag = DragGesture(minimumDistance: 0, coordinateSpace: .global)
        .onChanged { geo in
          let displacementY = geo.translation.height

          if isExpanded {
            guard displacementY > 0, scrollOffset >= -topEpsilon else { return }
            if !isDraggingSheet {
              isDraggingSheet = true
              dragStartY = topInset
            }
            sheetY = clamp(dragStartY + displacementY)
          } else {
            if !isDraggingSheet { isDraggingSheet = true }
            if dragStartY == 0 { dragStartY = sheetY }
            sheetY = clamp(dragStartY + displacementY)
          }
        }
        .onEnded { geo in
          let displacementY = geo.translation.height
          defer {
            isDraggingSheet = false
            dragStartY = 0
          }

          if isExpanded {
            if scrollOffset >= -topEpsilon, displacementY > collapseThreshold {
              withAnimation(expandCollapseAnim) {
                isExpanded = false
                sheetY = collapsed
              }
              DispatchQueue.main.async { scrollToTop(scrollProxy) }
            } else {
              withAnimation(expandCollapseAnim) {
                sheetY = topInset
              }
            }
          } else {
            if displacementY < -collapseThreshold {
              withAnimation(expandCollapseAnim) {
                isExpanded = true
                sheetY = topInset
              }
            } else {
              withAnimation(expandCollapseAnim) {
                sheetY = collapsed
              }
            }
          }
        }

      let visibleHeight = max(0, totalHeight - sheetY)

      ZStack(alignment: .top) {
        bodyContent
      }
      .background(background)
      .frame(width: availableWidth, height: visibleHeight, alignment: .top)
      .offset(y: sheetY)
      .gesture(drag)

      .onAppear {
        sheetY = isExpanded ? topInset : collapsed
      }
      .onChange(of: geo.size.height) { _ in
        withAnimation(layoutAnim) {
          sheetY = isExpanded ? topInset : collapsed
        }
      }
      .onChange(of: isExpanded) { expanded in
        withAnimation(expandCollapseAnim) {
          sheetY = expanded ? topInset : collapsed
        }
      }
      .onChange(of: peekHeight) { newPeek in
        let newCollapsedY = max(topInset, geo.size.height - max(0, newPeek))
        withAnimation(expandCollapseAnim) {
          if !isExpanded { sheetY = newCollapsedY }
        }
      }
    }
  }
}

private struct ScrollOffsetKey: PreferenceKey {
  static var defaultValue: CGFloat = .zero
  static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private struct TrackScrollOffset: View {
  var body: some View {
    GeometryReader { geo in
      Color.clear.preference(
        key: ScrollOffsetKey.self,
        value: geo.frame(in: .named(Constants.scrollKey)).minY
      )
    }
    .frame(height: Constants.height)
  }
}

extension TrackScrollOffset {
  enum Constants {
    static let scrollKey = "SCROLL"
    static let height = 0.0
  }
}
