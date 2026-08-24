import { trackMediaSource } from './media.js';

export function createPlayerController({ audio }) {
  if (!audio) throw new TypeError('An audio element is required');
  let currentTrack = null;
  return {
    audio,
    get track() { return currentTrack; },
    load(track) {
      if (!audio.paused) audio.pause();
      currentTrack = track;
      audio.src = trackMediaSource(track);
      audio.currentTime = 0;
      return track;
    },
    async toggle() {
      if (audio.paused) await audio.play();
      else audio.pause();
      return !audio.paused;
    },
    async restart() {
      audio.currentTime = 0;
      await audio.play();
      return !audio.paused;
    },
    seek(seconds) {
      const duration = Number.isFinite(audio.duration) ? audio.duration : seconds;
      audio.currentTime = Math.max(0, Math.min(seconds, duration));
      return audio.currentTime;
    },
    setVolume(value) {
      audio.volume = Math.max(0, Math.min(Number(value), 1));
      return audio.volume;
    }
  };
}
