// Minecraft's bitmap GUI font (textures/font/ascii.png: 16 x 16 glyphs, ASCII at their code points), for
// titles in the mockups. Advance as the game computes it: the glyph's rightmost opaque column + 1, plus 1
// spacing (in 8 px units); space advances 4.

/** Reads glyph widths from the sheet. */
function mcFont(img) {
  const c = document.createElement('canvas');
  c.width = img.width; c.height = img.height;
  const g = c.getContext('2d');
  g.drawImage(img, 0, 0);
  const cell = img.width / 16, d = g.getImageData(0, 0, c.width, c.height).data, adv = [];
  for (let i = 0; i < 256; i++) {
    const cx = (i % 16) * cell, cy = Math.floor(i / 16) * cell;
    let w = 0;
    for (let x = cell - 1; x >= 0 && !w; x--) {
      for (let y = 0; y < cell; y++) if (d[((cy + y) * c.width + cx + x) * 4 + 3]) { w = x + 1; break; }
    }
    adv[i] = i === 32 ? 4 : w * 8 / cell + 1;
  }
  return { img, cell, adv, tinted: {} };
}

/** Draws text at logical (x, y) in colour, at scale screen px per logical px; with shadow, first the game's
 *  shadow: the colour's components divided by 4, one px down and right. */
function mcText(g, font, text, x, y, scale, color, shadow = false) {
  if (shadow) {
    const n = parseInt(color.slice(1), 16), sh = '#' + (((n & 0xFCFCFC) >> 2) >>> 0).toString(16).padStart(6, '0');
    mcText(g, font, text, x + 1, y + 1, scale, sh);
  }
  if (!font.tinted[color]) {
    const t = document.createElement('canvas');
    t.width = font.img.width; t.height = font.img.height;
    const tg = t.getContext('2d');
    tg.drawImage(font.img, 0, 0);
    tg.globalCompositeOperation = 'source-in'; tg.fillStyle = color; tg.fillRect(0, 0, t.width, t.height);
    font.tinted[color] = t;
  }
  const sheet = font.tinted[color], k = font.cell;
  g.imageSmoothingEnabled = false;
  for (const ch of text) {
    const i = ch.charCodeAt(0) & 255;
    g.drawImage(sheet, (i % 16) * k, Math.floor(i / 16) * k, k, k, x * scale, y * scale, 8 * scale, 8 * scale);
    x += font.adv[i];
  }
}

/** Text width in logical px (the game's Font.width): the sum of advances. */
function mcWidth(font, text) { let w = 0; for (const ch of text) w += font.adv[ch.charCodeAt(0) & 255]; return w; }
