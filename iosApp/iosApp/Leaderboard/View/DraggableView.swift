//
//  DraggableView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

// MARK: - Scroll Offset Plumbing

private struct ScrollOffsetKey: PreferenceKey {
  static var defaultValue: CGFloat = 0
  static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

/// offset notes in named space "SCROLL":
///   ≈ 0 at top, < 0 when scrolled down, > 0 when overscrolling at top
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

// MARK: - iOS 16+/15 scroll lock helper

private extension View {
  /// Locks ScrollView scrolling on iOS 16+, falls back to allowsHitTesting on iOS 15.
  @ViewBuilder
  func scrollLock(_ disabled: Bool) -> some View {
    if #available(iOS 16.0, *) {
      self.scrollDisabled(disabled)
    } else {
      // iOS 15 fallback: disables scroll AND taps when disabled == true
      self.allowsHitTesting(!disabled)
    }
  }
}

// MARK: - DraggableView

struct DraggableView<Content: View, StickyContent: View>: View {
  @Binding var isExpanded: Bool

  let topInset: CGFloat
  let peekHeight: CGFloat
  let background: Color
  @ViewBuilder let stickyContent: () -> StickyContent
  @ViewBuilder let content: () -> Content

  // Gesture / layout state
  @State private var sheetY: CGFloat = 0
  @State private var dragStartY: CGFloat = 0
  @State private var isDraggingSheet: Bool = false
  @State private var scrollOffset: CGFloat = 0

  // Keep a proxy to allow scroll-to-top from gesture end as well
  @State private var scrollProxy: ScrollViewProxy?

  // Tunables
  private let expandCollapseAnim = Animation.easeOut(duration: 0.25)
  private let layoutAnim = Animation.easeOut(duration: 0.2)
  private let collapseThreshold: CGFloat = 60 // pts of downward drag to trigger collapse

  // Scroll-to-top anchor ID (+ tolerance to treat "near top" as top)
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

  /// Immediate, no-animation jump to top (safe to call multiple times)
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

      // MARK: Scrollable body content
      let bodyContent =
      ScrollViewReader { proxy in
        VStack(spacing: 0) {
          stickyContent()

          ScrollView(showsIndicators: false) {
            // Real top anchor (non-zero height) for reliable scrollTo
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

          // Lock scrolling when collapsed (iOS 16+), fallback for iOS 15
          .scrollLock(!isExpanded)

          // Avoid gesture fights while actively dragging the sheet in expanded state
          .allowsHitTesting(!(isExpanded && isDraggingSheet))
        }
        .frame(maxWidth: .infinity, alignment: .top)
        .onAppear { scrollProxy = proxy }

        // Reset to top when we collapse (do it twice: now + next tick)
        .onChange(of: isExpanded) { expanded in
          if !expanded {
            scrollToTop(proxy)
            DispatchQueue.main.async { scrollToTop(proxy) }
          }
        }
      }

      // MARK: Unified drag gesture for both states
      let drag = DragGesture(minimumDistance: 0, coordinateSpace: .global)
        .onChanged { geo in
          let displacementY = geo.translation.height

          if isExpanded {
            // Only start dragging the sheet if pulling down and we're at/near top
            guard displacementY > 0, scrollOffset >= -topEpsilon else { return }
            if !isDraggingSheet {
              isDraggingSheet = true
              dragStartY = topInset
            }
            sheetY = clamp(dragStartY + displacementY)
          } else {
            // Collapsed: allow drag movement (up to expand; down stays clamped)
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
            // Collapse if pulled down sufficiently from (near) top
            if scrollOffset >= -topEpsilon, displacementY > collapseThreshold {
              withAnimation(expandCollapseAnim) {
                isExpanded = false
                sheetY = collapsed
              }
              // Extra safety: also scroll to top after collapse lands
              DispatchQueue.main.async { scrollToTop(scrollProxy) }
            } else {
              withAnimation(expandCollapseAnim) {
                sheetY = topInset
              }
            }
          } else {
            // Collapsed → expand on an upward pull past threshold; otherwise stay collapsed.
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

      // Visible height of the clipped sheet
      let visibleHeight = max(0, totalHeight - sheetY)

      ZStack(alignment: .top) {
        bodyContent
      }
      .background(background)
      .frame(width: availableWidth, height: visibleHeight, alignment: .top)
      .offset(y: sheetY)
      // Always attach the gesture; internal logic decides when it acts.
      .gesture(drag)

      // MARK: Lifecycle / layout updates
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
