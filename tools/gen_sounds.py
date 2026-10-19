"""Generates the answer feedback sounds (original, synthesized) into app/src/main/res/raw/.

Usage (from the repo root): python tools/gen_sounds.py app/src/main/res/raw
"""
import math
import struct
import sys
import wave
from pathlib import Path

SAMPLE_RATE = 44100


def note(frequency, duration, harmonics, decay, attack=0.005, volume=1.0):
    """A plucked/bell-like note: a few harmonics with a short attack and an exponential decay."""
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        envelope = min(1.0, t / attack) * math.exp(-t * decay)
        value = sum(weight * math.sin(2 * math.pi * frequency * rank * t) for rank, weight in harmonics)
        samples.append(volume * envelope * value)
    return samples


def mix(length, *parts):
    """parts: (start_seconds, samples)."""
    out = [0.0] * int(SAMPLE_RATE * length)
    for start, samples in parts:
        offset = int(SAMPLE_RATE * start)
        for i, value in enumerate(samples):
            if offset + i < len(out):
                out[offset + i] += value
    return out


def write(path, samples, peak=0.7):
    highest = max(abs(s) for s in samples) or 1.0
    # Short fade-out so the file never ends on a click.
    fade = int(SAMPLE_RATE * 0.02)
    for i in range(fade):
        samples[-1 - i] *= i / fade
    frames = b"".join(struct.pack("<h", int(s / highest * peak * 32767)) for s in samples)
    with wave.open(str(path), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(SAMPLE_RATE)
        file.writeframes(frames)
    print(path, f"{len(samples) / SAMPLE_RATE:.2f}s")


BELL = [(1, 1.0), (2, 0.35), (3, 0.12), (4.2, 0.05)]
SOFT = [(1, 1.0), (3, 0.11), (5, 0.04)]

out_dir = Path(sys.argv[1])
out_dir.mkdir(parents=True, exist_ok=True)

# Correct: two bright ascending notes (E6 → A6), the second one ringing a little longer.
write(
    out_dir / "answer_correct.wav",
    mix(0.55, (0.0, note(1318.51, 0.16, BELL, decay=18)), (0.085, note(1760.0, 0.45, BELL, decay=9))),
)

# Wrong: two soft, low, descending notes (E4 → C4), short and not punishing.
write(
    out_dir / "answer_wrong.wav",
    mix(0.45, (0.0, note(329.63, 0.18, SOFT, decay=14, attack=0.01)), (0.12, note(261.63, 0.33, SOFT, decay=10, attack=0.01))),
    peak=0.6,
)
