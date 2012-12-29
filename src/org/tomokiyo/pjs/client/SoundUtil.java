package org.tomokiyo.pjs.client;

import com.allen_sauer.gwt.voices.client.Sound;
import com.allen_sauer.gwt.voices.client.SoundController;

public class SoundUtil {

  static private final SoundController soundController = new SoundController(); 
  static private final Sound soundOK = soundController.createSound(
    Sound.MIME_TYPE_AUDIO_MPEG,
    Resources.INSTANCE.soundOK().getUrl());
  
  static private final Sound soundNG = soundController.createSound(
    Sound.MIME_TYPE_AUDIO_MPEG,
    Resources.INSTANCE.soundNG().getUrl());

  // private constructor to prevent from being instantiated.
  private SoundUtil() {}

  static public void beepOK() {
    soundOK.play();
  }

  static public void beepNG() {
    soundNG.play();
  }
}
