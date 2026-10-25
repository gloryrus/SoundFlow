"""Generate only synthetic test audio; no licensed music is included."""
import math, struct, wave
from pathlib import Path
p = Path("assets/test.wav")
p.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(p), "wb") as w:
    w.setparams((1, 2, 16000, 0, "NONE", "not compressed"))
    w.writeframes(b"".join(struct.pack("<h", int(1600 * math.sin(2 * math.pi * 220 * i / 16000))) for i in range(16000 * 30)))
