import SwiftUI

struct ReportNudgeView: View {
  let onSubmit: (_ selectedReason: String, _ othersText: String) -> Void
  let onDismiss: () -> Void

  @State private var dragOffset: CGFloat = .zero
  @State private var showContainer: Bool = false
  @State private var selectedReason: String?
  @State private var othersText: String = ""
  @FocusState private var isTextEditorFocused: Bool
  @State private var keyboardHeight: CGFloat = .zero

  private var isSubmitEnabled: Bool {
    selectedReason != nil
  }

  var body: some View {
    GeometryReader { geometry in
      let bottomInset = geometry.safeAreaInsets.bottom

      ZStack(alignment: .bottom) {
        Color.black
          .opacity(Constants.backgroundOpacity)
          .edgesIgnoringSafeArea(.all)
          .onTapGesture {
            if isTextEditorFocused {
              isTextEditorFocused = false
            } else {
              dismissSheet()
            }
          }
          .transition(.opacity)

        if showContainer {
          VStack(spacing: .zero) {
            Rectangle()
              .fill(Constants.indicatorColor)
              .frame(
                width: Constants.indicatorWidth,
                height: Constants.indicatorHeight
              )
              .cornerRadius(Constants.indicatorCornerRadius)
              .padding(.top, Constants.indicatorTopPadding)

            VStack(spacing: Constants.mainContentSpacing) {
              VStack(spacing: Constants.titleSpacing) {
                Text(Constants.mainTitle)
                  .font(Constants.titleFont)
                  .foregroundColor(Constants.titleColor)

                Text(Constants.mainSubtitle)
                  .font(Constants.subtitleFont)
                  .foregroundColor(Constants.subtitleColor)
              }

              VStack(spacing: .zero) {
                ForEach(Constants.reasons, id: \.self) { reason in
                  reasonRow(reason: reason)
                    .padding(.vertical, Constants.reasonRowVerticalPadding)
                }
              }
              .padding(.top, Constants.reasonListTopPadding)

              if selectedReason == Constants.reasonOthers {
                VStack(alignment: .leading, spacing: Constants.textEditorSpacing) {
                  Text(Constants.othersHeaderTitle)
                    .font(Constants.othersHeaderFont)
                    .foregroundColor(Constants.othersHeaderColor)
                  ZStack(alignment: .topLeading) {
                    if #available(iOS 16.0, *) {
                      TextEditor(text: $othersText)
                        .scrollContentBackground(.hidden)
                        .font(Constants.othersTextFont)
                        .foregroundColor(Constants.othersTextColor)
                        .focused($isTextEditorFocused)
                        .frame(minHeight: Constants.textEditorMinHeight,
                               maxHeight: Constants.textEditorMaxHeight)
                        .padding(Constants.textEditorInnerPadding)
                        .background(Constants.textEditorBackground)
                        .cornerRadius(Constants.textEditorCornerRadius)
                    } else {
                      TextEditor(text: $othersText)
                        .font(Constants.othersTextFont)
                        .foregroundColor(Constants.othersTextColor)
                        .focused($isTextEditorFocused)
                        .frame(minHeight: Constants.textEditorMinHeight,
                               maxHeight: Constants.textEditorMaxHeight)
                        .padding(Constants.textEditorInnerPadding)
                        .background(Constants.textEditorBackground)
                        .cornerRadius(Constants.textEditorCornerRadius)
                        .onAppear {
                          UITextView.appearance().backgroundColor = .clear
                          UITextView.appearance().textColor = UIColor(Constants.othersTextColor)
                        }
                    }
                    if othersText.isEmpty {
                      Text(Constants.textEditorPlaceholder)
                        .font(Constants.othersTextFont)
                        .foregroundColor(Constants.placeholderTextColor)
                        .padding(.horizontal, Constants.placeholderHorizontalPadding)
                        .padding(.vertical, Constants.placeholderVerticalPadding)
                        .allowsHitTesting(false)
                    }
                  }
                }
                .padding(.top, Constants.textEditorTopPadding)
              }

              Button(action: {
                guard let reason = selectedReason else { return }
                onSubmit(reason, othersText)
                dismissSheet()
              }, label: {
                Text(Constants.submitButtonTitle)
                  .font(Constants.submitButtonFont)
                  .foregroundColor(Constants.submitButtonTextColor)
                  .frame(maxWidth: .infinity, minHeight: Constants.submitButtonHeight)
                  .background(
                    isSubmitEnabled
                    ? Constants.submitButtonEnabledGradient
                    : Constants.submitButtonDisabledGradient
                  )
                  .cornerRadius(Constants.submitButtonCornerRadius)
              }
              )
              .disabled(!isSubmitEnabled)
              .padding(.top, Constants.submitButtonTopPadding)
            }
            .padding(.horizontal, Constants.horizontalPadding)
            .padding(.top, Constants.topPadding)
            .padding(.bottom, Constants.bottomPadding + bottomInset)
          }
          .background(Constants.sheetBackgroundColor)
          .cornerRadius(Constants.popupCornerRadius)
          .offset(y: dragOffset - (isTextEditorFocused ? min(keyboardHeight, Constants.keyboardOffset) : .zero))
          .simultaneousGesture(
            TapGesture().onEnded {
              if isTextEditorFocused {
                isTextEditorFocused = false
              }
            }
          )
          .gesture(
            DragGesture()
              .onChanged { value in
                dragOffset = max(value.translation.height, 0)
              }
              .onEnded { value in
                if value.translation.height > Constants.dragDismissThreshold {
                  dismissSheet()
                } else {
                  withAnimation(.easeInOut(duration: Constants.shortAnimationDuration)) {
                    dragOffset = .zero
                  }
                }
              }
          )
          .transition(.move(edge: .bottom))
        }
      }
      .onAppear {
        withAnimation(.easeInOut(duration: Constants.mainAnimationDuration)) {
          showContainer = true
        }
      }
      .onDisappear {
        UIView.setAnimationsEnabled(true)
      }
      .edgesIgnoringSafeArea(.all)
      .onReceive(
        NotificationCenter.default.publisher(
          for: UIResponder.keyboardWillShowNotification
        )
      ) { notification in
        if let frame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect {
          withAnimation(.easeOut(duration: CGFloat.animationPeriod)) {
            keyboardHeight = frame.height - geometry.safeAreaInsets.bottom
          }
        }
      }
      .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
        withAnimation(.easeOut(duration: CGFloat.animationPeriod)) {
          keyboardHeight = .zero
        }
      }
    }
  }

  private func reasonRow(reason: String) -> some View {
    HStack(spacing: Constants.reasonRowSpacing) {
      Image(selectedReason == reason ? Constants.reasonSelectedImage : Constants.reasonUnselectedImage)
        .frame(width: Constants.circleDiameter, height: Constants.circleDiameter)
        .padding(.leading, Constants.reasonRowSpacing)

      Text(reason)
        .font(Constants.reasonFont)
        .foregroundColor(Constants.reasonTextColor)
        .padding(.vertical, Constants.reasonTextPadding)

      Spacer()
    }
    .background(Constants.reasonRowBGColor)
    .clipShape(RoundedRectangle(cornerRadius: Constants.reasonRowCornerRadius))
    .contentShape(RoundedRectangle(cornerRadius: Constants.reasonRowCornerRadius))
    .onTapGesture {
      selectedReason = reason
    }
  }

  private func dismissSheet() {
    withAnimation(.easeInOut(duration: Constants.mainAnimationDuration)) {
      showContainer = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.mainAnimationDuration) {
      onDismiss()
    }
  }
}

extension ReportNudgeView {
  enum Constants {
    static let mainTitle = "Report Video"
    static let mainSubtitle = "Why are you reporting this post?"

    static let reasonNudity = "Nudity / Porn"
    static let reasonViolence = "Violence / Gore"
    static let reasonOffensive = "Offensive"
    static let reasonSpam = "Spam / Ad"
    static let reasonOthers = "Others"

    static let reasons = [
      reasonNudity,
      reasonViolence,
      reasonOffensive,
      reasonSpam,
      reasonOthers
    ]

    static let othersHeaderTitle = "Please provide more details."

    static let submitButtonTitle = "Submit"

    static let mainAnimationDuration: Double = 0.25
    static let shortAnimationDuration: Double = 0.1
    static let dragDismissThreshold: CGFloat = 100

    static let backgroundOpacity: CGFloat = 0.8

    static let sheetBackgroundColor = YralColor.grey900.swiftUIColor
    static let popupCornerRadius: CGFloat = 20

    static let indicatorColor = YralColor.grey500.swiftUIColor
    static let indicatorWidth: CGFloat = 32
    static let indicatorHeight: CGFloat = 2
    static let indicatorCornerRadius: CGFloat = 12
    static let indicatorTopPadding: CGFloat = 12

    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let subtitleFont = YralFont.pt12.regular.swiftUIFont
    static let subtitleColor = YralColor.grey0.swiftUIColor
    static let titleSpacing: CGFloat = 8

    static let reasonFont = YralFont.pt16.medium.swiftUIFont
    static let reasonTextColor = YralColor.grey0.swiftUIColor
    static let reasonTextPadding = 12.0
    static let reasonRowSpacing: CGFloat = 12
    static let reasonRowVerticalPadding: CGFloat = 8
    static let reasonListTopPadding: CGFloat = 12
    static let reasonRowBGColor = YralColor.grey800.swiftUIColor
    static let reasonRowCornerRadius = 4.0
    static let reasonSelectedImage = "report_selected"
    static let reasonUnselectedImage = "report_unselected"

    static let othersHeaderFont = YralFont.pt14.medium.swiftUIFont
    static let othersHeaderColor = YralColor.grey300.swiftUIColor
    static let othersTextFont = YralFont.pt14.regular.swiftUIFont
    static let othersTextColor = YralColor.grey0.swiftUIColor
    static let textEditorPlaceholder = "Enter"
    static let textEditorBackground = YralColor.grey800.swiftUIColor
    static let textEditorCornerRadius: CGFloat = 8
    static let textEditorSpacing: CGFloat = 8
    static let textEditorTopPadding: CGFloat = 8
    static let textEditorMinHeight: CGFloat = 80
    static let textEditorMaxHeight: CGFloat = 80
    static let textEditorInnerPadding: CGFloat = 12
    static let placeholderTextColor = YralColor.grey600.swiftUIColor
    static let placeholderVerticalPadding = 20.0
    static let placeholderHorizontalPadding = 16.0

    static let circleStrokeWidth: CGFloat = 2
    static let circleDiameter: CGFloat = 18
    static let selectedCircleColor = YralColor.primary200.swiftUIColor
    static let unselectedCircleColor = YralColor.grey800.swiftUIColor

    static let submitButtonFont = YralFont.pt16.bold.swiftUIFont
    static let submitButtonTextColor = YralColor.grey50.swiftUIColor
    static let submitButtonHeight: CGFloat = 45
    static let submitButtonCornerRadius: CGFloat = 8
    static let submitButtonTopPadding: CGFloat = 8

    static let submitButtonEnabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )

    static let submitButtonDisabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 0.87, green: 0.6, blue: 0.75), location: 0.00),
        Gradient.Stop(color: Color(red: 0.77, green: 0.36, blue: 0.58), location: 0.33),
        Gradient.Stop(color: Color(red: 0.51, green: 0.33, blue: 0.43), location: 1.00)
      ],
      startPoint: UnitPoint(x: 1, y: 0.51),
      endPoint: UnitPoint(x: 0.03, y: 1)
    )

    static let keyboardOffset: CGFloat = 150

    static let topPadding = 28.0
    static let horizontalPadding: CGFloat = 16
    static let bottomPadding: CGFloat = 36
    static let mainContentSpacing: CGFloat = 16
  }
}
