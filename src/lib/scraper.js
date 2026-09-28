// @visuales-scraper-v1
// Parser del listado de directorios Apache 2.4 (autoindex) de Visuales UCLV.
// El HTML tiene una fila por entrada con esta forma:
//   <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td>
//       <td><a href="Nombre/">Nombre/</a></td>
//       <td align="right">2026-08-06 04:17  </td>
//       <td align="right">  - </td>...</tr>

const ROW_RE =
  /<tr>\s*<td valign="top"><img src="\/icons\/([^"]+)" alt="\[([^\]]+)\]"><\/td>\s*<td><a href="([^"]+)">([^<]+)<\/a><\/td>\s*<td align="right">([^<]*?)<\/td>\s*<td align="right">([^<]*?)<\/td>/g;

const ICON_TO_TYPE = {
  'folder.gif': 'dir',
  'back.gif': 'parent',
  'movie.gif': 'video',
  'image2.gif': 'image',
  'text.gif': 'text',
  'compressed.gif': 'archive',
  'sound2.gif': 'audio',
  'unknown.gif': 'file'
};

// Convierte "919M", "7.4K", "161K", "  -  " a bytes (número).
export function parseSize(s) {
  s = (s || '').trim();
  if (!s || s === '-') return 0;
  const m = /^([\d.]+)\s*([KMG]?)/.exec(s);
  if (!m) return 0;
  const n = parseFloat(m[1]);
  const mult = m[2] === 'K' ? 1024 : m[2] === 'M' ? 1048576 : m[2] === 'G' ? 1073741824 : 1;
  return Math.round(n * mult);
}

// Une el path base con un href del listado (absoluto o relativo).
export function joinPath(base, href) {
  if (href.startsWith('/')) return href;
  if (!base.endsWith('/')) base += '/';
  return base + href;
}

// Parsea el HTML y devuelve array de entradas normalizadas.
// basePath: path actual (ej: "/Peliculas/Extranjeras/2026/")
export function parseApacheListing(html, basePath) {
  const entries = [];
  let m;
  ROW_RE.lastIndex = 0;
  while ((m = ROW_RE.exec(html)) !== null) {
    const [, icon, , href, name, modified, sizeRaw] = m;
    const type = ICON_TO_TYPE[icon] || 'file';
    if (type === 'parent') continue;

    const cleanName = name.replace(/\/$/, '').trim();
    const path = joinPath(basePath, href);

    entries.push({
      name: cleanName,
      href,
      path,
      type,
      size: parseSize(sizeRaw),
      sizeRaw: sizeRaw.trim(),
      modified: modified.trim(),
      icon
    });
  }
  return entries;
}
