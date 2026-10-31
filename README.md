# OSSFlow

OSSFlow is a free, open-source voice dictation app for Android 14 and newer. A floating bubble listens while you speak, then cleaned text is pasted into the app you are using. You bring your own API key for Grok speech-to-text or any OpenAI-compatible transcription API. There is no account, and OSSFlow has no server of its own.

**Website:** [ossflow.xyz](https://ossflow.xyz)

**Setup:** [ossflow.xyz/setup](https://ossflow.xyz/setup)

Speak naturally. Own your data.

You said: um hey so can you like grab milk on the way? actually oat milk, the blue one

Inserted: Can you grab the blue oat milk on the way?

| | |
| --- | --- |
| Platform | Android 14 and newer (minimum SDK 34) |
| Current version | 1.5.3 (version code 20) |
| Package | `app.ossflow` |
| Price | Free. [GPL-3.0 only](LICENSE) |
| Account | None |
| Transcription | Grok speech-to-text, or any OpenAI-compatible API |
| Cleanup | Optional. OpenAI-compatible chat completions |

## Download

Download and open [OSSFlow.apk](https://github.com/w6liam/OSSFlow/raw/main/OSSFlow.apk) (version 1.5.3). If the phone asks, allow your browser or Files to install unknown apps, then install OSSFlow.

The file in this repository is the signed release build.

## Features

- Floating bubble. Tap to talk, tap again to finish. The bubble turns red while it is listening.
- Cleaned text is pasted into the text field you are using, and copied to the clipboard.
- History stays on the phone. Tap a row to copy it, swipe a row to delete it, or delete all transcripts after a confirmation.
- Optional cleanup: fillers, false starts, obvious repairs, punctuation, and a writing style (normal, formal, or informal).
- Personal dictionary for names and terms you want spelled correctly (up to 100 entries).
- Minutes saved, from a typing pace of 40 words per minute minus the time you spent speaking.
- Your API key. Grok speech-to-text, or any host that implements the OpenAI-compatible transcription API.
- No account. Android backup of the app is turned off.

## How it works

1. Focus a text field so the keyboard is up, then tap the bubble.
2. OSSFlow records on the phone: mono AAC, in an `.m4a` file in the app cache.
3. The recording is sent over HTTPS to the transcription host in Settings. OSSFlow deletes that cache file after the dictation attempt finishes, including when transcription or cleanup fails. Tap the × on the bubble to discard a recording before it is sent.
4. If cleanup is on, the transcript goes to an OpenAI-compatible chat completions endpoint. The model is instructed to return only the text you meant to type. If cleanup fails, the raw transcript is used.
5. Accessibility pastes the result into the focused field. The same text is placed on the clipboard and saved in History. If the paste does not land, the bubble says it was copied.

Drag the idle bubble to move it. Drop it on **Snooze** at the bottom of the screen to hide it. **End snooze now** brings it back.

When Accessibility is on, the idle bubble shows while the keyboard is open on a normal text field, and it hides on a password field. OSSFlow does not paste into password fields. A quiet notification stays up while the bubble is running.

If Display over other apps stays off after restricted settings are allowed, Accessibility alone can still show the bubble. On Android 17, Advanced Protection can block Accessibility for apps that are not a verified tool. Dictation still copies the text into History.

## Supported providers

Settings has two transcription modes.

**Grok** is the default. The base URL is `https://api.x.ai/v1`, the model is `grok-voice-transcribe-2.0`, and the language is `en`. Audio is posted to that host's `/stt` path. Create a key at [console.x.ai](https://console.x.ai/).

**OpenAI-compatible** is the mode labelled OpenAI in the app. Audio is posted to `{base URL}/audio/transcriptions`, the OpenAI audio transcriptions API. Set the base URL, API key, and model for any host that implements that API. If the model field is blank, the request uses `whisper-1`.

An OpenRouter address is rewritten to `https://openrouter.ai/api/v1` and sent on the OpenAI-compatible path, because that host has no `/stt` route.

Cleanup is optional and separate. It calls `{base URL}/chat/completions`. The default model is `grok-4.20-0309-non-reasoning`, using the same API key and base URL as transcription. You can turn cleanup off, or set another base URL, key, and model. When cleanup uses its own key, **Test cleanup** checks that host's `/models` endpoint.

**Custom Cleanup instructions** can name a dialect, for example `British English`. Those instructions override the writing style where they conflict. Writing style is normal, formal, or informal.

A personal dictionary holds up to 100 words, 50 characters each. When you add a word and a cleanup key is saved, OSSFlow asks the cleanup model for likely mishearings. Grok transcription receives the spellings as key terms. An OpenAI-compatible host receives them in a short vocabulary prompt. Cleanup is given the spelling and any heard-as phrases, and it is told to use a dictionary word only when the speech sounds like that word.

API URLs have to be `https://`.

## Privacy

Speech and transcripts are not stored on a server run by OSSFlow. The recording is sent only to the transcription host in Settings. OSSFlow deletes the cache file after the attempt finishes. Cleanup text goes only to the cleanup host you set.

History keeps the cleaned text and the raw transcript in a database on the phone. You can delete one transcript, or delete all transcripts after you confirm. Resetting the minutes-saved counter leaves History in place.

API keys stay in the app's private storage on the phone. OSSFlow uses Android encrypted preferences, and falls back to ordinary private preferences if that encryption cannot be set up.

Android backup of the app is turned off, and the app refuses cleartext HTTP.

The Accessibility service finds the focused text field and pastes into it (or splices the text in if paste is refused). The service description says it does not read other screens or collect what you type. Password fields are skipped.

## Set up the phone

Sideloaded apps cannot turn on Display over other apps or Accessibility until restricted settings are allowed. Do that before the bubble will work. The same steps are on [ossflow.xyz/setup](https://ossflow.xyz/setup).

1. Open OSSFlow. Allow the microphone and notifications when asked.
2. Open **Settings → Apps → OSSFlow**.
3. Tap the **⋮** menu and choose **Allow restricted settings**. Confirm with your screen lock. If the menu is missing, try to allow Display over other apps once. Android shows the menu after that denial.
4. Back in OSSFlow, tap **Allow** on **Display over other apps**. This is the icon overlay. Leave it on.
5. Tap **Allow** on **Accessibility**. In the system screen, turn OSSFlow on and confirm.
6. Return to OSSFlow. Each row should say **Allowed**. The bubble switch stays on.

## Add an API key

The app is already set to Grok. You only add a key.

1. Create an API key at [console.x.ai](https://console.x.ai/).
2. In OSSFlow, open **Settings**.
3. Under **Transcription**, leave **Grok** selected.
4. Leave **Base URL** as `https://api.x.ai/v1`.
5. Paste the key into **API key**.
6. Leave **Model** as `grok-voice-transcribe-2.0` and **Language** as `en`.
7. Under **Cleanup**, leave **Clean up the transcript** on, and leave **Use the same API key and base URL** on. The cleanup model stays `grok-4.20-0309-non-reasoning`.
8. Tap **Save**.

For another host, choose **OpenAI** under Transcription, set that host's base URL, API key, and model, then save.

## Dictate

1. Open any app and focus a text field so the keyboard is up.
2. Tap the bubble. It turns red while it is listening.
3. Speak, then tap the bubble again.
4. The text is inserted into the field. If it does not land, it is on the clipboard and in History. Tap a History row to copy it.

## FAQ

### Does OSSFlow work offline?

No. The phone records the audio, and transcription needs a network connection to the HTTPS API you configure. OSSFlow has no on-device speech model. History you have already saved can be opened and copied with no network connection.

### How much does OSSFlow cost?

The app is free, under the GNU General Public License, version 3 only. OSSFlow has no account and no subscription. Any charge for transcription or cleanup is set by the API provider you choose.

### Which providers does OSSFlow support?

Grok speech-to-text, and any OpenAI-compatible transcription API (`/audio/transcriptions`). Cleanup uses an OpenAI-compatible chat completions API. You choose the base URL, model, and API key. Details are in [Supported providers](#supported-providers).

### Which Android versions does OSSFlow support?

Android 14 and newer (minimum SDK 34). The current release targets SDK 37. On Android 17, Advanced Protection can block Accessibility for apps that are not a verified tool. When that happens, the dictated text is still copied into History.

## Open-source alternative to Wispr Flow and Granola Voice Typing for Android

OSSFlow is a free, open-source alternative to Wispr Flow and Granola Voice Typing for Android. You speak into a floating bubble, and cleaned text is pasted into the app you already have open. Transcription uses an API key you create for Grok speech-to-text or any OpenAI-compatible host. The licence is GPL-3.0 only.

## Build from source

OSSFlow is written in Kotlin with Jetpack Compose. A debug build does not need a signing key:

```
./gradlew :app:assembleDebug
```

Unit tests:

```
./gradlew :app:testDebugUnitTest
```

A release build needs `keystore.properties` in the project root. The signed `OSSFlow.apk` in this repository is the published release.

## Licence

Copyright (C) 2026 ossflow.xyz

OSSFlow is free software. You can redistribute it and modify it under the terms of the GNU General Public License, version 3, as published by the Free Software Foundation. This version only applies. Later versions of the licence do not.

This program is distributed without any warranty, including the implied warranties of merchantability and fitness for a particular purpose. See the [GNU General Public License](LICENSE) for details.

A person who distributes a modified version must follow that licence, including making the corresponding source available.
