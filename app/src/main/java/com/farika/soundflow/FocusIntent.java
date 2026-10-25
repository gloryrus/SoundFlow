package com.farika.soundflow;
/** User intent is independent of the player's temporary audio-focus pause. */
final class FocusIntent {
 boolean wanted, suspended;
 void play(){wanted=true;}
 void pause(){wanted=false;suspended=false;}
 void transientLoss(){if(wanted)suspended=true;}
 boolean gain(){boolean resume=wanted&&suspended;suspended=false;return resume;}
 boolean canPlay(){return wanted&&!suspended;}
}
