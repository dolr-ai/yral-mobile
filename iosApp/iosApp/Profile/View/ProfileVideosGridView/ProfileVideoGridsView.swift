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
    VStack(spacing: .zero) {
      LazyVGrid(columns: columns, spacing: Constants.verticalSpacing) {
        ForEach(videos, id: \.postID) { video in
          ZStack(alignment: .bottomLeading) {
            VideoThumbnailView(video: video)
              .onTapGesture {
                onVideoTapped?(video)
              }
            HStack(alignment: .bottom) {
              HStack(spacing: Constants.innerHStackSpacing) {
                Image(Constants.viewImage)
                  .resizable()
                  .frame(width: 24, height: 24)

                Text("\(video.viewCount)")
                  .font(Constants.viewTextFont)
                  .foregroundColor(Constants.viewTextColor)
              }
              .padding(.leading, Constants.buttonHorizontalPadding)
              Spacer()
              Button {
                onDelete?(video)
              }
              label: {
                ZStack(alignment: .bottomTrailing) {
                  Color.clear
                  Image(Constants.deleteImageName)
                }
                .frame(width: Constants.deleteTappableSize, height: Constants.deleteTappableSize)
              }
              .padding(.trailing, Constants.buttonHorizontalPadding)
            }
            .padding(.bottom, Constants.bottomPadding)
            if video.postID == currentlyDeletingPostInfo?.postID && showDeleteIndictor {
              ZStack(alignment: .center) {
                Color.black.opacity(Constants.loadingOpacity)
                LottieLoaderView(animationName: Constants.loaderName, resetProgess: false)
                  .frame(width: Constants.loaderSize, height: Constants.loaderSize)
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
}

extension ProfileVideosGridView {
  enum Constants {
    static let gridItemSpacing: CGFloat = 16.0
    static let verticalSpacing: CGFloat = 16.0
    static let innerHStackSpacing: CGFloat = 4.0

    static let thumbnailCornerRadius: CGFloat = 8.0
    static let buttonHorizontalPadding: CGFloat = 12.0
    static let bottomPadding: CGFloat = 12.0
    static let deleteTappableSize = 50.0

    static let deleteImageName = "delete_profile"
    static let viewImage = "video_views"
    static let likeImageNameSelected = "like_profile"
    static let likeImageNameUnselected = "dislike_profile"

    static let viewTextFont = YralFont.pt14.medium.swiftUIFont
    static let viewTextColor = YralColor.grey50.swiftUIColor

    static let loaderName = "Yral_Loader"
    static let loaderSize = 24.0
    static let loadingOpacity = 0.8
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
