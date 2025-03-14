//
//  CaptionsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct HashtagItem: Identifiable, Equatable {
  let id = UUID()
  var text: String
}

struct HashtagView: View {
  @Binding var hashtags: [HashtagItem]
  @State private var newHashtag: String = ""
  @State private var editingID: HashtagItem.ID?
  @FocusState private var newFieldIsFocused: Bool
  var onFocus: (() -> Void)?

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      Text(Constants.addHashtagText)
        .foregroundColor(Constants.addHashtagColor)
        .font(Constants.addHashtagFont)

      ScrollView(.horizontal, showsIndicators: false) {
        HStack(spacing: Constants.chipSpacing) {

          ForEach(hashtags) { item in
            if editingID == item.id {
              EditableChip(initialText: item.text) {
                editingID = nil
              }
              onCommit: { finalText in
                commitEdit(for: item.id, newText: finalText)
              }
              onFocus: {
                onFocus?()
              }
            } else {
              HashtagChip(tag: item.text)
                .onTapGesture {
                  editingID = item.id
                }
            }
          }
          TextField("", text: $newHashtag)
            .placeholder(when: newHashtag.isEmpty) {
              Text(Constants.placeholderText)
                .foregroundColor(Constants.placeholderColor)
                .font(Constants.enterFont)
            }
            .foregroundColor(Constants.enterColor)
            .font(Constants.enterFont)
            .autocapitalization(.none)
            .disableAutocorrection(true)
            .submitLabel(.done)
            .focused($newFieldIsFocused)
            .onChange(of: newFieldIsFocused) { isFocused in
              if isFocused {
                DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
                  onFocus?()
                }
              } else {
                addNewHashtag()
              }
            }
            .onSubmit {
              addNewHashtag()
              newFieldIsFocused = false
            }
            .onChange(of: newHashtag) { newValue in
              if newValue.last == " " {
                addNewHashtag()
              }
            }
            .tint(Constants.tintColor)
        }
        .padding(Constants.hStackPadding)
      }
      .background(Constants.hStackBGColor)
      .cornerRadius(Constants.hStackRadius)
      .overlay(
        RoundedRectangle(cornerRadius: Constants.hStackRadius)
          .stroke(
            newFieldIsFocused ? Constants.strokeColorSelected : Constants.strokeColorUnselected,
            lineWidth: .one
          )
      )
    }
  }

  private func commitEdit(for id: HashtagItem.ID, newText: String) {
    editingID = nil

    var trimmed = newText.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.hasPrefix("#") {
      trimmed.removeFirst()
    }

    guard let idx = hashtags.firstIndex(where: { $0.id == id }) else { return }

    if trimmed.isEmpty {
      hashtags.remove(at: idx)
    } else {
      hashtags[idx].text = trimmed
    }
  }

  private func addNewHashtag() {
    var trimmed = newHashtag.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.hasPrefix("#") {
      trimmed.removeFirst()
    }
    guard !trimmed.isEmpty else { return }

    hashtags.append(HashtagItem(text: trimmed))
    newHashtag = ""
  }
}

struct EditableChip: View {
  @State private var localText: String
  let onCancel: () -> Void
  let onCommit: (String) -> Void
  var onFocus: (() -> Void)?

  @FocusState private var isFocused: Bool
  typealias Constants = HashtagView.Constants

  init(initialText: String,
       onCancel: @escaping () -> Void,
       onCommit: @escaping (String) -> Void,
       onFocus: @escaping () -> Void) {
    _localText = State(initialValue: initialText)
    self.onCancel = onCancel
    self.onCommit = onCommit
    self.onFocus = onFocus
  }

  var body: some View {
    TextField("", text: $localText)
      .frame(height: Constants.hashtagChipHeight)
      .font(Constants.hashtagChipFont)
      .foregroundColor(Constants.hashtagChipTextColor)
      .padding(Constants.hashtagChipPadding)
      .background(Constants.hashtagChipBackgroundColor)
      .cornerRadius(Constants.hashtagChipCornerRadius)
      .focused($isFocused)
      .submitLabel(.done)
      .onAppear {
        isFocused = true
      }
      .onSubmit {
        commitOrRemove()
      }
      .onChange(of: isFocused) { focused in
        if focused {
          DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
            onFocus?()
          }
        }
        if !focused {
          commitOrRemove()
        }
      }
      .onChange(of: localText) { newValue in
        if newValue.last == " " {
          commitOrRemove()
        }
      }
  }

  private func commitOrRemove() {
    if localText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
      onCommit("")
    } else {
      onCommit(localText)
    }
  }
}

struct HashtagChip: View {
  let tag: String
  typealias Constants = HashtagView.Constants

  var body: some View {
    Text("#\(tag)")
      .font(Constants.hashtagChipFont)
      .foregroundColor(Constants.hashtagChipTextColor)
      .frame(height: Constants.hashtagChipHeight)
      .padding(Constants.hashtagChipPadding)
      .background(Constants.hashtagChipBackgroundColor)
      .cornerRadius(Constants.hashtagChipCornerRadius)
  }
}

extension HashtagView {
  enum Constants {
    static let vStackSpacing = 8.0
    static let addHashtagText = "Add Hashtag"
    static let addHashtagColor = Color(red: 0.83, green: 0.83, blue: 0.83)
    static let addHashtagFont = Font.custom("Kumbh Sans", size: 14).weight(.medium)
    static let enterColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let enterFont = Font.custom("Kumbh Sans", size: 14)
    static let placeholderText = "Hit enter to add #hashtags"
    static let placeholderColor = Color(red: 0.32, green: 0.32, blue: 0.32)
    static let chipSpacing = 6.0
    static let hStackPadding = 12.0
    static let hStackBGColor = Color(red: 0.09, green: 0.09, blue: 0.09)
    static let hStackRadius = 8.0
    static let strokeColorSelected = Color(red: 0.64, green: 0.64, blue: 0.64)
    static let strokeColorUnselected = Color(red: 0.13, green: 0.13, blue: 0.13)

    static let hashtagChipTextColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let hashtagChipFont = Font.custom("Kumbh Sans", size: 12)
    static let hashtagChipPadding: CGFloat = 5
    static let hashtagChipBackgroundColor = Color(red: 0.25, green: 0.25, blue: 0.25)
    static let hashtagChipCornerRadius: CGFloat = 8.0
    static let hashtagChipHeight = 10.0
    static let tintColor: Color =  Color(red: 0.89, green: 0, blue: 0.48)
  }
}

// MARK: - Preview
struct HashtagView_Previews: PreviewProvider {
  struct Wrapper: View {
    @State var tags = [
      HashtagItem(text: "SwiftUI"),
      HashtagItem(text: "iOS"),
      HashtagItem(text: "Kotlin")
    ]

    var body: some View {
      VStack {
        HashtagView(hashtags: $tags)
          .padding()
          .preferredColorScheme(.dark)

        Text("Current Tags: \(tags.map { $0.text }.joined(separator: ", "))")
          .foregroundColor(.white)
          .padding()
      }
      .background(Color.black.ignoresSafeArea())
    }
  }

  static var previews: some View {
    Wrapper()
  }
}
