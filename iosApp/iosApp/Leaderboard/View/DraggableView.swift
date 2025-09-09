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
      Color.clear
        .preference(key: ScrollOffsetKey.self,
                    value: geo.frame(in: .named("SCROLL")).minY)
    }
    .frame(height: 0)
  }
}

struct DraggableView<Content: View>: View {
  let topInset: CGFloat
  let peekHeight: CGFloat
  let background: Color
  let cornerRadius: CGFloat
  let content: () -> Content

  @State private var sheetY: CGFloat = 0
  @State private var dragStartY: CGFloat = 0
  @State private var scrollOffset: CGFloat = 0
  @State private var isDraggingSheet: Bool = false

  private var isPinnedToTop: Bool { sheetY <= topInset + 0.5 }

  init(
    topInset: CGFloat,
    peekHeight: CGFloat,
    background: Color = .black,
    cornerRadius: CGFloat = 20,
    @ViewBuilder content: @escaping () -> Content
  ) {
    self.topInset = topInset
    self.peekHeight = peekHeight
    self.background = background
    self.cornerRadius = cornerRadius
    self.content = content
  }

  var body: some View {
    GeometryReader { geo in
      let totalHeight = geo.size.height
      let collapsedY = max(topInset, totalHeight - peekHeight)
      let clamp: (CGFloat) -> CGFloat = { yPoint in max(topInset, min(yPoint, collapsedY)) }
      let snapThreshold: CGFloat = 60

      ZStack(alignment: .top) {
        VStack(spacing: 0) {
          Capsule()
            .frame(width: 40, height: 6)
            .opacity(0.3)
            .padding(.top, 8)
            .padding(.bottom, 8)

          if #available(iOS 16.4, *) {
            ScrollView(showsIndicators: false) {
              TrackScrollOffset()
              VStack(spacing: 12) {
                content()
              }
              .padding(.horizontal, 16)
              .padding(.bottom, 24)
            }
            .scrollBounceBehavior(.basedOnSize)
            .coordinateSpace(name: "SCROLL")
            // Scroll only when pinned to top and the sheet itself isn't being dragged.
            .allowsHitTesting(isPinnedToTop && !isDraggingSheet)
            .onPreferenceChange(ScrollOffsetKey.self) { value in
              scrollOffset = value
            }
          } else {
            ScrollView(showsIndicators: false) {
              TrackScrollOffset()
              VStack(spacing: 12) {
                content()
              }
              .padding(.horizontal, 16)
              .padding(.bottom, 24)
            }
            .coordinateSpace(name: "SCROLL")
            // Scroll only when pinned to top and the sheet itself isn't being dragged.
            .allowsHitTesting(isPinnedToTop && !isDraggingSheet)
            .onPreferenceChange(ScrollOffsetKey.self) { value in
              scrollOffset = value
            }
          }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .contentShape(Rectangle()) // capture drags across the whole surface
      }
      .background(background)
      .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
      .shadow(radius: 10, y: -2)
      .offset(y: sheetY)
      .gesture( // ⬅︎ back to normal priority
        DragGesture(minimumDistance: 0, coordinateSpace: .global)
          .onChanged { geo in
            let yDisplacement = geo.translation.height

            // Decide if the SHEET should move:
            // - If not pinned -> always the sheet (we're expanding from collapsed).
            // - If pinned & dragging DOWN -> only if list is at/above top.
            // - If pinned & dragging UP -> let ScrollView handle; do NOT move sheet.
            let shouldDragSheet: Bool = {
              if !isPinnedToTop { return true }
              if yDisplacement > 0 { return scrollOffset <= 0 } // pull-down collapse only when list is at top
              return false
            }()

            if shouldDragSheet {
              if !isDraggingSheet { isDraggingSheet = true }      // lock list while dragging sheet
              if dragStartY == 0 { dragStartY = sheetY }
              sheetY = clamp(dragStartY + yDisplacement)                     // clamp to avoid overshoot
            } else {
              // Let ScrollView own the gesture
              if isDraggingSheet { isDraggingSheet = false }
            }
          }
          .onEnded { geo in
            dragStartY = 0
            isDraggingSheet = false

            let yDisplacement = geo.translation.height
            withAnimation(.easeOut(duration: 0.25)) {
              if abs(yDisplacement) > snapThreshold {
                sheetY = (yDisplacement < 0) ? topInset : collapsedY
              } else {
                let mid = (collapsedY + topInset) / 2
                sheetY = (sheetY < mid) ? topInset : collapsedY
              }
            }
          }
      )
      .onAppear {
        sheetY = collapsedY // start collapsed
      }
    }
  }
}
