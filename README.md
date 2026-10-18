# OSSFlow

Open-source voice dictation for Android.

**Speak naturally. Own your data.**

OSSFlow turns what you say into clean text in any app on your phone. You bring your own AI key. There is no account, and nothing is stored on OSSFlow's servers.

- No account
- Your keys
- Nothing stored on our side
- Open source

You said: um hey so can you like grab milk on the way? actually oat milk, the blue one

Inserted: Can you grab the blue oat milk on the way?

[ossflow.xyz](https://ossflow.xyz)

History stays on the phone. Audio is sent only to the transcription host you set, then deleted.

## Install

Android 14 or newer.

Download and open [OSSFlow.apk](https://ossflow.xyz/OSSFlow.apk). If the phone asks, allow your browser or Files to install unknown apps, then install OSSFlow.

## Set up the phone

Sideloaded apps cannot turn on Display over other apps or Accessibility until restricted settings are allowed. Do that before the bubble will work.

1. Open OSSFlow. Allow the microphone and notifications when asked.
2. Open **Settings → Apps → OSSFlow**.
3. Tap the **⋮** menu and choose **Allow restricted settings**. Confirm with your screen lock. If the menu is missing, try to allow Display over other apps once. Android shows the menu after that denial.
4. Back in OSSFlow, tap **Allow** on **Display over other apps**. This is the icon overlay. Leave it on.
5. Tap **Allow** on **Accessibility**. In the system screen, turn OSSFlow on and confirm. Accessibility types into the field you are using. It does not read other screens.
6. Return to OSSFlow. Each row should say **Allowed**. The bubble switch stays on.

If Display over other apps stays off after restricted settings are allowed, Accessibility alone can still show the bubble. On Android 17, Advanced Protection can block Accessibility for apps that are not a verified tool. Dictation still copies the text into History.

## Add a Grok key (Or any OpenAI Compatible Key) 

The app is already set to Grok. You only add a key.

1. Create an API key at [console.x.ai](https://console.x.ai/).
2. In OSSFlow, open **Settings**.
3. Under **Transcription**, leave **Grok** selected.
4. Leave **Base URL** as `https://api.x.ai/v1`.
5. Paste the key into **API key**.
6. Leave **Model** as `grok-voice-transcribe-2.0` and **Language** as `en`.
7. Under **Cleanup**, leave **Clean up the transcript** on, and leave **Use the same API key and base URL** on. The cleanup model stays `grok-4.20-0309-non-reasoning`.
8. Tap **Save**.

Cleanup removes fillers and false starts. If you want a dialect, type it under **Custom Cleanup instructions**, for example `British English`, then save again.

## Dictate

1. Open any app and focus a text field so the keyboard is up.
2. Tap the bubble. It turns red while it is listening.
3. Speak, then tap the bubble again.
4. The text is inserted into the field. If it does not land, it is on the clipboard and in History. Tap a History row to copy it.

Drag the idle bubble to move it. Drop it on **Snooze** at the bottom of the screen to hide it. **End snooze now** brings it back.

## License

Copyright (C) 2026 ossflow.xyz

OSSFlow is free software. You can redistribute it and modify it under the terms of the GNU General Public License, version 3, as published by the Free Software Foundation. This version only applies. Later versions of the license do not.

This program is distributed without any warranty, including the implied warranties of merchantability and fitness for a particular purpose. See the [GNU General Public License](LICENSE) for details.

A person who distributes a modified version must follow that license, including making the corresponding source available.
