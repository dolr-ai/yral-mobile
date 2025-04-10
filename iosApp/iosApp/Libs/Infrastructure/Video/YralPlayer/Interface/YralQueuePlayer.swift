//
//  YralQueuePlayer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 10/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation

protocol YralQueuePlayer {
    var isMuted: Bool { get set }
    var currentItem: AVPlayerItem? { get }

    func play()
    func pause()
    func removeAllItems()
    func currentTime() -> CMTime
    // swiftlint:disable:next line_length
    func seek(to time: CMTime, toleranceBefore: CMTime, toleranceAfter: CMTime, completionHandler: @escaping @Sendable (Bool) -> Void)
}

extension AVQueuePlayer: YralQueuePlayer {}
