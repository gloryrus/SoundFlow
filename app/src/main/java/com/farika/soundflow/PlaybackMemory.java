package com.farika.soundflow;
import androidx.media3.exoplayer.DefaultLoadControl;
final class PlaybackMemory {static final int TARGET_BYTES=6*1024*1024,MIN_MS=8000,MAX_MS=30000;static DefaultLoadControl control(){return new DefaultLoadControl.Builder().setBufferDurationsMs(MIN_MS,MAX_MS,1500,3000).setTargetBufferBytes(TARGET_BYTES).setPrioritizeTimeOverSizeThresholds(false).setBackBuffer(0,false).build();}}
