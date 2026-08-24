export function resolveTrackByName(tracks, value) {
  const name = String(value || '').trim();
  if (!name) throw new Error('请输入歌曲名称。');

  const matches = tracks.filter((track) => String(track.title || '').trim().toLocaleLowerCase() === name.toLocaleLowerCase());
  if (!matches.length) throw new Error(`未找到名为“${name}”的歌曲。`);
  if (matches.length > 1) throw new Error(`找到多首名为“${name}”的同名歌曲，请换用更具体的名称。`);
  return matches[0];
}
