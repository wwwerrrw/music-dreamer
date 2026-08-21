import Vue from 'vue';
import Vuex from 'vuex';
import { API_PATHS } from '@music-dreamer/shared';
import { api } from '../api.js';
import { clearSession, loadSession } from '../auth/session.js';
import { login as loginWorkflow } from '../auth/workflows.js';
import { createPlayerController } from '../player/controller.js';
import { shouldPersistHistory } from '../player/history-policy.js';
import { createPlaybackQueue, movePlaybackQueue } from '../player/queue.js';

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
      state: () => ({ controller: null, track: null, queue: [], queueIndex: -1, playing: false, currentTime: 0, duration: 0, volume: 1, lastWriteAt: 0 }),
      mutations: {
        initialize(state, audio) { state.controller = createPlayerController({ audio }); state.volume = audio.volume; },
        setTrack(state, track) { state.track = track; state.currentTime = 0; state.duration = 0; },
        setQueue(state, queue) { state.queue = queue.tracks; state.queueIndex = queue.index; },
        setQueueIndex(state, index) { state.queueIndex = index; },
        setPlaying(state, value) { state.playing = value; },
        setTime(state, value) { state.currentTime = value; },
        setDuration(state, value) { state.duration = value; },
        setVolume(state, value) { state.volume = value; },
        setLastWriteAt(state, value) { state.lastWriteAt = value; }
      },
      actions: {
        async playTrack({ state, commit, dispatch }, track) {
          if (state.track && state.track.id !== track.id) await dispatch('persistHistory', 'track-change');
          const queueIndex = state.queue.findIndex((item) => item.id === track.id);
          if (queueIndex < 0) commit('setQueue', createPlaybackQueue([track], track.id));
          else commit('setQueueIndex', queueIndex);
          state.controller.load(track);
          commit('setTrack', track);
          await state.controller.toggle();
        },
        async playQueue({ commit, dispatch }, { tracks, startTrackId }) {
          const queue = createPlaybackQueue(tracks, startTrackId);
          if (queue.index < 0) return false;
          commit('setQueue', queue);
          await dispatch('playTrack', queue.tracks[queue.index]);
          return true;
        },
        async moveQueue({ state, dispatch }, direction) {
          const next = movePlaybackQueue({ tracks: state.queue, index: state.queueIndex }, direction);
          if (!next) return false;
          await dispatch('playTrack', next.track);
          return true;
        },
        previous({ dispatch }) { return dispatch('moveQueue', -1); },
        next({ dispatch }) { return dispatch('moveQueue', 1); },
        async toggle({ state }) { if (state.track) await state.controller.toggle(); },
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
