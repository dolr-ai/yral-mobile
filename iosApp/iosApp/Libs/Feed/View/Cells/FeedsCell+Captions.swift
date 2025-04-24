import UIKit

extension FeedsCell {
  func setupCaptionLabel() {
    contentView.addSubview(captionScrollView)
    captionScrollView.addSubview(captionLabel)

    captionScrollViewHeightConstraint = captionScrollView.heightAnchor
      .constraint(equalToConstant: Constants.captionSingleLineHeight)
    captionScrollViewHeightConstraint.isActive = true

    NSLayoutConstraint.activate([
      captionScrollView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.horizontalMargin
      ),
      captionScrollView.trailingAnchor.constraint(
        equalTo: actionsStackView.leadingAnchor,
        constant: -Constants.horizontalMargin
      ),
      captionScrollView.bottomAnchor.constraint(
        equalTo: contentView.bottomAnchor,
        constant: -Constants.captionsBottomMargin
      )
    ])

    NSLayoutConstraint.activate([
      captionLabel.topAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.topAnchor),
      captionLabel.leadingAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.leadingAnchor),
      captionLabel.trailingAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.trailingAnchor),
      captionLabel.bottomAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.bottomAnchor),
      captionLabel.widthAnchor.constraint(equalTo: captionScrollView.frameLayoutGuide.widthAnchor)
    ])

    let captionTapGesture = UITapGestureRecognizer(target: self, action: #selector(handleCaptionTap))
    captionScrollView.addGestureRecognizer(captionTapGesture)
  }

  @objc private func handleCaptionTap() {
    if !isCaptionExpanded {
      expandCaption()
    } else {
      collapseCaption()
    }
  }

  @objc func handleCellTap(_ gesture: UITapGestureRecognizer) {
    guard isCaptionCollapsible else { return }
    if isCaptionExpanded {
      collapseCaption()
    }
  }

  func setCaptionHeight(captionText: String) {
    captionLabel.text = captionText
    let availableWidth = contentView.frame.width - (Constants.horizontalMargin * .two) - actionsStackView.frame.width
    let fullHeight = captionLabel.heightForWidth(availableWidth, maxLines: .zero)
    let singleLineHeight = captionLabel.font.lineHeight
    let maxHeightForTenLines = singleLineHeight * Constants.maxLinesCaption
    let linesNeeded = ceil(fullHeight / singleLineHeight)
    if linesNeeded <= .one {
      isCaptionCollapsible = false
      collapsedCaptionHeight = singleLineHeight
      expandedCaptionHeight = singleLineHeight
      captionLabel.numberOfLines = .one
      captionLabel.lineBreakMode = .byTruncatingTail
      captionScrollView.isScrollEnabled = false
    } else {
      isCaptionCollapsible = true
      collapsedCaptionHeight = singleLineHeight
      if linesNeeded <= Constants.maxLinesCaption {
        expandedCaptionHeight = fullHeight
      } else {
        expandedCaptionHeight = maxHeightForTenLines
      }
      captionLabel.numberOfLines = .one
      captionLabel.lineBreakMode = .byTruncatingTail
      captionScrollView.isScrollEnabled = false
    }
    captionScrollViewHeightConstraint.constant = collapsedCaptionHeight
    isCaptionExpanded = false
    captionScrollView.setNeedsLayout()
    captionScrollView.layoutIfNeeded()
  }

  func expandCaption() {
    captionLabel.numberOfLines = .zero
    captionLabel.lineBreakMode = .byWordWrapping
    captionScrollView.isScrollEnabled = (
      expandedCaptionHeight >
      captionLabel.font.lineHeight * Constants.maxLinesCaption * 0.99
    )
    UIView.animate(withDuration: CGFloat.animationPeriod, animations: {
      self.captionScrollViewHeightConstraint.constant = self.expandedCaptionHeight
      self.layoutIfNeeded()
      self.captionScrollView.layoutIfNeeded()
    })
    isCaptionExpanded = true
  }

  func collapseCaption() {
    captionLabel.numberOfLines = .one
    captionLabel.lineBreakMode = .byTruncatingTail
    captionScrollView.isScrollEnabled = false
    UIView.animate(withDuration: CGFloat.animationPeriod, animations: {
      self.captionScrollViewHeightConstraint.constant = self.collapsedCaptionHeight
      self.layoutIfNeeded()
      self.captionScrollView.layoutIfNeeded()
    })
    isCaptionExpanded = false
  }
}
