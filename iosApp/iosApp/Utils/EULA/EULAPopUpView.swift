import SwiftUI
import SafariServices

struct EULAPopupView: View {
  @Binding var isPresented: Bool
  let onContinue: () -> Void

  @State private var isChecked = false
  @State private var showCard = false
  @State private var selectedURL: URL?

  var body: some View {
    GeometryReader { geo in
      ZStack {
        Color.black.opacity(Constants.dimmedBackgroundOpacity)
          .ignoresSafeArea()

        if showCard {
          ZStack {
            Constants.cardBackgroundColor
              .cornerRadius(Constants.cardCornerRadius)

            ScrollView(.vertical, showsIndicators: false) {
              VStack(spacing: .zero) {
                Constants.eulaImage
                  .resizable()
                  .aspectRatio(contentMode: .fit)
                  .frame(width: Constants.illustrationSize,
                         height: Constants.illustrationSize)
                  .padding(.top, Constants.eulaImagePadding)

                Text(Constants.title)
                  .font(Constants.titleFont)
                  .foregroundColor(Constants.titleColor)
                  .padding(.top, Constants.titlePadding)

                linksBlock
                  .padding(.top, Constants.linksBlockPadding)

                Text(Constants.reminder)
                  .multilineTextAlignment(.center)
                  .font(Constants.bodyFont)
                  .foregroundColor(Constants.bodyColor)
                  .padding(.horizontal, Constants.linksTextHorizontalPadding)
                  .padding(.top, Constants.policyTopPadding)

                checkboxBlock
                  .padding(.top, Constants.checkboxBlockPadding)

                Button {
                  isPresented = false
                  onContinue()
                } label: {
                  Text(Constants.continueLabel)
                    .font(Constants.buttonFont)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: Constants.buttonHeight)
                    .background(
                      (isChecked ? Constants.enabledGradient : Constants.disabledGradient)
                        .cornerRadius(Constants.buttonCornerRadius)
                    )
                }
                .disabled(!isChecked)
                .padding(.top, Constants.continueButtonpadding)
                .padding(.bottom, geo.safeAreaInsets.bottom + Constants.bottomPadding)
              }
              .padding(.horizontal, Constants.horizontalPadding)
              .edgesIgnoringSafeArea(.bottom)
            }
            .edgesIgnoringSafeArea(.bottom)
          }
          .frame(maxWidth: .infinity, alignment: .bottom)
          .frame(height: geo.size.height * Constants.screenRatio, alignment: .bottom)
          .frame(maxHeight: .infinity, alignment: .bottom)
          .padding(.bottom, -geo.safeAreaInsets.bottom)
          .shadow(radius: Constants.cardShadowRadius)
          .transition(.move(edge: .bottom))
        }
      }
      .onAppear { withAnimation(Constants.appearAnimation) { showCard = true } }
      .sheet(item: $selectedURL) { SafariView(url: $0) }
    }
  }

  @ViewBuilder private var linksBlock: some View {
    Text(makeLinkedSentence())
      .font(Constants.bodyFont)
      .foregroundColor(Constants.bodyColor)
      .multilineTextAlignment(.center)
      .onOpenURL { selectedURL = $0 }
  }

  private func makeLinkedSentence() -> AttributedString {
    var str = AttributedString(Constants.reviewLeadIn)
    func link(_ label: String, url: URL) -> AttributedString {
      var part = AttributedString(label)
      part.link = url
      part.foregroundColor = Constants.linkColor
      return part
    }
    str += link(Constants.termsLabel, url: Constants.termsURL)
    str += AttributedString(", ")
    str += link(Constants.privacyLabel, url: Constants.privacyURL)
    str += AttributedString(", \(Constants.andText) ")
    str += link(Constants.eulaLabel, url: Constants.eulaURL)
    str += AttributedString(".")
    return str
  }

  @ViewBuilder private var checkboxBlock: some View {
    HStack(alignment: .center, spacing: Constants.checkboxLabelSpacing) {
      Button { isChecked.toggle() } label: {
        Image(systemName: isChecked ? "checkmark.square.fill" : "square")
          .resizable()
          .frame(width: Constants.checkboxSize, height: Constants.checkboxSize)
          .foregroundColor(Constants.checkboxColor)
      }
      .buttonStyle(.plain)

      Text(Constants.checkboxText)
        .font(Constants.bodyFont)
        .foregroundColor(Constants.bodyColor)
        .fixedSize(horizontal: false, vertical: true)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.horizontal, 4.0)
  }
}

// swiftlint: disable identifier_name
private struct SafariView: UIViewControllerRepresentable {
  let url: URL
  func makeUIViewController(context: Context) -> SFSafariViewController { SFSafariViewController(url: url) }
  func updateUIViewController(_ vc: SFSafariViewController, context: Context) {}
}
// swiftlint: enable identifier_name

extension EULAPopupView {
  enum Constants {
    static let title = "End User License Agreement"
    static let reviewLeadIn = "Please review our "
    static let andText = "and"
    static let termsLabel = "Terms of Service"
    static let privacyLabel = "Privacy Policy"
    static let eulaLabel = "End‑User License Agreement (EULA)"
    // swiftlint: disable line_length
    static let reminder = "By clicking Continue, you accept these terms and acknowledge our zero‑tolerance policy for abusive or objectionable content."
    // swiftlint: enable line_length
    static let checkboxText = "I agree to all the Terms of Service, Policy & EULA"
    static let continueLabel = "Continue"

    static let termsURL = URL(string: "https://yral.com/terms-ios")!
    static let privacyURL = URL(string: "https://yral.com/privacy-policy")!
    static let eulaURL = URL(string: "https://yral.com/terms-ios")!

    static let screenRatio = 0.8
    static let horizontalPadding = 16.0
    static let linksTextHorizontalPadding: CGFloat = 24
    static let eulaImagePadding = 30.0
    static let titlePadding = 46.0
    static let linksBlockPadding = 16.0
    static let policyTopPadding: CGFloat = 30.0
    static let checkboxBlockPadding = 30.0
    static let checkboxHStackHorizontalPadding = 8.0
    static let continueButtonpadding = 28.0
    static let bottomPadding: CGFloat = 16

    static let cardCornerRadius: CGFloat = 20
    static let cardShadowRadius: CGFloat = 8

    static let illustrationSize: CGFloat = 152

    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let bodyFont = YralFont.pt14.swiftUIFont
    static let buttonFont = YralFont.pt16.bold.swiftUIFont

    static let titleColor = YralColor.grey0.swiftUIColor
    static let bodyColor = YralColor.grey50.swiftUIColor
    static let linkColor = YralColor.primary300.swiftUIColor
    static let cardBackgroundColor = YralColor.grey900.swiftUIColor
    static let checkboxColor = linkColor
    static let dimmedBackgroundOpacity = 0.8

    static let checkboxSize: CGFloat = 24
    static let checkboxLabelSpacing: CGFloat = 8

    static let buttonHeight: CGFloat = 45
    static let buttonCornerRadius: CGFloat = 8

    static let appearAnimation = Animation.easeInOut(duration: CGFloat.animationPeriod)

    static let enabledGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
      ],
      startPoint: .init(x: 0.94, y: 0.13),
      endPoint: .init(x: 0.35, y: 0.89)
    )

    static let disabledGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 0.87, green: 0.6, blue: 0.75), location: 0),
        .init(color: Color(red: 0.77, green: 0.36, blue: 0.58), location: 0.33),
        .init(color: Color(red: 0.51, green: 0.33, blue: 0.43), location: 1)
      ],
      startPoint: .init(x: 1, y: 0.51),
      endPoint: .init(x: 0.03, y: 1)
    )

    static let eulaImage = Image("eula_image")
  }
}
