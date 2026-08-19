const TIMESTAMP = /\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]/g;

export function parseLrc(source) {
  const lines = [];
  String(source || '').split(/\r?\n/).forEach((row) => {
    const text = row.replace(TIMESTAMP, '').trim();
    if (!text) return;
    for (const match of row.matchAll(TIMESTAMP)) {
      const fraction = (match[3] || '').padEnd(3, '0').slice(0, 3);
      lines.push({
        timeMs: (Number(match[1]) * 60 + Number(match[2])) * 1000 + Number(fraction || 0),
        text
      });
    }
  });
  return lines.sort((left, right) => left.timeMs - right.timeMs);
}

export function lyricAt(lines, positionMs) {
  let active = null;
  for (const line of lines) {
    if (line.timeMs > positionMs) break;
    active = line;
  }
  return active;
}
