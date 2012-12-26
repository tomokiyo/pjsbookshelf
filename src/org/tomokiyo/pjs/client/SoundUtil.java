package org.tomokiyo.pjs.client;

import com.google.gwt.core.client.GWT;
import com.allen_sauer.gwt.voices.client.Sound;
import com.allen_sauer.gwt.voices.client.SoundController;

public class SoundUtil {

  static private final String BEEP_NG_AUDIO_URL = GWT.getModuleBaseURL() + "audio/doh.mp3";

  static private final String BEEP_OK_AUDIO_URL = GWT.getModuleBaseURL() + "audio/notify.mp3";

  // private constructor to prevent from being instantiated.
  private SoundUtil() {}

  // If you want to eagerly initialize sound rather than lazily.
  static public void preload() {
    Sound sound = LazyOKSoundHolder.sound;
    sound = LazyNGSoundHolder.sound;
  }

  static public void beepOK() {
    LazyOKSoundHolder.sound.play();
  }

  static public void beepNG() {
    LazyNGSoundHolder.sound.play();
  }

  // Lazy initializer holder for beepOK sound.
  static private final class LazyOKSoundHolder {
    static private final Sound sound =
        LazySoundControllerHolder.soundController.createSound(
          Sound.MIME_TYPE_AUDIO_MPEG,
          BEEP_OK_AUDIO_URL);
  }

  // Lazy initializer holder for beepNG sound.
  static private final class LazyNGSoundHolder {
    static private final Sound sound =
        LazySoundControllerHolder.soundController.createSound(
          Sound.MIME_TYPE_AUDIO_MPEG,
          BEEP_NG_AUDIO_URL);
  }

  // Initialize static variable lazily and safely without DCL idiom.
  static private final class LazySoundControllerHolder {
    static private final SoundController soundController = new SoundController();
  }
}
