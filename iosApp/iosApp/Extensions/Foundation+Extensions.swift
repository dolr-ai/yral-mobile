//
//  Foundation+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 07/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension Collection {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}
