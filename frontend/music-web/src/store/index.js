import Vue from 'vue';
import Vuex from 'vuex';
import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';
import { api } from '../api.js';
import { clearSession, loadSession } from '../auth/session.js';
import { login as loginWorkflow } from '../auth/workflows.js';
import { createPlayerController } from '../player/controller.js';
import { shouldPersistHistory } from '../player/history-policy.js';
import {
  PLAYBACK_MODES,
  adjacentTrack,
  cyclePlaybackMode,
  normalizePlaybackSelection,
  trackForPlaybackEnd
} from '../player/queue.js';
import { mergeRecordedTrack } from '../player/play-count.js';

Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    auth: {
      namespaced: true,
      state: () => ({ session: loadSession(window.sessionStorage) }),
      mutations: {
        setSession(state, session) { state.session = session; },
        clearSession(state) { state.session = null; }
      },
      actions: {
        async login({ commit }, credentials) {
          const result = await loginWorkflow({ api, storage: window.sessionStorage, credentials });
          commit('setSession', result.session);
          return result;
        },
        async logout({ commit }) {
          try { await api.delete(API_PATHS.identity.logout); } finally {
            clearSession(window.sessionStorage);
            commit('clearSession');
          }
        }
      }
    },
    player: {
      namespaced: true,
      state: () => ({ controller: null, track: null, queue: [], queueIndex: -1, playing: false, currentTime: 0, duration: 0, volume: 1, playbackMode: PLAYBACK_MODES.ORDER, lastWriteAt: 0, playCountError: '' }),
      mutations: {
        initialize(state, audio) { state.controller = createPlayerController({ audio }); state.volume = audio.volume; },
        setSelection(state, selection) {
          state.track = selection.track;
          state.queue = selection.queue;
          state.queueIndex = selection.index;
          state.currentTime = 0;
          state.duration = 0;
        },
        mergeRecordedTrack(state, track) {
          state.track = mergeRecordedTrack(state.queue, state.track, track);
        },
        setPlayCountError(state, message) { state.playCountError = message; },
        setPlaying(state, value) { state.playing = value; },
        setTime(state, value) { state.currentTime = value; },
        setDuration(state, value) { state.duration = value; },
        setVolume(state, value) { state.volume = value; },
        cycleMode(state) { state.playbackMode = cyclePlaybackMode(state.playbackMode); },
        setLastWriteAt(state, value) { state.lastWriteAt = value; }
      },
      actions: {
        async playTrack({ state, commit, dispatch }, payload) {
          const selection = normalizePlaybackSelection(payload, state.queue);
          const track = selection.track;
          if (state.track && state.track.id !== track.id) await dispatch('persistHistory', 'track-change');
          state.controller.load(track);
          commit('setSelection', selection);
          await state.controller.toggle();
          commit('setPlayCountError', '');
          try {
            const recorded = await api.post(API_PATHS.catalog.play(track.id));
            commit('mergeRecordedTrack', mapTrackSummary(recorded.data));
          } catch (_problem) {
            commit('setPlayCountError', '播放次数暂未同步');
          }
        },
        async toggle({ state }) { if (state.track) await state.controller.toggle(); },
        previous({ state, dispatch }) {
          const track = adjacentTrack(state.queue, state.queueIndex, -1);
          if (track) return dispatch('playTrack', { track, queue: state.queue });
          return undefined;
        },
        next({ state, dispatch }) {
          const track = state.playbackMode === PLAYBACK_MODES.SHUFFLE
            ? trackForPlaybackEnd(state.queue, state.queueIndex, state.playbackMode)
            : adjacentTrack(state.queue, state.queueIndex, 1);
          if (track) return dispatch('playTrack', { track, queue: state.queue });
          return undefined;
        },
        cycleMode({ commit }) { commit('cycleMode'); },
        async ended({ state, dispatch }) {
          if (!state.track) return;
          if (state.playbackMode === PLAYBACK_MODES.REPEAT_ONE) {
            await state.controller.restart();
            return;
          }
          const track = trackForPlaybackEnd(state.queue, state.queueIndex, state.playbackMode);
          if (track) await dispatch('playTrack', { track, queue: state.queue });
        },
        seek({ state, commit, dispatch }, seconds) { commit('setTime', state.controller.seek(seconds)); dispatch('persistHistory', 'seeked'); },
        setVolume({ state, commit }, value) { commit('setVolume', state.controller.setVolume(value)); },
        async persistHistory({ state, commit }, event) {
          if (!state.track) return;
          const now = Date.now();
          if (!shouldPersistHistory({ event, now, lastWriteAt: state.lastWriteAt })) return;
          await api.put(`${API_PATHS.interaction.history}/${state.track.id}`, { positionMs: Math.round(state.currentTime * 1000) });
          commit('setLastWriteAt', now);
        }
      }
    }
  }
});
