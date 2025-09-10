//
//  DraggableView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

private struct ScrollOffsetKey: PreferenceKey {
  static var defaultValue: CGFloat = 0
  static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private struct TrackScrollOffset: View {
  var body: some View {
    GeometryReader { geo in
      Color.clear.preference(
        key: ScrollOffsetKey.self,
        value: geo.frame(in: .named("SCROLL")).minY
      )
    }
    .frame(height: 0)
  }
}

struct DraggableView<Content: View>: View {
  @Binding var isExpanded: Bool

  let topInset: CGFloat
  let peekHeight: CGFloat
  let background: Color
  let content: () -> Content

  @State private var sheetY: CGFloat = 0
  @State private var dragStartY: CGFloat = 0
  @State private var isDraggingSheet: Bool = false
  @State private var scrollOffset: CGFloat = 0

  init(
    isExpanded: Binding<Bool>,
    topInset: CGFloat,
    peekHeight: CGFloat,
    background: Color,
    @ViewBuilder content: @escaping () -> Content
  ) {
    self._isExpanded = isExpanded
    self.topInset = topInset
    self.peekHeight = peekHeight
    self.background = background
    self.content = content
  }

  private func collapsedY(_ totalHeight: CGFloat) -> CGFloat {
    max(topInset, totalHeight - peekHeight)
  }

  var body: some View {
    GeometryReader { geo in
      let totalHeight = geo.size.height
      let availableWidth = geo.size.width
      let collapsed = collapsedY(totalHeight)
      let clamp: (CGFloat) -> CGFloat = { yOffset in max(topInset, min(yOffset, collapsed)) }

      let bodyContent =
      VStack(spacing: 0) {
        ScrollView(showsIndicators: false) {
          TrackScrollOffset()
          VStack(spacing: .zero) {
            content()
          }
          .frame(maxWidth: .infinity, alignment: .leading)
        }
        .coordinateSpace(name: "SCROLL")
        .onPreferenceChange(ScrollOffsetKey.self) { scrollOffset = $0 }
        .allowsHitTesting(isExpanded)
      }
      .frame(maxWidth: .infinity, alignment: .top)

      let drag = DragGesture(minimumDistance: 0, coordinateSpace: .global)
        .onChanged { geo in
          guard !isExpanded else { return }
          if !isDraggingSheet { isDraggingSheet = true }
          if dragStartY == 0 { dragStartY = sheetY }
          sheetY = clamp(dragStartY + geo.translation.height)
        }
        .onEnded { _ in
          guard !isExpanded else { return }
          dragStartY = 0
          isDraggingSheet = false
          withAnimation(.easeOut(duration: 0.25)) {
            sheetY = topInset
            isExpanded = true
          }
        }

      let visibleHeight = max(0, totalHeight - sheetY)

      ZStack(alignment: .top) {
        bodyContent
      }
      .background(background)
      .frame(width: availableWidth, height: visibleHeight, alignment: .top)
      .offset(y: sheetY)
      .gesture(drag, including: isExpanded ? .none : .gesture)
      .onAppear {
        sheetY = isExpanded ? topInset : collapsed
      }
      .onChange(of: geo.size.height) { _ in
        withAnimation(.easeOut(duration: 0.2)) {
          sheetY = isExpanded ? topInset : collapsed
        }
      }
      .onChange(of: isExpanded) { expanded in
        withAnimation(.easeOut(duration: 0.25)) {
          sheetY = expanded ? topInset : collapsed
        }
      }
      .onChange(of: peekHeight) { newPeek in
        let newCollapsedY = max(topInset, geo.size.height - max(0, newPeek))
        withAnimation(.easeOut(duration: 0.25)) {
          if !isExpanded {
            sheetY = newCollapsedY
          }
        }
      }
    }
  }
}
