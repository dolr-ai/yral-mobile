//
//  AudioPlayer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 14/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import AudioToolbox

final class AudioPlayer {
    static let shared = AudioPlayer()

    private var soundID: SystemSoundID = 0
    private init() {}

    func play(named name: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: "caf") else {
            print("Samarth: ❗️Sound file not found")
            return
        }

        if soundID != 0 {
            AudioServicesDisposeSystemSoundID(soundID)
            soundID = 0
        }

        let status = AudioServicesCreateSystemSoundID(url as CFURL, &soundID)
        guard status == kAudioServicesNoError else {
            print("Samarth: ❗️Could not play sound: OSStatus \(status)")
            return
        }

        AudioServicesPlaySystemSound(soundID)
    }
}
