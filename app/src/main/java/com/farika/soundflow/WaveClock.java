package com.farika.soundflow;
/** All wave views sample the same monotonic clock, regardless of attach/draw order. */
final class WaveClock {static float bar(long millis,int k){double phase=(millis%600000L)/262.5;return .22f+.52f*(.5f+.5f*(float)Math.sin(phase+k*.9));}}
