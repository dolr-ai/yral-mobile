import UIKit

extension FeedsCell {
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
