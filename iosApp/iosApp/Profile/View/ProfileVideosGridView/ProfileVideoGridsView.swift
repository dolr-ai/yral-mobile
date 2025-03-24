//
//  ProfileVideoGridsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileVideosGridView: View {
  @Binding var videos: [ProfileVideoInfo]
  @Binding var currentlyDeletingPostInfo: ProfileVideoInfo?
  @Binding var showDeleteIndictor: Bool
  @State private var lastLoadedItemID: String?
  var onDelete: ((ProfileVideoInfo) -> Void)?
  var onVideoTapped: ((ProfileVideoInfo) -> Void)?
  var onLoadMore: (() -> Void)?

  private let columns = [
    GridItem(.flexible(), spacing: Constants.gridItemSpacing),
    GridItem(.flexible(), spacing: Constants.gridItemSpacing)
  ]

  var body: some View {
    LazyVGrid(columns: columns, spacing: Constants.verticalSpacing) {
      ForEach(videos, id: \.id) { video in
        ZStack(alignment: .bottomLeading) {
          VideoThumbnailView(video: video)
            .onTapGesture {
              onVideoTapped?(video)
            }

          HStack {
            HStack(spacing: Constants.innerHStackSpacing) {
              Image(video.isLiked ? Constants.likeImageNameSelected : Constants.likeImageNameUnselected)
              Text("\(video.likeCount)")
                .font(Constants.likeTextFont)
                .foregroundColor(Constants.likeTextColor)
            }
            .padding(.leading, Constants.buttonHorizontalPadding)
            Spacer()
            Button {
              onDelete?(video)
            }
            label: {
              Image(Constants.deleteImageName)
            }
            .padding(.trailing, Constants.buttonHorizontalPadding)
          }
          .padding(.bottom, Constants.bottomPadding)
          if video.postID == currentlyDeletingPostInfo?.postID && showDeleteIndictor {
            ZStack(alignment: .center) {
              Color.black.opacity(0.4)
              ProgressView()
                .background(Color.white.opacity(0.8))
                .padding()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
          }
        }
        .cornerRadius(Constants.thumbnailCornerRadius)
        .onAppear {
          if let lastVideo = videos.last, video.postID == lastVideo.postID {
            if lastLoadedItemID != lastVideo.postID {
              lastLoadedItemID = lastVideo.postID
              onLoadMore?()
            }
          }
        }
      }
    }
  }
}

extension ProfileVideosGridView {
  enum Constants {
    static let gridItemSpacing: CGFloat = 16.0
    static let verticalSpacing: CGFloat = 16.0
    static let innerHStackSpacing: CGFloat = 4.0

    static let thumbnailCornerRadius: CGFloat = 8.0
    static let buttonHorizontalPadding: CGFloat = 12.0
    static let bottomPadding: CGFloat = 12.0

    static let deleteImageName = "delete_video_profile"
    static let likeImageNameSelected = "like_selected_feed"
    static let likeImageNameUnselected = "like_unselected_feed"

    static let likeTextFont = YralFont.pt14.medium.swiftUIFont
    static let likeTextColor = YralColor.grey50.swiftUIColor
  }
}

struct VideoThumbnailView: View {
  let video: ProfileVideoInfo

  var body: some View {
    GeometryReader { _ in
      URLImage(url: video.thumbnailUrl)
    }
    .aspectRatio(Constants.thumbnailAspectRatio, contentMode: .fit)
  }
}

extension VideoThumbnailView {
  enum Constants {
    static let thumbnailAspectRatio: CGFloat = 0.75
  }
}
